package com.embedsuite.app.engine.workflow

import org.json.JSONObject

/**
 * Soft-sign sharing for workflows — includes a local signature marker for import validation.
 * Full cryptographic signing is planned for a future marketplace phase.
 */
object WorkflowMarketplace {

    const val LOCAL_SIGNATURE = "embedsuite-local"

    fun signForSharing(rawJson: String): String {
        val root = runCatching { JSONObject(rawJson) }.getOrNull()
            ?: return wrapWithSignature(rawJson)
        root.put("signature", LOCAL_SIGNATURE)
        return root.toString(2)
    }

    fun validateSignature(rawJson: String): SignatureValidation {
        val root = runCatching { JSONObject(rawJson) }.getOrNull()
            ?: return SignatureValidation.Invalid("JSON inválido")
        val sig = root.optString("signature", "")
        return when {
            sig.isBlank() -> SignatureValidation.Unsigned
            sig == LOCAL_SIGNATURE -> SignatureValidation.ValidLocal
            else -> SignatureValidation.Unknown(sig)
        }
    }

    fun stripSignature(rawJson: String): String {
        val root = runCatching { JSONObject(rawJson) }.getOrNull() ?: return rawJson
        root.remove("signature")
        return root.toString(2)
    }

    private fun wrapWithSignature(rawJson: String): String =
        JSONObject()
            .put("payload", rawJson)
            .put("signature", LOCAL_SIGNATURE)
            .toString(2)

    sealed class SignatureValidation {
        data object ValidLocal : SignatureValidation()
        data object Unsigned : SignatureValidation()
        data class Unknown(val value: String) : SignatureValidation()
        data class Invalid(val reason: String) : SignatureValidation()
    }
}
