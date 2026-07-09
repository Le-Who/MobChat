// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class UpdateHashes {
    private UpdateHashes() {
    }

    static String sha512(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            return hex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 is not available.", e);
        }
    }

    static String sha512(Path path) throws IOException {
        return sha512(Files.readAllBytes(path));
    }

    static String normalizeSha512(String hashText) {
        if (hashText == null) {
            return "";
        }
        String firstToken = hashText.trim().split("\\s+")[0];
        return firstToken.toLowerCase();
    }

    private static String hex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
