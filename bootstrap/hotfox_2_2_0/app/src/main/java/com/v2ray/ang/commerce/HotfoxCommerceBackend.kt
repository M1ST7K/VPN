package com.v2ray.ang.commerce

/**
 * Provider-agnostic HotFox commercial API. Provider secrets stay server-side.
 * Client polling is read-only regarding payment truth.
 */
interface HotfoxCommerceBackend {
    val available: Boolean

    /** Manual HTTPS import must remain usable even when this is false. */
    val manualImportAllowed: Boolean get() = true

    suspend fun listPlans(): CommerceResult<List<CommercePlan>>
    suspend fun createOrder(request: CreateOrderRequest): CommerceResult<CommerceOrder>
    suspend fun getOrder(orderId: String): CommerceResult<CommerceOrder>
    suspend fun getEntitlement(credential: String?): CommerceResult<CommerceEntitlement?>
    suspend fun restore(request: RestoreRequest): CommerceResult<CommerceEntitlement>
    suspend fun fetchManifest(credential: String): CommerceResult<CommerceManifest>
    suspend fun validatePromo(code: String, planId: String): CommerceResult<PromoQuote>
}
