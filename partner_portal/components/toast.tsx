'use client';
import React, {createContext, useCallback, useContext, useEffect, useMemo, useState} from 'react';

type Toast = {id:number; title:string; message?:string; tone:'success'|'error'|'info'};
type ToastContextValue = {toast:(input:Omit<Toast,'id'>)=>void};
const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({children}:{children:React.ReactNode}) {
  const [items,setItems] = useState<Toast[]>([]);
  const toast = useCallback((input:Omit<Toast,'id'>) => {
    const id = Date.now() + Math.floor(Math.random()*1000);
    setItems(prev => [...prev,{...input,id}].slice(-4));
    window.setTimeout(() => setItems(prev => prev.filter(x => x.id !== id)), 4200);
  },[]);
  useEffect(()=>{
    const handler=(event:Event)=>{
      const detail=(event as CustomEvent<{message?:string}>).detail;
      toast({title:'Request failed',message:detail?.message||'The operation could not be completed.',tone:'error'});
    };
    window.addEventListener('nearnow:api-error',handler);
    return ()=>window.removeEventListener('nearnow:api-error',handler);
  },[toast]);
  const value = useMemo(()=>({toast}),[toast]);
  return <ToastContext.Provider value={value}>
    {children}
    <div className="pointer-events-none fixed right-4 top-4 z-[100] flex w-[min(92vw,380px)] flex-col gap-3" aria-live="polite">
      {items.map(item => <div key={item.id} className={`pointer-events-auto rounded-2xl border p-4 shadow-2xl ${item.tone==='error'?'border-red-200 bg-red-50 text-red-900':item.tone==='success'?'border-green-200 bg-green-50 text-green-900':'border-line bg-white text-ink'}`}>
        <div className="flex items-start justify-between gap-3"><div><div className="font-bold">{item.title}</div>{item.message&&<div className="mt-1 text-sm opacity-80">{item.message}</div>}</div><button aria-label="Dismiss" className="text-lg leading-none opacity-50 hover:opacity-100" onClick={()=>setItems(prev=>prev.filter(x=>x.id!==item.id))}>×</button></div>
      </div>)}
    </div>
  </ToastContext.Provider>;
}

export function useToast(){
  const ctx = useContext(ToastContext);
  if(!ctx) throw new Error('useToast must be used inside ToastProvider');
  return ctx;
}
