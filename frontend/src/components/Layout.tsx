'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { USER_ROLE_LABELS } from '@/types';
import { notificationsApi } from '@/lib/api';
import { Notification } from '@/types';
import {
  LayoutDashboard,
  MapPin,
  Calendar,
  Package,
  Users,
  BarChart3,
  User,
  LogOut,
  ChevronLeft,
  ChevronRight,
  Bell,
  Check,
  Loader2,
  Trash2,
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
  const { user, logout, token } = useAuth();
  const router = useRouter();
  const pathname = usePathname();
  const [sidebarOpen, setSidebarOpen] = useState(true);

  // Notification state
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notificationsLoading, setNotificationsLoading] = useState(false);

  const handleLogout = () => {
    logout();
    router.push('/login');
    router.refresh();
  };

  const filteredNavigation = navigation.filter((item) =>
    user ? item.roles.includes(user.role) : false
  );

  // Fetch unread count and notifications
  const fetchUnreadCount = useCallback(async () => {
    if (!user || !token) return;
    try {
      const response = await notificationsApi.getUnreadCount();
      setUnreadCount(response.data || 0);
    } catch (error) {
      console.error('Erro ao buscar contagem de notificações:', error);
    }
  }, [user, token]);

  const fetchNotifications = useCallback(async () => {
    if (!user || !token) return;
    setNotificationsLoading(true);
    try {
      const response = await notificationsApi.getMyNotifications({ size: 20 });
      const data = response.data;
      setNotifications(data.content || data || []);
    } catch (error) {
      console.error('Erro ao buscar notificações:', error);
    } finally {
      setNotificationsLoading(false);
    }
  }, [user, token]);

  const markAsRead = async (id: string) => {
    try {
      await notificationsApi.markAsRead(id);
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, isRead: true } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error('Erro ao marcar notificação como lida:', error);
    }
  };

  const markAllAsRead = async () => {
    try {
      await notificationsApi.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (error) {
      console.error('Erro ao marcar todas como lidas:', error);
    }
  };

  const deleteNotification = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    try {
      await notificationsApi.deleteNotification(id);
      setNotifications(prev => prev.filter(n => n.id !== id));
    } catch (error) {
      console.error('Erro ao excluir notificação:', error);
      alert('Apenas notificações lidas podem ser excluídas');
    }
  };

  // Fetch on mount and when user changes
  useEffect(() => {
    if (user) {
      fetchUnreadCount();
      // Poll for unread count every 30 seconds
      const interval = setInterval(fetchUnreadCount, 30000);
      return () => clearInterval(interval);
    }
  }, [user, fetchUnreadCount]);

  // Show notifications for ESTABLISHMENT, ADMIN, and COLLECTOR roles
  const showNotifications = user && ['ESTABLISHMENT', 'ADMIN', 'COLLECTOR'].includes(user.role);

  const toggleSidebar = () => setSidebarOpen(!sidebarOpen);

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Mobile overlay backdrop */}
      {!sidebarOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 lg:hidden"
          onClick={toggleSidebar}
          aria-hidden="true"
        />
      )}

      {/* Layout container - flex on desktop, stacked on mobile */}
      <div className="layout-container">
        {/* Sidebar */}
        <aside className={`sidebar ${sidebarOpen ? 'sidebar-open' : 'sidebar-closed'}`}>
          <div className="flex flex-col h-full">
            <div className="flex items-center justify-between h-16 px-4 border-b border-gray-200">
              <div className="flex items-center space-x-2">
                <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center flex-shrink-0">
                  <MapPin className="w-5 h-5 text-white" />
                </div>
                {sidebarOpen && (
                  <span className="text-xl font-bold text-gray-900 whitespace-nowrap">Óleo Coleta</span>
                )}
              </div>
              {/* Mobile-only close button inside sidebar */}
              <button
                onClick={toggleSidebar}
                className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 flex-shrink-0 lg:hidden"
                aria-label="Fechar menu"
              >
                <ChevronLeft className="w-5 h-5" />
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
                >
                  <item.icon className={`w-5 h-5 ${sidebarOpen ? 'mr-3' : 'mx-auto'}`} aria-hidden="true" />
                  {sidebarOpen && <span className="whitespace-nowrap">{item.name}</span>}
                </Link>
              ))}
            </nav>

            <div className="p-4 border-t border-gray-200 flex-shrink-0">
              <div className={`flex ${sidebarOpen ? 'flex-row items-center space-x-3' : 'flex-col items-center space-y-2'}`}>
                <div className="w-10 h-10 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0">
                  <User className="w-5 h-5 text-primary-600" />
                </div>
                {sidebarOpen && (
                  <Link
                    href="/profile"
                    className="flex-1 min-w-0 flex items-center space-x-2 px-2 py-1.5 rounded-lg text-gray-700 hover:bg-gray-100 transition-colors cursor-pointer group"
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
                  className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 flex-shrink-0 w-10 h-10"
                  title="Sair"
                >
                  <LogOut className="w-5 h-5" />
                </button>
              </div>
            </div>
          </div>
        </aside>

        {/* Main content area */}
        <div className="main-content">
          <header className="sticky top-0 z-30 bg-white border-b border-gray-200">
            <div className="flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8">
              <div className="flex items-center space-x-4">
                {/* Sidebar toggle button - always visible in header, consistent chevron icon */}
                <button
                  onClick={toggleSidebar}
                  className="p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100"
                  aria-label={sidebarOpen ? 'Recolher menu' : 'Expandir menu'}
                >
                  {sidebarOpen ? <ChevronLeft className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
                </button>
              </div>

              <div className="flex items-center space-x-4">
                {/* Notification Bell - for ESTABLISHMENT, ADMIN, COLLECTOR */}
                {showNotifications && (
                  <div className="relative">
                    <button
                      onClick={() => {
                        setNotificationsOpen(!notificationsOpen);
                        if (!notificationsOpen) {
                          fetchNotifications();
                        }
                      }}
                      className="relative p-2 rounded-lg text-gray-500 hover:text-gray-700 hover:bg-gray-100 transition-colors"
                      aria-label="Notificações"
                      aria-expanded={notificationsOpen}
                    >
                      <Bell className="w-5 h-5" />
                      {unreadCount > 0 && (
                        <span className="absolute -top-1 -right-1 flex items-center justify-center w-5 h-5 bg-red-500 text-white text-xs font-medium rounded-full">
                          {unreadCount > 9 ? '9+' : unreadCount}
                        </span>
                      )}
                    </button>

                    {/* Notifications Dropdown */}
                    {notificationsOpen && (
                      <div className="absolute right-0 top-full mt-2 w-96 bg-white rounded-lg shadow-lg border border-gray-200 z-50">
                        <div className="flex items-center justify-between p-4 border-b border-gray-200">
                          <h3 className="text-lg font-semibold text-gray-900">Notificações</h3>
                          {unreadCount > 0 && (
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                markAllAsRead();
                              }}
                              className="text-sm text-primary-600 hover:text-primary-700"
                            >
                              Marcar todas como lidas
                            </button>
                          )}
                        </div>

                        <div className="max-h-96 overflow-y-auto">
                          {notificationsLoading ? (
                            <div className="flex items-center justify-center p-8">
                              <Loader2 className="w-6 h-6 animate-spin text-primary-600" />
                            </div>
                          ) : notifications.length === 0 ? (
                            <div className="p-8 text-center text-gray-500">
                              <Bell className="w-12 h-12 mx-auto mb-4 text-gray-300" />
                              <p>Nenhuma notificação</p>
                            </div>
                          ) : (
                            <ul className="divide-y divide-gray-100">
                              {notifications.map((notification) => (
                                <li key={notification.id} className={`p-4 ${!notification.isRead ? 'bg-blue-50' : ''}`}>
                                  <div className="flex items-start space-x-3">
                                    <div className="flex-1 min-w-0">
                                      <p className={`text-sm font-medium ${!notification.isRead ? 'text-gray-900' : 'text-gray-600'}`}>
                                        {notification.title}
                                      </p>
                                      <p className="text-sm text-gray-500 mt-1">{notification.message}</p>
                                      <p className="text-xs text-gray-400 mt-2">
                                        {new Date(notification.sentAt).toLocaleString('pt-BR')}
                                      </p>
                                    </div>
                                    {!notification.isRead && (
                                      <button
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          markAsRead(notification.id);
                                        }}
                                        className="p-1 rounded text-gray-400 hover:text-primary-600 hover:bg-primary-50 transition-colors"
                                        aria-label="Marcar como lida"
                                      >
                                        <Check className="w-4 h-4" />
                                      </button>
                                    )}
                                    {notification.isRead && (
                                      <button
                                        onClick={(e) => deleteNotification(notification.id, e)}
                                        className="p-1 rounded text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                                        aria-label="Excluir notificação"
                                      >
                                        <Trash2 className="w-4 h-4" />
                                      </button>
                                    )}
                                  </div>
                                </li>
                              ))}
                            </ul>
                          )}
                        </div>
                      </div>
                    )}

                    {/* Click outside to close */}
                    {notificationsOpen && (
                      <div
                        className="fixed inset-0 z-40"
                        onClick={() => setNotificationsOpen(false)}
                        aria-hidden="true"
                      />
                    )}
                  </div>
                )}

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
    </div>
  );
}