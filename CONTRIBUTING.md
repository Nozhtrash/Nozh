# Contributing to NOZH

First off, thank you for considering contributing to NOZH. This project follows a strict **"Do No Harm"** philosophy.

## Core Rules

1. **NO Hallucinations**: Do not assume APIs exist. Verify everything against the codebase or official Fabric/Minecraft documentation.
2. **Safety First**:
    * All critical paths must be wrapped in `try-catch` blocks.
    * Never crash the game from a profiler or governor thread.
    * The `CrashLoopGuard` must be respected.
3. **Zero Allocations**: Use `FrameTimeSampler` and `RollingWindowStats` patterns. Do not allocate objects (`new ...`) inside the render loop (`onFrame`).
4. **Strict Typing**: Use Enums (`ActionType`, `DecisionSeverity`) instead of strings or magic numbers.
5. **Hygiene**:
    *   **NO `System.out` / `System.err` / `printStackTrace`**. (Commit will be rejected)
    *   Use `NozhConstants.LOGGER` for all logging.

## Development Workflow

We use a **pre-commit hook** to ensure quality:
1.  **Automatic Tests**: `./gradlew quickTest` runs before every commit.
2.  **Sanitation Check**: Staged files are scanned for banned keywords.

**To bypass checks (Emergency Only):**
```bash
git commit --no-verify -m "Emergency fix"
```
*Note: Bypassing checks for non-emergencies is grounds for PR rejection.*

## Pull Request Process

1. **Compile & Test**: Run `./gradlew build` before submitting. Ensure no warnings are introduced.
2. **Phase Adherence**: Identify which "Phase" your change belongs to (see `ROADMAP.md`).
3. **Documentation**: Update `ARCHITECTURE.md` if you change core logic.

## Mod Architecture

NOZH is divided into:

* `dev.nozh.core`: Pure Java logic (No Minecraft dependencies where possible).
* `dev.nozh.client`: Fabric/Minecraft integration glue.
* `dev.nozh.api`: Public interfaces and records.

Please keep `core` clean of `net.minecraft` imports unless absolutely necessary (e.g., inside an `ActionHandler`).
