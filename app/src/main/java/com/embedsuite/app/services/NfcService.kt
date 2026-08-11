package com.embedsuite.app.services

import com.embedsuite.app.connection.DeviceEvent
import com.embedsuite.app.connection.NfcCard
import com.embedsuite.app.connection.NfcReaderStatus
import com.embedsuite.app.connection.XibalbaAdapter
import com.embedsuite.app.data.NfcDumpEntity
import com.embedsuite.app.data.NfcDumpRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * NfcService — lector PN532 continuo vía TEH-Link.
 *
 * Arranca el reader headless del firmware, escucha `nfc.card.detected` en
 * tiempo real, persiste las tarjetas como dumps y dispara feedback auditivo
 * en el T-Embed. Estado observable para la UI.
 */
class NfcService(
    private val xibalba: XibalbaAdapter,
    private val nfcDumpRepository: NfcDumpRepository,
    private val scope: CoroutineScope
) {
    sealed class NfcState {
        data object Idle : NfcState()
        data object Reading : NfcState()
        data class Error(val message: String) : NfcState()
    }

    private val _state = MutableStateFlow<NfcState>(NfcState.Idle)
    val state: StateFlow<NfcState> = _state.asStateFlow()

    private val _detectedCards = MutableStateFlow<List<NfcCard>>(emptyList())
    val detectedCards: StateFlow<List<NfcCard>> = _detectedCards.asStateFlow()

    private val _lastCard = MutableStateFlow<NfcCard?>(null)
    val lastCard: StateFlow<NfcCard?> = _lastCard.asStateFlow()

    private var collecting = false

    suspend fun startReader(timeoutSec: Int = 60): Result<Unit> {
        _state.value = NfcState.Reading
        startCollecting()
        val result = xibalba.startNfcReader(timeoutSec)
        result.onFailure { _state.value = NfcState.Error(it.message ?: "nfc_reader_start_failed") }
        return result
    }

    suspend fun stopReader() {
        xibalba.stopNfcReader()
        _state.value = NfcState.Idle
    }

    suspend fun readOnce(timeoutSec: Int = 10): Result<NfcCard> {
        val result = xibalba.nfcRead(timeoutSec)
        result.onSuccess { card -> onCardDetected(card) }
        return result
    }

    suspend fun writeTag(text: String): Result<Unit> = xibalba.writeNfcTag(text)

    suspend fun status(): Result<NfcReaderStatus> = xibalba.getNfcStatus()

    fun clearHistory() {
        _detectedCards.value = emptyList()
        _lastCard.value = null
    }

    private fun startCollecting() {
        if (collecting) return
        collecting = true
        scope.launch {
            xibalba.observeNfcCards().collect { event ->
                onCardDetected(
                    NfcCard(
                        uid = event.uid,
                        type = event.type,
                        sak = event.sak,
                        atqa = event.atqa,
                        timestampMs = event.timestampMs
                    )
                )
            }
        }
        scope.launch {
            xibalba.observeEvents().collect { event ->
                if (event is DeviceEvent.NfcReaderStateChanged && !event.running) {
                    _state.value = NfcState.Idle
                }
            }
        }
    }

    private fun onCardDetected(card: NfcCard) {
        if (card.uid.isBlank()) return
        _lastCard.value = card
        _detectedCards.value = (_detectedCards.value + card).takeLast(100)

        // Persistir como dump para la pantalla NFC/IR y el mapa de calor
        scope.launch {
            runCatching {
                nfcDumpRepository.save(
                    NfcDumpEntity(
                        uid = card.uid,
                        tagType = card.type.ifBlank { "ISO14443A" },
                        rawDump = "UID: ${card.uid}\nSAK: ${card.sak}\nATQA: ${card.atqa}",
                        parsedSectors = "",
                        timestamp = if (card.timestampMs > 0) card.timestampMs
                        else System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
