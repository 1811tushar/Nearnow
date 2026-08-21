'use client';
import {useState} from 'react';
import {useMutation,useQuery,useQueryClient} from '@tanstack/react-query';
import {get,put} from '@/lib/api';
import {Button,Card,PageHeader,ErrorState,Skeleton} from '@/components/ui';
import type {Page} from '@/types';

type User={id:number;email:string;fullName?:string;role:string};
export default function UsersPage(){
 const [page,setPage]=useState(0); const qc=useQueryClient();
 const q=useQuery({queryKey:['admin-users',page],queryFn:()=>get<Page<User>>(`/admin/users?page=${page}&size=20`)});
 const m=useMutation({mutationFn:({id,role}:{id:number;role:string})=>put(`/admin/users/${id}/role`,{role}),onSuccess:()=>qc.invalidateQueries({queryKey:['admin-users']})});
 return <><PageHeader title="Users & roles" subtitle="Admin-controlled user directory and role assignment."/><Card className="overflow-hidden">{q.isLoading?<div className="p-5"><Skeleton className="h-12"/></div>:q.isError?<ErrorState message={(q.error as Error).message} onRetry={()=>q.refetch()}/>:<div className="divide-y">{q.data?.content.map(u=><div key={u.id} className="grid gap-3 p-5 md:grid-cols-[80px_1fr_180px_150px] md:items-center"><span className="text-sm text-gray-500">#{u.id}</span><div><b>{u.fullName||'Unnamed'}</b><div className="text-xs text-gray-500">{u.email}</div></div><select value={u.role} disabled={m.isPending} onChange={e=>m.mutate({id:u.id,role:e.target.value})} className="rounded-xl border px-3 py-2 text-sm"><option>user</option><option>admin</option><option>warehouse_manager</option><option>vendor</option><option>rider</option></select><span className="text-xs text-gray-500">Changing role invalidates the old JWT.</span></div>)}</div>}<div className="flex justify-between border-t p-4"><Button disabled={page===0} onClick={()=>setPage(p=>p-1)}>Previous</Button><span className="py-2 text-sm text-gray-500">Page {page+1} / {q.data?.totalPages||1}</span><Button disabled={!q.data||page+1>=(q.data?.totalPages||1)} onClick={()=>setPage(p=>p+1)}>Next</Button></div></Card></>}
