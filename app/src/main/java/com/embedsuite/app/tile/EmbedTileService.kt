package com.embedsuite.app.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.embedsuite.app.AppContainer
import com.embedsuite.app.connection.ConnectionState
import com.embedsuite.app.connection.TransportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EmbedTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
        scope.launch {
            AppContainer.instance?.connectionManager?.connectionState?.collectLatest {
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        val container = AppContainer.instance ?: return
        scope.launch {
            val state = container.connectionManager.connectionState.value
            if (state is ConnectionState.Connected) {
                container.connectionManager.disconnect()
            } else {
                container.connectionManager.connect(TransportType.USB)
            }
            updateTile()
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        when (val state = AppContainer.instance?.connectionManager?.connectionState?.value) {
            is ConnectionState.Connected -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "EMBED LINK"
                tile.subtitle = "Conectado (${state.type.name})"
            }
            ConnectionState.Connecting -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = "EMBED"
                tile.subtitle = "Conectando..."
            }
            else -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = "EMBED LINK"
                tile.subtitle = "Tap para conectar USB"
            }
        }
        tile.updateTile()
    }
}
