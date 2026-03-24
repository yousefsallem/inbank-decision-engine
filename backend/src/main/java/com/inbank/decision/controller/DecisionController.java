package com.inbank.decision.controller;

import com.inbank.decision.model.LoanDecision;
import com.inbank.decision.model.LoanRequest;
import com.inbank.decision.service.DecisionEngineService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DecisionController {

    private final DecisionEngineService decisionEngineService;

    public DecisionController(DecisionEngineService decisionEngineService) {
        this.decisionEngineService = decisionEngineService;
    }

    @PostMapping("/decision")
    public ResponseEntity<LoanDecision> getDecision(@Valid @RequestBody LoanRequest request) {
        LoanDecision decision = decisionEngineService.evaluate(
                request.getPersonalCode(),
                request.getLoanAmount(),
                request.getLoanPeriod()
        );
        return ResponseEntity.ok(decision);
    }
}
