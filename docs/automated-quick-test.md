# Automated Modpack Quick Test Runner

This guide explains how to run the automated quick-test executor and how to
prepare large modpacks (Better Minecraft, ATM9) for consistent runs.

## What the runner does

The runner translates `docs/modpack-quick-test.md` into reproducible steps with
teleports, actions, and fixed durations. It outputs:

- `quick-test-plan.json` (scenario steps + timing windows)
- `quick-test-results.json` (metadata + P95/P99 stats)
- `quick-test-results.csv` (table format from `docs/benchmarking.md`)

## Running the Gradle task

From the repo root:

```bash
./gradlew quickTest \
  -Pmodpack="Better Minecraft" \
  -Pseed=123456789 \
  -Pscenarios=mobs,mining,nether,rain \
  -PoutputDir=build/test-results/quick-test \
  -PsamplesDir=/path/to/frametime-samples
```

Optional timing overrides:

```bash
./gradlew quickTest -PwarmupSeconds=60 -PmeasurementSeconds=180 -PcooldownSeconds=30
```

### Scenario list

Available scenario IDs (defaults to all):

- `mobs`
- `mining`
- `nether`
- `rain`

## Providing frametime samples

The executor computes P95/P99 from sample files. Place one file per scenario in
`-PsamplesDir`, using either `.csv` or `.txt`:

```
/path/to/frametime-samples/
  mobs.csv
  mining.csv
  nether.csv
  rain.csv
```

Each file can contain one number per line or comma/space-separated values:

```
13.4, 15.2, 11.9
14.1
```

If no samples are provided, the runner emits the plan and placeholder results
with notes explaining what is missing.

## Environment metadata

Provide hardware and environment metadata via environment variables so the
JSON/CSV outputs follow `docs/benchmarking.md`:

```
export NOZH_CPU="Ryzen 7 7800X3D"
export NOZH_GPU="RTX 4070"
export NOZH_RAM="32GB DDR5"
export NOZH_DISPLAY="2560x1440 @ 144Hz"
export NOZH_RENDERER="Sodium 0.5.3"
export NOZH_SHADERS="Complementary 4.0"
export NOZH_POWER_MODE="High Performance"
export NOZH_BACKGROUND_LOAD="Discord + OBS"
export NOZH_MC_VERSION="1.20.1"
```

## Runner setup for large modpacks

### Better Minecraft

- Use a dedicated launcher profile with **clean configs**.
- Allocate **8–10 GB RAM** (avoid auto-managed allocation).
- Disable shader packs until baseline data is captured.
- Use render distance **12–16** and simulation distance **8–12**.
- Pre-generate a world with the target seed, and note teleport coords for each
  scenario area (mob farm, cave, nether biome, rain view).

### All The Mods 9 (ATM9)

- Use a dedicated profile and **fresh world** for consistent chunk state.
- Allocate **10–12 GB RAM** and avoid background downloads/updates.
- Confirm that chunk pre-generation is complete before running scenarios.
- Capture the same scenario coordinates in a shared runbook so each run uses the
  exact teleport location.

## Suggested workflow

1. Launch the modpack with the target seed.
2. Gather scenario coordinates and update your local runbook.
3. Record frametime samples while executing each scenario.
4. Run `./gradlew quickTest` with `-PsamplesDir` to generate JSON/CSV output.
