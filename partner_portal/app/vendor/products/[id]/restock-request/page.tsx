'use client';
import {use, useState} from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {post, get} from '@/lib/api';
import {Button, Card, PageHeader, Skeleton, ErrorState} from '@/components/ui';
import type {RestockRequest} from '@/types';

function StatusBadge({status}: {status: RestockRequest['status']}) {
  const tone =
    status === 'APPROVED' ? 'bg-green-100 text-green-700' :
    status === 'REJECTED' ? 'bg-red-100 text-red-700' :
    'bg-amber-100 text-amber-700';
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${tone}`}>{status}</span>;
}

export default function P({params}: {params: Promise<{id: string}>}) {
  const {id} = use(params);
  const qc = useQueryClient();
  const [quantity, setQuantity] = useState('10');
  const [note, setNote] = useState('');

  // Every past request this vendor has ever sent — approved, rejected, or
  // still pending. The backend already returns these newest-first, so no
  // client-side sort is needed.
  const historyQ = useQuery({
    queryKey: ['vendor-restock-requests'],
    queryFn: () => get<RestockRequest[]>('/vendor/restock-requests'),
  });

  const m = useMutation({
    mutationFn: () => post(`/vendor/products/${id}/restock-request`, {quantity: Number(quantity), note}),
    onSuccess: () => {
      setNote('');
      qc.invalidateQueries({queryKey: ['vendor-restock-requests']});
    },
  });

  return (
    <>
      <PageHeader title="Restock request" subtitle="Ask the assigned warehouse to replenish this vendor product." />
      <Card className="max-w-xl p-6">
        <label className="text-sm font-semibold">
          Quantity
          <input
            type="number"
            min="1"
            value={quantity}
            onChange={e => setQuantity(e.target.value)}
            className="mt-1 w-full rounded-xl border px-3 py-2"
          />
        </label>
        <label className="mt-4 block text-sm font-semibold">
          Note
          <textarea
            value={note}
            onChange={e => setNote(e.target.value)}
            maxLength={1000}
            className="mt-1 w-full rounded-xl border px-3 py-2"
          />
        </label>
        {m.error && <p className="mt-4 text-sm text-red-600">{(m.error as Error).message}</p>}
        {m.isSuccess && <p className="mt-4 text-sm text-green-700">Restock request created.</p>}
        <div className="mt-5">
          <Button disabled={m.isPending || Number(quantity) < 1} onClick={() => m.mutate()}>
            {m.isPending ? 'Sending…' : 'Send request'}
          </Button>
        </div>
      </Card>

      <div className="mt-8 max-w-xl">
        <h2 className="font-black">Your restock request history</h2>
        <p className="mt-0.5 text-sm text-gray-500">Every request you've sent, across all your products, and its current status.</p>
        <Card className="mt-3 overflow-hidden">
          {historyQ.isLoading ? (
            <div className="p-5"><Skeleton className="h-14" /></div>
          ) : historyQ.isError ? (
            <div className="p-5"><ErrorState message={(historyQ.error as Error).message} onRetry={() => historyQ.refetch()} /></div>
          ) : (historyQ.data ?? []).length === 0 ? (
            <div className="p-10 text-center text-sm text-gray-400">No restock requests sent yet.</div>
          ) : (
            <div className="divide-y divide-line">
              {(historyQ.data ?? []).map(r => (
                <div key={r.id} className="flex flex-col gap-2 p-4 md:flex-row md:items-center md:justify-between">
                  <div>
                    <b>{r.productName}</b>
                    <div className="text-xs text-gray-500">
                      Qty {r.quantity} · {r.note || 'No note'} · {new Date(r.createdAt).toLocaleString()}
                    </div>
                  </div>
                  <StatusBadge status={r.status} />
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </>
  );
}
