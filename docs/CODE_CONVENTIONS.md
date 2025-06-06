# Code Conventions

This document outlines the coding standards and naming conventions used in the project. For the full project structure, see [ARCHITECTURE.md](ARCHITECTURE.md).

## Naming Conventions

### Files and Classes
- **Activities**: `*Activity.kt` (e.g., `MainActivity.kt`)
- **Fragments**: `*Fragment.kt` (e.g., `ProfileFragment.kt`)
- **ViewModels**: `*ViewModel.kt` (e.g., `ProfileViewModel.kt`)
- **Adapters**: `*Adapter.kt` (e.g., `TaskAdapter.kt`)
- **Interfaces**: `I*` (e.g., `IRepository.kt`)
- **Data Classes**: `*Data.kt` (e.g., `UserData.kt`)
- **Utils**: `*Utils.kt` (e.g., `DateUtils.kt`)
- **Extensions**: `*Extensions.kt` (e.g., `ViewExtensions.kt`)

### Layout Files
- **Activities**: `activity_*.xml` (e.g., `activity_main.xml`)
- **Fragments**: `fragment_*.xml` (e.g., `fragment_profile.xml`)
- **Items**: `item_*.xml` (e.g., `item_task.xml`)
- **Dialogs**: `dialog_*.xml` (e.g., `dialog_settings.xml`)
- **Custom Views**: `view_*.xml` (e.g., `view_progress.xml`)

### Resource Files
- **Drawables**: `ic_*` for icons, `bg_*` for backgrounds
- **Colors**: `color_*` (e.g., `color_primary.xml`)
- **Strings**: `string_*` (e.g., `string_app_name.xml`)
- **Dimensions**: `dimen_*` (e.g., `dimen_margin.xml`)

## Code Style

### Kotlin
- Use 4 spaces for indentation
- Maximum line length: 100 characters
- Use `val` over `var` when possible
- Use expression bodies for simple functions
- Use string templates over concatenation
- Use scope functions appropriately

### XML
- Use 4 spaces for indentation
- One attribute per line
- Order attributes: id, layout, style, other
- Use descriptive IDs
- Use styles and themes

## Documentation
- Use KDoc for public APIs
- Document complex logic
- Keep comments up to date
- Use TODO comments for future work

## Testing
- Test file naming: `*Test.kt`
- Test class naming: `*Test`
- Test method naming: `test*`
- Use descriptive test names

## Git
- Branch naming: `feature/*`, `bugfix/*`, `hotfix/*`
- Commit messages: Present tense, descriptive
- Pull request naming: `[Type] Description`

## Architecture
For detailed information about architecture patterns, see [ARCHITECTURE.md](ARCHITECTURE.md).

## SOLID Principles
We follow the SOLID principles for maintainable and scalable code:
- **S**ingle Responsibility
- **O**pen/Closed
- **L**iskov Substitution
- **I**nterface Segregation
- **D**ependency Inversion

## Best Practices
1. Follow Android Architecture Components
2. Use ViewBinding
3. Implement proper error handling
4. Write unit tests
5. Use dependency injection
6. Follow Material Design guidelines
7. Implement proper logging
8. Use proper resource management 