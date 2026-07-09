// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public final class GitHubReleaseClient implements UpdateSource {
    private static final String API_BASE = "https://api.github.com";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final String owner;
    private final String repo;
    private final String userAgent;
    private final HttpClient client;

    public GitHubReleaseClient(String owner, String repo, String modVersion) {
        this(owner, repo, "MobChat-Updater/" + modVersion, HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(REQUEST_TIMEOUT)
                .build());
    }

    GitHubReleaseClient(String owner, String repo, String userAgent, HttpClient client) {
        this.owner = owner;
        this.repo = repo;
        this.userAgent = userAgent;
        this.client = client;
    }

    @Override
    public Optional<UpdateCandidate> findUpdate(
            String archiveBaseName,
            String currentVersion,
            String minecraftVersion,
            boolean allowPrerelease
    ) throws IOException, InterruptedException {
        String json = downloadText(API_BASE + "/repos/" + owner + "/" + repo + "/releases?per_page=20");
        List<GitHubReleaseParser.Release> releases = GitHubReleaseParser.parse(json);
        return GitHubReleaseSelector.select(releases, archiveBaseName, currentVersion, minecraftVersion, allowPrerelease);
    }

    @Override
    public String downloadText(String url) throws IOException, InterruptedException {
        HttpRequest request = request(url).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        ensureSuccess(url, response.statusCode());
        return response.body();
    }

    @Override
    public byte[] downloadBytes(String url) throws IOException, InterruptedException {
        HttpRequest request = request(url).build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        ensureSuccess(url, response.statusCode());
        return response.body();
    }

    private HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", userAgent)
                .GET();
    }

    private void ensureSuccess(String url, int statusCode) throws IOException {
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("GitHub update request failed with HTTP " + statusCode + ": " + url);
        }
    }
}
