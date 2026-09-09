package com.v2ray.ang.commerce

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Deterministic test/sandbox adapter. It never treats a browser `success=true` return as paid.
 * Only [markPaidFromVerifiedWebhook] (stand-in for a server webhook) can move an order to PAID
 * and issue an entitlement. This is a fixture, not production payment.
 */
class SandboxCommerceBackend(
    private val clock: () -> Long = { System.currentTimeMillis() / 1000L },
) : HotfoxCommerceBackend {
    override val available: Boolean = true
    override val manualImportAllowed: Boolean = true

    private val ordersByIdempotency = ConcurrentHashMap<String, CommerceOrder>()
    private val ordersById = ConcurrentHashMap<String, CommerceOrder>()
    private val fulfilledOrderIds = ConcurrentHashMap.newKeySet<String>()
    private val entitlements = ConcurrentHashMap<String, CommerceEntitlement>()
    private val createCount = AtomicInteger(0)
    private val getOrderCount = AtomicInteger(0)

    val catalog: List<CommercePlan> = listOf(
        CommercePlan(
            id = "plan_1m",
            displayName = "1 месяц",
            subtitle = "Доступ на 30 дней",
            durationDays = 30,
            priceMinor = 29900,
            currency = "RUB",
            isRecommended = false,
            sortOrder = 1,
        ),
        CommercePlan(
            id = "plan_3m",
            displayName = "3 месяца",
            subtitle = "Доступ на 90 дней",
            durationDays = 90,
            priceMinor = 74900,
            currency = "RUB",
            badge = "выгодно",
            isRecommended = true,
            sortOrder = 2,
        ),
        CommercePlan(
            id = "plan_12m",
            displayName = "12 месяцев",
            subtitle = "Доступ на 365 дней",
            durationDays = 365,
            priceMinor = 249000,
            currency = "RUB",
            sortOrder = 3,
        ),
    )

    fun createdOrderCount(): Int = createCount.get()

    fun getOrderCount(): Int = getOrderCount.get()

    override suspend fun listPlans(): CommerceResult<List<CommercePlan>> =
        CommerceResult.Ok(catalog.sortedBy { it.sortOrder })

    override suspend fun createOrder(request: CreateOrderRequest): CommerceResult<CommerceOrder> {
        if (request.idempotencyKey.isBlank() || request.planId.isBlank()) {
            return CommerceResult.Err(CommerceError.INVALID)
        }
        if (catalog.none { it.id == request.planId }) {
            return CommerceResult.Err(CommerceError.NOT_FOUND)
        }
        ordersByIdempotency[request.idempotencyKey]?.let { return CommerceResult.Ok(it) }
        createCount.incrementAndGet()
        val order = CommerceOrder(
            id = "ord_${UUID.randomUUID().toString().replace("-", "").take(16)}",
            planId = request.planId,
            state = OrderState.CREATED,
            checkoutUrl = "https://sandbox.hotfox.invalid/checkout/${request.planId}",
            idempotencyKey = request.idempotencyKey,
            createdAtEpochSeconds = clock(),
        )
        ordersByIdempotency[request.idempotencyKey] = order
        ordersById[order.id] = order
        return CommerceResult.Ok(order)
    }

    override suspend fun getOrder(orderId: String): CommerceResult<CommerceOrder> {
        getOrderCount.incrementAndGet()
        val order = ordersById[orderId] ?: return CommerceResult.Err(CommerceError.NOT_FOUND)
        return CommerceResult.Ok(order)
    }

    /**
     * Records that the user returned from hosted checkout. Must not grant entitlement.
     */
    fun noteCheckoutReturn(orderId: String?): CommerceOrder? {
        val order = orderId?.let { ordersById[it] } ?: return null
        if (order.state == OrderState.CREATED) {
            val opened = OrderStateMachine.transition(order, OrderState.CHECKOUT_OPEN)
            ordersById[order.id] = opened
            ordersByIdempotency[opened.idempotencyKey] = opened
            return opened
        }
        return order
    }

    /**
     * Test stand-in for a verified backend webhook. Not callable from a browser return parser.
     */
    fun markPaidFromVerifiedWebhook(orderId: String, providerPaymentId: String): CommerceOrder {
        val current = ordersById[orderId] ?: error("unknown order")
        if (OrderStateMachine.isPaidTruth(current.state)) {
            return current
        }
        var next = current
        if (next.state == OrderState.CREATED) {
            next = OrderStateMachine.transition(next, OrderState.CHECKOUT_OPEN)
        }
        if (next.state == OrderState.CHECKOUT_OPEN) {
            next = OrderStateMachine.transition(next, OrderState.PENDING)
        }
        next = OrderStateMachine.transition(next, OrderState.PAID).copy(
            paidAtEpochSeconds = clock(),
            providerPaymentId = providerPaymentId,
        )
        next = OrderStateMachine.transition(next, OrderState.ENTITLEMENT_PROVISIONING)
        next = OrderStateMachine.transition(next, OrderState.FULFILLED)
        ordersById[orderId] = next
        ordersByIdempotency[next.idempotencyKey] = next
        if (fulfilledOrderIds.add(orderId)) {
            val plan = catalog.first { it.id == next.planId }
            val now = clock()
            val entitlement = CommerceEntitlement(
                entitlementId = "ent_$orderId",
                customerId = "cust_sandbox",
                source = EntitlementSource.HOTFOX,
                planId = next.planId,
                status = EntitlementStatus.ACTIVE,
                startsAtEpochSeconds = now,
                expiresAtEpochSeconds = now + plan.durationDays * 86400L,
                orderId = orderId,
            )
            entitlements[entitlement.entitlementId] = entitlement
        }
        return next
    }

    fun forgetEntitlement(credential: String) {
        entitlements.remove(credential)
        entitlements.entries.removeIf { it.value.entitlementId == credential }
    }

    override suspend fun getEntitlement(credential: String?): CommerceResult<CommerceEntitlement?> {
        if (credential.isNullOrBlank()) return CommerceResult.Ok(null)
        val match = entitlements[credential]
            ?: entitlements.values.firstOrNull { it.entitlementId == credential }
        return CommerceResult.Ok(match)
    }

    override suspend fun claimEntitlement(orderId: String, installId: String): CommerceResult<CommerceEntitlement> {
        if (orderId.isBlank() || installId.isBlank()) {
            return CommerceResult.Err(CommerceError.INVALID)
        }
        val order = ordersById[orderId] ?: return CommerceResult.Err(CommerceError.NOT_FOUND)
        if (!OrderStateMachine.isPaidTruth(order.state)) {
            return CommerceResult.Err(CommerceError.REJECTED, "order_not_paid")
        }
        val entitlement = entitlements.values.firstOrNull { it.orderId == orderId }
            ?: return CommerceResult.Err(CommerceError.NOT_FOUND, "entitlement_not_ready")
        return CommerceResult.Ok(entitlement)
    }

    override suspend fun restore(request: RestoreRequest): CommerceResult<CommerceEntitlement> {
        val recovered = entitlements.values.firstOrNull { entitlement ->
            request.recoveryCode == entitlement.entitlementId ||
                request.providerTransactionId != null &&
                ordersById[entitlement.orderId]?.providerPaymentId == request.providerTransactionId
        }
        return if (recovered != null) {
            CommerceResult.Ok(recovered)
        } else {
            CommerceResult.Err(CommerceError.NOT_FOUND)
        }
    }

    override suspend fun fetchManifest(credential: String): CommerceResult<CommerceManifest> {
        val entitlement = entitlements[credential] ?: entitlements.values.firstOrNull()
        if (entitlement == null || entitlement.status != EntitlementStatus.ACTIVE) {
            return CommerceResult.Err(CommerceError.UNAUTHORIZED)
        }
        return CommerceResult.Ok(
            CommerceManifest(
                format = "opaque",
                payload = "hotfox-sandbox-manifest-not-a-paid-proof",
            ),
        )
    }

    override suspend fun validatePromo(code: String, planId: String): CommerceResult<PromoQuote> {
        val plan = catalog.firstOrNull { it.id == planId }
            ?: return CommerceResult.Err(CommerceError.NOT_FOUND)
        if (code.isBlank()) return CommerceResult.Err(CommerceError.INVALID)
        if (!code.equals("SANDBOX", ignoreCase = true)) {
            return CommerceResult.Ok(
                PromoQuote(code, planId, plan.priceMinor, plan.currency, valid = false, reason = "unknown"),
            )
        }
        return CommerceResult.Ok(
            PromoQuote(code, planId, plan.priceMinor / 2, plan.currency, valid = true),
        )
    }
}
