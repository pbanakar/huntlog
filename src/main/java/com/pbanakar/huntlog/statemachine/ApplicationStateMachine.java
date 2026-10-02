package com.pbanakar.huntlog.statemachine;

import com.pbanakar.huntlog.enums.ApplicationStatus;
import com.pbanakar.huntlog.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates the valid status transitions for a job application.
 *
 * <p><b>Why a Map instead of if-else / switch?</b></p>
 * <ul>
 *   <li><b>O(1) lookup</b> — checking whether a transition is valid is a constant-time
 *       map + set lookup, regardless of how many states or transitions exist.</li>
 *   <li><b>Easy to extend</b> — adding a new state or transition is a one-line change
 *       in the static initializer; no control-flow refactoring needed.</li>
 *   <li><b>Easy to test</b> — each transition can be verified independently with a
 *       simple parameterised test; the data structure is the single source of truth.</li>
 *   <li><b>Self-documenting</b> — reading the map literal immediately tells you
 *       every legal transition without tracing branching logic.</li>
 * </ul>
 */
@Component
public class ApplicationStateMachine {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> TRANSITIONS;

    static {
        Map<ApplicationStatus, Set<ApplicationStatus>> map = new EnumMap<>(ApplicationStatus.class);

        map.put(ApplicationStatus.APPLIED,
                EnumSet.of(ApplicationStatus.SCREENING, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN));
        map.put(ApplicationStatus.SCREENING,
                EnumSet.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN));
        map.put(ApplicationStatus.INTERVIEW,
                EnumSet.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN));
        map.put(ApplicationStatus.OFFER,
                EnumSet.of(ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN));

        // Terminal states — no outgoing transitions
        map.put(ApplicationStatus.ACCEPTED, EnumSet.noneOf(ApplicationStatus.class));
        map.put(ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class));
        map.put(ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class));

        TRANSITIONS = Collections.unmodifiableMap(map);
    }

    /**
     * Validates that a transition from {@code from} to {@code to} is legal.
     *
     * @throws InvalidStatusTransitionException if the transition is not allowed
     */
    public void validate(ApplicationStatus from, ApplicationStatus to) {
        Set<ApplicationStatus> allowed = getAllowedTransitions(from);
        if (!allowed.contains(to)) {
            throw new InvalidStatusTransitionException(from, to, allowed);
        }
    }

    /**
     * Returns the set of statuses reachable from {@code from}.
     * Returns an empty set for terminal states (ACCEPTED, REJECTED, WITHDRAWN).
     */
    public Set<ApplicationStatus> getAllowedTransitions(ApplicationStatus from) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(ApplicationStatus.class));
    }
}
