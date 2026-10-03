# AGENTS.md

Context file for AI agents working on jruby.

**Dual Format**: This file combines Category A (Operations Manual) and Category B (Context Guide) for comprehensive agent guidance.

## Project Overview

jruby is a Ruby project using Makefile.

**Key Info:**
- **Primary Language:** Ruby
- **Build System:** Makefile
- **Test Framework:** RSpec
- **Total Files:** 11911
- **Test Files:** 3534
- **AI Readiness Score:** 93/100 (Agent-Optimized)

---

## 🚨 AI Policy & Operations

Extracted from CONTRIBUTING.md - operational constraints and procedures.

### AI Policy

- Thank you for your interest in JRuby! We appreciate any assistance you can provide. This guide will help you make sure your pull request is structured according to our standards.
- `lib`: Ruby standard library and gem sources live here, along with the primary build artifact of JRuby itself.
- `lib/ruby/stdlib`: The Ruby standard library. This is mostly populated at build time from default gems.
- `test/mri`: The suite of tests maintained in the standard Ruby (CRuby) repository. These tests are only updated by copying from the CRuby repository and should not be modified.
- Match the existing JRuby code standards and avoid including unrelated whitespace or formatting changes in your PR. JRuby's code standards are largely the same as accepted Java and Ruby standards. Please do not use tabs for indentation.

### Development Procedures

- Complete documentation for building JRuby lives within [BUILDING.md](BUILDING.md). Short instructions are also provided here.
- 2. Build JRuby using `./mvnw`
- 3. Bootstrap testing sources and libraries with `./mvnw -Pbootstrap`
- 4. Put the `bin/` directory in your `PATH` env (optional but recommended). Alternatively, configure your Ruby installation manager (rvm, chruby, etc) to point at the cloned JRuby as `jruby-master` or similar.
- `bin`: The launcher scripts for JRuby live here along with any binscripts installed by gems.



## 🏗️ Architecture & Context Guide

This section provides architectural context and agent-understanding for the codebase.

### Prerequisites

- **Ruby:** 2.7+ (or applicable language version)
- **Package Manager:** Bundler
- **Test Runner:** RSpec



### Project Structure

```
jruby/
├── pom.xml
├── pom.xml
├── pom.xml
├── src/                  # Source code
├── tests/                # Test suite (3534 files)
└── README.md             # Project documentation
```

### Architecture Overview

#### Key Components
- **Main Entry:** JavaInterfaceBenchmark.java, FixnumCreationBenchmark.java, StringLoopBenchmark.java, NilCheckBenchmark.java, ArrayCopyBenchmark.java
- **Test Suite:** 3534 test files
- **Build Configuration:** pom.xml, pom.xml, pom.xml

#### Design Principles

1. **Modularity** - Code organized by functionality with clear separation of concerns
2. **Testability** - Comprehensive test coverage across critical paths
3. **Clarity** - Explicit naming and structure for AI agent understanding
4. **Consistency** - Uniform patterns and conventions throughout codebase
5. **Maintainability** - Well-documented code with clear intent

### Directory Map

| Directory | Purpose |
|-----------|----------|
| `lib/` | Library code |
| `spec/` | Test specifications |
| `test/` | Test suite |


### Development Workflow

#### Initial Setup

```bash
git clone https://github.com/YOUR_ORG/jruby.git
cd jruby
bundle install
```

#### Development Commands

**Running Tests:**
```bash
bundle exec rspec         # Run RSpec tests
bundle exec rspec spec/   # Run specific directory
bundle exec rspec -v      # Verbose output
```

#### Code Quality
```bash
bundle exec rubocop       # Lint with RuboCop
bundle exec rubocop -a    # Auto-fix issues
```

### Code Style & Conventions

- **Naming:** Use Ruby conventions (snake_case for functions, PascalCase for classes)
- **Type Hints:** Yes (strongly encouraged)
- **Error Handling:** Yes - handle errors at boundaries; let exceptions propagate when another layer owns recovery
- **Logging:** Yes
- **Testing:** Yes - write tests alongside code changes

### Testing Strategy

**Framework:** RSpec
**Test Files:** 3534 found

Before committing:
1. Run the full test suite: `bundle exec rspec`
2. Run specific test directory: `bundle exec rspec spec/`
3. Lint with RuboCop: `bundle exec rubocop`
4. Auto-fix issues: `bundle exec rubocop -a`

### Writing Documentation

When updating docs:
1. Always include explanatory text before code snippets
2. Describe *why* and *what* before showing *how*
3. Keep sections focused on a single concept
4. Use clear, concrete examples

## Known Gotchas & Warnings

- `test/jruby`: Primarily JRuby-specific tests, for functionality specific to JRuby that does not fit elsewhere. This suite has shrunk over the years as we moved more to `spec` suites and you probably don't want to add new tests here.
- Keep your PRs topical; don't combine many unrelated changes into a single pull request. This helps us review and merge your changes quickly and provides a better historical record for future maintainers. This also applies to your commits; if you can do many smaller commits, that's usually better for auditing changes in the future.
- Match the existing JRuby code standards and avoid including unrelated whitespace or formatting changes in your PR. JRuby's code standards are largely the same as accepted Java and Ruby standards. Please do not use tabs for indentation.

### Contributing Guidelines

This project has a detailed contribution guide at **`CONTRIBUTING.md`**.

**Key Requirements:**
- Review the contribution guide for all requirements
- Follow established patterns in the codebase
- Ensure alignment with project's contribution policies

### Common Patterns

When contributing to this project:
1. Read existing code in the area you're modifying
2. Follow the established patterns and style
3. Write tests for new functionality
4. Use clear, descriptive variable and function names
5. Add docstrings for public APIs
6. Update tests when changing behavior

### What We Value

✅ Well-tested code with clear intent
✅ Consistent code style and naming conventions
✅ Code that is easy for AI agents to understand
✅ Clear, descriptive commit messages
✅ Modular, reusable components
✅ Comprehensive documentation

### What We Avoid

❌ Large functions doing multiple things
❌ Commented-out dead code
❌ Inconsistent naming or patterns
❌ Unclear error messages
❌ Unexplained magic numbers or strings
❌ Skipped tests or test TODOs

### AI Readiness Dimensions (Scoring)

This project is evaluated across 8 dimensions:

1. **Architecture** (20/100) - Code organization and modularity
2. **Testing** (15/100) - Test coverage and quality
3. **Dependencies** (12/100) - Dependency management
4. **Conventions** (8/100) - Consistent patterns
5. **Entry Points** (10/100) - Clear main/start locations
6. **Security** (10/100) - Input validation and error handling
7. **Build** (10/100) - Clear build/setup instructions
8. **Documentation** (8/100) - Code and project documentation

### Next Steps

Before making changes:
1. Read relevant source files to understand the existing code
2. Look at existing tests for similar functionality
3. Follow the patterns you see in the codebase
4. Write tests for your changes
5. Run `pytest` to verify nothing breaks
6. Run code quality checks: `ruff check . && mypy .`
7. Format your code: `ruff format .`

---

*Generated by Braxis - keeping AI agents in sync with your code*
