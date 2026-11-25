# SPDX-FileCopyrightText: 2026 lewho LLC
# SPDX-License-Identifier: GPL-3.0-or-later
import argparse
from collections import Counter, defaultdict
from dataclasses import dataclass
import hashlib
import io
import json
from pathlib import Path
import re
import subprocess
import tokenize


ROOT = Path(__file__).resolve().parents[1]
BASELINE = "tools/comment-baseline.json"
WRAPPERS = {"gradlew", "gradlew.bat"}
LINE_SUFFIXES = {".accesswidener", ".properties", ".toml", ".yaml", ".yml", ".sh"}


@dataclass(frozen=True)
class Comment:
    path: str
    line: int
    start: int
    end: int
    text: str
    protected: str

    @property
    def fingerprint(self):
        return hashlib.sha256(self.text.encode()).hexdigest()


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, check=True, stdout=subprocess.PIPE)


def quoted_end(source, start, delimiter, backslash=True):
    index = start + len(delimiter)
    while index < len(source):
        if backslash and source[index] == "\\":
            index += 2
        elif source.startswith(delimiter, index):
            return index + len(delimiter)
        else:
            index += 1
    return len(source)


def c_spans(source, dialect):
    index = 0
    while index < len(source):
        if source.startswith("//", index) and dialect != "css":
            end = source.find("\n", index)
            end = len(source) if end == -1 else end
            yield index, end
            index = end
        elif source.startswith("/*", index):
            end = source.find("*/", index + 2)
            end = len(source) if end == -1 else end + 2
            yield index, end
            index = end
        elif source[index] in "\"'`":
            delimiter = source[index]
            if dialect in {"java", "gradle"} and source.startswith(delimiter * 3, index):
                delimiter *= 3
            index = quoted_end(source, index, delimiter)
        elif dialect == "gradle" and source.startswith("$/", index):
            end = source.find("/$", index + 2)
            index = len(source) if end == -1 else end + 2
        elif dialect in {"gradle", "js"} and source[index] == "/" and re.search(r"(?:[=~(:,\[!]|return)\s*$", source[:index]):
            index = quoted_end(source, index, "/")
        else:
            index += 1


def java_spans(source):
    translated = []
    offsets = []
    backslashes = 0
    last_escaped = False
    index = 0
    while index < len(source):
        offsets.append(index)
        char = source[index]
        escape = None
        if char == "\\" and (backslashes % 2 == 0 or last_escaped) and source.startswith("u", index + 1):
            escape = re.match(r"\\u+([0-9a-fA-F]{4})", source[index:])
        if escape:
            char = chr(int(escape[1], 16))
            index += len(escape[0])
        else:
            index += 1
        translated.append(char)
        backslashes = backslashes + 1 if char == "\\" else 0
        last_escaped = escape is not None

    offsets.append(len(source))
    normalized = "".join(translated).replace("\r", "\n")
    for start, end in c_spans(normalized, "java"):
        yield offsets[start], offsets[end]


def python_spans(source):
    offsets = [0]
    for line in source.splitlines(keepends=True):
        offsets.append(offsets[-1] + len(line))
    for token in tokenize.generate_tokens(io.StringIO(source).readline):
        if token.type == tokenize.COMMENT:
            yield offsets[token.start[0] - 1] + token.start[1], offsets[token.end[0] - 1] + token.end[1]


def html_spans(source):
    pattern = r"<!--.*?(?:-->|\Z)|<(style|script)\b[^>]*>(.*?)</\1\s*>"
    for match in re.finditer(pattern, source, re.IGNORECASE | re.DOTALL):
        if match[1] is None:
            yield match.span()
        else:
            dialect = "css" if match[1].lower() == "style" else "js"
            for start, end in c_spans(match[2], dialect):
                yield match.start(2) + start, match.start(2) + end


def line_spans(source, dialect):
    start = 0
    quote = ""
    here_doc = None
    for line in source.splitlines(keepends=True):
        if here_doc is not None:
            if line.strip() == here_doc:
                here_doc = None
            start += len(line)
            continue
        if dialect in {".properties", ".gitignore", ".bat"}:
            pattern = r"(?i)^\s*(?:@?rem(?:\s|$)|::)" if dialect == ".bat" else r"^[ \t]*[#!]" if dialect == ".properties" else r"^#"
            match = re.match(pattern, line)
            if match:
                yield start + len(line) - len(line.lstrip()), start + len(line.rstrip("\n"))
            start += len(line)
            continue
        index = 0
        while index < len(line):
            char = line[index]
            if char == "\\" and quote != "'":
                index += 2
                continue
            if quote:
                if char == quote:
                    quote = ""
            elif char in "'\"":
                quote = char
            elif char == "#" and (dialect in {".toml", ".accesswidener"} or index == 0 or line[index - 1].isspace()
                                 or (dialect == ".sh" and line[index - 1] in ";|&()")):
                yield start + index, start + len(line.rstrip("\n"))
                break
            elif dialect == ".sh" and line.startswith("<<", index):
                match = re.match(r"<<-?[ \t]*['\"]?([A-Za-z_][A-Za-z_0-9]*)['\"]?(?=\s|\)|$)", line[index:])
                if match:
                    here_doc = match[1]
                    index += len(match[0])
                    continue
            index += 1
        start += len(line)


