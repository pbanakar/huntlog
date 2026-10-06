package com.pbanakar.huntlog.dto.response;

public class CompanyCountResponse {

    private String company;
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
