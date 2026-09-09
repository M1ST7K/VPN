package com.v2ray.ang.commerce

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class WebhookEventType {
    PENDING,
    PAID,
    FAILED,
    CANCELLED,
}

data class ProviderWebhookEvent(
    val eventId: String,
    val orderId: String,
    val providerPaymentId: String,
    val type: WebhookEventType,
    val occurredAtEpochSeconds: Long,
    val signature: String,
)

sealed class WebhookApplyResult {
    data class Applied(val order: CommerceOrder) : WebhookApplyResult()
    data class Duplicate(val order: CommerceOrder) : WebhookApplyResult()
    data class Rejected(val reason: String) : WebhookApplyResult()
}

/**
 * Server-side webhook authenticity. The HMAC secret is never shipped as a
 * production provider key; callers inject it (CI fixture or environment).
 */
object WebhookSignature {
    fun canonical(event: ProviderWebhookEvent): String =
        "${event.eventId}|${event.orderId}|${event.providerPaymentId}|${event.type.name}|${event.occurredAtEpochSeconds}"

    fun sign(secret: String, canonical: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(canonical.toByteArray(Charsets.UTF_8)).joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    fun sign(secret: String, event: ProviderWebhookEvent): String = sign(secret, canonical(event))

    fun verify(secret: String, event: ProviderWebhookEvent): Boolean {
        if (secret.isBlank() || event.signature.isBlank()) return false
        val expected = sign(secret, event)
        val left = expected.toByteArray(Charsets.UTF_8)
        val right = event.signature.lowercase().toByteArray(Charsets.UTF_8)
        return left.size == right.size && MessageDigest.isEqual(left, right)
    }
}

/**
 * Replay-safe webhook application. Duplicate event IDs and already-paid
 * out-of-order events do not grant a second entitlement. Invalid signatures never
 * change order state.
 */
object WebhookReconciliation {
    fun apply(
        current: CommerceOrder,
        event: ProviderWebhookEvent,
        seenEventIds: MutableSet<String>,
        paymentOwners: MutableMap<String, String>,
        signed: Boolean,
    ): WebhookApplyResult {
        if (!signed) return WebhookApplyResult.Rejected("invalid_signature")
        if (event.orderId != current.id) return WebhookApplyResult.Rejected("order_mismatch")
        if (event.eventId.isBlank() || event.providerPaymentId.isBlank()) {
            return WebhookApplyResult.Rejected("missing_event_identity")
        }
        if (seenEventIds.contains(event.eventId)) {
            return WebhookApplyResult.Duplicate(current)
        }
        val owner = paymentOwners[event.providerPaymentId]
        if (owner != null && owner != current.id) {
            return WebhookApplyResult.Rejected("payment_id_conflict")
        }
        seenEventIds.add(event.eventId)
        return when (event.type) {
            WebhookEventType.PAID -> applyPaid(current, event, paymentOwners)
            WebhookEventType.PENDING -> applyPending(current)
            WebhookEventType.CANCELLED -> applyTerminal(current, OrderState.CANCELLED)
            WebhookEventType.FAILED -> applyTerminal(current, OrderState.FAILED)
        }
    }

    private fun applyPaid(
        current: CommerceOrder,
        event: ProviderWebhookEvent,
        paymentOwners: MutableMap<String, String>,
    ): WebhookApplyResult {
        paymentOwners[event.providerPaymentId] = current.id
        if (OrderStateMachine.isPaidTruth(current.state)) {
            return WebhookApplyResult.Duplicate(current)
        }
        if (OrderStateMachine.isTerminalFailure(current.state)) {
            return WebhookApplyResult.Rejected("already_terminal")
        }
        var next = towardPending(current)
        next = OrderStateMachine.transition(next, OrderState.PAID).copy(
            paidAtEpochSeconds = event.occurredAtEpochSeconds,
            providerPaymentId = event.providerPaymentId,
        )
        next = OrderStateMachine.transition(next, OrderState.ENTITLEMENT_PROVISIONING)
        return WebhookApplyResult.Applied(next)
    }

    private fun applyPending(current: CommerceOrder): WebhookApplyResult {
        if (OrderStateMachine.isPaidTruth(current.state)) {
            return WebhookApplyResult.Duplicate(current)
        }
        if (OrderStateMachine.isTerminalFailure(current.state)) {
            return WebhookApplyResult.Rejected("already_terminal")
        }
        val next = towardPending(current)
        return if (next.state == current.state) {
            WebhookApplyResult.Duplicate(next)
        } else {
            WebhookApplyResult.Applied(next)
        }
    }

    private fun applyTerminal(current: CommerceOrder, terminal: OrderState): WebhookApplyResult {
        if (OrderStateMachine.isPaidTruth(current.state)) {
            return WebhookApplyResult.Rejected("already_paid")
        }
        if (current.state == terminal) return WebhookApplyResult.Duplicate(current)
        if (!OrderStateMachine.canTransition(current.state, terminal)) {
            return WebhookApplyResult.Rejected("illegal_transition")
        }
        return WebhookApplyResult.Applied(OrderStateMachine.transition(current, terminal))
    }

    private fun towardPending(order: CommerceOrder): CommerceOrder {
        var next = order
        if (next.state == OrderState.CREATED) {
            next = OrderStateMachine.transition(next, OrderState.CHECKOUT_OPEN)
        }
        if (next.state == OrderState.CHECKOUT_OPEN) {
            next = OrderStateMachine.transition(next, OrderState.PENDING)
        }
        return next
    }
}
