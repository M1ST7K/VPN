package com.v2ray.ang.commerce

/**
 * Fail-closed commercial backend when no HTTPS HotFox API is configured.
 * Manual/external subscription import remains allowed.
 */
object UnavailableCommerceBackend : HotfoxCommerceBackend {
    override val available: Boolean = false
    override val manualImportAllowed: Boolean = true

    override suspend fun listPlans(): CommerceResult<List<CommercePlan>> =
        CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)

    override suspend fun createOrder(request: CreateOrderRequest): CommerceResult<CommerceOrder> =
        CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)

    override suspend fun getOrder(orderId: String): CommerceResult<CommerceOrder> =
        CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)

    override suspend fun getEntitlement(credential: String?): CommerceResult<CommerceEntitlement?> =
        CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)

    override suspend fun claimEntitlement(orderId: String, installId: String): CommerceResult<CommerceEntitlement> =
        CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)

    override suspend fun restore(request: RestoreRequest): CommerceResult<CommerceEntitlement> =
        CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)

    override suspend fun fetchManifest(credential: String): CommerceResult<CommerceManifest> =
        CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)

    override suspend fun validatePromo(code: String, planId: String): CommerceResult<PromoQuote> =
        CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)
}
