// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public final class GitHubReleaseParser {
    private GitHubReleaseParser() {
    }

    public static List<Release> parse(String json) {
        List<Release> releases = new ArrayList<>();
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) {
            return releases;
        }

        for (JsonElement releaseElement : root.getAsJsonArray()) {
            if (!releaseElement.isJsonObject()) {
                continue;
            }
            JsonObject release = releaseElement.getAsJsonObject();
            releases.add(new Release(
                    stringValue(release, "tag_name"),
                    stringValue(release, "name"),
                    stringValue(release, "html_url"),
                    booleanValue(release, "draft"),
                    booleanValue(release, "prerelease"),
                    stringValue(release, "body"),
                    parseAssets(release.getAsJsonArray("assets"))
            ));
        }
        return releases;
    }

    private static List<Asset> parseAssets(JsonArray assetArray) {
        List<Asset> assets = new ArrayList<>();
        if (assetArray == null) {
            return assets;
        }
        for (JsonElement assetElement : assetArray) {
            if (!assetElement.isJsonObject()) {
                continue;
            }
            JsonObject asset = assetElement.getAsJsonObject();
            assets.add(new Asset(
                    stringValue(asset, "name"),
                    stringValue(asset, "browser_download_url")
            ));
        }
        return assets;
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static boolean booleanValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && !value.isJsonNull() && value.getAsBoolean();
    }

    public record Release(
            String tagName,
            String name,
            String htmlUrl,
            boolean draft,
            boolean prerelease,
            String body,
            List<Asset> assets
    ) {
    }

    public record Asset(String name, String browserDownloadUrl) {
    }
}
