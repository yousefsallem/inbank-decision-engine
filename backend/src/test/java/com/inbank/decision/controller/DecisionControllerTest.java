package com.inbank.decision.controller;

import com.inbank.decision.exception.GlobalExceptionHandler;
import com.inbank.decision.exception.InvalidPersonalCodeException;
import com.inbank.decision.model.LoanDecision;
import com.inbank.decision.registry.CreditRegistry;
import com.inbank.decision.service.DecisionEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DecisionControllerTest {

    private StubDecisionEngineService decisionEngineService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        decisionEngineService = new StubDecisionEngineService();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DecisionController(decisionEngineService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("should return 200 and a decision payload for a valid request")
    void shouldReturnDecisionPayload() throws Exception {
        decisionEngineService.response = LoanDecision.positive(2400, 24);

        mockMvc.perform(post("/api/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personalCode": "49002010976",
                                  "loanAmount": 4000,
                                  "loanPeriod": 24
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("POSITIVE"))
                .andExpect(jsonPath("$.approvedAmount").value(2400))
                .andExpect(jsonPath("$.approvedPeriod").value(24));
    }

    @Test
    @DisplayName("should return 400 for malformed personal code")
    void shouldReturnBadRequestForMalformedPersonalCode() throws Exception {
        mockMvc.perform(post("/api/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personalCode": "abc",
                                  "loanAmount": 4000,
                                  "loanPeriod": 24
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("exactly 11 digits")));
    }

    @Test
    @DisplayName("should return 404 when personal code is not in registry")
    void shouldReturnNotFoundForUnknownCode() throws Exception {
        decisionEngineService.exception =
                new InvalidPersonalCodeException("Personal code not found in registry: 00000000000");

        mockMvc.perform(post("/api/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personalCode": "00000000000",
                                  "loanAmount": 4000,
                                  "loanPeriod": 24
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("INVALID_PERSONAL_CODE"))
                .andExpect(jsonPath("$.message").value("Personal code not found in registry: 00000000000"));
    }

    @Test
    @DisplayName("should return 400 for malformed JSON")
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personalCode": "49002010976",
                                  "loanAmount": 4000,
                                  "loanPeriod": 24
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("Request body is malformed or contains invalid field types."));
    }

    @Test
    @DisplayName("should return 400 for invalid field types")
    void shouldReturnBadRequestForInvalidFieldTypes() throws Exception {
        mockMvc.perform(post("/api/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "personalCode": "49002010976",
                                  "loanAmount": "abc",
                                  "loanPeriod": 24
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("Request body is malformed or contains invalid field types."));
    }

    private static final class StubDecisionEngineService extends DecisionEngineService {
        private LoanDecision response;
        private RuntimeException exception;

        private StubDecisionEngineService() {
            super(new CreditRegistry());
        }

        @Override
        public LoanDecision evaluate(String personalCode, int loanAmount, int loanPeriod) {
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
