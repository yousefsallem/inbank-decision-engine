package com.inbank.decision.service;

import com.inbank.decision.exception.InvalidPersonalCodeException;
import com.inbank.decision.model.LoanDecision;
import com.inbank.decision.registry.CreditRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DecisionEngineServiceTest {

    private static final Map<String, Integer> DEFAULT_REGISTRY = Map.of(
            "49002010965", -1,
            "49002010976", 100,
            "49002010987", 300,
            "49002010998", 1000
    );

    @Test
    @DisplayName("should return NEGATIVE decision when applicant has debt")
    void shouldReturnNegativeForDebtors() {
        LoanDecision result = serviceWithDefaults().evaluate("49002010965", 4000, 24);
        assertThat(result.getDecision()).isEqualTo(LoanDecision.Decision.NEGATIVE);
        assertThat(result.getApprovedAmount()).isNull();
        assertThat(result.getApprovedPeriod()).isNull();
        assertThat(result.getMessage()).contains("debt");
    }

    @Test
    @DisplayName("should throw InvalidPersonalCodeException for unknown code")
    void shouldThrowForUnknownCode() {
        assertThatThrownBy(() -> serviceWithDefaults().evaluate("00000000000", 4000, 24))
                .isInstanceOf(InvalidPersonalCodeException.class);
    }

    @Test
    @DisplayName("segment 1: should approve max amount at requested period")
    void segment1_approvesAtRequestedPeriod() {
        LoanDecision result = serviceWithDefaults().evaluate("49002010976", 4000, 24);
        assertThat(result.getDecision()).isEqualTo(LoanDecision.Decision.POSITIVE);
        assertThat(result.getApprovedAmount()).isEqualTo(2400);
        assertThat(result.getApprovedPeriod()).isEqualTo(24);
    }

    @Test
    @DisplayName("requested amount scoring should be evaluated before max-offer calculation")
    void evaluatesRequestedAmountFirst() {
        LoanDecision result = serviceWithDefaults().evaluate("49002010976", 2000, 24);
        assertThat(result.getDecision()).isEqualTo(LoanDecision.Decision.POSITIVE);
        assertThat(result.getApprovedAmount()).isEqualTo(2400);
        assertThat(result.getApprovedPeriod()).isEqualTo(24);
    }

    @Test
    @DisplayName("segment 1: should extend period when amount too low")
    void segment1_extendsPeriodWhenNeeded() {
        LoanDecision result = serviceWithDefaults().evaluate("49002010976", 4000, 12);
        assertThat(result.getDecision()).isEqualTo(LoanDecision.Decision.POSITIVE);
        assertThat(result.getApprovedAmount()).isEqualTo(2000);
        assertThat(result.getApprovedPeriod()).isEqualTo(20);
    }

    @Test
    @DisplayName("segment 2: should approve higher amount at requested period")
    void segment2_approvesHigherAmount() {
        LoanDecision result = serviceWithDefaults().evaluate("49002010987", 4000, 24);
        assertThat(result.getDecision()).isEqualTo(LoanDecision.Decision.POSITIVE);
        assertThat(result.getApprovedAmount()).isEqualTo(7200);
    }

    @Test
    @DisplayName("segment 3: should cap approved amount at 10000")
    void segment3_capsAt10000() {
        LoanDecision result = serviceWithDefaults().evaluate("49002010998", 4000, 24);
        assertThat(result.getDecision()).isEqualTo(LoanDecision.Decision.POSITIVE);
        assertThat(result.getApprovedAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("should return negative when no valid amount exists at any period")
    void returnsNegativeWhenImpossible() {
        LoanDecision result = serviceWithOverrides(Map.of("00000000001", 1))
                .evaluate("00000000001", 4000, 12);
        assertThat(result.getDecision()).isEqualTo(LoanDecision.Decision.NEGATIVE);
        assertThat(result.getApprovedAmount()).isNull();
    }

    @Test
    @DisplayName("approved amount should always be within constraints")
    void approvedAmountWithinConstraints() {
        LoanDecision result = serviceWithDefaults().evaluate("49002010976", 4000, 60);
        if (result.getDecision() == LoanDecision.Decision.POSITIVE) {
            assertThat(result.getApprovedAmount()).isBetween(2000, 10000);
        }
    }

    private DecisionEngineService serviceWithDefaults() {
        return serviceWithOverrides(Map.of());
    }

    private DecisionEngineService serviceWithOverrides(Map<String, Integer> overrides) {
        Map<String, Integer> entries = new HashMap<>(DEFAULT_REGISTRY);
        entries.putAll(overrides);
        return new DecisionEngineService(new StubCreditRegistry(entries));
    }

    private static final class StubCreditRegistry extends CreditRegistry {
        private final Map<String, Integer> entries;

        private StubCreditRegistry(Map<String, Integer> entries) {
            this.entries = entries;
        }

        @Override
        public int getCreditModifier(String personalCode) {
            Integer value = entries.get(personalCode);
            if (value == null) {
                throw new InvalidPersonalCodeException("Personal code not found in registry: " + personalCode);
            }
            return value;
        }
    }
}
