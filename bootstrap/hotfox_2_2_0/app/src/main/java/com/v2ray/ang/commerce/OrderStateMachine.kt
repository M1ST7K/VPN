package com.v2ray.ang.commerce

/**
 * Authoritative order transitions. Payment truth is never derived from a browser return.
 * Fulfillment of a given order id is idempotent: PAID/FULFILLED cannot grant twice.
 */
object OrderStateMachine {
    private val allowed: Map<OrderState, Set<OrderState>> = mapOf(
        OrderState.CREATE_REQUESTED to setOf(OrderState.CREATED, OrderState.FAILED),
        OrderState.CREATED to setOf(
            OrderState.CHECKOUT_OPEN, OrderState.CANCELLED, OrderState.EXPIRED, OrderState.FAILED,
        ),
        OrderState.CHECKOUT_OPEN to setOf(
            OrderState.PENDING, OrderState.PAID, OrderState.CANCELLED, OrderState.EXPIRED, OrderState.FAILED,
        ),
        OrderState.PENDING to setOf(
            OrderState.PAID, OrderState.FAILED, OrderState.CANCELLED, OrderState.EXPIRED,
        ),
        OrderState.PAID to setOf(
            OrderState.ENTITLEMENT_PROVISIONING, OrderState.FULFILLED, OrderState.REFUND_PENDING,
        ),
        OrderState.ENTITLEMENT_PROVISIONING to setOf(OrderState.FULFILLED, OrderState.FAILED),
        OrderState.FULFILLED to setOf(OrderState.REFUND_PENDING),
        OrderState.REFUND_PENDING to setOf(OrderState.REFUNDED),
        OrderState.CANCELLED to emptySet(),
        OrderState.FAILED to emptySet(),
        OrderState.EXPIRED to emptySet(),
        OrderState.REFUNDED to emptySet(),
    )

    fun canTransition(from: OrderState, to: OrderState): Boolean {
        if (from == to) return true
        return allowed[from]?.contains(to) == true
    }

    fun transition(order: CommerceOrder, to: OrderState): CommerceOrder {
        check(canTransition(order.state, to)) {
            "illegal order transition ${order.state} → $to"
        }
        return order.copy(state = to)
    }

    fun isPaidTruth(state: OrderState): Boolean =
        state == OrderState.PAID ||
            state == OrderState.ENTITLEMENT_PROVISIONING ||
            state == OrderState.FULFILLED

    fun isTerminalFailure(state: OrderState): Boolean =
        state == OrderState.FAILED || state == OrderState.CANCELLED || state == OrderState.EXPIRED
}
