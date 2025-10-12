# Development Guide

This guide covers everything you need to know for developing and contributing to bb-maid.

## Table of Contents<!-- omit in toc -->

- [Development Guide](#development-guide)
  - [Local Development Setup](#local-development-setup)
    - [Clone the Repository](#clone-the-repository)
    - [Option A: Run tasks directly using babashka (quick testing)](#option-a-run-tasks-directly-using-babashka-quick-testing)
    - [Option B: Install locally with bbin (test as end-users would experience it)](#option-b-install-locally-with-bbin-test-as-end-users-would-experience-it)
    - [When to use each option](#when-to-use-each-option)
  - [Project Structure](#project-structure)
  - [Running Tests](#running-tests)
  - [Adding Tests](#adding-tests)
  - [Tab Completion Development](#tab-completion-development)

## Local Development Setup

### Clone the Repository

```sh
git clone https://github.com/simonneutert/bb-maid.git
cd bb-maid
```

### Option A: Run tasks directly using babashka (quick testing)

```sh
bb tasks        # List available tasks
bb clean .      # Clean current directory
bb clean-in 7d  # Create cleanup file
bb test         # Run tests
```

### Option B: Install locally with bbin (test as end-users would experience it)

```sh
# Install from project root
bbin install .

# Now test the actual bb-maid command
bb-maid clean --help
bb-maid clean-in 7d --gitignore

# After making changes, reinstall to test updates
bbin install . --force
```

### When to use each option

- Use **Option A** (`bb` tasks) for quick iteration during development
- Use **Option B** (`bbin install .`) to test the actual user experience, including:
  - Command-line interface behavior
  - Tab completion functionality
  - Installation and upgrade workflows

## Project Structure

The project uses a proper Babashka structure with:
- Source code in `src/simonneutert/bb_maid.clj`
- Tasks defined in `bb.edn` that call functions directly
- Tests in `test/simonneutert/bb_maid_test.clj`
- Tab completion scripts in `completions/`

## Running Tests

bb-maid includes a test suite using `clojure.test`. To run the tests:

```sh
bb test
```

Or run the test runner directly:

```sh
bb test-runner.clj
```

The test suite covers:
- Date parsing and validation
- Duration string parsing (`7d`, `30d`, etc.)
- Command-line option parsing
- Option combinations and defaults
- Symlink handling behavior (default: disabled, with --follow-links flag)
- Git integration features
- Gitignore file handling

## Adding Tests

Tests are located in `test/simonneutert/bb_maid_test.clj`. To add new tests:

1. Add your test to the test namespace
2. Run `bb test` to verify

Example test:

```clojure
(deftest my-new-test
  (testing "Description of what you're testing"
    (is (= expected-value (function-call args)))))
```

## Tab Completion Development

Tab completion scripts are located in the `completions/` directory:
- `_bb-maid` - Zsh completion
- `bb-maid.bash` - Bash completion

When adding new commands or options, make sure to update both completion scripts.

To test tab completion during development:

**Zsh:**
```sh
# Temporarily load local completion
fpath=(./completions $fpath)
autoload -Uz compinit && compinit
```

**Bash:**
```sh
# Temporarily load local completion
source ./completions/bb-maid.bash
```
