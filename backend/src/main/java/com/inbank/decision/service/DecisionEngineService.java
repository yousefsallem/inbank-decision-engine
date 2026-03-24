package com.inbank.decision.service;

import com.inbank.decision.model.LoanDecision;
import com.inbank.decision.registry.CreditRegistry;
import org.springframework.stereotype.Service;

@Service
public class DecisionEngineService {

    private static final int MIN_AMOUNT = 2000;
    private static final int MAX_AMOUNT = 10000;
    private static final int MAX_PERIOD = 60;

    private final CreditRegistry creditRegistry;

    public DecisionEngineService(CreditRegistry creditRegistry) {
        this.creditRegistry = creditRegistry;
    }

    public LoanDecision evaluate(String personalCode, int loanAmount, int loanPeriod) {
        // Throws InvalidPersonalCodeException if code is not found.
        int creditModifier = creditRegistry.getCreditModifier(personalCode);

        if (creditModifier < 0) {
            return LoanDecision.negative("Applicant has active debt. No loan can be approved.");
        }

        // Evaluate requested input first using the assignment scoring formula.
        if (isAmountApprovable(creditModifier, loanAmount, loanPeriod)) {
            int approvedAmount = Math.min(creditModifier * loanPeriod, MAX_AMOUNT);
            return LoanDecision.positive(approvedAmount, loanPeriod);
        }

        // Formula: credit_score = (modifier / amount) * period
        // Rearranged: max approvable amount = modifier * period
        // The requested amount does not cap the result; the engine returns the best valid offer.
        for (int period = loanPeriod; period <= MAX_PERIOD; period++) {
            int maxApprovable = creditModifier * period;

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
}
