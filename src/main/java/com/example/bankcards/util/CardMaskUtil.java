package com.example.bankcards.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CardMaskUtil {

    private final CardEncryptionUtil cardEncryptionUtil;

    public String decryptAndMask(String encryptedNumber) {
        String plainNumber = cardEncryptionUtil.decrypt(encryptedNumber);
        return mask(plainNumber);
    }

    public String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() != 16) {
            throw new IllegalArgumentException("Masked card number empty or invalid length");
        }
        return "**** **** **** " + cardNumber.substring(12);
    }
}
