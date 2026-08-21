'use client';
import {useEffect, useMemo, useState} from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {get, put} from '@/lib/api';
import {Button, Card, PageHeader, ErrorState, Skeleton} from '@/components/ui';
import {useToast} from '@/components/toast';
import type {Page, Product, Stock} from '@/types';
import {Search, Plus, X, ArrowLeft, PackageSearch, PackagePlus, AlertTriangle, CheckCircle2} from 'lucide-react';

export default function StockPage() {
  const qc = useQueryClient();
  const {toast} = useToast();
  const [filter, setFilter] = useState('');
  const [adding, setAdding] = useState(false);

  const stockQ = useQuery({
    queryKey: ['stock'],
    queryFn: () => get<Stock[]>('/warehouse/stock'),
  });

  const adjust = useMutation({
    mutationFn: (x: {productId: number; quantity: number}) => put<Stock>('/warehouse/stock', x),
    onSuccess: () => qc.invalidateQueries({queryKey: ['stock']}),
  });

  const stock = stockQ.data ?? [];

  // Passed to the modal so it can flag products already on this store's
  // stock sheet instead of letting the manager create a second row for one.
  const stockedProductIds = useMemo(() => new Set(stock.map(s => s.productId)), [stock]);

  const rows = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return stock;
    return stock.filter(s => `${s.productName} ${s.barcode}`.toLowerCase().includes(q));
  }, [stock, filter]);

  function saveAdjust(s: Stock, quantity: number) {
    adjust.mutate(
      {productId: s.productId, quantity},
      {
        onSuccess: () =>
          toast({title: 'Stock updated', message: `${s.productName} is now at ${quantity} units.`, tone: 'success'}),
      }
    );
  }

  return (
    <>
      <PageHeader
        title="Store stock"
        subtitle="Your assigned store only. Adjust counts, or bring a new product onto warehouse-managed stock."
        action={
          <Button onClick={() => setAdding(true)} className="inline-flex items-center gap-2">
            <Plus size={16} /> Add product to stock
          </Button>
        }
      />

      {!stockQ.isLoading && !stockQ.isError && stock.length > 0 && (
        <div className="relative mb-5 max-w-sm">
          <Search size={16} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            value={filter}
            onChange={e => setFilter(e.target.value)}
            placeholder="Search by product or barcode"
            className="focus-ring w-full rounded-xl border border-line bg-white py-2.5 pl-10 pr-4 text-sm outline-none"
          />
        </div>
      )}

      <Card>
        {stockQ.isLoading ? (
          <div className="space-y-3 p-5">
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
          </div>
        ) : stockQ.isError ? (
          <ErrorState message={(stockQ.error as Error).message} onRetry={() => stockQ.refetch()} />
        ) : stock.length === 0 ? (
          <div className="p-12 text-center">
            <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-mint">
              <PackageSearch className="text-leaf" size={24} />
            </div>
            <h3 className="font-bold">No products on your stock sheet yet</h3>
            <p className="mx-auto mt-1 max-w-sm text-sm text-gray-500">
              Nothing has been moved onto warehouse-managed stock for your store. Add the first product to start
              tracking counts.
            </p>
            <Button onClick={() => setAdding(true)} className="mx-auto mt-5 inline-flex items-center gap-2">
              <Plus size={16} /> Add product to stock
            </Button>
          </div>
        ) : rows.length === 0 ? (
          <div className="p-10 text-center text-sm text-gray-500">No stock rows match &ldquo;{filter}&rdquo;.</div>
        ) : (
          <div className="divide-y divide-line">
            {rows.map(s => (
              <StockRow key={s.id} s={s} disabled={adjust.isPending} onSave={q => saveAdjust(s, q)} />
            ))}
          </div>
        )}
      </Card>

      {adding && (
        <AddStockModal
          excludeIds={stockedProductIds}
          close={() => setAdding(false)}
          onAdded={(name, quantity) => {
            qc.invalidateQueries({queryKey: ['stock']});
            setAdding(false);
            toast({title: 'Added to store stock', message: `${name} — ${quantity} units.`, tone: 'success'});
          }}
        />
      )}
    </>
  );
}

function StockRow({s, onSave, disabled}: {s: Stock; onSave: (n: number) => void; disabled: boolean}) {
  const [value, setValue] = useState(String(s.quantity));
  const low = s.quantity <= 5;
  const dirty = value !== String(s.quantity);

  return (
    <div className="flex flex-col gap-3 p-5 md:flex-row md:items-center md:justify-between">
      <div className="min-w-0">
        <div className="flex items-center gap-2">
          <b className="truncate">{s.productName}</b>
          {low && (
            <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-[11px] font-bold uppercase tracking-wide text-red-700">
              <AlertTriangle size={11} /> Low
            </span>
          )}
        </div>
        <div className="mt-0.5 text-xs text-gray-400">Barcode {s.barcode || '—'} · Store #{s.storeId}</div>
      </div>
      <div className="flex shrink-0 items-center gap-2">
        <input
          type="number"
          min="0"
          value={value}
          onChange={e => setValue(e.target.value)}
          className="focus-ring w-24 rounded-xl border border-line px-3 py-2 text-sm outline-none"
        />
        <Button disabled={disabled || !dirty || value === '' || Number(value) < 0} onClick={() => onSave(Number(value))}>
          Save
        </Button>
      </div>
    </div>
  );
}

