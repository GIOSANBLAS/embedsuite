package com.embedsuite.app.core.tehlink

/**
 * Core TEH-Link bridge — migration path:
 *
 * - **Today:** [com.embedsuite.app.connection.TehLinkClient] owns transport + NDJSON I/O.
 * - **v1.x:** Call sites may import typealiases below without breaking packages.
 * - **Future:** Move TehLinkClient implementation into `core.tehlink` and delete aliases.
 */
typealias TehLinkClient = com.embedsuite.app.connection.TehLinkClient
typealias TehLinkDeviceInfo = com.embedsuite.app.connection.TehLinkDeviceInfo
typealias TehLinkHardeningInfo = com.embedsuite.app.connection.TehLinkHardeningInfo
typealias TehLinkDeviceStatus = com.embedsuite.app.connection.TehLinkDeviceStatus
