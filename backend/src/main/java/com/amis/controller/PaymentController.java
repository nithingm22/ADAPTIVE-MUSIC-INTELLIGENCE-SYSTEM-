package com.amis.controller;

import com.amis.dto.response.ApiResponse;
import com.amis.model.Payment;
import com.amis.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "http://localhost:3000")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /payments/checkout
     * Body: { plan, cardNumber, expiry, cvv, cardHolder }
     * Validates card, charges, upgrades tier.
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkout(
            @RequestBody Map<String, String> body, Authentication auth) {
        try {
            Map<String, Object> result = paymentService.checkout(
                auth.getName(),
                body.get("plan"),
                body.get("cardNumber"),
                body.get("expiry"),
                body.get("cvv"),
                body.get("cardHolder")
            );
            boolean success = Boolean.TRUE.equals(result.get("success"));
            return success
                ? ResponseEntity.ok(ApiResponse.success("Payment processed", result))
                : ResponseEntity.ok(ApiResponse.error((String) result.get("reason")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /** GET /payments/history — user's transaction log */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Payment>>> history(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getPaymentHistory(auth.getName())));
    }

    /** GET /payments/plans — static plan catalog */
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> plans() {
        return ResponseEntity.ok(ApiResponse.success(List.of(
            Map.of("id","FREE",    "price",0.0,   "label","Free",    "storage","500 MB",  "recommendationLimit",10, "playlistLimit",3,  "smartLimit",10),
            Map.of("id","PREMIUM", "price",9.99,  "label","Premium", "storage","2 GB",    "recommendationLimit",50, "playlistLimit",-1, "smartLimit",50),
            Map.of("id","FAMILY",  "price",14.99, "label","Family",  "storage","5 GB",    "recommendationLimit",50, "playlistLimit",-1, "smartLimit",50)
        )));
    }

    /** GET /user/me — refresh current user profile post-upgrade */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getCurrentUserProfile(auth.getName())));
    }
}
