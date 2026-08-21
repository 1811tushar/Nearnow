import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../data/auth_repository.dart';
import '../../profile/providers/user_provider.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/primary_button.dart';
import '../../../core/widgets/custom_text_field.dart';
import '../../../l10n/app_localizations.dart';

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final emailController = TextEditingController();
  final passwordController = TextEditingController();
  final fullNameController = TextEditingController();
  bool _isRegisterMode = false;

  @override
  void dispose() {
    emailController.dispose();
    passwordController.dispose();
    fullNameController.dispose();
    super.dispose();
  }

  String _authErrorMessage(AppLocalizations l10n, String? code, String fallback) {
    switch (code) {
      case 'invalid-credential':
        return l10n.authErrorInvalidCredential;
      case 'email-already-in-use':
        return l10n.authErrorEmailInUse;
      case 'invalid-email':
        return l10n.authErrorInvalidEmail;
      case 'network-request-failed':
        return l10n.authErrorNetworkError;
      default:
        return fallback;
    }
  }

  void _showForgotPasswordSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      builder: (sheetContext) => const _ForgotPasswordSheet(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final l10n = AppLocalizations.of(context)!;

    return Scaffold(
      appBar: AppBar(title: Text(_isRegisterMode ? l10n.register : l10n.login)),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            children: [
              if (_isRegisterMode) ...[
                TextFormField(
                  controller: fullNameController,
                  decoration: const InputDecoration(labelText: 'Full Name'),
                  validator: (v) =>
                      (v == null || v.trim().isEmpty) ? 'Full name is required' : null,
                ),
                const SizedBox(height: 16),
              ],
              TextFormField(
                controller: emailController,
                keyboardType: TextInputType.emailAddress,
                decoration: InputDecoration(labelText: l10n.email),
                validator: Validators.email,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: passwordController,
                obscureText: true,
                decoration: InputDecoration(labelText: l10n.password),
                validator: Validators.password,
              ),
              if (!_isRegisterMode)
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton(
                    onPressed: () => _showForgotPasswordSheet(context),
                    child: Text(l10n.forgotPassword),
                  ),
                ),
              const SizedBox(height: 8),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: auth.isLoading
                      ? null
                      : () async {
                          if (!_formKey.currentState!.validate()) return;

                          final userProvider = context.read<UserProvider>();
                          final success = _isRegisterMode
                              ? await auth.register(
                                  emailController.text.trim(),
                                  passwordController.text,
                                  userProvider,
                                  fullName: fullNameController.text.trim(),
                                )
                              : await auth.login(
                                  emailController.text.trim(),
                                  passwordController.text,
                                  userProvider,
                                );
                          if (!success && context.mounted) {
                            ScaffoldMessenger.of(context).showSnackBar(
                              SnackBar(
                                content: Text(_authErrorMessage(
                                  l10n,
                                  auth.error,
                                  _isRegisterMode
                                      ? l10n.registrationFailed
                                      : l10n.loginFailed,
                                )),
                              ),
                            );
                          }
                        },
                  child: auth.isLoading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : Text(_isRegisterMode ? l10n.register : l10n.login),
                ),
              ),
              const SizedBox(height: 10),
              TextButton(
                onPressed: () => setState(() => _isRegisterMode = !_isRegisterMode),
                child: Text(_isRegisterMode
                    ? 'Already have an account? Login'
                    : "Don't have an account? Register"),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Two-step bottom sheet: email -> (code + new password). Mirrors the
/// backend's actual two-call shape (forgot-password, then
/// reset-password) rather than pretending it's one request — same
/// design as the portal's /forgot-password page. Deliberately talks to
/// AuthRepository directly rather than routing through AuthProvider:
/// neither call logs the user in or out, so there's no global
/// isLoggedIn/isLoading state this sheet needs to touch or notify.
class _ForgotPasswordSheet extends StatefulWidget {
  const _ForgotPasswordSheet();

  @override
  State<_ForgotPasswordSheet> createState() => _ForgotPasswordSheetState();
}

class _ForgotPasswordSheetState extends State<_ForgotPasswordSheet> {
  final _repository = AuthRepository();
  final _emailFormKey = GlobalKey<FormState>();
  final _resetFormKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _otpController = TextEditingController();
  final _newPasswordController = TextEditingController();

  bool _codeSent = false;
  bool _isLoading = false;
  String? _error;

  @override
  void dispose() {
    _emailController.dispose();
    _otpController.dispose();
    _newPasswordController.dispose();
    super.dispose();
  }

  Future<void> _requestCode() async {
    if (!_emailFormKey.currentState!.validate()) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      await _repository.forgotPassword(_emailController.text.trim());
      if (!mounted) return;
      setState(() {
        _codeSent = true;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e is ApiException ? e.message : 'Request failed';
        _isLoading = false;
      });
    }
  }

  Future<void> _submitReset() async {
    if (!_resetFormKey.currentState!.validate()) return;
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      await _repository.resetPassword(
        _emailController.text.trim(),
        _otpController.text.trim(),
        _newPasswordController.text,
      );
      if (!mounted) return;
      final l10n = AppLocalizations.of(context)!;
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.passwordResetSuccess)),
      );
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = e is ApiException ? e.message : 'Reset failed';
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Padding(
      padding: EdgeInsets.only(
        left: 20,
        right: 20,
        top: 20,
        // Keeps the sheet above the on-screen keyboard.
        bottom: MediaQuery.of(context).viewInsets.bottom + 20,
      ),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(l10n.resetPassword, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            Text(
              _codeSent
                  ? l10n.passwordResetEmailSent
                  : l10n.resetPasswordInstructions,
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 20),
            if (!_codeSent) ...[
              Form(
                key: _emailFormKey,
                child: CustomTextField(
                  controller: _emailController,
                  label: l10n.email,
                  keyboardType: TextInputType.emailAddress,
                  validator: Validators.email,
                ),
              ),
              const SizedBox(height: 16),
              if (_error != null) ...[
                Text(_error!, style: const TextStyle(color: Colors.red)),
                const SizedBox(height: 12),
              ],
              PrimaryButton(
                label: l10n.sendResetLink,
                isLoading: _isLoading,
                onPressed: _requestCode,
              ),
            ] else ...[
              Form(
                key: _resetFormKey,
                child: Column(
                  children: [
                    CustomTextField(
                      controller: _otpController,
                      label: l10n.resetCodeLabel,
                      keyboardType: TextInputType.number,
                      validator: (v) => (v == null || v.trim().length != 6)
                          ? l10n.resetCodeLabel
                          : null,
                    ),
                    const SizedBox(height: 16),
                    CustomTextField(
                      controller: _newPasswordController,
                      label: l10n.newPasswordLabel,
                      obscureText: true,
                      validator: Validators.password,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              if (_error != null) ...[
                Text(_error!, style: const TextStyle(color: Colors.red)),
                const SizedBox(height: 12),
              ],
              PrimaryButton(
                label: l10n.resetPasswordButton,
                isLoading: _isLoading,
                onPressed: _submitReset,
              ),
              const SizedBox(height: 8),
              TextButton(
                onPressed: _isLoading ? null : () => setState(() => _codeSent = false),
                child: Text(l10n.useDifferentEmail),
              ),
            ],
          ],
        ),
      ),
    );
  }
}