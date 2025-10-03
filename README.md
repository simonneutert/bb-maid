# Babashka Cleanup Script<!-- omit in toc -->

This Babashka script recursively searches through directories from a given entry
point, identifies files named in the format `cleanup-maid-YYYY-MM-DD`, and
deletes their parent directories if the date has passed. The script will prompt
the user for confirmation before deleting any directories.

<img width="auto" height="200" alt="bbmaid602c47af-06c1-4ed8-8ae7-d9464a6ac571" src="https://github.com/user-attachments/assets/8e4dee17-6a38-4724-884c-5022a09868d4" />

---

## Concept

Think of this script like a helper, that keeps your digital household clean. Each directory is like a container of food, and inside each container, there’s a label (cleanup-maid-YYYY-MM-DD) that tells you when it expires.

If today’s date is past the expiration date on the label, the script asks you, “Hey, this has expired—should I throw it out?” If you say yes, it cleans out the whole container (directory), just like you’d toss out spoiled food to keep your fridge fresh.

This way, your system stays clean, just like your fridge stays free of expired leftovers (most of the time)!

## Table of Contents<!-- omit in toc -->

- [Concept](#concept)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
  - [Quick Install with bbin (Recommended)](#quick-install-with-bbin-recommended)
  - [Manual Installation](#manual-installation)
    - [Local Development](#local-development)
  - [Verify Installation](#verify-installation)
- [Usage](#usage)
  - [Cleaning Up Expired Directories](#cleaning-up-expired-directories)
    - [Options](#options)
    - [Examples](#examples)
  - [Listing Cleanup Files](#listing-cleanup-files)
  - [Creating Cleanup Files](#creating-cleanup-files)
    - [Examples](#examples-1)
  - [Example](#example)
- [Tab Completion](#tab-completion)
  - [What Gets Completed](#what-gets-completed)
  - [Setup for Zsh](#setup-for-zsh)
  - [Setup for Bash](#setup-for-bash)
- [How It Works](#how-it-works)
  - [Safety Features](#safety-features)
- [Development](#development)
  - [Running Tests](#running-tests)
  - [Adding Tests](#adding-tests)
- [License](#license)

## Prerequisites

- [Babashka](https://github.com/babashka/babashka) installed on your system

## Installation

### Quick Install with bbin (Recommended)

The easiest way to install bb-maid is to use [bbin](https://github.com/babashka/bbin), a script manager for Babashka:

```sh
bbin install io.github.simonneutert/bb-maid
```

This will make the `bb-maid` command available globally on your system.

**Requirements:**
- [bbin](https://github.com/babashka/bbin) installed
- Java/JDK installed (required by bbin for dependency resolution)
- `~/.local/bin` in your PATH (add to your `~/.zshrc` or `~/.bashrc` if needed):
  ```sh
  export PATH="$HOME/.local/bin:$PATH"
  ```

After installation, verify it works:
```sh
bb-maid
# Should display usage information
```

**Note:** If you don't have Java or prefer not to install it, use the manual installation methods below.

### Manual Installation

#### Local Development

1. Clone the repository:

```sh
git clone https://github.com/simonneutert/bb-maid.git
cd bb-maid
```

2. Run tasks directly using babashka:

```sh
bb tasks        # List available tasks
bb clean .      # Clean current directory
bb clean-in 7d  # Create cleanup file
bb test         # Run tests
```

The project uses a proper Babashka structure with:
- Source code in `src/simonneutert/bb_maid.clj`
- Tasks defined in `bb.edn` that call functions directly
- Tests in `test/simonneutert/bb_maid_test.clj`

### Verify Installation

**For bbin installation:**
```sh
bb-maid
# Should display: Usage information
```

**For local repository:**
```sh
bb tasks
# Should display: Available tasks (clean, clean-in)
```

## Usage

> **Note:** If you installed via bbin, use the `bb-maid` command directly. If you cloned the repository, use `bb` with the task names (`bb clean`, `bb clean-in`).

### Cleaning Up Expired Directories

**With bbin installation:**
```sh
bb-maid clean [path] [options]
```

**With local repository:**
```sh
bb clean [path] [options]
```

The script will **recursively search** all subdirectories for files named `cleanup-maid-YYYY-MM-DD`, check if the date has passed, and prompt you before deleting each expired directory.

**If no path is provided, it defaults to the current directory.**

#### Options

- `--max-depth <n>` - Limit how deep to recurse into subdirectories (default: unlimited)
- `--follow-links` - Follow symbolic links (default: disabled for safety)
- `--yes` or `-y` - Skip confirmation prompts (useful for automation)
- `--dry-run` or `-n` - Show what would be deleted without actually deleting
- `--list` - List all cleanup files sorted by date without deleting anything

#### Examples

```sh
# Clean current directory (dry run)
bb-maid clean --dry-run

# Clean specific directory
bb-maid clean ~/projects --dry-run

# Only search 2 levels deep in current directory
bb-maid clean --max-depth 2

# Auto-confirm all deletions in a specific directory (be careful!)
bb-maid clean ~/temp-files --yes

# Combine options
bb-maid clean ~/projects --max-depth 3 --dry-run

# List all cleanup files to see what's scheduled
bb-maid clean --list

# List cleanup files in a specific directory
bb-maid clean ~/projects --list
```

**Note**: Symbolic links are NOT followed by default for safety reasons.

### Listing Cleanup Files

Want to see what directories are scheduled for cleanup without deleting anything? Use the `--list` option:

```sh
# List all cleanup files in current directory
bb-maid clean --list

# List with color-coded expiration status:
# 🔴 Red: Expired (ready for cleanup)
# 🟡 Yellow: Expiring soon (within 7 days)
# 🟢 Green: Expiring later (more than 7 days away)
```

**Example output:**
```
┏ INFO
┃ Found 4 cleanup files:
┗

2025-09-28 (EXPIRED 5 days ago)     /projects/old-feature
2025-10-01 (EXPIRED 2 days ago)     /temp/downloads
2025-10-06 (in 3 days)              /cache/build-artifacts
2025-10-13 (in 10 days)             /logs/october
```

The `--list` option is perfect for:
- 📊 Getting an overview of scheduled cleanups
- 🔍 Checking what's about to expire
- ✅ Verifying cleanup files before running actual deletion

### Creating Cleanup Files

To create a cleanup file that will expire after a specified duration:

**With bbin installation:**
```sh
bb-maid clean-in 7d [path]
```

**With local repository:**
```sh
bb clean-in 7d [path]
```

This command creates a file named `cleanup-maid-YYYY-MM-DD` with a date 7 days in the future. You can use any number of days (e.g., `1d`, `30d`, `90d`).

**If no path is provided, it defaults to the current directory.**

#### Examples

```sh
# Create cleanup file in current directory (expires in 7 days)
bb-maid clean-in 7d

# Create cleanup file in specific directory (expires in 30 days)
bb-maid clean-in 30d ~/temp-projects
```

### Example

Alternatively, you can manually create a cleanup file using:

```sh
touch cleanup-maid-$(date -d "+7 days" +"%Y-%m-%d" 2>/dev/null || date -v+7d +"%Y-%m-%d")
```

## Tab Completion

Tab completion helps you work faster by auto-completing commands and suggesting common durations.

### What Gets Completed

- `bb-maid <TAB>` → suggests `clean` and `clean-in` commands
- `bb-maid clean <TAB>` → shows available directories
- `bb-maid clean --<TAB>` → shows available options (--max-depth, --follow-links, --yes, --dry-run)
- `bb-maid clean-in <TAB>` → suggests common durations (1d, 7d, 14d, 30d, 60d, 90d)

### Setup for Zsh

Add this to your `~/.zshrc`:

```sh
# bb-maid tab completion
# If installed via bbin:
fpath=(~/.gitlibs/libs/io.github.simonneutert/bb-maid/*/completions(N) $fpath) && autoload -Uz compinit && compinit
```

```sh
# If cloned from git:
fpath=(/path/to/your/clone/bb-maid/completions $fpath)

autoload -Uz compinit && compinit
```

Then restart your terminal or run `source ~/.zshrc`.

### Setup for Bash

Add this to your `~/.bashrc`:

```sh
# bb-maid tab completion
# If installed via bbin:
for f in ~/.gitlibs/libs/io.github.simonneutert/bb-maid/*/completions/bb-maid.bash(N); do
  source "$f"
done
```

```sh
# If cloned from git:
source /path/to/your/clone/bb-maid/completions/bb-maid.bash
```

Then restart your terminal or run `source ~/.bashrc`.

## How It Works

1. **Recursive Scanning**: The script recursively scans **all subdirectories** of the given entry point, searching through the entire directory tree.
2. **Pattern Matching**: It looks for filenames matching `cleanup-maid-YYYY-MM-DD`.
3. **Date Check**: If the date in the filename is past today, the script identifies the directory as expired.
4. **User Confirmation**: Before any deletion, the script prompts you for confirmation.
5. **Safe Deletion**: Only after confirmation, the parent directory (and all its contents) is deleted.

### Safety Features

- **Directory Validation**: The script validates that the specified directory exists before attempting any operations. If a non-existent directory is provided, it will display a clear error message and exit gracefully.
- **Symlinks**: The script does **NOT** follow symbolic links by default, preventing infinite loops and protecting content outside the target directory tree. When symlinks are encountered, they are:
  - **Logged with warnings**: Each skipped symlink is reported with a yellow warning message
  - **Summarized**: A final count shows total symlinks skipped
  - **Optional traversal**: Use `--follow-links` flag to traverse symlinks if needed
- **Confirmation Required**: Every deletion requires explicit user confirmation (no silent deletions).
- **Non-destructive by default**: The script only suggests deletions; you have final control.
- **Dry-run mode**: Test operations safely with `--dry-run` before actual deletion.

## Development

### Running Tests

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

### Adding Tests

Tests are located in `test/simonneutert/bb_maid_test.clj`. To add new tests:

1. Add your test to the test namespace
2. Run `bb test` to verify

Example test:

```clojure
(deftest my-new-test
  (testing "Description of what you're testing"
    (is (= expected-value (function-call args)))))
```

## License

MIT License
