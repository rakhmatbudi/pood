package com.restaurant.management.models;
public class RoundingConfig {
    private int roundingBelow;
    private int roundingDigit;
    private String roundingDigitDescription;
    private int roundingNumber;

    public RoundingConfig(int roundingBelow, int roundingDigit, String roundingDigitDescription, int roundingNumber) {
        this.roundingBelow = roundingBelow;
        this.roundingDigit = roundingDigit;
        this.roundingDigitDescription = roundingDigitDescription;
        this.roundingNumber = roundingNumber;
    }

    // Getters
    public int getRoundingBelow() { return roundingBelow; }
    public int getRoundingDigit() { return roundingDigit; }
    public String getRoundingDigitDescription() { return roundingDigitDescription; }
    public int getRoundingNumber() { return roundingNumber; }
}