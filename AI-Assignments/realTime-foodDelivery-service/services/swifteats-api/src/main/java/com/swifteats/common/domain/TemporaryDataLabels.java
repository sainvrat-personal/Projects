package com.swifteats.common.domain;

/**
 * Marks demo, seed, and user-entered records as temporary with a shared name prefix.
 */
public final class TemporaryDataLabels {

    public static final String PREFIX = "[T] ";

    private TemporaryDataLabels() {
    }

    public static String prefix(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.startsWith(PREFIX)) {
            return trimmed;
        }
        return PREFIX + trimmed;
    }
}
