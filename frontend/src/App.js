import React, { useContext } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, AuthContext } from './context/AuthContext';
import { PlayerProvider } from './context/PlayerContext';
import Layout from './components/Layout';
import MusicPlayer from './components/MusicPlayer';

// Pages
import LoginPage         from './pages/LoginPage';
import RegisterPage      from './pages/RegisterPage';
import DashboardPage     from './pages/DashboardPage';
import DiscoverPage      from './pages/DiscoverPage';
import PlaylistsPage     from './pages/PlaylistsPage';
import PlaylistView      from './pages/PlaylistView';
import AnalyticsPage     from './pages/AnalyticsPage';
import AdminPage         from './pages/AdminPage';
import SmartPlaylistPage from './pages/SmartPlaylistPage';
import CollaborativePage from './pages/CollaborativePage';
import OfflinePage       from './pages/OfflinePage';
import PlayAnalyticsPage from './pages/PlayAnalyticsPage';
import PaymentPage        from './pages/PaymentPage';
import InvoicePage        from './pages/InvoicePage';
import SongsReferencePage from './pages/SongsReferencePage';

function PrivateRoute({ children, adminOnly = false }) {
  const { auth } = useContext(AuthContext);
  if (!auth) return <Navigate to="/login" replace />;
  if (adminOnly && auth.role !== 'ADMIN') return <Navigate to="/dashboard" replace />;
  return children;
}

function AppRoutes() {
  const { auth } = useContext(AuthContext);

  return (
    <BrowserRouter>
      <Routes>
        {/* Public */}
        <Route path="/login"    element={auth ? <Navigate to="/dashboard" /> : <LoginPage />} />
        <Route path="/register" element={auth ? <Navigate to="/dashboard" /> : <RegisterPage />} />

        {/* Protected — wrapped in Layout + Player */}
        <Route path="/*" element={
          <PrivateRoute>
            <PlayerProvider>
              <Layout>
                <Routes>
                  <Route path="dashboard"       element={<DashboardPage />} />
                  <Route path="discover"        element={<DiscoverPage />} />
                  <Route path="playlists"       element={<PlaylistsPage />} />
                  <Route path="playlists/:id"   element={<PlaylistView />} />
                  <Route path="analytics"       element={<AnalyticsPage />} />
                  <Route path="smart-playlist"  element={<SmartPlaylistPage />} />
                  <Route path="collaborative"   element={<CollaborativePage />} />
                  <Route path="offline"         element={<OfflinePage />} />
                  <Route path="play-analytics"  element={<PlayAnalyticsPage />} />
                  <Route path="songs-reference" element={<SongsReferencePage />} />
                  <Route path="payment"         element={<PaymentPage />} />
                  <Route path="invoices"        element={<InvoicePage />} />
                  <Route path="admin" element={
                    <PrivateRoute adminOnly>
                      <AdminPage />
                    </PrivateRoute>
                  } />
                  <Route path="*" element={<Navigate to="dashboard" />} />
                </Routes>
                <MusicPlayer />
              </Layout>
            </PlayerProvider>
          </PrivateRoute>
        } />

        <Route path="/" element={<Navigate to="/dashboard" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}
