'use client';

import { useAuth } from '@/contexts/AuthContext';
import { Layout } from '@/components/Layout';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export default function CollectionPointsLayout({ children }: { children: React.ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, isLoading, router]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="spinner w-8 h-8"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return <Layout>{children}</Layout>;
}