# Contributing Guidelines

This document provides guidelines for contributing to the project.

## Development Workflow

### 1. Branch Strategy
- `main` - Production-ready code
- `develop` - Development branch
- `feature/*` - New features
- `bugfix/*` - Bug fixes
- `release/*` - Release preparation

### 2. Commit Messages
- Use present tense ("Add feature" not "Added feature")
- Use imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally

### 3. Pull Requests
- Update documentation
- Add tests for new functionality
- Ensure all tests pass
- Follow the existing code style

## Architecture Guidelines

### MVVM Pattern
- ViewModels for business logic
- LiveData/Flow for data observation
- Repository pattern for data operations
- Use cases for complex business logic

### Dependency Injection
- Use Hilt for dependency injection
- Provide dependencies through modules
- Use @Inject for constructor injection
- Use @Module for providing dependencies

## Testing Guidelines

### Unit Tests
- Test each class in isolation
- Use descriptive test names
- Follow AAA pattern (Arrange, Act, Assert)
- Mock dependencies

### Integration Tests
- Test component interactions
- Verify data flow
- Test repository implementations
- Medium execution time

### UI Tests
- Test user flows
- Use Espresso for UI testing
- Test edge cases
- Verify UI state changes

## Code Quality

### Linting
- Run ktlint before committing
- Fix all warnings
- Follow Kotlin style guide

### Documentation
- Document public APIs
- Keep README up to date
- Document architecture decisions
- Include examples where necessary

### Performance
- Profile critical paths
- Optimize memory usage
- Monitor app size
- Test on low-end devices

## Release Process

### 1. Versioning
- Follow semantic versioning
- Update version in build.gradle
- Update changelog

### 2. Testing
- Run all tests
- Perform manual testing
- Test on multiple devices
- Verify all features

### 3. Documentation
- Update documentation
- Generate API docs
- Update changelog
- Tag release

## Getting Help

- Check existing documentation
- Search closed issues
- Ask in discussions
- Contact maintainers 