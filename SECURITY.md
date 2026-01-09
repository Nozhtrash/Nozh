# Security Policy

## Supported Versions

We take security seriously. The following versions are currently supported with security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

**Please do NOT report security vulnerabilities through public GitHub issues.**

If you discover a security vulnerability, please report it responsibly:

### How to Report

1. **Via GitHub Security Advisories** (Recommended)
   - Go to the [Security tab](../../security/advisories)
   - Click "Report a vulnerability"
   - Fill out the form with details

2. **Via Email**
   - Send details to the repository maintainers
   - Include as much information as possible

### What to Include

Please include the following information:

- Type of vulnerability
- Full paths of affected source files
- Location of the affected code (tag/branch/commit)
- Step-by-step instructions to reproduce
- Proof-of-concept or exploit code (if possible)
- Impact assessment
- Potential fixes (if you have suggestions)

### Response Timeline

- **Initial Response**: Within 48 hours
- **Status Update**: Within 7 days
- **Fix Timeline**: Depends on severity
  - Critical: 1-7 days
  - High: 7-30 days
  - Medium: 30-90 days
  - Low: Next release cycle

## Security Best Practices for Contributors

### For Code Contributors

- Never commit credentials, API keys, or secrets
- Use environment variables for configuration
- Keep dependencies up to date
- Follow secure coding practices
- Run security checks before submitting PRs

### For Users

- Always use the latest stable version
- Keep your Minecraft and Fabric installations updated
- Only download from official sources
- Review permissions requested by mods
- Report suspicious behavior immediately

## Dependency Security

We regularly monitor and update dependencies for known vulnerabilities:

- Fabric API and Loader updates are tracked
- Security patches are prioritized
- Dependabot alerts are reviewed promptly

## Privacy Considerations

This mod respects user privacy:

- **No telemetry collection** without explicit opt-in
- **No personal data transmission** to external servers
- **Local-only processing** of game data
- **Transparent data handling** in all features

## Code Security Measures

- All contributions are reviewed before merging
- Automated security scanning on pull requests
- Regular dependency audits
- Code signing for releases (planned)

## Disclosure Policy

When a security vulnerability is confirmed:

1. We will work on a fix privately
2. Credit will be given to the reporter (if desired)
3. A security advisory will be published
4. A patched version will be released
5. Public disclosure after fix is available

## Hall of Fame

*Security researchers who responsibly disclose vulnerabilities will be listed here (with permission).*

---

**Last Updated**: January 2026

Thank you for helping keep our project and community safe!
