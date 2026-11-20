import { ProtectedLayout } from '@/components/ProtectedLayout';

export default function CollectorLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedLayout>{children}</ProtectedLayout>;
}
