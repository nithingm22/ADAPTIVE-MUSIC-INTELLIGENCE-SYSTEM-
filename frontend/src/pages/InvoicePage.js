import React, { useState, useEffect, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';

const API = 'http://localhost:8080';
const TIER_COLOR = { FREE: '#6366f1', PREMIUM: '#f59e0b', FAMILY: '#22c55e' };
const BRAND_ICON = { VISA: '💳', MASTERCARD: '💳', AMEX: '💳', DISCOVER: '💳', CARD: '💳' };

export default function InvoicePage() {
  const { auth } = useContext(AuthContext);
  const navigate = useNavigate();
  const [invoices, setInvoices] = useState([]);
  const [loading, setLoading]   = useState(true);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    if (!auth || !auth.token) {
      setLoading(false);
      return;
    }
    
    axios.get(`${API}/payments/history`, {
      headers: { Authorization: `Bearer ${auth.token}` }
    }).then(r => { setInvoices(r.data.data || []); setLoading(false); })
      .catch(() => setLoading(false));
  }, [auth]);

  if (loading) return <div style={s.page}><p style={{ color: '#64748b' }}>Loading invoices…</p></div>;

  return (
    <div style={s.page}>
      <div style={s.header}>
        <div>
          <h1 style={s.title}>🧾 Billing & Invoices</h1>
          <p style={s.subtitle}>Your complete payment history</p>
        </div>
        <button style={s.upgradeBtn} onClick={() => navigate('/payment')}>
          ⭐ Upgrade Plan
        </button>
      </div>

      {/* Current plan banner */}
      <div style={{ ...s.planBanner, borderColor: TIER_COLOR[auth.subscriptionTier] || '#6366f1' }}>
        <div style={s.bannerLeft}>
          <span style={{ fontSize: '28px' }}>
            {auth.subscriptionTier === 'PREMIUM' ? '⭐' : auth.subscriptionTier === 'FAMILY' ? '👨‍👩‍👧' : '🎵'}
          </span>
          <div>
            <div style={{ color: '#f1f5f9', fontWeight: 700 }}>Current Plan: {auth.subscriptionTier}</div>
            <div style={{ color: '#64748b', fontSize: '13px' }}>
              {auth.subscriptionTier === 'FREE' && 'Upgrade to unlock premium features'}
              {auth.subscriptionTier === 'PREMIUM' && '$9.99/month · All premium features active'}
              {auth.subscriptionTier === 'FAMILY' && '$14.99/month · Full family access active'}
            </div>
          </div>
        </div>
        {auth.subscriptionTier === 'FREE' && (
          <button style={{ ...s.upgradeBtn, fontSize: '13px' }} onClick={() => navigate('/payment')}>
            Upgrade Now →
          </button>
        )}
      </div>

      {invoices.length === 0 ? (
        <div style={s.empty}>
          <div style={{ fontSize: '48px', marginBottom: '12px' }}>📭</div>
          <p style={{ color: '#94a3b8' }}>No payment history yet.</p>
          <button style={s.upgradeBtn} onClick={() => navigate('/payment')}>View Plans</button>
        </div>
      ) : (
        <div style={s.table}>
          {/* Header */}
          <div style={s.tableHead}>
            {['Date', 'Plan', 'Amount', 'Card', 'Status', 'Transaction ID', ''].map(h => (
              <div key={h} style={s.th}>{h}</div>
            ))}
          </div>
          {/* Rows */}
          {invoices.map(inv => {
            const ok = inv.status === 'SUCCESS';
            return (
              <div key={inv.id} style={s.tableRow}>
                <div style={s.td}>{inv.paidAt ? new Date(inv.paidAt).toLocaleDateString() : '—'}</div>
                <div style={s.td}>
                  <span style={{ ...s.planTag, color: TIER_COLOR[inv.plan], background: (TIER_COLOR[inv.plan] || '#6366f1') + '22' }}>
                    {inv.plan}
                  </span>
                </div>
                <div style={s.td}>${inv.amount?.toFixed(2)}</div>
                <div style={s.td}>
                  {inv.cardBrand} •••• {inv.cardLastFour}
                </div>
                <div style={s.td}>
                  <span style={{ ...s.statusTag, background: ok ? '#052e16' : '#450a0a', color: ok ? '#86efac' : '#fca5a5' }}>
                    {ok ? '✓ Paid' : '✗ Failed'}
                  </span>
                </div>
                <div style={{ ...s.td, fontFamily: 'monospace', fontSize: '11px', color: '#64748b' }}>
                  {inv.transactionId || '—'}
                </div>
                <div style={s.td}>
                  {ok && <button style={s.receiptBtn} onClick={() => setSelected(inv)}>Receipt</button>}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Receipt modal */}
      {selected && (
        <div style={s.overlay} onClick={() => setSelected(null)}>
          <div style={s.modal} onClick={e => e.stopPropagation()}>
            <div style={s.modalHeader}>
              <h2 style={{ color: '#f1f5f9', margin: 0 }}>🧾 Receipt</h2>
              <button style={s.closeBtn} onClick={() => setSelected(null)}>✕</button>
            </div>

            <div style={s.receiptBody}>
              <div style={{ textAlign: 'center', marginBottom: '20px' }}>
                <div style={{ fontSize: '40px' }}>🎵</div>
                <div style={{ color: '#f1f5f9', fontWeight: 700, fontSize: '18px' }}>AMIS Music</div>
                <div style={{ color: '#64748b', fontSize: '13px' }}>Adaptive Music Intelligence System</div>
              </div>

              <div style={s.divider} />

              {[
                ['Receipt No.',    selected.transactionId],
                ['Date',          new Date(selected.paidAt).toLocaleString()],
                ['Plan',          selected.plan + ' (Monthly)'],
                ['Cardholder',    auth.name],
                ['Card',          `${selected.cardBrand} ending in ${selected.cardLastFour}`],
                ['Subtotal',      `$${selected.amount?.toFixed(2)}`],
                ['Tax (18%)',     `$${(selected.amount * 0.18).toFixed(2)}`],
                ['Total Charged', `$${(selected.amount * 1.18).toFixed(2)}`],
                ['Status',        '✓ Payment Successful'],
              ].map(([k, v]) => (
                <div key={k} style={s.receiptRow}>
                  <span style={{ color: '#64748b' }}>{k}</span>
                  <span style={{ color: k === 'Total Charged' ? '#22c55e' : '#e2e8f0', fontWeight: k === 'Total Charged' ? 700 : 400 }}>{v}</span>
                </div>
              ))}

              <div style={s.divider} />
              <p style={{ color: '#475569', fontSize: '12px', textAlign: 'center', marginTop: '12px' }}>
                Thank you for subscribing to AMIS. This is your official receipt.<br />
                For support: support@amis.music
              </p>
            </div>

            <button style={s.printBtn} onClick={() => window.print()}>🖨️ Print Receipt</button>
          </div>
        </div>
      )}
    </div>
  );
}

const s = {
  page: { padding: '32px 24px', color: '#f1f5f9', maxWidth: '1000px' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' },
  title: { fontSize: '24px', fontWeight: 700, margin: 0 },
  subtitle: { color: '#94a3b8', fontSize: '14px', marginTop: '4px' },
  upgradeBtn: { padding: '10px 18px', borderRadius: '8px', border: 'none', background: '#6366f1', color: '#fff', fontWeight: 600, fontSize: '14px', cursor: 'pointer' },

  planBanner: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#1e293b', border: '1px solid', borderRadius: '12px', padding: '16px 20px', marginBottom: '24px', flexWrap: 'wrap', gap: '12px' },
  bannerLeft: { display: 'flex', alignItems: 'center', gap: '14px' },

  empty: { textAlign: 'center', padding: '60px 20px' },

  table: { background: '#1e293b', borderRadius: '12px', border: '1px solid #334155', overflow: 'hidden' },
  tableHead: { display: 'grid', gridTemplateColumns: '120px 100px 80px 160px 100px 1fr 80px', background: '#0f172a', padding: '10px 16px', gap: '8px' },
  tableRow: { display: 'grid', gridTemplateColumns: '120px 100px 80px 160px 100px 1fr 80px', padding: '12px 16px', gap: '8px', borderTop: '1px solid #1e293b', alignItems: 'center' },
  th: { color: '#64748b', fontSize: '11px', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' },
  td: { color: '#cbd5e1', fontSize: '13px' },
  planTag: { padding: '2px 8px', borderRadius: '4px', fontSize: '11px', fontWeight: 700 },
  statusTag: { padding: '3px 8px', borderRadius: '4px', fontSize: '12px', fontWeight: 600 },
  receiptBtn: { padding: '4px 10px', borderRadius: '6px', border: '1px solid #475569', background: 'transparent', color: '#94a3b8', fontSize: '12px', cursor: 'pointer' },

  overlay: { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, padding: '20px' },
  modal: { background: '#1e293b', borderRadius: '14px', padding: '28px', maxWidth: '440px', width: '100%', border: '1px solid #334155' },
  modalHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' },
  closeBtn: { background: 'none', border: 'none', color: '#64748b', fontSize: '18px', cursor: 'pointer' },
  receiptBody: {},
  divider: { height: '1px', background: '#334155', margin: '16px 0' },
  receiptRow: { display: 'flex', justifyContent: 'space-between', fontSize: '13px', padding: '5px 0' },
  printBtn: { width: '100%', marginTop: '20px', padding: '11px', borderRadius: '8px', border: 'none', background: '#334155', color: '#f1f5f9', fontWeight: 600, fontSize: '14px', cursor: 'pointer' },
};
