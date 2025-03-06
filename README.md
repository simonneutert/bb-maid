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

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
  - [Example](#example)
  - [Set a shell alias](#set-a-shell-alias)
- [How It Works](#how-it-works)
- [License](#license)

## Prerequisites

- [Babashka](https://github.com/babashka/babashka) installed on your system

## Installation

Clone the repository and navigate to the script directory:

```sh
cd bb-maid
```

To show the available tasks, run:

```sh
bb tasks
```

## Usage

Run the script and specify the entry directory:

```sh
bb clean /path/to/start
```

The script will search for files named `cleanup-maid-YYYY-MM-DD`, check if the
date has passed, and prompt the user before deleting the corresponding
directory.

### Example

To create a test file that will trigger deletion in 7 days, use the following
command:

```sh
touch cleanup-maid-$(date -d "+7 days" +"%Y-%m-%d" 2>/dev/null || date -v+7d +"%Y-%m-%d")
```

This command creates a file named `cleanup-maid-YYYY-MM-DD` with a date 7 days in the future.

### Set a shell alias

To make the script easier to run, add an alias to your shell configuration file:

```sh
alias bb-maid='cd /path/to/bb-maid && bb clean'
```

Now you can run the script with the following command:

```sh
bb-maid /path/to/start
```

## How It Works

1. The script scans all files in subdirectories of the given entry point.
2. It looks for filenames matching `cleanup-maid-YYYY-MM-DD`.
3. If the date in the filename is past today, the script prompts the user for
   confirmation before deleting the directory.

## License

MIT License
