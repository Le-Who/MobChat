// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Compression {

    public static byte[] compressString(String data) {
        Deflater deflater = new Deflater();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length());
            deflater.setInput(data.getBytes(StandardCharsets.UTF_8));
            deflater.finish();

            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toByteArray();
        } catch (RuntimeException e) {
            return null;
        } finally {
            deflater.end();
        }
    }

    public static String decompressString(byte[] data) throws DataFormatException {
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(data);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && !inflater.finished()) {
                    throw new DataFormatException("Incomplete login snapshot");
                }
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toString(StandardCharsets.UTF_8);
        } finally {
            inflater.end();
        }
    }
}
