// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

public final class UpdateVersion {
    private UpdateVersion() {
    }

    public static boolean isNewer(String candidateVersion, String currentVersion) {
        return compareModVersions(candidateVersion, currentVersion) > 0;
    }

    public static String minecraftVersion(String version) {
        if (version == null) {
            return "";
        }
        int plus = version.indexOf('+');
        if (plus < 0 || plus == version.length() - 1) {
            return "";
        }
        return version.substring(plus + 1);
    }

    public static int compareModVersions(String left, String right) {
        String[] leftParts = modPart(left).split("[.-]");
        String[] rightParts = modPart(right).split("[.-]");
        int max = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < max; i++) {
            int leftValue = i < leftParts.length ? leadingNumber(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? leadingNumber(rightParts[i]) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static String modPart(String value) {
        if (value == null || value.isBlank()) {
            return "0";
        }
        String normalized = value.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        int plus = normalized.indexOf('+');
        if (plus >= 0) {
            normalized = normalized.substring(0, plus);
        }
        return normalized;
    }

    private static int leadingNumber(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        int end = 0;
        while (end < value.length() && Character.isDigit(value.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        return Integer.parseInt(value.substring(0, end));
    }
}
