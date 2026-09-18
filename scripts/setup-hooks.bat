@echo off
REM ===========================================================================
REM FixLink - Setup Git Pre-Commit Hooks (Windows)
REM ===========================================================================

echo Configuring Git core.hooksPath to .githooks...
git config core.hooksPath .githooks

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Git pre-commit hooks have been successfully configured!
    echo Pre-commit hooks will automatically check code formatting before each commit.
) else (
    echo [ERROR] Failed to configure git hooks. Make sure you are inside the Git repository.
)

pause
