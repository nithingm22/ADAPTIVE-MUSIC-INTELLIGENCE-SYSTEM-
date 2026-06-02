import axios from 'axios';

const BASE = 'http://localhost:8080';

// ── Bare axios calls used by Auth pages (no token needed) ─────────────────
export const login    = (data) => axios.post(`${BASE}/auth/login`,    data).then(res => res.data.data);
export const register = (data) => axios.post(`${BASE}/auth/register`, data).then(res => res.data.data);

// ── Authenticated client ───────────────────────────────────────────────────
// Creates an axios instance that automatically:
//   1. Attaches the Bearer token from localStorage
//   2. Intercepts UPGRADE_REQUIRED errors and redirects to /payment
export function createClient() {
  const raw  = localStorage.getItem('amis_auth');
  const auth = raw ? JSON.parse(raw) : null;

  const client = axios.create({ baseURL: BASE });

  // Attach token to every request
  client.interceptors.request.use(config => {
    if (auth?.token) config.headers.Authorization = `Bearer ${auth.token}`;
    return config;
  });

  // Global error handler — catch upgrade-required errors from any endpoint
  client.interceptors.response.use(
    res => res,
    err => {
      const msg = err.response?.data?.message || '';
      if (msg.startsWith('UPGRADE_REQUIRED')) {
        // Redirect to payment page — React Router not available here so use window
        if (!window.location.pathname.includes('/payment')) {
          window.location.href = '/payment';
        }
      }
      return Promise.reject(err);
    }
  );

  return client;
}

export default createClient();
