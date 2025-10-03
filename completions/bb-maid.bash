_bb-maid() {
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    # Subcommands
    local subcommands="clean clean-in"
    
    # Clean command options
    local clean_opts="--max-depth --follow-links --yes -y --dry-run -n"

    # If we're on the first argument
    if [[ ${COMP_CWORD} -eq 1 ]] ; then
        COMPREPLY=( $(compgen -W "${subcommands}" -- ${cur}) )
        return 0
    fi

    # Find which subcommand we're completing for
    local subcommand="${COMP_WORDS[1]}"
    
    case "${subcommand}" in
        clean)
            # If previous word was --max-depth, suggest numbers
            if [[ ${prev} == "--max-depth" ]] ; then
                COMPREPLY=( $(compgen -W "1 2 3 5 10" -- ${cur}) )
                return 0
            fi
            
            # If current word starts with -, suggest options
            if [[ ${cur} == -* ]] ; then
                COMPREPLY=( $(compgen -W "${clean_opts}" -- ${cur}) )
                return 0
            fi
            
            # Otherwise suggest directories
            COMPREPLY=( $(compgen -d -- ${cur}) )
            return 0
            ;;
        clean-in)
            if [[ ${prev} == "clean-in" ]] ; then
                COMPREPLY=( $(compgen -W "1d 7d 14d 30d 60d 90d" -- ${cur}) )
                return 0
            fi
            ;;
    esac
}

complete -F _bb-maid bb-maid
