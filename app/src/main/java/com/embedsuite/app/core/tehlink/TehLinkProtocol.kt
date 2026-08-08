package com.embedsuite.app.core.tehlink

/**
 * TEH-Link protocol surface for EmbedSuite v1.0.0+.
 *
 * NDJSON over USB serial / WiFi / BLE is the only official transport channel.
 * Each line is one JSON object with at least `cmd` and optional `id`.
 */
object TehLinkProtocol {

    const val CHANNEL_NDJSON = "ndjson"

    const val CMD_PING = "ping"
    const val CMD_GET_INFO = "get_info"
    const val CMD_PAIR = "pair"
    const val CMD_GET_STATUS = "get_status"
    const val CMD_LIST_ACTIONS = "list_actions"
    const val CMD_RUN_ACTION = "run_action"

    val PUBLIC_COMMANDS: Set<String> = setOf(
        CMD_PING,
        CMD_GET_INFO,
        CMD_PAIR,
        CMD_GET_STATUS,
        CMD_LIST_ACTIONS,
        CMD_RUN_ACTION
    )
}
