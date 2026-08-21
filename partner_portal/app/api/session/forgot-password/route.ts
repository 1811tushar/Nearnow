import {NextResponse} from 'next/server';

// Same proxy pattern as app/api/session/route.ts (login) — the browser
// never talks to the backend directly, only to this Next.js route,
// which forwards to Spring Boot server-side. No cookie is set here
// (unlike login) because this endpoint doesn't authenticate anyone —
// it just asks the backend to email/log an OTP.
export async function POST(req: Request) {
  const body = await req.json();
  const base = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api';
  const r = await fetch(`${base}/auth/forgot-password`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(body),
  });
  const data = await r.json();
  if (!r.ok || data?.success === false) {
    return NextResponse.json({message: data?.message || 'Request failed'}, {status: r.status});
  }
  return NextResponse.json({message: data.message});
}
