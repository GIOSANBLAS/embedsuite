package com.embedsuite.app.core.ble

import org.json.JSONObject

/** Comandos TEH-Link serializados para envío por BLE. */
object BleCommand {
    fun ping(id: String = "ping") = JSONObject().put("cmd", "ping").put("id", id).toString()

    fun getInfo(id: String = "info") = JSONObject().put("cmd", "get_info").put("id", id).toString()

    fun runAction(action: String, params: JSONObject = JSONObject(), id: String = action) =
        JSONObject()
            .put("cmd", "run_action")
            .put("id", id)
            .put("action", action)
            .put("params", params)
            .toString()

    fun encoderRotate(steps: Int = 1, direction: String = "cw", id: String = "encoder") =
        runAction(
            "encoder_rotate",
            JSONObject().put("steps", steps).put("direction", direction),
            id
        )
}
