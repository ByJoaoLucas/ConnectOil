import { ProtectedLayout } from '@/components/ProtectedLayout';

export default function IncidentsLayout({ children }: { children: React.ReactNode }) {
  return <ProtectedLayout>{children}</ProtectedLayout>;
}
