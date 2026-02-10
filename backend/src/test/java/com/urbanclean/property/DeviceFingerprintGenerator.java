package com.urbanclean.property;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generator for device fingerprints for property-based testing.
 */
public class DeviceFingerprintGenerator extends Generator<String> {

    public DeviceFingerprintGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate random fingerprint components
        String userAgent = "UA-" + random.nextInt(1000, 9999);
        String language = random.choose(new String[]{"en-US", "es-ES", "fr-FR", "de-DE", "it-IT"});
        String timezone = random.choose(new String[]{"UTC", "EST", "PST", "CET", "JST"});
        String screen = random.choose(new String[]{"1920x1080", "1366x768", "2560x1440", "3840x2160"});
        
        // Combine and hash
        String combined = userAgent + "|" + language + "|" + timezone + "|" + screen;
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
