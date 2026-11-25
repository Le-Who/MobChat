# SPDX-FileCopyrightText: 2026 lewho LLC
# SPDX-License-Identifier: GPL-3.0-or-later
import unittest

from comment_lint import comments, remove, snapshot, violations


class CommentLintTests(unittest.TestCase):
    def matching_text(self, path, source):
        return [entry.text for entry in comments(path, source) if not entry.protected]

    def test_java_strings_and_text_blocks_are_code(self):
        code = 'String url = "https://example.invalid";\nString json = """\n{ "a": "/*literal*/" }\n""";\n// actual\n'
        self.assertEqual(self.matching_text("Example.java", code), ["// actual"])

    def test_java_lexes_unicode_before_identifying_comments(self):
        code = r"int a; \u002f\u002f comment\u000aint b;" + "\n"
        items = list(comments("Example.java", code))
        self.assertEqual(len(items), 1)
        self.assertEqual(items[0].text, r"\u002f\u002f comment")
        self.assertIn(r"\u000aint b;", remove(code, items))

    def test_removal_keeps_separation_between_tokens(self):
        code = "int/* rationale */counter; // note\n    // more\nreturn counter;\n"
        self.assertEqual(remove(code, comments("Example.java", code)), "int counter;\nreturn counter;\n")

    def test_other_source_formats_ignore_literal_markers(self):
        self.assertEqual(self.matching_text("build.gradle", 'def re = ~/https:\\/\\/.*/ // note'), ["// note"])
        self.assertEqual(self.matching_text("build.sh", 'cat <<\'EOF\'\n# content\nEOF\necho "# literal";# note\n'), ["# note"])
        self.assertEqual(self.matching_text("a.py", 'print("# literal") # note\n'), ["# note"])
        self.assertEqual(self.matching_text("a.html", '<style>p { color: #abc; } /* style */</style><script>let x = "//"; // script\n</script><!-- html -->'),
                         ["/* style */", "// script", "<!-- html -->"])

    def test_allowance_cannot_be_replaced_or_duplicated(self):
        allowed = snapshot(comments("Example.java", "// approved\n"))
        shifted = snapshot(comments("Example.java", "\n// approved\n"))
        duplicate = snapshot(comments("Example.java", "// approved\n// approved\n"))
        edited = snapshot(comments("Example.java", "// changed\n"))
        moved = snapshot(comments("Other.java", "// approved\n"))
        self.assertEqual(violations(shifted, allowed), {})
        self.assertEqual(sum(violations(duplicate, allowed)["Example.java"].values()), 1)
        self.assertTrue(violations(edited, allowed))
        self.assertTrue(violations(moved, allowed))

    def test_protects_legal_headers_and_the_mixin_overwrite_tags(self):
        code = "// SPDX-License-Identifier: GPL-3.0-or-later\n/** @author Test @reason Required */\n@Overwrite\nvoid f() {}\n"
        self.assertEqual(self.matching_text("Example.java", code), [])
        self.assertEqual(self.matching_text("Example.java", "class X {\n/** @author A @reason B */\nvoid f() {}\n}"),
                         ["/** @author A @reason B */"])


if __name__ == "__main__":
    unittest.main()
