package com.v2ray.ang.commerce

/**
 * Browser/Custom Tabs returns are never payment proof.
 * At most they may carry an order id so the app can poll the backend read-only.
 */
object CheckoutReturnParser {
    fun extractOrderId(uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        val query = uriString.substringAfter('?', missingDelimiterValue = "")
        if (query.isBlank()) return null
        val params = query.split('&').mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) return@mapNotNull null
            part.substring(0, index) to part.substring(index + 1)
        }.toMap()
        return params["orderId"] ?: params["order_id"] ?: params["order"]
    }

    fun isPaidProof(@Suppress("UNUSED_PARAMETER") uriString: String?): Boolean = false

    fun looksLikeCheckoutReturn(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return uriString.contains("success=", ignoreCase = true) ||
            extractOrderId(uriString) != null ||
            uriString.contains("checkout", ignoreCase = true)
    }
}
