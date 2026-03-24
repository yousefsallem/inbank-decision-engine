package com.inbank.decision.registry;

import com.inbank.decision.exception.InvalidPersonalCodeException;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class CreditRegistry {

    private static final int DEBT_FLAG = -1;

    private static final Map<String, Integer> REGISTRY = Map.of(
            "49002010965", DEBT_FLAG,
            "49002010976", 100,
            "49002010987", 300,
            "49002010998", 1000
    );

    public int getCreditModifier(String personalCode) {
        return Optional.ofNullable(REGISTRY.get(personalCode))
                .orElseThrow(() -> new InvalidPersonalCodeException(
                        "Personal code not found in registry: " + personalCode));
    }
}
