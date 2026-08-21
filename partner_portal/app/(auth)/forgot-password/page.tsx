'use client';
import {useState} from 'react';
import {useRouter} from 'next/navigation';
import {useToast} from '@/components/toast';
import {Button} from '@/components/ui';

// Two-step form on one page: step 1 collects just the email and calls
// /api/session/forgot-password. Step 2 (OTP + new password) only
// appears after that succeeds — matching the backend's actual two-call
// shape (forgot-password, then reset-password) rather than pretending
// it's one request.
export default function ForgotPassword() {
  const r = useRouter();
  const {toast} = useToast();
  const [step, setStep] = useState<1 | 2>(1);
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [err, setErr] = useState('');
  const [busy, setBusy] = useState(false);

  async function requestCode(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr('');
    try {
      const x = await fetch('/api/session/forgot-password', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email}),
      });
      const d = await x.json();
      if (!x.ok) throw new Error(d.message);
      toast({title: 'Code sent', message: d.message, tone: 'success'});
      setStep(2);
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Request failed';
      setErr(message);
    } finally {
      setBusy(false);
    }
  }

  async function submitReset(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setErr('');
    try {
      const x = await fetch('/api/session/reset-password', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({email, otp, newPassword}),
      });
      const d = await x.json();
      if (!x.ok) throw new Error(d.message);
      toast({title: 'Password reset', message: 'Please sign in with your new password.', tone: 'success'});
      r.push('/login');
    } catch (e) {
      const message = e instanceof Error ? e.message : 'Reset failed';
      setErr(message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="grid min-h-screen place-items-center bg-ink p-6">
      <div className="grid w-full max-w-5xl overflow-hidden rounded-3xl bg-white shadow-2xl md:grid-cols-2">
        <div className="hidden bg-mint p-10 md:block">
          <div className="text-2xl font-black">Near<span className="text-leaf">Now</span></div>
          <div className="mt-20 max-w-sm">
            <div className="mb-3 inline-flex rounded-full bg-white px-3 py-1 text-xs font-bold text-leaf">
              ACCOUNT RECOVERY
            </div>
            <h1 className="text-5xl font-black leading-tight">Locked out? Let&apos;s fix that.</h1>
            <p className="mt-5 text-gray-600">We&apos;ll send a one-time code you can use to set a new password.</p>
          </div>
        </div>
        <div className="p-7 md:p-12">
          <div className="mb-10 text-2xl font-black md:hidden">Near<span className="text-leaf">Now</span></div>
          <h2 className="text-3xl font-black">{step === 1 ? 'Forgot password' : 'Enter your code'}</h2>
          <p className="mt-2 text-sm text-gray-500">
            {step === 1
              ? "Enter the email on your account and we'll send a reset code."
              : `We sent a code for ${email}. It expires in 10 minutes.`}
          </p>

          {step === 1 ? (
            <form onSubmit={requestCode} className="mt-8 space-y-5">
              <label className="block text-sm font-semibold">
                Email
                <input
                  required
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  className="focus-ring mt-2 w-full rounded-xl border border-line px-4 py-3 outline-none"
                  placeholder="you@nearnow.com"
                />
              </label>
              {err && <div className="rounded-xl bg-red-50 p-3 text-sm font-medium text-red-700">{err}</div>}
              <Button disabled={busy} className="w-full">{busy ? 'Sending…' : 'Send reset code'}</Button>
            </form>
          ) : (
            <form onSubmit={submitReset} className="mt-8 space-y-5">
              <label className="block text-sm font-semibold">
                Reset code
                <input
                  required
                  inputMode="numeric"
                  maxLength={6}
                  value={otp}
                  onChange={e => setOtp(e.target.value)}
                  className="focus-ring mt-2 w-full rounded-xl border border-line px-4 py-3 outline-none tracking-widest"
                  placeholder="123456"
                />
              </label>
              <label className="block text-sm font-semibold">
                New password
                <input
                  required
                  minLength={6}
                  type="password"
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  className="focus-ring mt-2 w-full rounded-xl border border-line px-4 py-3 outline-none"
                  placeholder="••••••••"
                />
              </label>
              {err && <div className="rounded-xl bg-red-50 p-3 text-sm font-medium text-red-700">{err}</div>}
              <Button disabled={busy} className="w-full">{busy ? 'Resetting…' : 'Reset password'}</Button>
              <button
                type="button"
                onClick={() => setStep(1)}
                className="w-full text-center text-xs font-semibold text-gray-400 hover:text-gray-600"
              >
                Use a different email
              </button>
            </form>
          )}

          <p className="mt-6 text-xs text-gray-400">
            Remembered it? <a href="/login" className="font-semibold text-leaf">Back to sign in</a>
          </p>
        </div>
      </div>
    </main>
  );
}
