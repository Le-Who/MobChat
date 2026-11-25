---
name: delete-comment
description: Audit and remove comments from MobChat source while preserving required directives and documenting decisions.
---

# Delete comments

1. Run `python tools/comment_lint.py inventory` from the repository root. Narrow by path when the request is scoped. Review each remaining entry against the surrounding code, callers, tests, and Git history when intent is unclear.
2. Record one disposition per entry. Delete **Tautology** when the code already shows the same fact. Delete **Stale** after verifying the claim no longer holds. For **Excuse**, identify the defective behavior and file or reuse an issue if the user authorized issue creation. Then delete the comment. Move **Decision Record** rationale into the matching ADR under `docs/adr/` or add one when the decision is new.
3. Keep licenses, generated wrapper text, and tool-required directives. Leave a comment in the baseline when its rationale cannot be verified. Record the missing evidence with its path and line. Keep source tokens separated when removing inline block comments.
4. Run `python tools/comment_lint.py shrink` and `python tools/comment_lint.py check`. Run the focused check for any behavior changed while resolving an excuse. Report category counts, physical source lines removed, ADRs, issues, and unresolved entries.

The baseline can only shrink. The GitHub workflow compares it to the base commit for both pull requests and pushes to `main`.
