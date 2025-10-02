#compdef bb-maid

_bb-maid() {
    local -a commands
    commands=(
        'clean-in:Create a cleanup file with expiration date (e.g., bb-maid clean-in 7d)'
    )

    _arguments -C \
        '1: :->command' \
        '*::arg:->args'

    case $state in
        command)
            if [[ ${words[2]} == "clean-in" ]]; then
                _message 'duration (e.g., 7d for 7 days)'
            else
                _files -/
                _describe 'command' commands
            fi
            ;;
        args)
            case ${words[2]} in
                clean-in)
                    _message 'duration (e.g., 7d for 7 days)'
                    ;;
                *)
                    _files -/
                    ;;
            esac
            ;;
    esac
}

_bb-maid "$@"
