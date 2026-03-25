package com.inbank.decision.service;

import com.inbank.decision.exception.InvalidLoanRequestException;
import com.inbank.decision.model.LoanDecision;
import com.inbank.decision.registry.CreditRegistry;
import org.springframework.stereotype.Service;

@Service
public class DecisionEngineService {

    private static final int MIN_AMOUNT = 2000;
    private static final int MAX_AMOUNT = 10000;
    private static final int MIN_PERIOD = 12;
    private static final int MAX_PERIOD = 60;

    private final CreditRegistry creditRegistry;

    public DecisionEngineService(CreditRegistry creditRegistry) {
        this.creditRegistry = creditRegistry;
    }

    public LoanDecision evaluate(String personalCode, int loanAmount, int loanPeriod) {
        validateInputs(personalCode, loanAmount, loanPeriod);

        // Throws InvalidPersonalCodeException if code is not found.
        int creditModifier = creditRegistry.getCreditModifier(personalCode);

        if (creditModifier < 0) {
            return LoanDecision.negative("Applicant has active debt. No loan can be approved.");
        }

        // Evaluate requested input first using the assignment scoring formula.
        if (isAmountApprovable(creditModifier, loanAmount, loanPeriod)) {
            int approvedAmount = Math.min(highestValidLoanAmount(creditModifier, loanPeriod), MAX_AMOUNT);
            return LoanDecision.positive(approvedAmount, loanPeriod);
        }

        // Formula: credit_score = (modifier / amount) * period
        // Rearranged: max approvable amount = modifier * period
        // The requested amount does not cap the result; the engine returns the best valid offer.
        for (int period = loanPeriod; period <= MAX_PERIOD; period++) {
            int maxApprovable = highestValidLoanAmount(creditModifier, period);

            if (maxApprovable >= MIN_AMOUNT) {
                int approvedAmount = Math.min(maxApprovable, MAX_AMOUNT);
                return LoanDecision.positive(approvedAmount, period);
            }
        }

        return LoanDecision.negative(
                "No suitable loan amount could be found within the allowed period range (12–60 months)."
        );
    }

    private boolean isAmountApprovable(int creditModifier, int loanAmount, int loanPeriod) {
        return calculateCreditScore(creditModifier, loanAmount, loanPeriod) >= 1.0;
    }

    private double calculateCreditScore(int creditModifier, int loanAmount, int loanPeriod) {
        return ((double) creditModifier / loanAmount) * loanPeriod;
    }

    private int highestValidLoanAmount(int creditModifier, int loanPeriod) {
        return creditModifier * loanPeriod;
    }

    private void validateInputs(String personalCode, int loanAmount, int loanPeriod) {
        if (personalCode == null || personalCode.isBlank()) {
            throw new InvalidLoanRequestException("Personal code must not be blank.");
        }
        if (loanAmount < MIN_AMOUNT || loanAmount > MAX_AMOUNT) {
            throw new InvalidLoanRequestException("Loan amount must be between 2000 and 10000.");
        }
        if (loanPeriod < MIN_PERIOD || loanPeriod > MAX_PERIOD) {
            throw new InvalidLoanRequestException("Loan period must be between 12 and 60 months.");
        }
    }
}
