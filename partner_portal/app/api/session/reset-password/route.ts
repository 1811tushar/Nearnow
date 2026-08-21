import {NextResponse} from 'next/server';

export async function POST(req: Request) {
  const body = await req.json();
  const base = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api';
  const r = await fetch(`${base}/auth/reset-password`, {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(body),
  });
  const data = await r.json();
  if (!r.ok || data?.success === false) {
    return NextResponse.json({message: data?.message || 'Reset failed'}, {status: r.status});
  }
  return NextResponse.json({message: data.message});
}
