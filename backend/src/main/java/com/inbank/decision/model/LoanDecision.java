package com.inbank.decision.model;

public class LoanDecision {

    public enum Decision { POSITIVE, NEGATIVE }

    private final Decision decision;
    private final Integer approvedAmount;
    private final Integer approvedPeriod;
    private final String message;

    private LoanDecision(Decision decision, Integer approvedAmount, Integer approvedPeriod, String message) {
        this.decision = decision;
        this.approvedAmount = approvedAmount;
        this.approvedPeriod = approvedPeriod;
        this.message = message;
    }

    public static LoanDecision positive(int amount, int period) {
        return new LoanDecision(Decision.POSITIVE, amount, period,
                "Loan approved for " + amount + " € over " + period + " months.");
    }

    public static LoanDecision negative(String reason) {
        return new LoanDecision(Decision.NEGATIVE, null, null, reason);
    }

    public Decision getDecision() { return decision; }
    public Integer getApprovedAmount() { return approvedAmount; }
    public Integer getApprovedPeriod() { return approvedPeriod; }
    public String getMessage() { return message; }
}
