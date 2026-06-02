package com.amis.service;

import com.amis.model.Payment;
import java.util.List;
import java.util.Map;

public interface PaymentService {
    Map<String, Object> checkout(String userEmail, String plan,
                                  String cardNumber, String expiry,
                                  String cvv, String cardHolder);
    List<Payment> getPaymentHistory(String userEmail);
    Map<String, Object> getCurrentUserProfile(String userEmail);
}
