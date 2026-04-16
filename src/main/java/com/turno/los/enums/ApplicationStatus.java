package com.turno.los.enums;

/**
 * Lifecycle states of a loan application.
 *
 * APPLIED              → Initial state when customer submits.
 * APPROVED_BY_SYSTEM   → Automated check passed.
 * REJECTED_BY_SYSTEM   → Automated check failed.
 * UNDER_REVIEW         → Needs human (agent) review.
 * APPROVED_BY_AGENT    → Agent approved after review.
 * REJECTED_BY_AGENT    → Agent rejected after review.
 */
public enum ApplicationStatus {
    APPLIED,
    APPROVED_BY_SYSTEM,
    REJECTED_BY_SYSTEM,
    UNDER_REVIEW,
    APPROVED_BY_AGENT,
    REJECTED_BY_AGENT
}