def sources():
    paths = git("ls-files", "-z", "--cached", "--others", "--exclude-standard").stdout.decode("utf-8").split("\0")
    for name in sorted(set(paths)):
        path = ROOT / name
        if path.is_file() and (name in WRAPPERS or name == ".gitignore" or path.suffix in LINE_SUFFIXES | {".py", ".java", ".gradle", ".html", ".css", ".js", ".ts", ".bat"}):
            yield name, path.read_text(encoding="utf-8-sig")


def comments(path, source):
    suffix = Path(path).suffix
    if suffix == ".java":
        spans = java_spans(source)
    elif suffix in {".gradle", ".css", ".js", ".ts"}:
        spans = c_spans(source, suffix[1:])
    elif suffix == ".py":
        spans = python_spans(source)
    elif suffix == ".html":
        spans = html_spans(source)
    else:
        spans = line_spans(source, ".sh" if path == "gradlew" else suffix or path)
    for start, end in spans:
        line = source.count("\n", 0, start) + 1
        text = source[start:end]
        protected = ""
        if path in WRAPPERS:
            protected = "Gradle-generated wrapper"
        elif (line == 1 and text.startswith("#!")) or re.match(r"#.*coding[:=]\s*[-\w.]+", text) and line <= 2 and suffix == ".py":
            protected = "Interpreter directive"
        elif re.match(r"(?://|#)\s*(?:SPDX-(?:FileCopyrightText|License-Identifier):|Assets CC-BY-NC-SA-4\.0; CreatureChat)", text) and line <= 5:
            protected = "License header"
        elif re.match(r"(?://|#)\s*(?:noinspection\b|type:\s*ignore\b|noqa\b|shellcheck\b|fmt:\s*(?:off|on)\b)", text):
            protected = "Tool directive"
        elif suffix == ".java" and text.startswith("/**") and "@reason " in text and "@author " in text and re.match(r"\s*@Overwrite\b", source[end:]):
            protected = "Mixin annotation processor directive"
        yield Comment(path, line, start, end, text, protected)


def snapshot(items):
    result = defaultdict(Counter)
    for item in items:
        if not item.protected:
            result[item.path][item.fingerprint] += 1
    return dict(sorted((path, dict(sorted(counts.items()))) for path, counts in result.items()))


def remove(source, items):
    for item in sorted(items, key=lambda entry: entry.start, reverse=True):
        first_line = source.rfind("\n", 0, item.start) + 1
        last_line = source.find("\n", item.end)
        if last_line == -1:
            last_line = len(source)
        if not source[first_line:item.start].strip() and not source[item.end:last_line].strip():
            source = source[:first_line] + source[min(last_line + 1, len(source)):]
        else:
            before = source[:item.start].rstrip(" \t")
            after = source[item.end:]
            space = " " if after and after[0] not in "\n \t" else ""
            source = before + space + after
    return source


def violations(current, allowed):
    return {path: Counter(counts) - Counter(allowed.get(path, {})) for path, counts in current.items()
            if Counter(counts) - Counter(allowed.get(path, {}))}


def main():
    parser = argparse.ArgumentParser(description="Enforce the repository's comment ratchet.")
    parser.add_argument("mode", choices=("check", "shrink", "inventory"))
    parser.add_argument("--base", help="also ensure the committed baseline does not grow relative to this Git ref")
    args = parser.parse_args()
    source_by_path = dict(sources())
    entries = [item for path, source in source_by_path.items() for item in comments(path, source)]
    if args.mode == "inventory":
        for item in entries:
            print(json.dumps({**vars(item), "fingerprint": item.fingerprint}, ensure_ascii=True))
        return 0

    baseline = ROOT / BASELINE
    allowed = json.loads(baseline.read_text(encoding="utf-8"))
    current = snapshot(entries)
    additions = violations(current, allowed)
    if additions:
        for item in entries:
            if not item.protected and item.fingerprint in additions.get(item.path, {}):
                print(f"{item.path}:{item.line}: new comment")
        return 1

    if args.base:
        git("rev-parse", "--verify", f"{args.base}^{{commit}}")
        previous = git("ls-tree", args.base, "--", BASELINE).stdout
        if previous and violations(allowed, json.loads(git("show", f"{args.base}:{BASELINE}").stdout)):
            print("The comment baseline grew relative to the base commit.")
            return 1

    if args.mode == "shrink":
        baseline.write_text(json.dumps(current, indent=2) + "\n", encoding="utf-8", newline="\n")
    elif current != allowed:
        print("Remove unused allowances with: python tools/comment_lint.py shrink")
        return 1

    total = sum(sum(counts.values()) for counts in current.values())
    print(f"Comment ratchet passed with {total} grandfathered comments.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
