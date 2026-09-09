package com.v2ray.ang.commerce

import com.google.gson.JsonParser
import com.v2ray.ang.util.LogUtil
import com.v2ray.ang.util.SecretRedactor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * HTTPS client for the 2.3 HotFox commercial contract.
 * Never sends a client-supplied price. Never logs credentials.
 */
class HttpHotfoxCommerceBackend(
    private val baseUrl: String,
    private val http: OkHttpClient = defaultClient(),
) : HotfoxCommerceBackend {
    override val available: Boolean = true
    override val manualImportAllowed: Boolean = true

    override suspend fun listPlans(): CommerceResult<List<CommercePlan>> {
        return get("/v1/plans") { body ->
            val root = JSONObject(body)
            val array = root.optJSONArray("plans") ?: JSONArray()
            val plans = ArrayList<CommercePlan>(array.length())
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                plans.add(
                    CommercePlan(
                        id = item.getString("id"),
                        displayName = item.optString("displayName"),
                        subtitle = item.optString("subtitle"),
                        durationDays = item.optInt("durationDays"),
                        priceMinor = item.optLong("priceMinor"),
                        currency = item.optString("currency", "RUB"),
                        oldPriceMinor = item.optLong("oldPriceMinor").takeIf { item.has("oldPriceMinor") },
                        badge = item.optString("badge").takeIf { it.isNotBlank() },
                        isRecommended = item.optBoolean("isRecommended"),
                        sortOrder = item.optInt("sortOrder"),
                        version = item.optInt("version", 1),
                    ),
                )
            }
            plans.sortedBy { it.sortOrder }
        }
    }

    override suspend fun createOrder(request: CreateOrderRequest): CommerceResult<CommerceOrder> {
        val payload = JSONObject()
            .put("planId", request.planId)
            .put("installId", request.installId)
            .put("promoCode", request.promoCode ?: JSONObject.NULL)
        return post(
            path = "/v1/orders",
            json = payload.toString(),
            headers = mapOf("Idempotency-Key" to request.idempotencyKey),
        ) { parseOrder(it) }
    }

    override suspend fun getOrder(orderId: String): CommerceResult<CommerceOrder> =
        get("/v1/orders/$orderId") { parseOrder(it) }

    override suspend fun getEntitlement(credential: String?): CommerceResult<CommerceEntitlement?> {
        if (credential.isNullOrBlank()) return CommerceResult.Ok(null)
        return get("/v1/entitlement", credential) { body ->
            if (body.isBlank() || body == "null") null else parseEntitlement(body)
        }
    }

    override suspend fun restore(request: RestoreRequest): CommerceResult<CommerceEntitlement> {
        val payload = JSONObject()
            .put("installId", request.installId)
            .put("recoveryCode", request.recoveryCode ?: JSONObject.NULL)
            .put("providerTransactionId", request.providerTransactionId ?: JSONObject.NULL)
        return post("/v1/entitlement/restore", payload.toString()) { parseEntitlement(it) }
    }

    override suspend fun fetchManifest(credential: String): CommerceResult<CommerceManifest> {
        return get("/v1/subscription/manifest", credential) { body ->
            val root = JSONObject(body)
            CommerceManifest(
                format = root.optString("format", "opaque"),
                payload = root.optString("payload").takeIf { it.isNotBlank() },
                subscriptionUrl = root.optString("subscriptionUrl").takeIf { it.isNotBlank() },
            )
        }
    }

    override suspend fun validatePromo(code: String, planId: String): CommerceResult<PromoQuote> {
        val payload = JSONObject().put("code", code).put("planId", planId)
        return post("/v1/promo/validate", payload.toString()) { body ->
            val root = JSONObject(body)
            PromoQuote(
                code = code,
                planId = planId,
                finalPriceMinor = root.optLong("finalPriceMinor"),
                currency = root.optString("currency", "RUB"),
                valid = root.optBoolean("valid"),
                reason = root.optString("reason").takeIf { it.isNotBlank() },
            )
        }
    }

    private fun parseOrder(body: String): CommerceOrder {
        val root = JSONObject(body)
        return CommerceOrder(
            id = root.getString("id"),
            planId = root.optString("planId"),
            state = OrderState.valueOf(root.optString("state", OrderState.CREATED.name)),
            checkoutUrl = root.optString("checkoutUrl").takeIf { it.isNotBlank() },
            idempotencyKey = root.optString("idempotencyKey"),
            createdAtEpochSeconds = root.optLong("createdAtEpochSeconds"),
            paidAtEpochSeconds = root.optLong("paidAtEpochSeconds").takeIf { root.has("paidAtEpochSeconds") },
            providerPaymentId = root.optString("providerPaymentId").takeIf { it.isNotBlank() },
        )
    }

    private fun parseEntitlement(body: String): CommerceEntitlement {
        val root = JSONObject(body)
        return CommerceEntitlement(
            entitlementId = root.getString("entitlementId"),
            customerId = root.optString("customerId"),
            source = EntitlementSource.valueOf(root.optString("source", EntitlementSource.HOTFOX.name)),
            planId = root.optString("planId"),
            status = EntitlementStatus.valueOf(root.optString("status", EntitlementStatus.ACTIVE.name)),
            startsAtEpochSeconds = root.optLong("startsAtEpochSeconds"),
            expiresAtEpochSeconds = root.optLong("expiresAtEpochSeconds"),
            orderId = root.optString("orderId").takeIf { it.isNotBlank() },
            credentialVersion = root.optInt("credentialVersion", 1),
        )
    }

    private fun <T> get(
        path: String,
        credential: String? = null,
        parse: (String) -> T,
    ): CommerceResult<T> = execute(
        Request.Builder().url(endpoint(path)).get().apply {
            credential?.let { header("Authorization", "Bearer $it") }
        }.build(),
        parse,
    )

    private fun <T> post(
        path: String,
        json: String,
        headers: Map<String, String> = emptyMap(),
        credential: String? = null,
        parse: (String) -> T,
    ): CommerceResult<T> {
        val request = Request.Builder()
            .url(endpoint(path))
            .post(json.toRequestBody(JSON))
            .apply {
                headers.forEach { (k, v) -> header(k, v) }
                credential?.let { header("Authorization", "Bearer $it") }
            }
            .build()
        return execute(request, parse)
    }

    private fun <T> execute(request: Request, parse: (String) -> T): CommerceResult<T> {
        return try {
            http.newCall(request).execute().use { response ->
                val body = response.body.string()
                when (response.code) {
                    in 200..299 -> {
                        // Reject non-JSON success bodies that could hide an error page.
                        if (body.isNotBlank() && !isJson(body) && request.method != "GET") {
                            return CommerceResult.Err(CommerceError.INVALID)
                        }
                        CommerceResult.Ok(parse(body))
                    }
                    401, 403 -> CommerceResult.Err(CommerceError.UNAUTHORIZED)
                    404 -> CommerceResult.Err(CommerceError.NOT_FOUND)
                    409 -> CommerceResult.Err(CommerceError.CONFLICT)
                    400, 422 -> CommerceResult.Err(CommerceError.INVALID)
                    else -> CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)
                }
            }
        } catch (error: Exception) {
            LogUtil.e("HotFoxCommerce", SecretRedactor.redact(error.message.orEmpty()))
            CommerceResult.Err(CommerceError.BACKEND_UNAVAILABLE)
        }
    }

    private fun endpoint(path: String): String =
        baseUrl.trimEnd('/') + if (path.startsWith("/")) path else "/$path"

    private fun isJson(body: String): Boolean = try {
        JsonParser.parseString(body)
        true
    } catch (_: Exception) {
        false
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
