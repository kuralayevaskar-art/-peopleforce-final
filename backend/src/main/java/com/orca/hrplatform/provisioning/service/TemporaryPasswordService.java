package com.orca.hrplatform.provisioning.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
public class TemporaryPasswordService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> WORDS = List.of("Dmu", "Orca", "Start", "People", "Access");
    private static final String SYMBOLS = "!@#$%";

    public String generate() {
        String word = WORDS.get(RANDOM.nextInt(WORDS.size()));
        int number = 1000 + RANDOM.nextInt(9000);
        char symbol = SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length()));
        return word + number + symbol;
    }
}
