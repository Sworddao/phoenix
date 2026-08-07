# Contributing to Phoenix

Thank you for your interest in contributing to Phoenix! This document provides guidelines and information for contributors.

---

## Getting Started

1. **Read the specification** — [SPEC.md](SPEC.md) is the single source of truth
2. **Fork the repository**
3. **Create a feature branch** — `git checkout -b feature/your-feature-name`
4. **Make your changes**
5. **Test thoroughly**
6. **Submit a pull request**

---

## Development Setup

### Prerequisites

- Android Studio (latest stable)
- JDK 17+
- Kotlin 1.9+
- Gradle 8.0+

### Building

```bash
./gradlew assembleDebug
```

### Testing

```bash
./gradlew testDebugUnitTest
```

The unit suite covers data models, repositories (mock and Room-backed via an in-memory `RoomTestDb` harness), ViewModels, the spaced repetition engine, and Room migration integrity (currently 898 tests).

---

## Code Standards

### Kotlin Style

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Prefer extension functions when appropriate
- Use coroutines for asynchronous operations

### Architecture

- Follow MVVM + Clean Architecture patterns
- Keep UI logic in Composables
- Use ViewModels for state management
- Repositories handle data access
- Use Cases encapsulate business logic

### Commit Messages

Use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add NPC dialogue system
fix: resolve audio playback issue
docs: update README
refactor: simplify quest completion logic
```

---

## Pull Request Guidelines

### Before Submitting

- [ ] Code compiles without errors
- [ ] All existing tests pass
- [ ] New tests added for new functionality
- [ ] Documentation updated if needed
- [ ] No hardcoded strings (use resources)
- [ ] Accessibility requirements met

### PR Description

Include:

- What changes were made
- Why the changes were necessary
- How to test the changes
- Any screenshots or recordings

---

## Reporting Issues

### Bug Reports

Include:

- Device model and Android version
- Steps to reproduce
- Expected behavior
- Actual behavior
- Screenshots if applicable

### Feature Requests

Include:

- Clear description of the feature
- How it aligns with the project vision
- Any mockups or examples

---

## Areas for Contribution

- **Audio recording** — Native Mandarin voice samples
- **Curriculum development** — Vocabulary and grammar content
- **UI/UX design** — Interface improvements
- **Accessibility** — Screen reader support, contrast, etc.
- **Testing** — Unit tests, integration tests
- **Documentation** — Guides, tutorials, translations
- **Bug fixes** — Check open issues

---

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Questions?

Open a discussion in the repository or create an issue.

Thank you for helping make language learning accessible to everyone!
