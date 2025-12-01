# Fish completion for bb-maid

# Disable file completion by default
complete -c bb-maid -f

# Main commands
complete -c bb-maid -n "not __fish_seen_subcommand_from clean clean-in gitignore" -a "clean" -d "Clean up expired directories"
complete -c bb-maid -n "not __fish_seen_subcommand_from clean clean-in gitignore" -a "clean-in" -d "Create a cleanup file with expiration date"
complete -c bb-maid -n "not __fish_seen_subcommand_from clean clean-in gitignore" -a "gitignore" -d "Add cleanup-maid-* pattern to .gitignore"

# Options for 'clean' command
complete -c bb-maid -n "__fish_seen_subcommand_from clean" -l max-depth -d "Limit recursion depth" -r
complete -c bb-maid -n "__fish_seen_subcommand_from clean" -l follow-links -d "Follow symbolic links"
complete -c bb-maid -n "__fish_seen_subcommand_from clean" -l yes -s y -d "Skip confirmation prompts"
complete -c bb-maid -n "__fish_seen_subcommand_from clean" -l dry-run -s n -d "Show what would be deleted"
complete -c bb-maid -n "__fish_seen_subcommand_from clean" -a "(__fish_complete_directories)" -d "Directory to clean"

# Duration suggestions for 'clean-in' command
complete -c bb-maid -n "__fish_seen_subcommand_from clean-in; and not __fish_seen_argument -s g -l gitignore" -a "1d" -d "1 day"
complete -c bb-maid -n "__fish_seen_subcommand_from clean-in; and not __fish_seen_argument -s g -l gitignore" -a "7d" -d "7 days"
complete -c bb-maid -n "__fish_seen_subcommand_from clean-in; and not __fish_seen_argument -s g -l gitignore" -a "14d" -d "14 days"
complete -c bb-maid -n "__fish_seen_subcommand_from clean-in; and not __fish_seen_argument -s g -l gitignore" -a "30d" -d "30 days"
complete -c bb-maid -n "__fish_seen_subcommand_from clean-in; and not __fish_seen_argument -s g -l gitignore" -a "60d" -d "60 days"
complete -c bb-maid -n "__fish_seen_subcommand_from clean-in; and not __fish_seen_argument -s g -l gitignore" -a "90d" -d "90 days"

# Options for 'clean-in' command
complete -c bb-maid -n "__fish_seen_subcommand_from clean-in" -l gitignore -d "Add to .gitignore"

# Directory completion for 'clean-in' after duration
complete -c bb-maid -n "__fish_seen_subcommand_from clean-in; and __fish_seen_argument -a '1d' '7d' '14d' '30d' '60d' '90d'" -a "(__fish_complete_directories)" -d "Directory"

# Directory completion for 'gitignore' command
complete -c bb-maid -n "__fish_seen_subcommand_from gitignore" -a "(__fish_complete_directories)" -d "Directory to add gitignore"
