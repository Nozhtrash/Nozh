# Build Configuration Notes

## Fabric Loom Version

### Issue
The project originally used `loom_version=1.6-SNAPSHOT` which is no longer available in the Fabric Maven repository. Additionally, some build environments may have restricted network access to `maven.fabricmc.net`.

### Solution
The `settings.gradle` has been updated with a proper plugin resolution strategy to correctly resolve Fabric Loom from the Fabric Maven repository:

```groovy
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = 'Fabric'
            url = 'https://maven.fabricmc.net/'
        }
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == 'fabric-loom') {
                useModule("net.fabricmc:fabric-loom:${requested.version}")
            }
        }
    }
}
```

### Recommended Versions for Minecraft 1.20.1

If the current `loom_version` in `gradle.properties` fails to resolve, try these stable versions:
- `1.3-SNAPSHOT` (commonly used for MC 1.20.1)
- `1.4-SNAPSHOT` (newer, if available)
- `1.2.7` (last stable 1.2.x release)
- `1.1.12` (older but very stable)

### Troubleshooting

If you encounter build failures with fabric-loom:

1. **Check Network Access**: Ensure you can reach `maven.fabricmc.net`:
   ```bash
   ping maven.fabricmc.net
   curl -I https://maven.fabricmc.net/
   ```

2. **Clear Gradle Cache**: Sometimes old cached metadata causes issues:
   ```bash
   rm -rf ~/.gradle/caches/
   ./gradlew clean --refresh-dependencies
   ```

3. **Try a Different Version**: Update `loom_version` in `gradle.properties` to a known stable version.

4. **Use Gradle Offline Mode**: If you have previously built the project successfully:
   ```bash
   ./gradlew build --offline
   ```

## Environment Limitations

In some CI/CD or sandboxed environments, access to external Maven repositories may be restricted. If building in such an environment:

- Pre-cache dependencies in your CI pipeline
- Use a local Maven mirror or proxy (e.g., Nexus, Artifactory)
- Consider using Gradle's dependency locking feature
