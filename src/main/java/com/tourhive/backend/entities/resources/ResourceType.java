package com.tourhive.backend.entities.resources;

public enum ResourceType {
    NONE,
    VIDEO,
    AUDIO,
    IMAGE,
    TEXT,
    PDF,
    MSWORD,
    MSEXCEL,
    ZIP,
    OTHER;

    public static ResourceType getResourceType(String contentType) {
        if (contentType == null) {
            return ResourceType.NONE;
        }
        String lower = contentType.toLowerCase();
        if (lower.contains("image")) {
            return ResourceType.IMAGE;
        } else if (lower.contains("video")) {
            return ResourceType.VIDEO;
        } else if (lower.contains("audio")) {
            return ResourceType.AUDIO;
        } else if (lower.contains("msword") || lower.contains("wordprocessingml")) {
            return ResourceType.MSWORD;
        } else if (lower.contains("ms-excel") || lower.contains("spreadsheetml")) {
            return ResourceType.MSEXCEL;
        } else if (lower.contains("pdf")) {
            return ResourceType.PDF;
        } else if (lower.contains("zip") || lower.contains("compressed")) {
            return ResourceType.ZIP;
        } else if (lower.contains("text")) {
            return ResourceType.TEXT;
        } else {
            return ResourceType.OTHER;
        }
    }
}
