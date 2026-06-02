import React, { useState, useContext, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { AuthContext } from '../context/AuthContext';

const API = 'http://localhost:8080';

const PLANS = [
  {
    id: 'FREE', label: 'Free', price: 0, priceLabel: 'Free forever',
    color: '#6366f1', icon: '🎵',
    features: [
      '500 MB offline storage (3 songs max)',
      '10 recommendations per request',
      '3 playlists max',
      '10 songs per smart playlist',
      'View-only collaborative playlists',
      'Basic analytics (trending + streak)',
      'Standard quality (128 kbps)',
    ],
    locked: ['Collaborative editing', 'Advanced analytics', 'Unlimited downloads'],
  },
  {
    id: 'PREMIUM', label: 'Premium', price: 9.99, priceLabel: '$9.99 / month',
    color: '#f59e0b', icon: '⭐', recommended: true,
    features: [
      '2 GB offline storage (450+ songs)',
      'Unlimited offline downloads',
      '50 recommendations per request',
      'Unlimited playlists',
      '50 songs per smart playlist',
      'Full collaborative editing (BL6)',
      'Full analytics — hours, genres, artists (BL8)',
      'High quality audio (320 kbps)',
      'Ad-free experience',
    ],
  },
  {
    id: 'FAMILY', label: 'Family', price: 14.99, priceLabel: '$14.99 / month',
    color: '#22c55e', icon: '👨‍👩‍👧',
    features: [
      '5 GB offline storage (1100+ songs)',
      'Everything in Premium',
      'Up to 5 collaborators per playlist',
      'Shared family analytics dashboard',
    ],
  },
];

const BRAND_ICONS = { VISA: '💳 Visa', MASTERCARD: '💳 Mastercard', AMEX: '💳 Amex', DISCOVER: '💳 Discover', CARD: '💳' };

function detectBrand(num) {
  const n = num.replace(/\s/g, '');
  if (/^4/.test(n))           return 'VISA';
  if (/^5[1-5]|^2[2-7]/.test(n)) return 'MASTERCARD';
  if (/^3[47]/.test(n))       return 'AMEX';
  if (/^6011|^65/.test(n))    return 'DISCOVER';
  return 'CARD';
}

function formatCardNumber(val, brand) {
  const digits = val.replace(/\D/g, '');
  if (brand === 'AMEX') {
    // Amex: 4-6-5
    return digits.replace(/^(\d{0,4})(\d{0,6})(\d{0,5}).*/, (_, a, b, c) =>
      [a, b, c].filter(Boolean).join(' ')).substring(0, 17);
  }
  // Standard: 4-4-4-4
  return digits.replace(/(\d{4})(?=\d)/g, '$1 ').substring(0, 19);
}

function luhn(num) {
  const n = num.replace(/\s/g, '');
  let sum = 0, alt = false;
  for (let i = n.length - 1; i >= 0; i--) {
    let d = parseInt(n[i]);
    if (alt) { d *= 2; if (d > 9) d -= 9; }
    sum += d; alt = !alt;
  }
  return sum % 10 === 0;
}

function isExpiryValid(exp) {
  const [m, y] = exp.split('/').map(Number);
  if (!m || !y || m < 1 || m > 12) return false;
  const now = new Date();
  return new Date(2000 + y, m) > now;
}

export default function PaymentPage() {
  const { auth, setAuth } = useContext(AuthContext);
  const navigate = useNavigate();

  const [step, setStep]         = useState(1); // 1=plans, 2=form, 3=processing, 4=result
  const [selectedPlan, setPlan] = useState(null);
  const [result, setResult]     = useState(null);

  // Card form state
  const [cardNumber, setCardNumber] = useState('');
  const [expiry, setExpiry]         = useState('');
  const [cvv, setCvv]               = useState('');
  const [cardHolder, setHolder]     = useState('');
  const [showCvv, setShowCvv]       = useState(false);
  const [errors, setErrors]         = useState({});
  const [processingStep, setProcStep] = useState(0);

  const brand   = detectBrand(cardNumber);
  const cvvLen  = brand === 'AMEX' ? 4 : 3;
  const currentTier = auth?.subscriptionTier || 'FREE';

  // ── Processing animation ──────────────────────────────────────────────
  const PROC_STEPS = [
    'Connecting to payment processor…',
    'Verifying card details…',
    'Authorising transaction…',
    'Confirming with your bank…',
    'Finalising subscription…',
  ];

  useEffect(() => {
    if (step !== 3) return;
    let i = 0;
    const t = setInterval(() => { setProcStep(p => Math.min(p + 1, PROC_STEPS.length - 1)); i++; if (i >= PROC_STEPS.length) clearInterval(t); }, 500);
    return () => clearInterval(t);
  }, [step]);

  // ── Validation ────────────────────────────────────────────────────────
  function validate() {
    const e = {};
    const clean = cardNumber.replace(/\s/g, '');
    if (!clean) e.cardNumber = 'Card number is required';
    else if (!luhn(clean)) e.cardNumber = 'Invalid card number';
    if (!expiry) e.expiry = 'Required';
    else if (!isExpiryValid(expiry)) e.expiry = 'Card expired or invalid';
    if (!cvv) e.cvv = 'Required';
    else if (cvv.length !== cvvLen) e.cvv = `Must be ${cvvLen} digits`;
    if (!cardHolder.trim()) e.cardHolder = 'Name on card is required';
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  // ── Submit ────────────────────────────────────────────────────────────
  async function handlePay() {
    if (!validate()) return;
    setStep(3); setProcStep(0);

    // Simulate 2.5s processing delay
    await new Promise(r => setTimeout(r, 2500));

    try {
      const res = await axios.post(`${API}/payments/checkout`, {
        plan: selectedPlan.id,
        cardNumber: cardNumber.replace(/\s/g, ''),
        expiry, cvv, cardHolder,
      }, { headers: { Authorization: `Bearer ${auth.token}` } });

      const data = res.data.data;
      setResult(data);

      if (data?.success) {
        // Refresh auth tier immediately — no re-login needed
        setAuth({ ...auth, subscriptionTier: data.newTier });
      }
      setStep(4);
    } catch (err) {
      setResult({ success: false, reason: err.response?.data?.message || 'Payment failed. Please try again.' });
      setStep(4);
    }
  }

  // ── Card input handlers ───────────────────────────────────────────────
  function handleCardInput(val) {
    const b = detectBrand(val);
    setCardNumber(formatCardNumber(val, b));
  }

  function handleExpiryInput(val) {
    let v = val.replace(/\D/g, '');
    if (v.length >= 3) v = v.substring(0, 2) + '/' + v.substring(2, 4);
    setExpiry(v);
  }

  const tax = selectedPlan ? +(selectedPlan.price * 0.18).toFixed(2) : 0;
  const total = selectedPlan ? +(selectedPlan.price + tax).toFixed(2) : 0;

  // ── STEP 1: Plan Selection ────────────────────────────────────────────
  if (step === 1) return (
    <div style={s.page}>
      <h1 style={s.title}>Choose Your Plan</h1>
      <p style={s.subtitle}>Upgrade anytime. Cancel anytime. Billed monthly.</p>

      <div style={s.planGrid}>
        {PLANS.map(plan => {
          const isCurrent = plan.id === currentTier;
          const isSelected = selectedPlan?.id === plan.id;
          return (
            <div key={plan.id} style={{
              ...s.planCard,
              border: `2px solid ${isSelected ? plan.color : isCurrent ? '#475569' : '#334155'}`,
              background: isSelected ? plan.color + '15' : '#1e293b',
              position: 'relative',
            }}>
              {plan.recommended && <div style={{ ...s.recommendedBadge, background: plan.color }}>Most Popular</div>}
              {isCurrent && <div style={s.currentBadge}>Your Plan</div>}

              <div style={{ ...s.planIcon, background: plan.color + '22' }}>{plan.icon}</div>
              <h2 style={{ ...s.planName, color: plan.color }}>{plan.label}</h2>
              <div style={s.planPrice}>{plan.priceLabel}</div>

              <ul style={s.featureList}>
                {plan.features.map((f, i) => (
                  <li key={i} style={s.featureItem}>
                    <span style={{ color: plan.color }}>✓</span> {f}
                  </li>
                ))}
                {plan.locked?.map((f, i) => (
                  <li key={i} style={{ ...s.featureItem, color: '#475569' }}>
                    <span>✗</span> {f}
                  </li>
                ))}
              </ul>

              {plan.price === 0
                ? <div style={{ ...s.planBtn, background: '#334155', color: '#64748b', cursor: 'default' }}>Current Plan</div>
                : isCurrent
                  ? <div style={{ ...s.planBtn, background: '#334155', color: '#64748b', cursor: 'default' }}>Active</div>
                  : <button style={{ ...s.planBtn, background: plan.color }}
                      onClick={() => { setPlan(plan); setStep(2); }}>
                      Choose {plan.label} →
                    </button>
              }
            </div>
          );
        })}
      </div>

      <div style={s.testCards}>
        <p style={s.testTitle}>🧪 Test Card Numbers</p>
        <div style={s.testGrid}>
          {[
            { num: '4242 4242 4242 4242', brand: 'Visa', result: '✅ Always succeeds' },
            { num: '5555 5555 5555 4444', brand: 'Mastercard', result: '✅ Always succeeds' },
            { num: '3714 496353 98431', brand: 'Amex', result: '✅ Always succeeds' },
            { num: '4000 0000 0000 0002', brand: 'Visa', result: '❌ Always declined' },
          ].map((c, i) => (
            <div key={i} style={s.testCard}>
              <code style={s.testNum}>{c.num}</code>
              <span style={s.testBrand}>{c.brand}</span>
              <span style={s.testResult}>{c.result}</span>
            </div>
          ))}
        </div>
        <p style={s.testNote}>Use any future expiry (e.g. 12/28), any 3-digit CVV, any name.</p>
      </div>
    </div>
  );

  // ── STEP 2: Payment Form ──────────────────────────────────────────────
  if (step === 2) return (
    <div style={s.checkoutPage}>
      {/* Left: form */}
      <div style={s.formCol}>
        <button style={s.backBtn} onClick={() => setStep(1)}>← Back</button>
        <h1 style={s.title}>Payment Details</h1>
        <p style={s.subtitle}>Your payment is secured with 256-bit SSL encryption 🔒</p>

        {/* Card number */}
        <div style={s.fieldGroup}>
          <label style={s.label}>Card Number</label>
          <div style={s.cardInputWrap}>
            <input style={{ ...s.input, ...(errors.cardNumber ? s.inputErr : {}) }}
              placeholder="1234 5678 9012 3456"
              value={cardNumber}
              onChange={e => handleCardInput(e.target.value)}
              maxLength={brand === 'AMEX' ? 17 : 19}
              inputMode="numeric"
            />
            {cardNumber && <span style={s.brandTag}>{BRAND_ICONS[brand]}</span>}
          </div>
          {errors.cardNumber && <p style={s.err}>{errors.cardNumber}</p>}
        </div>

        <div style={s.row}>
          {/* Expiry */}
          <div style={{ ...s.fieldGroup, flex: 1 }}>
            <label style={s.label}>Expiry</label>
            <input style={{ ...s.input, ...(errors.expiry ? s.inputErr : {}) }}
              placeholder="MM/YY" value={expiry} maxLength={5}
              onChange={e => handleExpiryInput(e.target.value)}
              inputMode="numeric"
            />
            {errors.expiry && <p style={s.err}>{errors.expiry}</p>}
          </div>
          {/* CVV */}
          <div style={{ ...s.fieldGroup, flex: 1 }}>
            <label style={s.label}>CVV {brand === 'AMEX' ? '(4 digits)' : '(3 digits)'}</label>
            <div style={s.cardInputWrap}>
              <input style={{ ...s.input, ...(errors.cvv ? s.inputErr : {}) }}
                placeholder={'•'.repeat(cvvLen)} value={cvv} maxLength={cvvLen}
                type={showCvv ? 'text' : 'password'}
                onChange={e => setCvv(e.target.value.replace(/\D/g, '').substring(0, cvvLen))}
                inputMode="numeric"
              />
              <button style={s.eyeBtn} onClick={() => setShowCvv(!showCvv)} type="button">
                {showCvv ? '🙈' : '👁️'}
              </button>
            </div>
            {errors.cvv && <p style={s.err}>{errors.cvv}</p>}
          </div>
        </div>

        {/* Cardholder */}
        <div style={s.fieldGroup}>
          <label style={s.label}>Name on Card</label>
          <input style={{ ...s.input, ...(errors.cardHolder ? s.inputErr : {}) }}
            placeholder="Full name as on card"
            value={cardHolder}
            onChange={e => setHolder(e.target.value)}
          />
          {errors.cardHolder && <p style={s.err}>{errors.cardHolder}</p>}
        </div>

        <div style={s.securityRow}>
          <span>🔒 SSL Secured</span>
          <span>🛡️ PCI Compliant</span>
          <span>🔄 Cancel Anytime</span>
        </div>

        <button style={{ ...s.payBtn, background: selectedPlan.color }} onClick={handlePay}>
          Pay {total > 0 ? `$${total.toFixed(2)}` : 'Nothing'} — Activate {selectedPlan.label}
        </button>
      </div>

      {/* Right: Order Summary */}
      <div style={s.summaryCol}>
        <div style={s.summaryBox}>
          <h3 style={s.summaryTitle}>Order Summary</h3>

          <div style={{ ...s.planIcon, background: selectedPlan.color + '22', margin: '0 auto 12px', fontSize: '32px', width: '56px', height: '56px' }}>
            {selectedPlan.icon}
          </div>
          <div style={{ textAlign: 'center', marginBottom: '20px' }}>
            <div style={{ ...s.planName, color: selectedPlan.color, fontSize: '20px' }}>{selectedPlan.label}</div>
            <div style={{ color: '#94a3b8', fontSize: '13px' }}>Monthly subscription</div>
          </div>

          <div style={s.lineItem}><span>Subtotal</span><span>${selectedPlan.price.toFixed(2)}</span></div>
          <div style={s.lineItem}><span>Tax (18% GST)</span><span>${tax.toFixed(2)}</span></div>
          <div style={{ ...s.lineItem, ...s.totalLine }}>
            <span>Total due today</span>
            <span style={{ color: selectedPlan.color }}>${total.toFixed(2)}</span>
          </div>

          <div style={s.summaryFeatures}>
            {selectedPlan.features.slice(0, 4).map((f, i) => (
              <div key={i} style={s.summaryFeature}>
                <span style={{ color: selectedPlan.color }}>✓</span> {f}
              </div>
            ))}
          </div>

          <div style={s.guarantee}>
            <span style={{ fontSize: '20px' }}>💰</span>
            <div>
              <div style={{ color: '#f1f5f9', fontWeight: 600, fontSize: '13px' }}>30-Day Money Back</div>
              <div style={{ color: '#64748b', fontSize: '12px' }}>No questions asked</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  // ── STEP 3: Processing ────────────────────────────────────────────────
  if (step === 3) return (
    <div style={s.centerPage}>
      <div style={s.processingBox}>
        <div style={s.spinner} />
        <h2 style={{ color: '#f1f5f9', marginBottom: '8px' }}>Processing Payment</h2>
        <p style={{ color: '#94a3b8', fontSize: '14px', marginBottom: '28px' }}>
          Please don't close this window
        </p>
        <div style={s.procSteps}>
          {PROC_STEPS.map((step, i) => (
            <div key={i} style={{
              ...s.procStep,
              color: i <= processingStep ? '#e2e8f0' : '#475569',
              fontWeight: i === processingStep ? 600 : 400,
            }}>
              <span style={{ color: i < processingStep ? '#22c55e' : i === processingStep ? '#f59e0b' : '#475569' }}>
                {i < processingStep ? '✓' : i === processingStep ? '▶' : '○'}
              </span>
              {step}
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  // ── STEP 4: Result ────────────────────────────────────────────────────
  if (step === 4) return (
    <div style={s.centerPage}>
      {result?.success ? (
        <div style={s.resultBox}>
          <div style={s.successIcon}>🎉</div>
          <h2 style={{ color: '#22c55e', fontSize: '24px', marginBottom: '6px' }}>Payment Successful!</h2>
          <p style={{ color: '#94a3b8', marginBottom: '24px' }}>
            You are now on the <strong style={{ color: selectedPlan?.color }}>{result.plan}</strong> plan.
            All premium features are now unlocked.
          </p>

          {/* Receipt */}
          <div style={s.receipt}>
            <div style={s.receiptTitle}>🧾 Transaction Receipt</div>
            {[
              ['Transaction ID', result.transactionId],
              ['Plan', result.plan],
              ['Amount Charged', `$${result.amount?.toFixed(2)}`],
              ['Card', `${result.cardBrand} •••• ${result.cardLastFour}`],
              ['Date', new Date(result.paidAt).toLocaleString()],
            ].map(([k, v]) => (
              <div key={k} style={s.receiptRow}>
                <span style={{ color: '#64748b' }}>{k}</span>
                <span style={{ color: '#e2e8f0', fontWeight: 500 }}>{v}</span>
              </div>
            ))}
          </div>

          <div style={s.resultActions}>
            <button style={{ ...s.payBtn, background: selectedPlan?.color, maxWidth: '200px' }}
              onClick={() => navigate('/dashboard')}>
              🎵 Start Listening
            </button>
            <button style={{ ...s.payBtn, background: '#334155', maxWidth: '200px' }}
              onClick={() => navigate('/invoices')}>
              View Invoices
            </button>
          </div>
        </div>
      ) : (
        <div style={s.resultBox}>
          <div style={s.failIcon}>❌</div>
          <h2 style={{ color: '#ef4444', fontSize: '22px', marginBottom: '6px' }}>Payment Failed</h2>
          <p style={{ color: '#94a3b8', marginBottom: '24px' }}>{result?.reason || 'An error occurred.'}</p>
          <div style={s.resultActions}>
            <button style={{ ...s.payBtn, background: '#6366f1', maxWidth: '200px' }}
              onClick={() => { setStep(2); setErrors({}); }}>
              Try Again
            </button>
            <button style={{ ...s.payBtn, background: '#334155', maxWidth: '200px' }}
              onClick={() => setStep(1)}>
              Choose Different Plan
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ── Styles ─────────────────────────────────────────────────────────────────
const s = {
  page: { padding: '32px 24px', color: '#f1f5f9', maxWidth: '1000px', margin: '0 auto' },
  checkoutPage: { display: 'flex', gap: '32px', padding: '32px 24px', color: '#f1f5f9', maxWidth: '960px', margin: '0 auto', flexWrap: 'wrap' },
  centerPage: { display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '80vh', padding: '24px' },
  title: { fontSize: '26px', fontWeight: 700, margin: '0 0 6px' },
  subtitle: { color: '#94a3b8', fontSize: '14px', marginBottom: '28px' },

  planGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px', marginBottom: '36px' },
  planCard: { borderRadius: '14px', padding: '24px', display: 'flex', flexDirection: 'column', gap: '12px' },
  recommendedBadge: { position: 'absolute', top: '-12px', left: '50%', transform: 'translateX(-50%)', color: '#fff', fontSize: '11px', fontWeight: 700, padding: '4px 12px', borderRadius: '99px' },
  currentBadge: { position: 'absolute', top: '12px', right: '12px', background: '#334155', color: '#94a3b8', fontSize: '10px', fontWeight: 700, padding: '2px 8px', borderRadius: '4px' },
  planIcon: { width: '48px', height: '48px', borderRadius: '12px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '24px' },
  planName: { fontSize: '22px', fontWeight: 700, margin: 0 },
  planPrice: { color: '#94a3b8', fontSize: '14px' },
  featureList: { listStyle: 'none', padding: 0, margin: 0, flex: 1 },
  featureItem: { color: '#cbd5e1', fontSize: '13px', padding: '3px 0', display: 'flex', gap: '8px' },
  planBtn: { padding: '12px', borderRadius: '8px', border: 'none', color: '#fff', fontWeight: 600, fontSize: '14px', cursor: 'pointer', textAlign: 'center', marginTop: 'auto' },

  testCards: { background: '#1e293b', borderRadius: '12px', padding: '20px', border: '1px solid #334155' },
  testTitle: { color: '#94a3b8', fontWeight: 600, fontSize: '14px', marginBottom: '12px' },
  testGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '10px', marginBottom: '10px' },
  testCard: { background: '#0f172a', borderRadius: '8px', padding: '10px 12px', display: 'flex', flexDirection: 'column', gap: '4px' },
  testNum: { color: '#818cf8', fontSize: '13px', fontFamily: 'monospace' },
  testBrand: { color: '#64748b', fontSize: '11px' },
  testResult: { fontSize: '12px', color: '#94a3b8' },
  testNote: { color: '#475569', fontSize: '12px', margin: 0 },

  formCol: { flex: '1 1 340px', minWidth: 0 },
  summaryCol: { flex: '0 0 280px' },
  backBtn: { background: 'none', border: 'none', color: '#6366f1', cursor: 'pointer', fontSize: '14px', padding: 0, marginBottom: '16px' },
  fieldGroup: { marginBottom: '16px' },
  label: { display: 'block', color: '#cbd5e1', fontSize: '13px', fontWeight: 500, marginBottom: '6px' },
  input: { width: '100%', padding: '12px 14px', borderRadius: '8px', border: '1px solid #475569', background: '#0f172a', color: '#f1f5f9', fontSize: '14px', boxSizing: 'border-box', outline: 'none' },
  inputErr: { border: '1px solid #ef4444' },
  cardInputWrap: { position: 'relative' },
  brandTag: { position: 'absolute', right: '12px', top: '50%', transform: 'translateY(-50%)', fontSize: '12px', color: '#94a3b8', pointerEvents: 'none' },
  eyeBtn: { position: 'absolute', right: '10px', top: '50%', transform: 'translateY(-50%)', background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px' },
  err: { color: '#f87171', fontSize: '12px', marginTop: '4px' },
  row: { display: 'flex', gap: '12px' },
  securityRow: { display: 'flex', gap: '16px', color: '#64748b', fontSize: '12px', marginBottom: '20px', flexWrap: 'wrap' },
  payBtn: { width: '100%', padding: '14px', borderRadius: '10px', border: 'none', color: '#fff', fontWeight: 700, fontSize: '15px', cursor: 'pointer' },

  summaryBox: { background: '#1e293b', borderRadius: '14px', padding: '24px', border: '1px solid #334155', position: 'sticky', top: '20px' },
  summaryTitle: { color: '#f1f5f9', fontWeight: 700, fontSize: '16px', marginBottom: '16px' },
  lineItem: { display: 'flex', justifyContent: 'space-between', color: '#94a3b8', fontSize: '14px', marginBottom: '8px' },
  totalLine: { borderTop: '1px solid #334155', paddingTop: '10px', marginTop: '4px', color: '#f1f5f9', fontWeight: 700, fontSize: '15px' },
  summaryFeatures: { background: '#0f172a', borderRadius: '8px', padding: '12px', margin: '16px 0' },
  summaryFeature: { color: '#94a3b8', fontSize: '12px', padding: '3px 0', display: 'flex', gap: '8px' },
  guarantee: { display: 'flex', alignItems: 'center', gap: '12px', background: '#052e16', border: '1px solid #166534', borderRadius: '8px', padding: '12px' },

  processingBox: { background: '#1e293b', borderRadius: '16px', padding: '40px', textAlign: 'center', border: '1px solid #334155', maxWidth: '400px', width: '100%' },
  spinner: { width: '56px', height: '56px', border: '4px solid #334155', borderTop: '4px solid #6366f1', borderRadius: '50%', animation: 'spin 0.8s linear infinite', margin: '0 auto 20px' },
  procSteps: { display: 'flex', flexDirection: 'column', gap: '10px', textAlign: 'left' },
  procStep: { display: 'flex', alignItems: 'center', gap: '10px', fontSize: '14px', transition: 'color 0.3s' },

  resultBox: { background: '#1e293b', borderRadius: '16px', padding: '40px', textAlign: 'center', border: '1px solid #334155', maxWidth: '520px', width: '100%' },
  successIcon: { fontSize: '56px', marginBottom: '12px' },
  failIcon: { fontSize: '56px', marginBottom: '12px' },
  receipt: { background: '#0f172a', borderRadius: '10px', padding: '16px', marginBottom: '24px', textAlign: 'left' },
  receiptTitle: { color: '#94a3b8', fontWeight: 600, fontSize: '13px', marginBottom: '12px' },
  receiptRow: { display: 'flex', justifyContent: 'space-between', fontSize: '13px', padding: '6px 0', borderBottom: '1px solid #1e293b' },
  resultActions: { display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' },
};

// Inject spinner CSS once
if (!document.getElementById('amis-spin-css')) {
  const style = document.createElement('style');
  style.id = 'amis-spin-css';
  style.textContent = '@keyframes spin { to { transform: rotate(360deg); } }';
  document.head.appendChild(style);
}
