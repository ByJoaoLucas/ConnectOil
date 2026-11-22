import axios from 'axios';

export const getApiBaseUrl = () => {
  const configuredUrl = typeof window === 'undefined'
    ? process.env.NEXT_PUBLIC_INTERNAL_API_URL || process.env.NEXT_PUBLIC_API_URL
    : process.env.NEXT_PUBLIC_API_URL;
  return (configuredUrl || '/api').replace(/\/+$/, '');
};

export const refreshAccessTokenRequest = (refreshToken: string) =>
  axios.post(`${getApiBaseUrl()}/auth/refresh`, null, {
    params: { refreshToken },
  });

export const api = axios.create({
  baseURL: getApiBaseUrl(),
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
});

// Request interceptor for auth
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('accessToken');
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const response = await refreshAccessTokenRequest(refreshToken);

          const { accessToken, refreshToken: newRefreshToken, user } = response.data;

          localStorage.setItem('accessToken', accessToken);
          localStorage.setItem('refreshToken', newRefreshToken);
          localStorage.setItem('user', JSON.stringify(user));

          if (originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
          }

          return api(originalRequest);
        } catch (refreshError) {
          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          localStorage.removeItem('user');
          if (typeof window !== 'undefined') {
            window.location.href = '/login';
          }
          return Promise.reject(refreshError);
        }
      }
    }

    return Promise.reject(error);
  }
);

// API endpoints
export const authApi = {
  login: (email: string, password: string) =>
    api.post('/auth/login', { email, password }),
  register: (data: any) =>
    api.post('/auth/register', data),
  refreshToken: (refreshToken: string) =>
    api.post('/auth/refresh', null, { params: { refreshToken } }),
  getCurrentUser: () =>
    api.get('/auth/me'),
  changePassword: (currentPassword: string, newPassword: string) =>
    api.put('/auth/password', null, { params: { currentPassword, newPassword } }),
  updateProfile: (data: any) =>
    api.put('/users/me', data),
  deleteAccount: () =>
    api.delete('/auth/account'),
};

export const usersApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/users', { params }),
  getAllList: () =>
    api.get('/users/list'),
  getById: (id: string) =>
    api.get(`/users/${id}`),
  getByRole: (role: string, params?: { page?: number; size?: number }) =>
    api.get(`/users/role/${role}`, { params }),
  getCollectors: () =>
    api.get('/users/collectors'),
  create: (data: any) =>
    api.post('/users', data),
  update: (id: string, data: any) =>
    api.put(`/users/${id}`, data),
  delete: (id: string) =>
    api.delete(`/users/${id}`),
  activate: (id: string) =>
    api.put(`/users/${id}/activate`),
  assignCollector: (id: string) =>
    api.put(`/users/${id}/collector`),
};

export const collectionPointsApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/collection-points', { params }),
  getAllList: () =>
    api.get('/collection-points/list'),
  getById: (id: string) =>
    api.get(`/collection-points/${id}`),
  getByStatus: (status: string, params?: { page?: number; size?: number }) =>
    api.get(`/collection-points/status/${status}`, { params }),
  getByCollector: (collectorId: string, params?: { page?: number; size?: number }) =>
    api.get(`/collection-points/collector/${collectorId}`, { params }),
  getActiveByCollector: (collectorId: string) =>
    api.get(`/collection-points/collector/${collectorId}/active`),
  getNearFull: () =>
    api.get('/collection-points/near-full'),
  getByLocation: (minLat: number, maxLat: number, minLon: number, maxLon: number) =>
    api.get('/collection-points/location', { params: { minLat, maxLat, minLon, maxLon } }),
  getByCityAndState: (city: string, state: string) =>
    api.get(`/collection-points/city/${city}/state/${state}`),
  getUnassigned: () =>
    api.get('/collection-points/unassigned'),
  // US02 - Establishment endpoints
  getMyPoints: (params?: { page?: number; size?: number }) =>
    api.get('/collection-points/my-points', { params }),
  getMyPointsList: () =>
    api.get('/collection-points/my-points/list'),
  create: (data: any) =>
    api.post('/collection-points', data),
  update: (id: string, data: any) =>
    api.put(`/collection-points/${id}`, data),
  delete: (id: string) =>
    api.delete(`/collection-points/${id}`),
  assignCollector: (pointId: string, collectorId: string) =>
    api.put(`/collection-points/${pointId}/collector/${collectorId}`),
  unassignCollector: (pointId: string) =>
    api.delete(`/collection-points/${pointId}/collector`),
  updateVolume: (pointId: string, volumeLiters: number) =>
    api.put(`/collection-points/${pointId}/volume`, null, { params: { volumeLiters } }),
  addVolume: (pointId: string, additionalLiters: number) =>
    api.put(`/collection-points/${pointId}/volume/add`, null, { params: { additionalLiters } }),
  removeVolume: (pointId: string, removedLiters: number) =>
    api.put(`/collection-points/${pointId}/volume/remove`, null, { params: { removedLiters } }),
};

