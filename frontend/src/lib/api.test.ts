import axios from 'axios';
import {
  api,
  getApiBaseUrl,
  notificationsApi,
  refreshAccessTokenRequest,
  systemConfigApi,
} from './api';

describe('API client configuration', () => {
  const originalApiUrl = process.env.NEXT_PUBLIC_API_URL;
  const originalInternalApiUrl = process.env.NEXT_PUBLIC_INTERNAL_API_URL;

  afterEach(() => {
    if (originalApiUrl === undefined) {
      delete process.env.NEXT_PUBLIC_API_URL;
    } else {
      process.env.NEXT_PUBLIC_API_URL = originalApiUrl;
    }

    if (originalInternalApiUrl === undefined) {
      delete process.env.NEXT_PUBLIC_INTERNAL_API_URL;
    } else {
      process.env.NEXT_PUBLIC_INTERNAL_API_URL = originalInternalApiUrl;
    }

    jest.restoreAllMocks();
  });

  it('uses NEXT_PUBLIC_API_URL in the browser without duplicating /api', () => {
    process.env.NEXT_PUBLIC_API_URL = 'https://api.example.com/api/';

    expect(getApiBaseUrl()).toBe('https://api.example.com/api');
  });

  it('uses the internal API URL during SSR', () => {
    const browserWindow = global.window;
    Object.defineProperty(global, 'window', {
      configurable: true,
      value: undefined,
    });
    process.env.NEXT_PUBLIC_INTERNAL_API_URL = 'http://backend:8080/api/';

    expect(getApiBaseUrl()).toBe('http://backend:8080/api');

    Object.defineProperty(global, 'window', {
      configurable: true,
      value: browserWindow,
    });
  });

  it('uses /api as fallback', () => {
    delete process.env.NEXT_PUBLIC_API_URL;
    delete process.env.NEXT_PUBLIC_INTERNAL_API_URL;

    expect(getApiBaseUrl()).toBe('/api');
  });

  it('uses the notification routes exposed by the backend', () => {
    const getSpy = jest.spyOn(api, 'get').mockResolvedValue({} as never);

    notificationsApi.getUnreadCount();
    notificationsApi.getAll();

    expect(getSpy).toHaveBeenNthCalledWith(1, '/notifications/count/unread');
    expect(getSpy).toHaveBeenNthCalledWith(2, '/notifications', { params: undefined });
  });

  it('uses the system configuration routes exposed by the backend', () => {
    const getSpy = jest.spyOn(api, 'get').mockResolvedValue({} as never);
    const postSpy = jest.spyOn(api, 'post').mockResolvedValue({} as never);
    const deleteSpy = jest.spyOn(api, 'delete').mockResolvedValue({} as never);

    systemConfigApi.getAllAsList();
    systemConfigApi.update('BASE_OIL_PRICE', '12.50');
    systemConfigApi.delete('configuration-id');

    expect(getSpy).toHaveBeenCalledWith('/system-config/list');
    expect(postSpy).toHaveBeenCalledWith('/system-config', {
      configKey: 'BASE_OIL_PRICE',
      configValue: '12.50',
    });
    expect(deleteSpy).toHaveBeenCalledWith('/system-config/configuration-id');
  });

  it('refreshes tokens through the same API base URL', async () => {
    process.env.NEXT_PUBLIC_API_URL = 'https://api.example.com/api';
    const postSpy = jest.spyOn(axios, 'post').mockResolvedValue({ data: {} } as never);

    await refreshAccessTokenRequest('refresh-token');

    expect(postSpy).toHaveBeenCalledWith(
      'https://api.example.com/api/auth/refresh',
      null,
      { params: { refreshToken: 'refresh-token' } },
    );
  });
});
