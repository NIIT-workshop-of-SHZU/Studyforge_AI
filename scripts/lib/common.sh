# Shared helpers for local start scripts (macOS + Linux).

port_in_use() {
    local port="$1"

    if command -v ss >/dev/null 2>&1; then
        if ss -ltn 2>/dev/null | awk '{print $4}' | grep -Eq "[:.]${port}$"; then
            return 0
        fi
    fi

    if command -v lsof >/dev/null 2>&1; then
        if lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
            return 0
        fi
    fi

    if command -v netstat >/dev/null 2>&1; then
        if netstat -an 2>/dev/null | grep -E "[.:]${port}[[:space:]].*LISTEN" >/dev/null 2>&1; then
            return 0
        fi
    fi

    return 1
}

# Use a local HTTP proxy only when it is actually listening (e.g. Clash on 7897).
# Forcing a dead proxy breaks outbound AI calls from the Java API.
apply_local_proxy_if_available() {
    local proxy_port="${STUDYFORGE_PROXY_PORT:-7897}"

    if [ -n "${http_proxy:-}" ] || [ -n "${https_proxy:-}" ]; then
        return 0
    fi

    if port_in_use "$proxy_port"; then
        export http_proxy="http://127.0.0.1:${proxy_port}"
        export https_proxy="http://127.0.0.1:${proxy_port}"
        echo "Using local HTTP proxy http://127.0.0.1:${proxy_port}"
    else
        unset http_proxy https_proxy HTTP_PROXY HTTPS_PROXY 2>/dev/null || true
        echo "No local proxy on port ${proxy_port}; outbound API calls go direct"
    fi
}

# Start a command in the background, detached from the terminal.
start_detached() {
    local log_file="$1"
    shift

    if command -v setsid >/dev/null 2>&1; then
        setsid "$@" >"$log_file" 2>&1 </dev/null &
    else
        nohup "$@" >"$log_file" 2>&1 </dev/null &
    fi
}
