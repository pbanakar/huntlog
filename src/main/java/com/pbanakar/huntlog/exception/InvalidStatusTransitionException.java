package com.pbanakar.huntlog.exception;

import com.pbanakar.huntlog.enums.ApplicationStatus;

import java.util.Set;

public class InvalidStatusTransitionException extends RuntimeException {

    private final ApplicationStatus from;
    private final ApplicationStatus to;
    private final Set<ApplicationStatus> allowed;

    public InvalidStatusTransitionException(ApplicationStatus from,
                                            ApplicationStatus to,
                                            Set<ApplicationStatus> allowed) {
        super(String.format("Cannot transition from %s to %s. Allowed transitions: %s",
                from, to, allowed));
        this.from = from;
        this.to = to;
        this.allowed = allowed;
    }

    public ApplicationStatus getFrom() {
        return from;
    }

    public ApplicationStatus getTo() {
        return to;
    }

    public Set<ApplicationStatus> getAllowed() {
        return allowed;
    }
}
