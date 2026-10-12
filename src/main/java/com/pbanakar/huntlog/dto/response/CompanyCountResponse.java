package com.pbanakar.huntlog.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Count of applications by company")
public class CompanyCountResponse {

    @Schema(example = "Google", description = "Company name")
    private String company;

    @Schema(example = "3", description = "Total number of applications submitted to this company")
    private Long count;

    public CompanyCountResponse() {
    }

    public CompanyCountResponse(String company, Long count) {
        this.company = company;
        this.count = count;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
