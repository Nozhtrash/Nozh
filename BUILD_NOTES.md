# Build Configuration Notes

## Current Build Status

The repository uses Fabric Loom `1.6-SNAPSHOT` for building Minecraft 1.20.1 mods. This build configuration is correct but requires network access to `https://maven.fabricmc.net/` to download the Fabric Loom Gradle plugin.

### Network Requirements

The build process requires access to:
- `maven.fabricmc.net` - For Fabric Loom plugin and Fabric API
- `repo.maven.apache.org` - For Maven Central dependencies
- `plugins.gradle.org` - For Gradle plugin portal

### Common Build Issues

#### Issue: "Plugin fabric-loom was not found"

**Symptoms:**
```
Plugin [id: 'fabric-loom', version: '1.6-SNAPSHOT'] was not found
```

**Causes:**
1. Network connectivity issues to maven.fabricmc.net
2. DNS resolution problems
3. Corporate firewall or proxy blocking access
4. Snapshot version no longer available

**Solutions:**

1. **Verify Network Access:**
   ```bash
   ping maven.fabricmc.net
   curl -I https://maven.fabricmc.net/
   ```

2. **Check DNS Resolution:**
   If DNS fails, try adding to `/etc/hosts` or using a different DNS server.

3. **Clear Gradle Cache:**
   ```bash
   rm -rf ~/.gradle/caches/
   ./gradlew clean --refresh-dependencies
   ```

4. **Use Stable Version** (if SNAPSHOT unavailable):
   Edit `gradle.properties` and try these versions in order:
   ```properties
   loom_version=1.6-SNAPSHOT   # Current (preferred)
   loom_version=1.5-SNAPSHOT   # Alternative
   loom_version=1.4-SNAPSHOT   # Older but stable
   loom_version=1.3-SNAPSHOT   # Most stable for MC 1.20.1
   ```

5. **Corporate Network Setup:**
   If behind a corporate proxy, configure in `~/.gradle/gradle.properties`:
   ```properties
   systemProp.http.proxyHost=your.proxy.host
   systemProp.http.proxyPort=8080
   systemProp.https.proxyHost=your.proxy.host
   systemProp.https.proxyPort=8080
   ```

6. **Use Local Maven Mirror:**
   If you have a local Nexus/Artifactory instance:
   ```groovy
   // In settings.gradle
   pluginManagement {
       repositories {
           maven {
               url = 'https://your-nexus.company.com/repository/fabric/'
           }
           maven {
               name = 'Fabric'
               url = 'https://maven.fabricmc.net/'
           }
           mavenCentral()
           gradlePluginPortal()
       }
   }
   ```

### CI/CD Considerations

For automated builds:

1. **Cache Dependencies:** Use Gradle build cache and dependency caching in CI
2. **Network Timeouts:** Increase timeout values if downloads are slow
3. **Retry Logic:** Implement retry logic for transient network failures
4. **Offline Builds:** Once dependencies are cached, use `--offline` mode

### Environment-Specific Issues

**Sandboxed/Restricted Environments:**
Some build environments (containers, CI runners, security sandboxes) may have restricted outbound network access. In such cases:

1. The build will fail with connection/DNS errors
2. Pre-cache dependencies before entering the restricted environment
3. Use offline build mode with cached dependencies
4. Consider vendoring critical dependencies if policies allow

**Docker/Container Builds:**
```dockerfile
# Pre-download dependencies in a connected environment
RUN ./gradlew build --no-daemon
# Then use offline mode in restricted environment
RUN ./gradlew build --offline --no-daemon
```

## Alternative: Building Without Network

If you have previously built the project successfully and have cached dependencies:

```bash
./gradlew build --offline --no-daemon
```

This will use only locally cached dependencies and skip all network requests.

## Troubleshooting Checklist

- [ ] Can you ping maven.fabricmc.net?
- [ ] Can you access https://maven.fabricmc.net/ in a browser?
- [ ] Have you tried clearing Gradle cache?
- [ ] Have you tried a different loom version?
- [ ] Are you behind a corporate firewall/proxy?
- [ ] Is your network restricting Maven repository access?
- [ ] Have you configured proxy settings in Gradle?

## Getting Help

If build issues persist after trying the above solutions:

1. Check [Fabric Discord](https://discord.gg/v6v4pMv) #toolchain-dev channel
2. Review [Fabric Wiki](https://fabricmc.net/wiki/) for Loom documentation
3. Search [Fabric Loom GitHub Issues](https://github.com/FabricMC/fabric-loom/issues)
4. Check this repository's GitHub Issues for similar problems

