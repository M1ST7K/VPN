package com.v2ray.ang.commerce

data class CheckoutIntent(
    val planId: String,
    val idempotencyKey: String,
    val state: OrderState,
    val orderId: String? = null,
)

interface CheckoutIntentStore {
    fun load(): CheckoutIntent?
    fun save(intent: CheckoutIntent)
}

class InMemoryCheckoutIntentStore : CheckoutIntentStore {
    @Volatile
    var intent: CheckoutIntent? = null

    override fun load(): CheckoutIntent? = intent

    override fun save(intent: CheckoutIntent) {
        this.intent = intent
    }
}

/**
 * Persist the idempotency key before network dispatch. Reuse after lost responses
 * and while an order is still in-flight. Mint a new key only after a terminal outcome.
 */
object CheckoutIdempotency {
    fun reuseOrMint(
        existing: CheckoutIntent?,
        planId: String,
        mint: () -> String,
    ): CheckoutIntent {
        if (existing != null && existing.planId == planId && !isClosed(existing.state)) {
            return existing
        }
        return CheckoutIntent(
            planId = planId,
            idempotencyKey = mint(),
            state = OrderState.CREATE_REQUESTED,
            orderId = null,
        )
    }

    fun isClosed(state: OrderState): Boolean =
        state == OrderState.FAILED ||
            state == OrderState.CANCELLED ||
            state == OrderState.EXPIRED ||
            state == OrderState.REFUNDED ||
            state == OrderState.FULFILLED
}
