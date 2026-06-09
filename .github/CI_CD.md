# CI/CD Pipeline Documentation

## Overview

This project uses GitHub Actions to automatically run tests and lint checks on every pull request. The CI pipeline ensures code quality and stability before merging.

## Workflows

### 1. **PR Checks** (`.github/workflows/pr-checks.yml`)
Runs on every pull request and includes:

- **Lint Check**: Validates code style and Android lint rules
- **Unit Tests**: Runs all unit tests with `./gradlew test`
- **Build**: Assembles debug APK to verify successful build
- **Code Analysis**: Runs comprehensive code quality checks with `./gradlew check`

**Runs on**: `ubuntu-latest`  
**Triggers**: Pull requests to `main`, `develop`, or `master`

### 2. **Instrumented Tests** (`.github/workflows/instrumented-tests.yml`)
Runs Android instrumented tests on an emulator:

- **Android Emulator**: Spins up API level 30 emulator
- **Instrumented Tests**: Executes all Android instrumented tests
- **Artifacts**: Uploads test results for review

**Runs on**: `macos-latest`  
**Triggers**: Pull requests to `main`, `develop`, or `master`

## Local Testing

Before pushing, you can run the same checks locally:

```bash
# Lint check
./gradlew lint

# Unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Code quality checks
./gradlew check

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Run all checks together
./gradlew build
```

## Pipeline Status

Check the "Checks" tab on your pull request to see real-time pipeline status. All checks must pass before merging.

## Troubleshooting

### Build failures
- Ensure your code compiles: `./gradlew assemble`
- Check for kotlin syntax errors: `./gradlew compileDebugKotlin`

### Lint failures
- Review lint warnings: `./gradlew lint`
- Fix issues and commit changes

### Test failures
- Run tests locally: `./gradlew test`
- Check test reports in `build/reports/tests/`

### Instrumented test failures
- Ensure emulator compatibility (API 30+)
- Check device logs in artifacts

## Adding New Checks

To add additional checks to the pipeline:

1. Modify `.github/workflows/pr-checks.yml` or `.github/workflows/instrumented-tests.yml`
2. Add a new job step with your check command
3. Commit and push the changes

## Dependencies

- **JDK 11**: Required for compilation
- **Gradle**: Managed via gradle wrapper (gradlew)
- **Android SDK**: Handled by GitHub Actions environment

## Performance Tips

- The workflow uses gradle caching to speed up builds
- Jobs run in parallel for faster feedback
- Instrumented tests run on macOS for better emulator performance

