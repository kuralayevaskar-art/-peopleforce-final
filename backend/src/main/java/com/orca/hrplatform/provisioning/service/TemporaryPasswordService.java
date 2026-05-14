package com.orca.hrplatform.provisioning.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TemporaryPasswordService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SYMBOLS = "!@#$%";
    private static final String ALL = UPPER + LOWER + DIGITS + SYMBOLS;

    public String generate() {
        List<Character> chars = new ArrayList<>();
        chars.add(randomChar(UPPER));
        chars.add(randomChar(LOWER));
        chars.add(randomChar(DIGITS));
        chars.add(randomChar(SYMBOLS));

        while (chars.size() < 14) {
            chars.add(randomChar(ALL));
        }

        Collections.shuffle(chars, RANDOM);
        StringBuilder password = new StringBuilder(chars.size());
        for (Character value : chars) {
            password.append(value);
        }
        return password.toString();
    }

    private char randomChar(String source) {
        return source.charAt(RANDOM.nextInt(source.length()));
    }
}
