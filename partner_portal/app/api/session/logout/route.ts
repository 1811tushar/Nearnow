import {NextResponse} from 'next/server';export async function POST(){const r=NextResponse.json({ok:true});r.cookies.delete('nearnow_token');r.cookies.delete('nearnow_role');return r}
