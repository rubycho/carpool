package com.example.voiceassistant

/**
 * TODO: graceful state machine implementation
 */
enum class AppState {
    STANDBY,
    C_LISTEN,
    C_NETWORK,
    C_ANSWER,
    P_ALERT,
    P_LISTEN,
    P_ALERT2,
    P_LISTEN2,
    P_DISMISS,
    P_PROC
}
