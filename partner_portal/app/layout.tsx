import './globals.css';
import {QueryProvider} from '@/components/query-provider';
import {ToastProvider} from '@/components/toast';

export default function RootLayout({children}:{children:React.ReactNode}){
  return <html lang="en"><body><QueryProvider><ToastProvider>{children}</ToastProvider></QueryProvider></body></html>;
}