export const schedulesApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/schedules', { params }),
  getAllList: () =>
    api.get('/schedules/list'),
  getById: (id: string) =>
    api.get(`/schedules/${id}`),
  getByCollector: (collectorId: string, params?: { page?: number; size?: number }) =>
    api.get(`/schedules/collector/${collectorId}`, { params }),
  getByCollectorAndStatus: (collectorId: string, status: string) =>
    api.get(`/schedules/collector/${collectorId}/status/${status}`),
  getByPoint: (pointId: string) =>
    api.get(`/schedules/point/${pointId}`),
  getByDate: (date: string) =>
    api.get(`/schedules/date/${date}`),
  getUpcoming: (collectorId: string) =>
    api.get(`/schedules/collector/${collectorId}/upcoming`),
  getOverdue: () =>
    api.get('/schedules/overdue'),
  getByCollectorAndDateRange: (collectorId: string, start: string, end: string) =>
    api.get(`/schedules/collector/${collectorId}/range`, { params: { start, end } }),
  // US03 - Collector endpoint (assigned + available)
  getMySchedules: () =>
    api.get('/schedules/collector/me'),
  // US02 - Establishment endpoints
  getEstablishmentSchedules: (params?: { page?: number; size?: number }) =>
    api.get('/schedules/my-schedules', { params }),
  getEstablishmentSchedulesList: () =>
    api.get('/schedules/my-schedules/list'),
  create: (data: any) =>
    api.post('/schedules', data),
  update: (id: string, data: any) =>
    api.put(`/schedules/${id}`, data),
  confirm: (id: string) =>
    api.put(`/schedules/${id}/confirm`),
  accept: (id: string) =>
    api.put(`/schedules/${id}/accept`),
  start: (id: string) =>
    api.put(`/schedules/${id}/start`),
  arrive: (id: string) =>
    api.put(`/schedules/${id}/arrive`),
  complete: (id: string) =>
    api.put(`/schedules/${id}/complete`),
  cancel: (id: string, reason: string) =>
    api.put(`/schedules/${id}/cancel`, null, { params: { reason } }),
  reschedule: (id: string, newDate: string, newStartTime?: string) =>
    api.put(`/schedules/${id}/reschedule`, null, { params: { newDate, newStartTime } }),
  delete: (id: string) =>
    api.delete(`/schedules/${id}`),
};

export const recordsApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/records', { params }),
  getAllList: () =>
    api.get('/records/list'),
  getById: (id: string) =>
    api.get(`/records/${id}`),
  getByPoint: (pointId: string, params?: { page?: number; size?: number }) =>
    api.get(`/records/point/${pointId}`, { params }),
  getByCollector: (collectorId: string, params?: { page?: number; size?: number }) =>
    api.get(`/records/collector/${collectorId}`, { params }),
  getByCollectorAndDateRange: (collectorId: string, start: string, end: string) =>
    api.get(`/records/collector/${collectorId}/range`, { params: { start, end } }),
  getByDateRange: (start: string, end: string) =>
    api.get('/records/range', { params: { start, end } }),
  getPendingVerification: () =>
    api.get('/records/pending-verification'),
  getPendingVerificationByCollector: (collectorId: string) =>
    api.get(`/records/collector/${collectorId}/pending-verification`),
  create: (data: any) =>
    api.post('/records', data),
  update: (id: string, data: any) =>
    api.put(`/records/${id}`, data),
  verify: (id: string, managerId: string) =>
    api.put(`/records/${id}/verify`, null, { params: { managerId } }),
  delete: (id: string) =>
    api.delete(`/records/${id}`),
  getTotalVolumeByCollector: (collectorId: string, start: string, end: string) =>
    api.get(`/records/reports/volume/collector/${collectorId}`, { params: { start, end } }),
  getTotalVolumeByPoint: (pointId: string, start: string, end: string) =>
    api.get(`/records/reports/volume/point/${pointId}`, { params: { start, end } }),
  getCollectionCountByCollector: (collectorId: string, start: string, end: string) =>
    api.get(`/records/reports/count/collector/${collectorId}`, { params: { start, end } }),
  getAverageQualityByCollector: (collectorId: string, start: string, end: string) =>
    api.get(`/records/reports/quality/collector/${collectorId}`, { params: { start, end } }),
  getDailyVolumeReport: (start: string, end: string) =>
    api.get('/records/reports/daily-volume', { params: { start, end } }),
  getCollectorPerformanceReport: (start: string, end: string) =>
    api.get('/records/reports/collector-performance', { params: { start, end } }),
  getOilTypeVolumeReport: (start: string, end: string) =>
    api.get('/records/reports/oil-type-volume', { params: { start, end } }),
};

