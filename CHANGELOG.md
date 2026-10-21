<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Roundtripper Changelog

## [Unreleased]

## [1.0.0] - 2026-10-21

### Added

- Settings page (Settings | Tools | Roundtripper) for registering executables with a friendly name.
- Roundtripper sub-menu in the editor context menu, with one entry per registered executable.
- `text` mode: pipes the selection through the executable and replaces it with stdout on exit code 0.
- `position` mode: inserts the executable's stdout at the caret on exit code 0.
- Notifications with the exit code and stderr when an executable fails.
