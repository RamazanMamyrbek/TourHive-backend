package com.tourhive.backend.entities.audit;

public enum AuditAction {
    USER_BLOCKED,
    USER_UNBLOCKED,
    USER_ROLE_CHANGED,
    TOUR_APPROVED,
    TOUR_REJECTED,
    BUSINESS_VERIFIED,
    BUSINESS_REJECTED,
    REPORT_RESOLVED,
    REPORT_DISMISSED
}
