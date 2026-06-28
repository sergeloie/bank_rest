package com.example.bankcards.util;

import org.springframework.stereotype.Component;

@Component
public class CardMaskUtil {

    public String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            throw new RuntimeException("Masked card number empty or invalid length");
        }
        return "**** **** **** " + cardNumber.substring(12);
    }
}
