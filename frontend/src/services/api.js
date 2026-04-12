import axios from 'axios';

const API_URL = 'http://localhost:8080/hotel-management';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post(`${API_URL}/auth/refresh`, { refreshToken });
        const { accessToken } = response.data;
        localStorage.setItem('accessToken', accessToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: (username, password) =>
    api.post('/auth/login', { username, password }),

  register: (data) =>
    api.post('/auth/register', data),

  logout: () =>
    api.post('/auth/logout'),

  refresh: (refreshToken) =>
    api.post('/auth/refresh', { refreshToken }),
};

export const userService = {
  getAll: () => api.get('/user'),
  getById: (id) => api.get(`/user/${id}`),
  create: (data) => api.post('/user/create', data),
  update: (id, data) => api.put(`/user/${id}/update`, data),
  delete: (id) => api.delete(`/user/${id}/delete`),
};

export const clientService = {
  getAll: () => api.get('/client'),
  getById: (id) => api.get(`/client/${id}`),
  getByEmail: (email) => api.get(`/client/email/${email}`),
  create: (data) => api.post('/client/create', data),
  update: (id, data) => api.put(`/client/${id}/update`, data),
  delete: (id) => api.delete(`/client/${id}/delete`),
};

export default api;
