package com.urbanclean.property;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Generator for realistic IP addresses for property-based testing.
 */
public class IpAddressGenerator extends Generator<String> {

    public IpAddressGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        // Generate IPv4 address
        int octet1 = random.nextInt(1, 255);
        int octet2 = random.nextInt(0, 255);
        int octet3 = random.nextInt(0, 255);
        int octet4 = random.nextInt(1, 255);
        
        return String.format("%d.%d.%d.%d", octet1, octet2, octet3, octet4);
    }
}
