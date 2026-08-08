package com.embedsuite.app.core.device

data class DeviceProfile(
    val id: String,
    val name: String,
    val hardwareKind: DeviceHardwareKind,
    val capabilities: Set<DeviceCapability>,
    val firmwareVersion: String = "",
    val productName: String = "",
    val lastSeenMs: Long = 0L,
    val notes: String = ""
)
