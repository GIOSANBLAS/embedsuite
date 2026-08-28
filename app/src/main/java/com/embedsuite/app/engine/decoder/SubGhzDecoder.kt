package com.embedsuite.app.engine.decoder

import com.embedsuite.app.engine.decoder.protocols.CameDecoder
import com.embedsuite.app.engine.decoder.protocols.Em4100Decoder
import com.embedsuite.app.engine.decoder.protocols.PrincetonDecoder
import com.embedsuite.app.rf.DecodedRfSignal
import com.embedsuite.app.rf.RfProtocolDecoder

/** Decodifica capturas .sub y líneas de consola (skill Módulo B). */
object SubGhzDecoder {

    data class DecodeResult(
        val protocol: String,
        val summary: String,
        val fields: Map<String, String> = emptyMap(),
        val source: String
    )

    fun decodeSubFile(content: String, fileName: String = ""): Result<DecodeResult> =
        SubFileParser.parseLegacy(content, fileName).map { cap ->
            decodeFromTimings(cap.protocol, cap.rawTimings, cap.frequencyHz)
                ?: DecodeResult(
                    protocol = cap.protocol,
                    summary = "RAW ${cap.rawTimings.size} pulsos @ ${cap.frequencyHz / 1_000_000.0} MHz",
                    source = fileName.ifBlank { "sub" }
                )
        }

    fun decodeLine(line: String): DecodeResult? =
        RfProtocolDecoder.decode(line)?.toDecodeResult(line)

    private fun decodeFromTimings(protocol: String, timings: List<Int>, freqHz: Long): DecodeResult? {
        val freqMhz = freqHz / 1_000_000.0
        PrincetonDecoder.decode(timings)?.let {
            return DecodeResult("Princeton", it.summary, it.fields, "timings")
        }
        CameDecoder.decode(timings)?.let {
            return DecodeResult("CAME", it.summary, it.fields, "timings")
        }
        Em4100Decoder.decode(timings)?.let {
            return DecodeResult("EM4100", it.summary, it.fields, "timings")
        }
        if (protocol.contains("Princeton", ignoreCase = true)) {
            return DecodeResult(protocol, "Princeton (declarado en archivo)", emptyMap(), "meta")
        }
        if (protocol.contains("CAME", ignoreCase = true)) {
            return DecodeResult("CAME", "CAME (declarado en archivo)", emptyMap(), "meta")
        }
        return null
    }

    private fun DecodedRfSignal.toDecodeResult(source: String) = DecodeResult(
        protocol = protocol,
        summary = "$protocol ${hexKey.ifBlank { rawSummary }}".trim(),
        fields = fields,
        source = source
    )
}
