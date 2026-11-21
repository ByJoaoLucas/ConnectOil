'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { USER_ROLE_LABELS } from '@/types';
import {
  LayoutDashboard,
  MapPin,
  Calendar,
  Package,
  Users,
  BarChart3,
  User,
  LogOut,
  Menu,
  X,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';

const navigation = [
  { name: 'Dashboard', href: '/dashboard', icon: LayoutDashboard, roles: ['ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT'] },
  { name: 'Pontos de Coleta', href: '/collection-points', icon: MapPin, roles: ['ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT'] },
  { name: 'Agendamentos', href: '/schedules', icon: Calendar, roles: ['ADMIN', 'COLLECTOR', 'ESTABLISHMENT'] },
  { name: 'Registros', href: '/records', icon: Package, roles: ['ADMIN', 'COLLECTOR', 'ESTABLISHMENT'] },
  { name: 'Meu Perfil', href: '/profile', icon: User, roles: ['ADMIN', 'COLLECTOR', 'CLIENT', 'ESTABLISHMENT'] },
  { name: 'Relatórios', href: '/reports', icon: BarChart3, roles: ['ADMIN'] },
  { name: 'Usuários', href: '/users', icon: Users, roles: ['ADMIN'] },
];

interface LayoutProps {
  children: React.ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);

  const handleLogout = () => {
    logout();
    router.push('/login');
    router.refresh();
  };

  const filteredNavigation = navigation.filter((item) =>
    user ? item.roles.includes(user.role) : false
  );

  return (
    <div className="min-h-screen bg-gray-50">
      {mobileSidebarOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={() => setMobileSidebarOpen(false)}
        />
      )}

      <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : 'sidebar-closed'} ${mobileSidebarOpen ? 'sidebar-open' : ''}`}>
        <div className="flex flex-col h-full">
          <div className="flex items-center justify-between h-16 px-4 border-b border-gray-200">
            <div className="flex items-center space-x-2">
              <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
                <MapPin className="w-5 h-5 text-white" />
              </div>
              {sidebarOpen && (
                <span className="text-xl font-bold text-gray-900">Óleo Coleta</span>
              )}
            </div>
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 lg:hidden"
              aria-label={sidebarOpen ? 'Fechar menu' : 'Abrir menu'}
            >
              {sidebarOpen ? <ChevronLeft className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
            </button>
          </div>

          <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
            {filteredNavigation.map((item) => (
              <Link
                key={item.name}
                href={item.href}
                className={`flex items-center px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  pathname === item.href
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                }`}
                onClick={() => setMobileSidebarOpen(false)}
              >
                <item.icon className={`w-5 h-5 mr-3 ${sidebarOpen ? '' : 'mx-auto'}`} aria-hidden="true" />
                {sidebarOpen && <span>{item.name}</span>}
              </Link>
            ))}
          </nav>

          <div className="p-4 border-t border-gray-200">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center">
                <User className="w-5 h-5 text-primary-600" />
              </div>
              {sidebarOpen && (
                <Link
                  href="/profile"
                  className="flex-1 min-w-0 flex items-center space-x-2 px-2 py-1.5 rounded-lg text-gray-700 hover:bg-gray-100 transition-colors cursor-pointer group"
                  onClick={() => setMobileSidebarOpen(false)}
                  title="Meu Perfil"
                >
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900 truncate group-hover:text-primary-600 transition-colors">{user?.name}</p>
                    <p className="text-xs text-gray-500 truncate">{user?.role ? USER_ROLE_LABELS[user.role as keyof typeof USER_ROLE_LABELS] : ''}</p>
                  </div>
                  <User className="w-4 h-4 text-gray-400 group-hover:text-primary-600 transition-colors opacity-0 group-hover:opacity-100 ml-auto" aria-hidden="true" />
                </Link>
              )}
              <button
                onClick={handleLogout}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                title="Sair"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </div>
          </div>
        </div>
      </aside>

      <div className="main-content">
        <header className="sticky top-0 z-30 bg-white border-b border-gray-200">
          <div className="flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8">
            <div className="flex items-center space-x-4">
              <button
                onClick={() => setMobileSidebarOpen(true)}
                className="lg:hidden p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                aria-label="Abrir menu"
              >
                <Menu className="w-6 h-6" />
              </button>

              <button
                onClick={() => setSidebarOpen(!sidebarOpen)}
                className="hidden lg:flex p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                aria-label={sidebarOpen ? 'Recolher menu' : 'Expandir menu'}
              >
                {sidebarOpen ? <ChevronLeft className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
              </button>
            </div>

            <div className="flex items-center space-x-4">
              <div className="hidden sm:block text-sm text-gray-500">
                {user?.name}
              </div>
            </div>
          </div>
        </header>

        <main className="p-4 sm:p-6 lg:p-8">
          {children}
        </main>
      </div>
    </div>
  );
}