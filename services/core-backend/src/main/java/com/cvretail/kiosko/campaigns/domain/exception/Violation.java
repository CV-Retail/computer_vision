package com.cvretail.kiosko.campaigns.domain.exception;

/** One broken rule: the affected field, a stable machine-readable code and an English message. */
public record Violation(String field, String code, String message) {
}
