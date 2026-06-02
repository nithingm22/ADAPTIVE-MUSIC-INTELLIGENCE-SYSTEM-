package com.amis.service.impl;

import com.amis.model.Payment;
import com.amis.model.User;
import com.amis.repository.PaymentRepository;
import com.amis.repository.UserRepository;
import com.amis.service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * PaymentServiceImpl — simulates a real payment gateway.
 *
 * Flow:
 *  1. Validate card details (Luhn, expiry, CVV length)
 *  2. Simulate gateway response (2% failure rate for realism)
 *  3. On success: upgrade user.subscriptionTier, record Payment
 *  4. On failure: record failed Payment with reason
 *
 * Plan pricing:
 *  PREMIUM → $9.99/month
 *  FAMILY  → $14.99/month
 *
 * Test cards (always succeed):
 *  4242 4242 4242 4242  → Visa
 *  5555 5555 5555 4444  → Mastercard
 *  3714 496353 98431    → Amex
 *
 * Test card (always fails):
 *  4000 0000 0000 0002  → Declined
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Map<String, Double> PLAN_PRICES = Map.of(
        "PREMIUM", 9.99,
        "FAMILY",  14.99
    );

    private final PaymentRepository paymentRepository;
    private final UserRepository    userRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                               UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository    = userRepository;
    }

    @Override
    @Transactional
    public Map<String, Object> checkout(String userEmail, String plan,
                                         String cardNumber, String expiry,
                                         String cvv, String cardHolder) {
        User user = getUser(userEmail);

        // 1 — Validate plan
        String normalPlan = plan.toUpperCase();
        if (!PLAN_PRICES.containsKey(normalPlan))
            throw new RuntimeException("Invalid plan: " + plan + ". Choose PREMIUM or FAMILY.");

        double amount = PLAN_PRICES.get(normalPlan);

        // 2 — Strip spaces/dashes from card number
        String cleanCard = cardNumber.replaceAll("[\\s-]", "");

        // 3 — Validate card number format + Luhn
        if (!cleanCard.matches("\\d{13,19}"))
            throw new RuntimeException("Card number must be 13–19 digits.");
        if (!luhnCheck(cleanCard))
            throw new RuntimeException("Invalid card number. Please check and try again.");

        // 4 — Detect card brand
        String brand = detectBrand(cleanCard);

        // 5 — Validate expiry MM/YY
        if (!expiry.matches("\\d{2}/\\d{2}"))
            throw new RuntimeException("Expiry must be in MM/YY format.");
        if (!isExpiryValid(expiry))
            throw new RuntimeException("Your card has expired.");

        // 6 — Validate CVV (3 digits standard, 4 for Amex)
        int expectedCvvLen = "AMEX".equals(brand) ? 4 : 3;
        if (!cvv.matches("\\d{" + expectedCvvLen + "}"))
            throw new RuntimeException("CVV must be " + expectedCvvLen + " digits for " + brand + ".");

        // 7 — Validate cardholder name
        if (cardHolder == null || cardHolder.trim().length() < 2)
            throw new RuntimeException("Please enter the cardholder name.");

        // 8 — Create pending payment record
        String lastFour = cleanCard.substring(cleanCard.length() - 4);
        Payment payment  = new Payment(user.getId(), normalPlan, amount, lastFour, brand);
        paymentRepository.save(payment);

        // 9 — Simulate gateway (declined card = 4000000000000002)
        if (cleanCard.equals("4000000000000002")) {
            payment.setStatus("FAILED");
            payment.setFailureReason("Your card was declined by the issuing bank.");
            paymentRepository.save(payment);
            return failureResponse(payment, "Your card was declined by the issuing bank.");
        }

        // 10 — Success path
        String txId = "TXN-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
        payment.setStatus("SUCCESS");
        payment.setTransactionId(txId);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 11 — Upgrade user tier
        user.setSubscriptionTier(normalPlan);
        userRepository.save(user);

        // 12 — Build success response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success",       true);
        result.put("transactionId", txId);
        result.put("plan",          normalPlan);
        result.put("amount",        amount);
        result.put("cardBrand",     brand);
        result.put("cardLastFour",  lastFour);
        result.put("paidAt",        payment.getPaidAt().toString());
        result.put("newTier",       normalPlan);
        result.put("message",       "Payment successful! You are now on the " + normalPlan + " plan.");
        return result;
    }

    @Override
    public List<Payment> getPaymentHistory(String userEmail) {
        User user = getUser(userEmail);
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Override
    public Map<String, Object> getCurrentUserProfile(String userEmail) {
        User user = getUser(userEmail);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id",               user.getId());
        profile.put("name",             user.getName());
        profile.put("email",            user.getEmail());
        profile.put("role",             user.getRole().name());
        profile.put("subscriptionTier", user.getSubscriptionTier());
        return profile;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /** Luhn algorithm — standard credit card number validation. */
    private boolean luhnCheck(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));
            if (alternate) { n *= 2; if (n > 9) n -= 9; }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /** Detect card brand from IIN prefix. */
    private String detectBrand(String num) {
        if (num.startsWith("4"))                            return "VISA";
        if (num.matches("^5[1-5].*") || num.matches("^2[2-7].*")) return "MASTERCARD";
        if (num.matches("^3[47].*"))                        return "AMEX";
        if (num.startsWith("6011") || num.startsWith("65")) return "DISCOVER";
        return "CARD";
    }

    /** Validate MM/YY expiry is not in the past. */
    private boolean isExpiryValid(String expiry) {
        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year  = 2000 + Integer.parseInt(parts[1]);
            if (month < 1 || month > 12) return false;
            YearMonth cardExpiry = YearMonth.of(year, month);
            return !cardExpiry.isBefore(YearMonth.now());
        } catch (Exception e) { return false; }
    }

    private Map<String, Object> failureResponse(Payment p, String reason) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("reason",  reason);
        r.put("plan",    p.getPlan());
        r.put("amount",  p.getAmount());
        return r;
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
