'use client';
import {useMemo, useState} from 'react';
import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query';
import {get, put, del} from '@/lib/api';
import {Button, Card, PageHeader, ErrorState, Skeleton} from '@/components/ui';
import {useToast} from '@/components/toast';
import type {Page, Product} from '@/types';
import {Search, Plus, X, Building2, Mail, BadgeCheck, CircleSlash, ChevronDown, Package, Trash2} from 'lucide-react';

type Vendor = {
  id: number;
  userId: number;
  email: string;
  businessName: string;
  businessAddress?: string;
  gstNumber?: string;
  active: boolean;
};

type User = {id: number; email: string; fullName?: string; role: string};

type FormState = {
  userId: number | null;
  email: string;
  businessName: string;
  businessAddress: string;
  gstNumber: string;
};

const emptyForm: FormState = {userId: null, email: '', businessName: '', businessAddress: '', gstNumber: ''};

export default function VendorsPage() {
  const qc = useQueryClient();
  const {toast} = useToast();
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState<FormState | null>(null);
  const [userPickerOpen, setUserPickerOpen] = useState(false);
  const [userQuery, setUserQuery] = useState('');
  const [manageProductsFor, setManageProductsFor] = useState<Vendor | null>(null);
  const [productSearch, setProductSearch] = useState('');

  const vendorsQ = useQuery({
    queryKey: ['admin-vendors'],
    queryFn: () => get<Page<Vendor>>('/admin/vendors?page=0&size=100'),
  });

  // Loaded once the create/edit modal actually needs it, so the page
  // itself doesn't pay for a users fetch until an admin opens the form.
  const usersQ = useQuery({
    queryKey: ['admin-users-for-vendor-picker'],
    queryFn: () => get<Page<User>>('/admin/users?page=0&size=100'),
    enabled: editing !== null,
  });

  const save = useMutation({
    mutationFn: (f: FormState) =>
      put<Vendor>('/admin/vendors', {
        userId: f.userId,
        businessName: f.businessName.trim(),
        businessAddress: f.businessAddress.trim(),
        gstNumber: f.gstNumber.trim() || null,
      }),
    onSuccess: (_data, variables) => {
      toast({
        title: 'Vendor profile saved',
        message: `${variables.businessName} is now linked to ${variables.email}.`,
        tone: 'success',
      });
      setEditing(null);
      qc.invalidateQueries({queryKey: ['admin-vendors']});
    },
  });

  // Products already linked to the vendor currently being managed.
  const vendorProductsQ = useQuery({
    queryKey: ['admin-vendor-products', manageProductsFor?.id],
    queryFn: () => get<Product[]>(`/admin/vendors/${manageProductsFor!.id}/products`),
    enabled: manageProductsFor !== null,
  });

  // Catalog-wide search used to find a product to assign. Only fires
  // once the admin has typed something — no point loading the whole
  // catalog just because the panel is open.
  const productSearchQ = useQuery({
    queryKey: ['admin-product-search-for-vendor', productSearch],
    queryFn: () => get<Page<Product>>(`/admin/products?page=0&size=20&q=${encodeURIComponent(productSearch.trim())}`),
    enabled: manageProductsFor !== null && productSearch.trim().length > 1,
  });

  const assignProduct = useMutation({
    mutationFn: ({productId, vendorId}: {productId: number; vendorId: number}) =>
      put<void>(`/admin/products/${productId}/vendor/${vendorId}`),
    onSuccess: (_data, variables) => {
      toast({title: 'Product assigned', message: 'The product now belongs to this vendor.', tone: 'success'});
      qc.invalidateQueries({queryKey: ['admin-vendor-products', variables.vendorId]});
    },
  });

  const unassignProduct = useMutation({
    mutationFn: ({productId}: {productId: number; vendorId: number}) =>
      del<void>(`/admin/products/${productId}/vendor`),
    onSuccess: (_data, variables) => {
      toast({title: 'Product unassigned', message: 'The product no longer belongs to this vendor.', tone: 'success'});
      qc.invalidateQueries({queryKey: ['admin-vendor-products', variables.vendorId]});
    },
  });

  const assignedProductIds = useMemo(
    () => new Set((vendorProductsQ.data ?? []).map(p => p.id)),
    [vendorProductsQ.data]
  );

  const vendors = vendorsQ.data?.content ?? [];

  const vendorLookup = useMemo(() => new Map(vendors.map(v => [v.userId, v])), [vendors]);

  const filteredVendors = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return vendors;
    return vendors.filter(v =>
      v.businessName.toLowerCase().includes(q) ||
      v.email.toLowerCase().includes(q) ||
      (v.gstNumber || '').toLowerCase().includes(q)
    );
  }, [vendors, query]);

  // Only users with role=vendor are eligible — the backend rejects anything
  // else with "User must have role=vendor before a vendor profile is created".
  const eligibleUsers = useMemo(() => {
    const users = usersQ.data?.content ?? [];
    return users.filter(u => u.role === 'vendor');
  }, [usersQ.data]);

  const filteredEligibleUsers = useMemo(() => {
    const q = userQuery.trim().toLowerCase();
    const pool = eligibleUsers;
    if (!q) return pool;
    return pool.filter(u =>
      u.email.toLowerCase().includes(q) || (u.fullName || '').toLowerCase().includes(q)
    );
  }, [eligibleUsers, userQuery]);

  function openCreate() {
    setEditing({...emptyForm});
    setUserQuery('');
    setUserPickerOpen(false);
  }

  function openEdit(v: Vendor) {
    setEditing({
      userId: v.userId,
      email: v.email,
      businessName: v.businessName,
      businessAddress: v.businessAddress || '',
      gstNumber: v.gstNumber || '',
    });
    setUserQuery('');
    setUserPickerOpen(false);
  }

  function pickUser(u: User) {
    setEditing(prev => prev && {...prev, userId: u.id, email: u.email});
    setUserPickerOpen(false);
  }

  function openManageProducts(v: Vendor) {
    setManageProductsFor(v);
    setProductSearch('');
  }

  function closeManageProducts() {
    setManageProductsFor(null);
    setProductSearch('');
  }

  const canSave = !!editing?.userId && editing.businessName.trim().length > 0 && editing.businessAddress.trim().length > 0;

  return (
    <>
      <PageHeader
        title="Vendors"
        subtitle="Directory of vendor profiles managed by Admin."
        action={
          <Button onClick={openCreate} className="inline-flex items-center gap-2">
            <Plus size={16} /> New vendor profile
          </Button>
        }
      />

      {!vendorsQ.isLoading && !vendorsQ.isError && vendors.length > 0 && (
        <div className="relative mb-5 max-w-sm">
          <Search size={16} className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
          <input
            value={query}
            onChange={e => setQuery(e.target.value)}
            placeholder="Search by business, email or GST"
            className="focus-ring w-full rounded-xl border border-line bg-white py-2.5 pl-10 pr-4 text-sm outline-none"
          />
        </div>
      )}

      <Card>
        {vendorsQ.isLoading ? (
          <div className="space-y-3 p-5">
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
          </div>
        ) : vendorsQ.isError ? (
          <ErrorState message={(vendorsQ.error as Error).message} onRetry={() => vendorsQ.refetch()} />
        ) : vendors.length === 0 ? (
          <div className="p-12 text-center">
            <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-mint">
              <Building2 className="text-leaf" size={24} />
            </div>
            <h3 className="font-bold">No vendor profiles yet</h3>
            <p className="mx-auto mt-1 max-w-sm text-sm text-gray-500">
              Vendor profiles link a user with role <code className="rounded bg-gray-100 px-1 py-0.5">vendor</code> to
              a business name and address. Create the first one to get started.
            </p>
            <Button onClick={openCreate} className="mx-auto mt-5 inline-flex items-center gap-2">
              <Plus size={16} /> New vendor profile
            </Button>
          </div>
        ) : filteredVendors.length === 0 ? (
          <div className="p-10 text-center text-sm text-gray-500">
            No vendors match &ldquo;{query}&rdquo;.
          </div>
        ) : (
          <div className="divide-y divide-line">
            {filteredVendors.map(v => (
              <div key={v.id} className="flex flex-col gap-3 p-5 md:flex-row md:items-center md:justify-between">
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <b className="truncate">{v.businessName}</b>
                    {v.active ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-mint px-2 py-0.5 text-[11px] font-bold uppercase tracking-wide text-leaf">
                        <BadgeCheck size={12} /> Active
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 px-2 py-0.5 text-[11px] font-bold uppercase tracking-wide text-gray-500">
                        <CircleSlash size={12} /> Inactive
                      </span>
                    )}
                  </div>
                  <div className="mt-1 flex items-center gap-1.5 text-sm text-gray-500">
                    <Mail size={13} /> {v.email}
                  </div>
                  <div className="mt-0.5 text-sm text-gray-500">{v.businessAddress || 'No address on file'}</div>
                  <div className="mt-0.5 text-xs text-gray-400">GST: {v.gstNumber || '—'} · User #{v.userId}</div>
                </div>
                <div className="flex shrink-0 gap-2">
                  <Button onClick={() => openManageProducts(v)} className="inline-flex items-center gap-1.5">
                    <Package size={14} /> Manage products
                  </Button>
                  <Button onClick={() => openEdit(v)}>Edit</Button>
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>

      {editing && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-5" onClick={() => setEditing(null)}>
          <div onClick={(e: React.MouseEvent) => e.stopPropagation()} className="w-full max-w-lg">
          <Card className="p-6">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-black">{vendorLookup.has(editing.userId ?? -1) ? 'Edit' : 'Create'} vendor profile</h2>
              <button aria-label="Close" onClick={() => setEditing(null)} className="text-gray-400 hover:text-ink">
                <X size={20} />
              </button>
            </div>

            <div className="mt-5 space-y-4">
              <div>
                <label className="text-sm font-semibold">Vendor user</label>
                <div className="relative mt-1.5">
                  <button
                    type="button"
                    onClick={() => setUserPickerOpen(o => !o)}
                    disabled={!!vendorLookup.has(editing.userId ?? -1)}
                    className="focus-ring flex w-full items-center justify-between rounded-xl border border-line px-3.5 py-2.5 text-left text-sm disabled:cursor-not-allowed disabled:bg-gray-50 disabled:text-gray-500"
                  >
                    {editing.userId ? (
                      <span>
                        {editing.email} <span className="text-gray-400">· #{editing.userId}</span>
                      </span>
                    ) : (
                      <span className="text-gray-400">Select a user with role = vendor</span>
                    )}
                    {!vendorLookup.has(editing.userId ?? -1) && <ChevronDown size={16} className="text-gray-400" />}
                  </button>

                  {userPickerOpen && (
                    <div className="absolute z-10 mt-1.5 w-full rounded-xl border border-line bg-white p-2 shadow-2xl">
                      <div className="relative mb-2">
                        <Search size={14} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                        <input
                          autoFocus
                          value={userQuery}
                          onChange={e => setUserQuery(e.target.value)}
                          placeholder="Search name or email"
                          className="focus-ring w-full rounded-lg border border-line py-2 pl-8 pr-3 text-sm outline-none"
                        />
                      </div>
                      <div className="max-h-52 overflow-y-auto">
                        {usersQ.isLoading ? (
                          <div className="p-3 text-sm text-gray-400">Loading users…</div>
                        ) : filteredEligibleUsers.length === 0 ? (
                          <div className="p-3 text-sm text-gray-400">
                            No users with role <code>vendor</code> match. Assign the role first from Users.
                          </div>
                        ) : (
                          filteredEligibleUsers.map(u => (
                            <button
                              key={u.id}
                              type="button"
                              onClick={() => pickUser(u)}
                              className="flex w-full flex-col rounded-lg px-3 py-2 text-left text-sm hover:bg-gray-100"
                            >
                              <span className="font-semibold">{u.fullName || 'Unnamed'}</span>
                              <span className="text-xs text-gray-500">
                                {u.email} · #{u.id}
                                {vendorLookup.has(u.id) && ' · already has a profile'}
                              </span>
                            </button>
                          ))
                        )}
                      </div>
                    </div>
                  )}
                </div>
                <p className="mt-1.5 text-xs text-gray-400">
                  Only users already promoted to <code>vendor</code> in Users are eligible.
                </p>
              </div>

              <label className="block text-sm font-semibold">
                Business name
                <input
                  value={editing.businessName}
                  onChange={e => setEditing({...editing, businessName: e.target.value})}
                  placeholder="e.g. Fresh Fields Produce"
                  className="focus-ring mt-1.5 w-full rounded-xl border border-line px-3.5 py-2.5 text-sm outline-none"
                />
              </label>

              <label className="block text-sm font-semibold">
                Business address
                <input
                  value={editing.businessAddress}
                  onChange={e => setEditing({...editing, businessAddress: e.target.value})}
                  placeholder="Street, city, pincode"
                  className="focus-ring mt-1.5 w-full rounded-xl border border-line px-3.5 py-2.5 text-sm outline-none"
                />
              </label>

              <label className="block text-sm font-semibold">
                GST number <span className="font-normal text-gray-400">(optional)</span>
                <input
                  value={editing.gstNumber}
                  onChange={e => setEditing({...editing, gstNumber: e.target.value})}
                  placeholder="22AAAAA0000A1Z5"
                  className="focus-ring mt-1.5 w-full rounded-xl border border-line px-3.5 py-2.5 text-sm outline-none"
                />
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <Button onClick={() => setEditing(null)}>Cancel</Button>
              <Button disabled={!canSave || save.isPending} onClick={() => editing && save.mutate(editing)}>
                {save.isPending ? 'Saving…' : 'Save vendor profile'}
              </Button>
            </div>
          </Card>
          </div>
        </div>
      )}

      {manageProductsFor && (
        <div className="fixed inset-0 z-50 grid place-items-center bg-black/40 p-5" onClick={closeManageProducts}>
          <div onClick={(e: React.MouseEvent) => e.stopPropagation()} className="w-full max-w-xl">
            <Card className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-black">Manage products</h2>
                  <p className="mt-0.5 text-sm text-gray-500">{manageProductsFor.businessName}</p>
                </div>
                <button aria-label="Close" onClick={closeManageProducts} className="text-gray-400 hover:text-ink">
                  <X size={20} />
                </button>
              </div>

              <div className="mt-5">
                <label className="text-sm font-semibold">Assigned products</label>
                <div className="mt-1.5 max-h-48 overflow-y-auto rounded-xl border border-line">
                  {vendorProductsQ.isLoading ? (
                    <div className="p-4"><Skeleton className="h-10" /></div>
                  ) : vendorProductsQ.isError ? (
                    <div className="p-4">
                      <ErrorState message={(vendorProductsQ.error as Error).message} onRetry={() => vendorProductsQ.refetch()} />
                    </div>
                  ) : (vendorProductsQ.data ?? []).length === 0 ? (
                    <div className="p-4 text-sm text-gray-400">No products linked to this vendor yet. Search below to assign one.</div>
                  ) : (
                    <div className="divide-y divide-line">
                      {(vendorProductsQ.data ?? []).map(p => (
                        <div key={p.id} className="flex items-center justify-between gap-3 px-3.5 py-2.5">
                          <div className="min-w-0">
                            <div className="truncate text-sm font-semibold">{p.name}</div>
                            <div className="text-xs text-gray-400">₹{p.price} · stock {p.stock}</div>
                          </div>
                          <button
                            type="button"
                            disabled={unassignProduct.isPending}
                            onClick={() => unassignProduct.mutate({productId: p.id, vendorId: manageProductsFor.id})}
                            className="inline-flex shrink-0 items-center gap-1 rounded-lg border border-line px-2.5 py-1.5 text-xs font-bold text-gray-600 hover:bg-gray-50 disabled:opacity-50"
                          >
                            <Trash2 size={13} /> Remove
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div className="mt-5">
                <label className="text-sm font-semibold">Assign a product</label>
                <div className="relative mt-1.5">
                  <Search size={14} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" />
                  <input
                    value={productSearch}
                    onChange={e => setProductSearch(e.target.value)}
                    placeholder="Search products by name (2+ characters)"
                    className="focus-ring w-full rounded-lg border border-line py-2 pl-8 pr-3 text-sm outline-none"
                  />
                </div>
                <div className="mt-2 max-h-48 overflow-y-auto rounded-xl border border-line">
                  {productSearch.trim().length <= 1 ? (
                    <div className="p-4 text-sm text-gray-400">Type at least 2 characters to search the catalog.</div>
                  ) : productSearchQ.isLoading ? (
                    <div className="p-4"><Skeleton className="h-10" /></div>
                  ) : productSearchQ.isError ? (
                    <div className="p-4">
                      <ErrorState message={(productSearchQ.error as Error).message} onRetry={() => productSearchQ.refetch()} />
                    </div>
                  ) : (productSearchQ.data?.content ?? []).length === 0 ? (
                    <div className="p-4 text-sm text-gray-400">No products match &ldquo;{productSearch}&rdquo;.</div>
                  ) : (
                    <div className="divide-y divide-line">
                      {(productSearchQ.data?.content ?? []).map(p => {
                        const alreadyAssigned = assignedProductIds.has(p.id);
                        return (
                          <div key={p.id} className="flex items-center justify-between gap-3 px-3.5 py-2.5">
                            <div className="min-w-0">
                              <div className="truncate text-sm font-semibold">{p.name}</div>
                              <div className="text-xs text-gray-400">₹{p.price} · stock {p.stock}</div>
                            </div>
                            <button
                              type="button"
                              disabled={alreadyAssigned || assignProduct.isPending}
                              onClick={() => assignProduct.mutate({productId: p.id, vendorId: manageProductsFor.id})}
                              className="shrink-0 rounded-lg bg-ink px-3 py-1.5 text-xs font-bold text-white disabled:cursor-not-allowed disabled:bg-gray-200 disabled:text-gray-400"
                            >
                              {alreadyAssigned ? 'Assigned' : 'Assign'}
                            </button>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>

              <div className="mt-6 flex justify-end">
                <Button onClick={closeManageProducts}>Done</Button>
              </div>
            </Card>
          </div>
        </div>
      )}
    </>
  );
}
