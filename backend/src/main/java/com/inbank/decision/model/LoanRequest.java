package com.inbank.decision.model;

import jakarta.validation.constraints.*;

public class LoanRequest {

    @NotBlank(message = "Personal code must not be blank")
    private String personalCode;

    @NotNull(message = "Loan amount is required")
    @Min(value = 2000, message = "Minimum loan amount is 2000")
    @Max(value = 10000, message = "Maximum loan amount is 10000")
    private Integer loanAmount;

    @NotNull(message = "Loan period is required")
    @Min(value = 12, message = "Minimum loan period is 12 months")
    @Max(value = 60, message = "Maximum loan period is 60 months")
    private Integer loanPeriod;

    public LoanRequest() {}

    public LoanRequest(String personalCode, Integer loanAmount, Integer loanPeriod) {
        this.personalCode = personalCode;
        this.loanAmount = loanAmount;
        this.loanPeriod = loanPeriod;
    }

    public String getPersonalCode() { return personalCode; }
    public void setPersonalCode(String personalCode) { this.personalCode = personalCode; }
    public Integer getLoanAmount() { return loanAmount; }
    public void setLoanAmount(Integer loanAmount) { this.loanAmount = loanAmount; }
    public Integer getLoanPeriod() { return loanPeriod; }
    public void setLoanPeriod(Integer loanPeriod) { this.loanPeriod = loanPeriod; }
}
