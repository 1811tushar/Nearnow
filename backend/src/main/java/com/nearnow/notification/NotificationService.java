package com.nearnow.notification;

import com.nearnow.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Own package, not common/ — "will 2+ features need this" (Section 4's
 * decision rule): currently only Order and Auth call this, so it's a
 * small dedicated domain-concern of its own, not generic cross-cutting
 * infrastructure like ApiResponse/exceptions.
 *
 * Order confirmations still just log (nothing downstream reads that
 * email yet, and there's no urgency — the order is already visible in
 * the app). Password-reset OTPs are different: the whole point is
 * getting a code to a device the caller doesn't have logged in, so
 * that one now sends a REAL email over Gmail SMTP (spring-boot-starter-mail
 * + an App Password in .env) — still free, no paid API, no vendor
 * account beyond the Gmail account already configured.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // @Async + the "notificationExecutor" bean (AsyncConfig) = this
    // method returns to its caller (OrderService.placeOrder()) IMMEDIATELY
    // — the actual notification "send" happens on a separate background
    // thread. This is the concrete fix for the note flagged all the way
    // back in Phase 8's roadmap: "order-confirmation notification
    // shouldn't block the request thread."
    @Async("notificationExecutor")
    public void sendOrderConfirmation(Order order) {
        // Free development mode: notifications are an application event/log.
        // No email/SMS provider is called and therefore no external bill is generated.
        log.info("Order confirmation event for order #{} to user #{}, total {}",
                order.getId(), order.getUser().getId(), order.getTotalAmount());
    }

    // This is the ONE place AuthService's forgot-password flow calls —
    // AuthService never sees the delivery mechanism, only that it was
    // "sent." Deliberately synchronous (no @Async): the caller needs this
    // to have actually happened before returning its generic "if that
    // email exists, we sent a code" response.
    //
    // Deliberately swallows any mail-sending failure rather than letting
    // it propagate: if it threw, AuthService.forgotPassword() would need
    // to either bubble a 500 (defeating the point of the generic
    // always-succeeds response) or catch it there instead — same
    // decision, better made once, here. A bad SMTP config still shows up
    // loudly in the log for YOU to notice; it just doesn't become the
    // caller's problem.
    public void sendPasswordResetOtp(String email, String otp) {
        log.info("Password reset OTP for {}: {} (valid 10 minutes)", email, otp);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your NearNow password reset code");
            message.setText("Your password reset code is " + otp
                    + ". It expires in 10 minutes. If you didn't request this, you can ignore this email.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", email, e.getMessage());
        }
    }
}

