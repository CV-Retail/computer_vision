package com.cvretail.kiosko.campaigns.domain.exception;

import java.util.List;

/** Thrown when a change would break an invariant of a campaign. Carries every violation found. */
public final class CampaignViolationException extends RuntimeException {

    private final List<Violation> violations;

    public CampaignViolationException(List<Violation> violations) {
        super(describe(violations));
        this.violations = List.copyOf(violations);
    }

    public CampaignViolationException(String field, String code, String message) {
        this(List.of(new Violation(field, code, message)));
    }

    public List<Violation> violations() {
        return violations;
    }

    public boolean hasCode(String code) {
        return violations.stream().anyMatch(violation -> violation.code().equals(code));
    }

    private static String describe(List<Violation> violations) {
        if (violations.isEmpty()) {
            throw new IllegalArgumentException("at least one violation is required");
        }
        return violations.stream()
                .map(violation -> violation.code() + " (" + violation.field() + "): " + violation.message())
                .reduce((first, second) -> first + "; " + second)
                .orElseThrow();
    }
}