export const productsApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/products', { params }),
  getAllList: () =>
    api.get('/products/list'),
  getById: (id: string) =>
    api.get(`/products/${id}`),
  getAvailable: () =>
    api.get('/products/available'),
  getByCategory: (category: string) =>
    api.get(`/products/category/${category}`),
  create: (data: any) =>
    api.post('/products', data),
  update: (id: string, data: any) =>
    api.put(`/products/${id}`, data),
  updateStock: (id: string, quantityChange: number) =>
    api.put(`/products/${id}/stock`, null, { params: { quantityChange } }),
  delete: (id: string) =>
    api.delete(`/products/${id}`),
};

export const exchangeItemsApi = {
  getBySchedule: (scheduleId: string) =>
    api.get(`/exchange-items/schedule/${scheduleId}`),
  createForSchedule: (scheduleId: string, data: any[]) =>
    api.post(`/exchange-items/schedule/${scheduleId}`, data),
  deleteBySchedule: (scheduleId: string) =>
    api.delete(`/exchange-items/schedule/${scheduleId}`),
};

export const incidentsApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/incidents', { params }),
  getAllList: () =>
    api.get('/incidents/list'),
  getById: (id: string) =>
    api.get(`/incidents/${id}`),
  getByPoint: (pointId: string) =>
    api.get(`/incidents/point/${pointId}`),
  getBySchedule: (scheduleId: string) =>
    api.get(`/incidents/schedule/${scheduleId}`),
  getByReporter: (reporterId: string) =>
    api.get(`/incidents/reporter/${reporterId}`),
  getByStatus: (status: string, params?: { page?: number; size?: number }) =>
    api.get(`/incidents/status/${status}`, { params }),
  getRecentByPoint: (pointId: string, since?: string) =>
    api.get(`/incidents/point/${pointId}/recent`, { params: { since } }),
  create: (data: any) =>
    api.post('/incidents', data),
  update: (id: string, data: any) =>
    api.put(`/incidents/${id}`, data),
  resolve: (id: string, resolutionNotes: string) =>
    api.put(`/incidents/${id}/resolve`, null, { params: { resolutionNotes } }),
  delete: (id: string) =>
    api.delete(`/incidents/${id}`),
};

export const notificationsApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/notifications', { params }),
  getMyNotifications: (params?: { page?: number; size?: number }) =>
    api.get('/notifications', { params }),
  getUnreadCount: () =>
    api.get('/notifications/count/unread'),
  markAsRead: (id: string) =>
    api.put(`/notifications/${id}/read`),
  markAllAsRead: () =>
    api.put('/notifications/read-all'),
  deleteNotification: (id: string) =>
    api.delete(`/notifications/${id}`),
};

export const collectorCoverageApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/collector-coverage', { params }),
  getAllList: () =>
    api.get('/collector-coverage/list'),
  getById: (id: string) =>
    api.get(`/collector-coverage/${id}`),
  getByCollector: (collectorId: string) =>
    api.get(`/collector-coverage/collector/${collectorId}`),
  getByCityState: (city: string, state: string) =>
    api.get(`/collector-coverage/city/${city}/state/${state}`),
  create: (data: any) =>
    api.post('/collector-coverage', data),
  update: (id: string, data: any) =>
    api.put(`/collector-coverage/${id}`, data),
  delete: (id: string) =>
    api.delete(`/collector-coverage/${id}`),
};

export const systemConfigApi = {
  getAll: (params?: { page?: number; size?: number; sort?: string }) =>
    api.get('/system-config', { params }),
  getByKey: (key: string) =>
    api.get(`/system-config/${key}`),
  getAllAsList: () =>
    api.get('/system-config/list'),
  create: (data: any) =>
    api.post('/system-config', data),
  update: (configKey: string, configValue: string) =>
    api.post('/system-config', { configKey, configValue }),
  delete: (id: string) =>
    api.delete(`/system-config/${id}`),
};

export const dashboardApi = {
  getStats: () =>
    api.get('/dashboard/stats'),
};

export default api;