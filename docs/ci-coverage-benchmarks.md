## CI additions (Coverage + Benchmarks)

This folder documents a CI hardening step aligned with `future.txt` and the repository's technical debt audit.

### Coverage workflow

- Runs `./gradlew test`
- If the Gradle task `jacocoTestReport` exists, runs it and uploads the report
- Always uploads test/coverage artifacts, but never fails if coverage isn't configured yet

### Benchmarks workflow

- If the Gradle task `jmh` exists, runs `./gradlew jmh`
- Uploads JMH artifacts if produced
- Designed to be safe even when JMH is not configured
