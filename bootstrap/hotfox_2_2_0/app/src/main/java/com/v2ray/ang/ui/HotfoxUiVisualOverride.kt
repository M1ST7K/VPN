package com.v2ray.ang.ui

/**
 * Presentation-only override used by the debug screenshot harness.
 *
 * Production code may read these fields (null/false is a no-op) but must never
 * write them. Writing is reserved for debug-only callers.
 *
 * This object does not mutate VPN, entitlement, payment, server, subscription,
 * traffic, or connection stores.
 */
object HotfoxUiVisualOverride {
    data class ReferenceServerRow(
        val title: String,
        val country: String,
        val pingLabel: String,
        val flagRes: Int,
    )

    data class ReferenceAppRow(
        val appName: String,
        val packageName: String,
        val selected: Boolean,
    )

    @Volatile
    var scenarioId: String? = null
        private set

    @Volatile
    var holdSplash: Boolean = false
        private set

    @Volatile
    var onboardingStep: String? = null
        private set

    @Volatile
    var connectionChrome: String? = null
        private set

    @Volatile
    var showAddSheet: Boolean = false
        private set

    @Volatile
    var serverDetailsFixture: Boolean = false
        private set

    @Volatile
    var appsFixture: Boolean = false
        private set

    @Volatile
    var serversFixture: Boolean = false
        private set

    @Volatile
    var subscriptionFixture: Boolean = false
        private set

    @Volatile
    var referenceServers: List<ReferenceServerRow> = emptyList()
        private set

    @Volatile
    var referenceApps: List<ReferenceAppRow> = emptyList()
        private set

    fun installDebugPresentation(
        scenarioId: String?,
        holdSplash: Boolean = false,
        onboardingStep: String? = null,
        connectionChrome: String? = null,
        showAddSheet: Boolean = false,
        serverDetailsFixture: Boolean = false,
        appsFixture: Boolean = false,
        serversFixture: Boolean = false,
        subscriptionFixture: Boolean = false,
        referenceServers: List<ReferenceServerRow> = emptyList(),
        referenceApps: List<ReferenceAppRow> = emptyList(),
    ) {
        this.scenarioId = scenarioId
        this.holdSplash = holdSplash
        this.onboardingStep = onboardingStep
        this.connectionChrome = connectionChrome
        this.showAddSheet = showAddSheet
        this.serverDetailsFixture = serverDetailsFixture
        this.appsFixture = appsFixture
        this.serversFixture = serversFixture
        this.subscriptionFixture = subscriptionFixture
        this.referenceServers = referenceServers
        this.referenceApps = referenceApps
    }

    fun clear() {
        installDebugPresentation(null)
    }

    const val CHROME_DISCONNECTED = "DISCONNECTED"
    const val CHROME_CONNECTING = "CONNECTING"
    const val CHROME_CONNECTED = "CONNECTED"
}
