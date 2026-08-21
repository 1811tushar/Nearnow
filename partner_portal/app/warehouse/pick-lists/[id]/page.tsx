'use client';import {PickDetail} from '@/components/ops';import {useParams} from 'next/navigation';export default function P(){const p=useParams();return <PickDetail id={Number(p.id)}/>}
