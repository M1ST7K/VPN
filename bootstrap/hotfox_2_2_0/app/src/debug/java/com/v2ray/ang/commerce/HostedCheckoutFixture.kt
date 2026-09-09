package com.v2ray.ang.commerce

/**
 * Deterministic hosted-checkout stand-in. Debug/test only.
 * Opening or returning from checkout never marks an order paid.
 */
object HostedCheckoutFixture {
    fun returnUri(order: CommerceOrder, claimSuccess: Boolean): String {
        val success = if (claimSuccess) "true" else "false"
        return "https://hotfox.example/checkout/return?success=$success&orderId=${order.id}"
    }

    fun open(backend: SandboxCommerceBackend, orderId: String): CommerceOrder? =
        backend.noteCheckoutReturn(orderId)

    fun cancel(backend: SandboxCommerceBackend, orderId: String): CommerceOrder? =
        backend.cancelCheckout(orderId)
}
