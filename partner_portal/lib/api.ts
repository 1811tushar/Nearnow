export const API_BASE=typeof window==='undefined'?(process.env.NEXT_PUBLIC_API_BASE_URL||'http://localhost:8080/api'):'/api/backend';
export class ApiError extends Error{status:number;constructor(message:string,status=500){super(message);this.status=status}}
export async function api<T>(path:string,init:RequestInit={}){const headers=new Headers(init.headers);headers.set('Content-Type','application/json');const res=await fetch(`${API_BASE}${path}`,{...init,headers,credentials:'include',cache:'no-store'});const body=await res.json().catch(()=>null);if(!res.ok||body?.success===false){
  const error = new ApiError(body?.message||`Request failed (${res.status})`,res.status);
  if (typeof window !== 'undefined' && !['GET','HEAD'].includes((init.method || 'GET').toUpperCase())) window.dispatchEvent(new CustomEvent('nearnow:api-error',{detail:{message:error.message,status:error.status}}));
  throw error;
}return (body?.data??body) as T}
export const get=<T>(p:string)=>api<T>(p);export const post=<T>(p:string,b?:unknown)=>api<T>(p,{method:'POST',body:b===undefined?undefined:JSON.stringify(b)});export const put=<T>(p:string,b?:unknown)=>api<T>(p,{method:'PUT',body:b===undefined?undefined:JSON.stringify(b)});export const patch=<T>(p:string,b?:unknown)=>api<T>(p,{method:'PATCH',body:b===undefined?undefined:JSON.stringify(b)});export const del=<T>(p:string)=>api<T>(p,{method:'DELETE'});
