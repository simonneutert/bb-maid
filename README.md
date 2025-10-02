# Babashka Cleanup Script<!-- omit in toc -->

This Babashka script recursively searches through directories from a given entry
point, identifies files named in the format `cleanup-maid-YYYY-MM-DD`, and
deletes their parent directories if the date has passed. The script will prompt
the user for confirmation before deleting any directories.

---

![bb-maid-logo](https://github.com/user-attachments/assets/2e634a95-dd49-4f33-8bb2-87194753b0f7)

---

## Concept

Think of this script like a helper, that keeps your digital household clean. Each directory is like a container of food, and inside each container, there’s a label (cleanup-maid-YYYY-MM-DD) that tells you when it expires.

If today’s date is past the expiration date on the label, the script asks you, “Hey, this has expired—should I throw it out?” If you say yes, it cleans out the whole container (directory), just like you’d toss out spoiled food to keep your fridge fresh.

This way, your system stays clean, just like your fridge stays free of expired leftovers (most of the time)!

## Table of Contents<!-- omit in toc -->

- [Concept](#concept)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
  - [Quick Install with bbin (Requires Java)](#quick-install-with-bbin-requires-java)
  - [Manual Installation](#manual-installation)
    - [Option 1: Add to PATH](#option-1-add-to-path)
    - [Option 2: Script-adjacent bb.edn](#option-2-script-adjacent-bbedn)
  - [Verify Installation](#verify-installation)
- [Usage](#usage)
  - [Cleaning Up Expired Directories](#cleaning-up-expired-directories)
  - [Creating Cleanup Files](#creating-cleanup-files)
  - [Example](#example)
- [How It Works](#how-it-works)
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

#### Option 1: Add to PATH

1. Clone the repository:

```sh
git clone https://github.com/simonneutert/bb-maid.git
cd bb-maid
```

2. Make the script executable:

```sh
chmod +x maid.clj
```

3. Add the directory to your PATH or create a symlink:

```sh
# Add to PATH (add this to your ~/.zshrc or ~/.bashrc)
export PATH="$PATH:/path/to/bb-maid"

# OR create a symlink
ln -s /path/to/bb-maid/maid.clj /usr/local/bin/bb-maid
```

#### Option 2: Script-adjacent bb.edn

Copy `maid.clj` and `bb.edn` to a directory in your PATH (e.g., `~/bin` or `/usr/local/bin`). Babashka will automatically find and use the adjacent `bb.edn` file for dependencies.

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
bb-maid /path/to/start
```

**With local repository:**
```sh
bb clean /path/to/start
```

The script will search for files named `cleanup-maid-YYYY-MM-DD`, check if the
date has passed, and prompt the user before deleting the corresponding
directory.

### Creating Cleanup Files

To create a cleanup file that will expire after a specified duration:

**With bbin installation:**
```sh
bb-maid clean-in 7d
```

**With local repository:**
```sh
bb clean-in 7d
```

This command creates a file named `cleanup-maid-YYYY-MM-DD` with a date 7 days in the future. You can use any number of days (e.g., `1d`, `30d`, `90d`).

### Example

Alternatively, you can manually create a cleanup file using:

```sh
touch cleanup-maid-$(date -d "+7 days" +"%Y-%m-%d" 2>/dev/null || date -v+7d +"%Y-%m-%d")
```

## How It Works

1. The script scans all files in subdirectories of the given entry point.
2. It looks for filenames matching `cleanup-maid-YYYY-MM-DD`.
3. If the date in the filename is past today, the script prompts the user for
   confirmation before deleting the directory.

## License

MIT License
