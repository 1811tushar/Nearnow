import {NextRequest, NextResponse} from 'next/server';
import {cookies} from 'next/headers';

const ALLOWED_PREFIXES = ['/auth/me','/admin','/vendor','/warehouse','/rider','/products','/categories','/reviews','/addresses','/cart','/orders','/payments'];

async function forward(req: NextRequest, {params}:{params:Promise<{path:string[]}>}) {
  const {path} = await params;
  const route = '/' + path.join('/');
  if (!ALLOWED_PREFIXES.some(prefix => route === prefix || route.startsWith(prefix + '/'))) {
    return NextResponse.json({success:false,message:'Route not allowed'}, {status:403});
  }
  const token = (await cookies()).get('nearnow_token')?.value;
  if (!token) return NextResponse.json({success:false,message:'Not authenticated'}, {status:401});
  const base = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api';
  const url = `${base}${route}${req.nextUrl.search}`;
  const headers = new Headers();
  const ct = req.headers.get('content-type');
  if (ct) headers.set('Content-Type', ct);
  headers.set('Authorization', `Bearer ${token}`);
  const body = ['GET','HEAD'].includes(req.method) ? undefined : await req.text();
  const response = await fetch(url, {method:req.method, headers, body, cache:'no-store'});
  const text = await response.text();
  const out = new NextResponse(text, {status:response.status});
  out.headers.set('Content-Type', response.headers.get('content-type') || 'application/json');
  return out;
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
export const PATCH = forward;
export const DELETE = forward;
