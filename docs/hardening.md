# Hardening (v0.3)

This folder contains small, low-risk safeguards intended to prevent accidental regressions.

## Lang validation in CI

A GitHub Actions workflow validates that all `src/main/resources/assets/nozh/lang/*.json` files:

- Are valid JSON.
- Contain a minimal set of required keys (currently `key.nozh.apply_suggestion` and `category.nozh`).

If any file is malformed or missing keys, the workflow fails.
