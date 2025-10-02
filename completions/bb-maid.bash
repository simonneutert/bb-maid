_bb-maid() {
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    # Subcommands
    local subcommands="clean clean-in"

    # If we're on the first argument
    if [[ ${COMP_CWORD} -eq 1 ]] ; then
        COMPREPLY=( $(compgen -W "${subcommands}" -- ${cur}) )
        return 0
    fi

    # If previous word was clean
    if [[ ${prev} == "clean" ]] ; then
        COMPREPLY=( $(compgen -d -- ${cur}) )
        return 0
    fi

    # If previous word was clean-in
    if [[ ${prev} == "clean-in" ]] ; then
        COMPREPLY=( $(compgen -W "1d 7d 14d 30d 60d 90d" -- ${cur}) )
        return 0
    fi
}

complete -F _bb-maid bb-maid
