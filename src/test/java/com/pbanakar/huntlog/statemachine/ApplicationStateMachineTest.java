package com.pbanakar.huntlog.statemachine;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationStateMachineTest {

    private ApplicationStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new ApplicationStateMachine();
    }

    // --- Valid transitions ---

    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(ApplicationStatus.APPLIED, ApplicationStatus.SCREENING),
                Arguments.of(ApplicationStatus.APPLIED, ApplicationStatus.REJECTED),
                Arguments.of(ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN),
                Arguments.of(ApplicationStatus.SCREENING, ApplicationStatus.INTERVIEW),
                Arguments.of(ApplicationStatus.SCREENING, ApplicationStatus.REJECTED),
                Arguments.of(ApplicationStatus.SCREENING, ApplicationStatus.WITHDRAWN),
                Arguments.of(ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER),
                Arguments.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED),
                Arguments.of(ApplicationStatus.INTERVIEW, ApplicationStatus.WITHDRAWN),
                Arguments.of(ApplicationStatus.OFFER, ApplicationStatus.ACCEPTED),
                Arguments.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED),
                Arguments.of(ApplicationStatus.OFFER, ApplicationStatus.WITHDRAWN)
        );
    }

    @ParameterizedTest(name = "{0} → {1} should be valid")
    @MethodSource("validTransitions")
    @DisplayName("Every valid transition should not throw")
    void validTransition_doesNotThrow(ApplicationStatus from, ApplicationStatus to) {
        assertDoesNotThrow(() -> stateMachine.validate(from, to));
    }

    // --- Invalid transitions ---

    static Stream<Arguments> invalidTransitions() {
        return Stream.of(
                Arguments.of(ApplicationStatus.APPLIED, ApplicationStatus.OFFER),
                Arguments.of(ApplicationStatus.APPLIED, ApplicationStatus.ACCEPTED),
                Arguments.of(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW),
                Arguments.of(ApplicationStatus.SCREENING, ApplicationStatus.OFFER),
                Arguments.of(ApplicationStatus.SCREENING, ApplicationStatus.ACCEPTED),
                Arguments.of(ApplicationStatus.INTERVIEW, ApplicationStatus.SCREENING),
                Arguments.of(ApplicationStatus.OFFER, ApplicationStatus.INTERVIEW),
                Arguments.of(ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED),
                Arguments.of(ApplicationStatus.ACCEPTED, ApplicationStatus.WITHDRAWN),
                Arguments.of(ApplicationStatus.ACCEPTED, ApplicationStatus.APPLIED),
                Arguments.of(ApplicationStatus.REJECTED, ApplicationStatus.APPLIED),
                Arguments.of(ApplicationStatus.REJECTED, ApplicationStatus.SCREENING),
                Arguments.of(ApplicationStatus.WITHDRAWN, ApplicationStatus.APPLIED),
                Arguments.of(ApplicationStatus.WITHDRAWN, ApplicationStatus.SCREENING)
        );
    }

    @ParameterizedTest(name = "{0} → {1} should be invalid")
    @MethodSource("invalidTransitions")
    @DisplayName("Every invalid transition should throw with correct from/to/allowed")
    void invalidTransition_throwsWithCorrectDetails(ApplicationStatus from, ApplicationStatus to) {
        InvalidStatusTransitionException ex = assertThrows(
                InvalidStatusTransitionException.class,
                () -> stateMachine.validate(from, to)
        );
        assertEquals(from, ex.getFrom());
        assertEquals(to, ex.getTo());
        assertNotNull(ex.getAllowed());
        assertFalse(ex.getAllowed().contains(to));
    }

    // --- Terminal states ---

    @ParameterizedTest
    @EnumSource(value = ApplicationStatus.class, names = {"ACCEPTED", "REJECTED", "WITHDRAWN"})
    @DisplayName("Terminal states return empty set from getAllowedTransitions")
    void terminalStates_returnEmptySet(ApplicationStatus terminal) {
        Set<ApplicationStatus> allowed = stateMachine.getAllowedTransitions(terminal);
        assertTrue(allowed.isEmpty(), terminal + " should have no allowed transitions");
    }

    // --- getAllowedTransitions for non-terminal ---

    @Test
    @DisplayName("APPLIED allows SCREENING, REJECTED, WITHDRAWN")
    void applied_allowedTransitions() {
        Set<ApplicationStatus> allowed = stateMachine.getAllowedTransitions(ApplicationStatus.APPLIED);
        assertEquals(
                Set.of(ApplicationStatus.SCREENING, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
                allowed
        );
    }

    @Test
    @DisplayName("SCREENING allows INTERVIEW, REJECTED, WITHDRAWN")
    void screening_allowedTransitions() {
        Set<ApplicationStatus> allowed = stateMachine.getAllowedTransitions(ApplicationStatus.SCREENING);
        assertEquals(
                Set.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
                allowed
        );
    }

    @Test
    @DisplayName("INTERVIEW allows OFFER, REJECTED, WITHDRAWN")
    void interview_allowedTransitions() {
        Set<ApplicationStatus> allowed = stateMachine.getAllowedTransitions(ApplicationStatus.INTERVIEW);
        assertEquals(
                Set.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
                allowed
        );
    }

    @Test
    @DisplayName("OFFER allows ACCEPTED, REJECTED, WITHDRAWN")
    void offer_allowedTransitions() {
        Set<ApplicationStatus> allowed = stateMachine.getAllowedTransitions(ApplicationStatus.OFFER);
        assertEquals(
                Set.of(ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
                allowed
        );
    }
}
