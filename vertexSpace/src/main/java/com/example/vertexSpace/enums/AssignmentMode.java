package com.example.vertexSpace.enums;

public enum AssignmentMode {
    /**
     * Desk can only be assigned to users (not bookable)
     * Managed by Department Admin or System Admin
     */
    ASSIGNED,

    /**
     * Desk is bookable by anyone (hot-desking)
     * Cannot be assigned to specific users
     */
    HOT_DESK,

    /**
     * Not applicable - used for non-desk resources (rooms, parking)
     */
    NOT_APPLICABLE
}
