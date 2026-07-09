// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.update.GitHubReleaseParser;
import com.lewho.update.GitHubReleaseSelector;
import com.lewho.update.UpdateCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GitHubReleaseSelectorTests {

    @Test
    public void selectsNewestStableJarForCurrentMinecraftVersion() {
        List<GitHubReleaseParser.Release> releases = GitHubReleaseParser.parse("""
                [
                  {
                    "tag_name": "v3.1.0",
                    "name": "MobChat 3.1.0",
                    "html_url": "https://github.com/Le-Who/MobChat/releases/tag/v3.1.0",
                    "draft": false,
                    "prerelease": false,
                    "body": "Newer Minecraft only",
                    "assets": [
                      {
                        "name": "creaturechat-3.1.0+1.21.1.jar",
                        "browser_download_url": "https://example.invalid/creaturechat-3.1.0+1.21.1.jar"
                      },
                      {
                        "name": "creaturechat-3.1.0+1.21.1.jar.sha512",
                        "browser_download_url": "https://example.invalid/creaturechat-3.1.0+1.21.1.jar.sha512"
                      }
                    ]
                  },
                  {
                    "tag_name": "v3.0.2",
                    "name": "MobChat 3.0.2",
                    "html_url": "https://github.com/Le-Who/MobChat/releases/tag/v3.0.2",
                    "draft": false,
                    "prerelease": true,
                    "body": "Preview",
                    "assets": [
                      {
                        "name": "creaturechat-3.0.2+1.20.1.jar",
                        "browser_download_url": "https://example.invalid/creaturechat-3.0.2+1.20.1.jar"
                      },
                      {
                        "name": "creaturechat-3.0.2+1.20.1.jar.sha512",
                        "browser_download_url": "https://example.invalid/creaturechat-3.0.2+1.20.1.jar.sha512"
                      }
                    ]
                  },
                  {
                    "tag_name": "v3.0.1",
                    "name": "MobChat 3.0.1",
                    "html_url": "https://github.com/Le-Who/MobChat/releases/tag/v3.0.1",
                    "draft": false,
                    "prerelease": false,
                    "body": "Stable",
                    "assets": [
                      {
                        "name": "creaturechat-3.0.1+1.20.1.jar",
                        "browser_download_url": "https://example.invalid/creaturechat-3.0.1+1.20.1.jar"
                      },
                      {
                        "name": "creaturechat-3.0.1+1.20.1.jar.sha512",
                        "browser_download_url": "https://example.invalid/creaturechat-3.0.1+1.20.1.jar.sha512"
                      }
                    ]
                  }
                ]
                """);

        Optional<UpdateCandidate> selected = GitHubReleaseSelector.select(
                releases,
                "creaturechat",
                "3.0.0+1.20.1",
                "1.20.1",
                false
        );

        assertTrue(selected.isPresent());
        assertEquals("3.0.1+1.20.1", selected.get().version());
        assertEquals("creaturechat-3.0.1+1.20.1.jar", selected.get().assetName());
        assertEquals("https://example.invalid/creaturechat-3.0.1+1.20.1.jar.sha512", selected.get().sha512Url());
    }

    @Test
    public void prereleaseCanBeSelectedWhenAllowed() {
        List<GitHubReleaseParser.Release> releases = GitHubReleaseParser.parse("""
                [
                  {
                    "tag_name": "v3.0.2",
                    "name": "MobChat 3.0.2",
                    "html_url": "https://github.com/Le-Who/MobChat/releases/tag/v3.0.2",
                    "draft": false,
                    "prerelease": true,
                    "body": "Preview",
                    "assets": [
                      {
                        "name": "creaturechat-3.0.2+1.20.1.jar",
                        "browser_download_url": "https://example.invalid/creaturechat-3.0.2+1.20.1.jar"
                      },
                      {
                        "name": "creaturechat-3.0.2+1.20.1.jar.sha512",
                        "browser_download_url": "https://example.invalid/creaturechat-3.0.2+1.20.1.jar.sha512"
                      }
                    ]
                  },
                  {
                    "tag_name": "v3.0.1",
                    "name": "MobChat 3.0.1",
                    "html_url": "https://github.com/Le-Who/MobChat/releases/tag/v3.0.1",
                    "draft": false,
                    "prerelease": false,
                    "body": "Stable",
                    "assets": [
                      {
                        "name": "creaturechat-3.0.1+1.20.1.jar",
                        "browser_download_url": "https://example.invalid/creaturechat-3.0.1+1.20.1.jar"
                      },
                      {
                        "name": "creaturechat-3.0.1+1.20.1.jar.sha512",
                        "browser_download_url": "https://example.invalid/creaturechat-3.0.1+1.20.1.jar.sha512"
                      }
                    ]
                  }
                ]
                """);

        Optional<UpdateCandidate> selected = GitHubReleaseSelector.select(
                releases,
                "creaturechat",
                "3.0.0+1.20.1",
                "1.20.1",
                true
        );

        assertTrue(selected.isPresent());
        assertEquals("3.0.2+1.20.1", selected.get().version());
    }
}
