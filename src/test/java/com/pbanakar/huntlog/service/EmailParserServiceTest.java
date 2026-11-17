package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.sync.ParsedEmailApplication;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmailParserServiceTest {

    private EmailParserService parser;

    @BeforeEach
    void setUp() {
        parser = new EmailParserService();
    }

    @Test
    @DisplayName("Parse BeBetta application confirmation email via Keka")
    void testParseBeBettaApplication() {
        String from = "BeBetta Hiring team <no-reply@kekamail.com>";
        String subject = "Application for Software Engineering Intern - Server (Backend) received, Thank you!";
        String body = "Hi Prajwal Banakar, Thank you for applying to BeBetta for the role of Software Engineering Intern - Server (Backend) through our career portal.";
        LocalDate date = LocalDate.of(2026, 11, 9);

        ParsedEmailApplication result = parser.parse(from, subject, body, date);

        assertThat(result.isRelevant()).isTrue();
        assertThat(result.getCompany()).isEqualTo("BeBetta");
        assertThat(result.getPosition()).contains("Software Engineering Intern - Server (Backend)");
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(result.getEmailDate()).isEqualTo(date);
    }

    @Test
    @DisplayName("Parse Ververica application confirmation with HiBob portal URL")
    void testParseVervericaApplication() {
        String from = "\"No-reply (via Bob)\" <no-reply@hibob.com>";
        String subject = "Application Received: Technical Support Engineer at Ververica GmbH";
        String body = "Thank you for applying for the Technical Support Engineer position at Ververica GmbH! You can track the status of your application by visiting your Candidate Portal: https://ververicagmbh.careers.hibob.com/session?token=EZGbTuj9yYNx8yI1YuWU";
        LocalDate date = LocalDate.of(2026, 11, 4);

        ParsedEmailApplication result = parser.parse(from, subject, body, date);

        assertThat(result.isRelevant()).isTrue();
        assertThat(result.getCompany()).contains("Ververica");
        assertThat(result.getPosition()).isEqualTo("Technical Support Engineer");
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(result.getJobUrl()).contains("https://ververicagmbh.careers.hibob.com");
    }

    @Test
    @DisplayName("Parse PromptQL Talent Community application via Gem")
    void testParsePromptQLApplication() {
        String from = "PromptQL Recruiting <no-reply@gem.com>";
        String subject = "We've received your application. Thank you for your interest in PromptQL!";
        String body = "Thank you for applying for the Talent Community at PromptQL! Our team reviews applications on a rolling basis.";
        LocalDate date = LocalDate.of(2026, 11, 4);

        ParsedEmailApplication result = parser.parse(from, subject, body, date);

        assertThat(result.isRelevant()).isTrue();
        assertThat(result.getCompany()).isEqualTo("PromptQL");
        assertThat(result.getPosition()).contains("Talent Community");
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.APPLIED);
    }

    @Test
    @DisplayName("Parse Atlassian status rejection email via iCIMS")
    void testParseAtlassianRejection() {
        String from = "\"Atlassian @ icims\" <atlassian+autoreply@talent.icims.com>";
        String subject = "Application Status from Seattle - United States";
        String body = "Dear Prajwal, Thank you very much for your recent application to the Data Engineer Intern, 2027 Summer U.S. position at Atlassian. Unfortunately after careful review, we are unable to proceed further with your application as it does not meet the eligibility requirements of this role.";
        LocalDate date = LocalDate.of(2026, 11, 4);

        ParsedEmailApplication result = parser.parse(from, subject, body, date);

        assertThat(result.isRelevant()).isTrue();
        assertThat(result.getCompany()).contains("Atlassian");
        assertThat(result.getPosition()).contains("Data Engineer Intern, 2027 Summer U.S.");
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    @DisplayName("Parse interview / assessment invitation")
    void testParseInterviewInvitation() {
        String from = "interviews@hackerrankforwork.com";
        String subject = "Chakra AI has invited you to take a coding assessment";
        String body = "You have been invited by Chakra AI to take a technical assessment for the role of Software Engineer Intern.";
        LocalDate date = LocalDate.of(2026, 11, 6);

        ParsedEmailApplication result = parser.parse(from, subject, body, date);

        assertThat(result.isRelevant()).isTrue();
        assertThat(result.getPosition()).contains("Software Engineer Intern");
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.INTERVIEW);
    }

    @Test
    @DisplayName("Filter out marketing email containing promo keywords")
    void testFilterOutMarketingEmail() {
        String from = "Mastercard <talent@careers.mastercard.com>";
        String subject = "Your next big career opportunity awaits at Mastercard";
        String body = "Ready to take your career to the next level? Browse all jobs at Mastercard now.";

        ParsedEmailApplication result = parser.parse(from, subject, body, LocalDate.now());

        assertThat(result.isRelevant()).isFalse();
    }

    @Test
    @DisplayName("Filter out regular non-job emails")
    void testFilterOutIrrelevantEmail() {
        String from = "GitHub <notifications@github.com>";
        String subject = "[pbanakar/huntlog] New comment on commit 6355c13";
        String body = "Prajwal pushed 1 commit to main.";

        ParsedEmailApplication result = parser.parse(from, subject, body, LocalDate.now());

        assertThat(result.isRelevant()).isFalse();
    }
}