function AddStockModal({
  excludeIds,
  close,
  onAdded,
}: {
  excludeIds: Set<number>;
  close: () => void;
  onAdded: (name: string, quantity: number) => void;
}) {
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [selected, setSelected] = useState<Product | null>(null);
  const [quantity, setQuantity] = useState('0');

  useEffect(() => {
    const t = setTimeout(() => setDebounced(query.trim()), 300);
    return () => clearTimeout(t);
  }, [query]);

  const resultsQ = useQuery({
    queryKey: ['product-search-for-stock', debounced],
    queryFn: () =>
      debounced
        ? get<Page<Product>>(`/products/search?q=${encodeURIComponent(debounced)}&page=0&size=20`)
        : get<Page<Product>>('/products?page=0&size=20'),
  });

  const save = useMutation({
    mutationFn: () => put<Stock>('/warehouse/stock', {productId: selected!.id, quantity: Number(quantity)}),
    onSuccess: () => onAdded(selected!.name, Number(quantity)),
  });

  const results = resultsQ.data?.content ?? [];
  const qtyValid = quantity !== '' && !Number.isNaN(Number(quantity)) && Number(quantity) >= 0;

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-5" onClick={close}>
      <div onClick={(e: React.MouseEvent) => e.stopPropagation()} className="w-full max-w-lg">
        <Card className="p-6">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-black">Add product to stock</h2>
            <button aria-label="Close" onClick={close} className="text-gray-400 hover:text-ink">
              <X size={20} />
            </button>
          </div>

          {!selected ? (
            <div className="mt-5">
              <div className="relative mb-2">
                <Search size={14} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                <input
                  autoFocus
                  value={query}
                  onChange={e => setQuery(e.target.value)}
                  placeholder="Search products by name…"
                  className="focus-ring w-full rounded-xl border border-line py-2.5 pl-9 pr-3 text-sm outline-none"
                />
              </div>
              <p className="mb-2 text-xs text-gray-400">Catalog search — every product is eligible, not just ones already at your store.</p>
              <div className="max-h-72 overflow-y-auto rounded-xl border border-line">
                {resultsQ.isLoading ? (
                  <div className="space-y-2 p-3">
                    <Skeleton className="h-12" />
                    <Skeleton className="h-12" />
                  </div>
                ) : resultsQ.isError ? (
                  <div className="p-4 text-sm text-red-600">{(resultsQ.error as Error).message}</div>
                ) : results.length === 0 ? (
                  <div className="p-4 text-sm text-gray-400">
                    {debounced ? `No products match “${debounced}”.` : 'No products found.'}
                  </div>
                ) : (
                  results.map(p => {
                    const already = excludeIds.has(p.id);
                    return (
                      <button
                        key={p.id}
                        type="button"
                        disabled={already}
                        onClick={() => {
                          setSelected(p);
                          setQuantity('0');
                        }}
                        className="flex w-full items-center justify-between gap-3 border-b border-line px-3.5 py-2.5 text-left text-sm last:border-b-0 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-transparent"
                      >
                        <span className="min-w-0">
                          <span className="block truncate font-semibold">{p.name}</span>
                          <span className="text-xs text-gray-400">
                            {p.barcode || 'No barcode'} · ₹{Number(p.effectivePrice ?? p.salePrice ?? p.price).toFixed(2)}
                          </span>
                        </span>
                        {already && (
                          <span className="inline-flex shrink-0 items-center gap-1 rounded-full bg-mint px-2 py-0.5 text-[11px] font-bold uppercase tracking-wide text-leaf">
                            <CheckCircle2 size={11} /> On sheet
                          </span>
                        )}
                      </button>
                    );
                  })
                )}
              </div>
            </div>
          ) : (
            <div className="mt-5">
              <button
                type="button"
                onClick={() => setSelected(null)}
                className="mb-4 inline-flex items-center gap-1.5 text-sm font-semibold text-gray-500 hover:text-ink"
              >
                <ArrowLeft size={15} /> Choose a different product
              </button>

              <div className="flex items-center gap-3 rounded-xl bg-gray-50 p-3.5">
                <div className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-mint">
                  <PackagePlus className="text-leaf" size={18} />
                </div>
                <div className="min-w-0">
                  <div className="truncate font-bold">{selected.name}</div>
                  <div className="text-xs text-gray-500">{selected.barcode || 'No barcode'} · #{selected.id}</div>
                </div>
              </div>

              <label className="mt-4 block text-sm font-semibold">
                Initial quantity
                <input
                  type="number"
                  min="0"
                  autoFocus
                  value={quantity}
                  onChange={e => setQuantity(e.target.value)}
                  className="focus-ring mt-1.5 w-full rounded-xl border border-line px-3.5 py-2.5 text-sm outline-none"
                />
              </label>
              <p className="mt-1.5 text-xs text-gray-400">
                This creates the stock row for your store — you can adjust the count any time from the list.
              </p>

              {save.error && <p className="mt-3 text-sm text-red-600">{(save.error as Error).message}</p>}

              <div className="mt-6 flex justify-end gap-2">
                <Button onClick={close}>Cancel</Button>
                <Button disabled={!qtyValid || save.isPending} onClick={() => save.mutate()}>
                  {save.isPending ? 'Adding…' : 'Add to stock'}
                </Button>
              </div>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
