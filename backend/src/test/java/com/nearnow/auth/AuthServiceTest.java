package com.nearnow.auth;

import com.nearnow.common.exception.DuplicateResourceException;
import com.nearnow.common.exception.InvalidCredentialsException;
import com.nearnow.common.exception.ResourceNotFoundException;
import com.nearnow.common.security.JwtUtil;
import com.nearnow.notification.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService. Every collaborator (repository, password
 * encoder, JWT util, notification service) is mocked — these tests
 * verify AuthService's own branching logic in isolation, not the real
 * database or a real JWT signature. The Testcontainers integration test
 * covers the "does this actually work against a real Postgres" question.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private NotificationService notificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtUtil, notificationService);
    }

    // --- helpers -----------------------------------------------------

    /** User's id is DB-generated (@GeneratedValue), so tests that need a
     *  saved User with a known id set it via reflection rather than
     *  pretending there's a settable id field that doesn't exist. */
    private User userWithId(Long id, String email, String passwordHash, String fullName, String role) throws Exception {
        User user = new User(email, passwordHash, fullName, "");
        user.setRole(role);
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    // --- register ------------------------------------------------------

    @Test
    void register_savesHashedPasswordAndReturnsTokens() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("new@nearnow.com");
        request.setPassword("plainPassword123");
        request.setFullName("New User");

        when(userRepository.existsByEmail("new@nearnow.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword123")).thenReturn("hashedPassword");
        User saved = userWithId(1L, "new@nearnow.com", "hashedPassword", "New User", "user");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtUtil.generateToken(1L, "new@nearnow.com", "user", 0L)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(1L, "new@nearnow.com", 0L)).thenReturn("refresh-token");

        AuthResponseDTO response = authService.register(request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("new@nearnow.com");
        assertThat(response.getRole()).isEqualTo("user");

        // The password that hits the database must be the hash, never
        // the plaintext the client sent — this is the whole point of
        // passwordHash existing as a separate field from password.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashedPassword");
    }

    @Test
    void register_duplicateEmail_throwsAndNeverSaves() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("existing@nearnow.com");
        request.setPassword("plainPassword123");
        request.setFullName("Someone");

        when(userRepository.existsByEmail("existing@nearnow.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }

    // --- login -----------------------------------------------------

    @Test
    void login_correctCredentials_returnsTokens() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@nearnow.com");
        request.setPassword("correctPassword");

        User user = userWithId(2L, "user@nearnow.com", "hashedPassword", "User Name", "user");
        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correctPassword", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(2L, "user@nearnow.com", "user", 0L)).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(2L, "user@nearnow.com", 0L)).thenReturn("refresh-token");

        AuthResponseDTO response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("access-token");
        assertThat(response.getId()).isEqualTo(2L);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@nearnow.com");
        request.setPassword("wrongPassword");

        User user = userWithId(2L, "user@nearnow.com", "hashedPassword", "User Name", "user");
        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throwsSameExceptionAsWrongPassword() {
        // Deliberately the same exception type + message as a wrong
        // password (see AuthService's own comment) so a client can't
        // use the error to enumerate which emails are registered.
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("nobody@nearnow.com");
        request.setPassword("anyPassword");

        when(userRepository.findByEmail("nobody@nearnow.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    // --- refresh -----------------------------------------------------

    @Test
    void refresh_validToken_rotatesToNewTokenPair() throws Exception {
        String oldRefreshToken = "old-refresh-token";
        User user = userWithId(3L, "user@nearnow.com", "hashedPassword", "User Name", "user");

        when(jwtUtil.validateToken(oldRefreshToken)).thenReturn(true);
        when(jwtUtil.isRefreshToken(oldRefreshToken)).thenReturn(true);
        when(jwtUtil.extractEmail(oldRefreshToken)).thenReturn("user@nearnow.com");
        when(jwtUtil.extractAuthVersion(oldRefreshToken)).thenReturn(0L);
        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(3L, "user@nearnow.com", "user", 0L)).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken(3L, "user@nearnow.com", 0L)).thenReturn("new-refresh-token");

        AuthResponseDTO response = authService.refresh(oldRefreshToken);

        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void refresh_tokenFailsSignatureCheck_throws() {
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void refresh_accessTokenPassedInsteadOfRefreshToken_rejected() {
        // A valid-signature access token must NOT be usable at the
        // refresh endpoint — isRefreshToken() is what stops that.
        when(jwtUtil.validateToken("access-token")).thenReturn(true);
        when(jwtUtil.isRefreshToken("access-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("access-token"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_afterLogoutOnAnotherDevice_authVersionMismatchRejected() throws Exception {
        // Simulates: user logged out (authVersion bumped to 1 in the DB)
        // but this refresh token was issued back when authVersion was 0.
        String staleRefreshToken = "stale-refresh-token";
        User user = userWithId(4L, "user@nearnow.com", "hashedPassword", "User Name", "user");
        user.incrementAuthVersion(); // DB is now at authVersion=1

        when(jwtUtil.validateToken(staleRefreshToken)).thenReturn(true);
        when(jwtUtil.isRefreshToken(staleRefreshToken)).thenReturn(true);
        when(jwtUtil.extractEmail(staleRefreshToken)).thenReturn("user@nearnow.com");
        when(jwtUtil.extractAuthVersion(staleRefreshToken)).thenReturn(0L);
        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.refresh(staleRefreshToken))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Session expired, please log in again");
    }

    // --- logout --------------------------------------------------------

    @Test
    void logout_incrementsAuthVersionAndSaves() throws Exception {
        User user = userWithId(5L, "user@nearnow.com", "hashedPassword", "User Name", "user");
        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));

        authService.logout("user@nearnow.com");

        assertThat(user.getAuthVersion()).isEqualTo(1L);
        verify(userRepository).save(user);
    }

    // --- forgotPassword / resetPassword --------------------------------

    @Test
    void forgotPassword_knownEmail_setsOtpAndSendsNotification() throws Exception {
        User user = userWithId(6L, "user@nearnow.com", "hashedPassword", "User Name", "user");
        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedOtp");

        authService.forgotPassword("user@nearnow.com");

        assertThat(user.getResetOtpHash()).isEqualTo("hashedOtp");
        assertThat(user.getResetOtpExpiresAt()).isAfter(Instant.now());
        verify(userRepository).save(user);
        verify(notificationService).sendPasswordResetOtp(eq("user@nearnow.com"), anyString());
    }

    @Test
    void forgotPassword_unknownEmail_silentlyNoOps() {
        // No exception, no exception thrown, nothing saved or sent —
        // this is the deliberate enumeration-defense behavior described
        // in AuthService's own comment on this method.
        when(userRepository.findByEmail("nobody@nearnow.com")).thenReturn(Optional.empty());

        authService.forgotPassword("nobody@nearnow.com");

        verify(userRepository, never()).save(any());
        verify(notificationService, never()).sendPasswordResetOtp(anyString(), anyString());
    }

    @Test
    void resetPassword_validOtp_updatesPasswordAndBumpsAuthVersion() throws Exception {
        User user = userWithId(7L, "user@nearnow.com", "oldHash", "User Name", "user");
        user.setResetOtpHash("hashedOtp");
        user.setResetOtpExpiresAt(Instant.now().plusSeconds(300));

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("123456", "hashedOtp")).thenReturn(true);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newHash");

        authService.resetPassword("user@nearnow.com", "123456", "newPassword123");

        assertThat(user.getPasswordHash()).isEqualTo("newHash");
        assertThat(user.getResetOtpHash()).isNull();
        assertThat(user.getResetOtpExpiresAt()).isNull();
        assertThat(user.getAuthVersion()).isEqualTo(1L);
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_expiredOtp_throwsAndDoesNotChangePassword() throws Exception {
        User user = userWithId(8L, "user@nearnow.com", "oldHash", "User Name", "user");
        user.setResetOtpHash("hashedOtp");
        user.setResetOtpExpiresAt(Instant.now().minusSeconds(1)); // already expired

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resetPassword("user@nearnow.com", "123456", "newPassword123"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.getPasswordHash()).isEqualTo("oldHash");
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_wrongOtp_throws() throws Exception {
        User user = userWithId(9L, "user@nearnow.com", "oldHash", "User Name", "user");
        user.setResetOtpHash("hashedOtp");
        user.setResetOtpExpiresAt(Instant.now().plusSeconds(300));

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("000000", "hashedOtp")).thenReturn(false);

        assertThatThrownBy(() -> authService.resetPassword("user@nearnow.com", "000000", "newPassword123"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void resetPassword_noOtpEverRequested_throws() throws Exception {
        // resetOtpHash is null — user never called forgotPassword first.
        User user = userWithId(10L, "user@nearnow.com", "oldHash", "User Name", "user");

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resetPassword("user@nearnow.com", "123456", "newPassword123"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    // --- getCurrentUser --------------------------------------------------

    @Test
    void getCurrentUser_found_returnsSafeProfileWithoutPasswordHash() throws Exception {
        User user = userWithId(11L, "user@nearnow.com", "hashedPassword", "User Name", "user");

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));

        UserResponseDTO response = authService.getCurrentUser("user@nearnow.com");

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getEmail()).isEqualTo("user@nearnow.com");
        // UserResponseDTO has no passwordHash field at all — compile-time
        // guarantee, not just a runtime check, but confirming the right
        // DTO type came back is still worth asserting explicitly.
        assertThat(response).isInstanceOf(UserResponseDTO.class);
    }

    @Test
    void getCurrentUser_tokenValidButUserDeleted_throwsResourceNotFound() {
        when(userRepository.findByEmail("ghost@nearnow.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("ghost@nearnow.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- updateProfile ---------------------------------------------------

    @Test
    void updateProfile_updatesFullNameAndPhone() throws Exception {
        User user = userWithId(12L, "user@nearnow.com", "hashedPassword", "Old Name", "user");
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setFullName("New Name");
        request.setPhone("9999999999");

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO response = authService.updateProfile("user@nearnow.com", request);

        assertThat(response.getFullName()).isEqualTo("New Name");
        assertThat(response.getPhone()).isEqualTo("9999999999");
    }

    @Test
    void updateProfile_nullPhotoUrl_leavesExistingPhotoUnchanged() throws Exception {
        // request.getPhotoUrl() == null means "field wasn't sent" — the
        // service should NOT overwrite an existing photo with null.
        User user = userWithId(13L, "user@nearnow.com", "hashedPassword", "Old Name", "user");
        user.setPhotoUrl("https://example.com/old-photo.jpg");
        UpdateProfileRequestDTO request = new UpdateProfileRequestDTO();
        request.setFullName("New Name");

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO response = authService.updateProfile("user@nearnow.com", request);

        assertThat(response.getPhotoUrl()).isEqualTo("https://example.com/old-photo.jpg");
    }

    // --- updateNotificationPreference ------------------------------------

    @Test
    void updateNotificationPreference_togglesFlag() throws Exception {
        User user = userWithId(14L, "user@nearnow.com", "hashedPassword", "User Name", "user");
        assertThat(user.isNotificationsEnabled()).isTrue(); // default

        when(userRepository.findByEmail("user@nearnow.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO response = authService.updateNotificationPreference("user@nearnow.com", false);

        assertThat(response.isNotificationsEnabled()).isFalse();
        verify(userRepository, times(1)).save(user);
    }
}
