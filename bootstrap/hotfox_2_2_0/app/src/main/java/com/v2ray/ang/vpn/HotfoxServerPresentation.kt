package com.v2ray.ang.vpn

/**
 * Conservative city/country presentation. Never invents a country from a hostname.
 */
data class ServerPresentation(
    val title: String,
    val country: String?,
)

object HotfoxServerPresentation {
    private val known = mapOf(
        "amsterdam" to ("Амстердам" to "Нидерланды"),
        "ams" to ("Амстердам" to "Нидерланды"),
        "netherlands" to (null to "Нидерланды"),
        "nl" to (null to "Нидерланды"),
        "frankfurt" to ("Франкфурт" to "Германия"),
        "germany" to (null to "Германия"),
        "de" to (null to "Германия"),
        "paris" to ("Париж" to "Франция"),
        "france" to (null to "Франция"),
        "fr" to (null to "Франция"),
        "warsaw" to ("Варшава" to "Польша"),
        "poland" to (null to "Польша"),
        "pl" to (null to "Польша"),
        "singapore" to ("Сингапур" to "Сингапур"),
        "sg" to ("Сингапур" to "Сингапур"),
        "london" to ("Лондон" to "Великобритания"),
        "moscow" to ("Москва" to "Россия"),
        "stockholm" to ("Стокгольм" to "Швеция"),
        "vienna" to ("Вена" to "Австрия"),
        "prague" to ("Прага" to "Чехия"),
        "helsinki" to ("Хельсинки" to "Финляндия"),
        "riga" to ("Рига" to "Латвия"),
        "tallinn" to ("Таллин" to "Эстония"),
        "newyork" to ("Нью-Йорк" to "США"),
        "tokyo" to ("Токио" to "Япония"),
    )

    fun fromRemark(remark: String?, affiliationCountry: String? = null): ServerPresentation {
        val raw = remark?.trim().orEmpty()
        if (raw.isEmpty()) {
            return ServerPresentation("—", affiliationCountry?.takeIf { it.isNotBlank() })
        }
        affiliationCountry?.takeIf { it.isNotBlank() }?.let { country ->
            return ServerPresentation(raw, country)
        }
        val tokens = raw.lowercase()
            .replace(Regex("[^a-zа-яё0-9]+", RegexOption.IGNORE_CASE), " ")
            .split(' ')
            .filter { it.isNotBlank() }
        var city: String? = null
        var country: String? = null
        for (token in tokens) {
            val hit = known[token] ?: continue
            if (hit.first != null && city == null) city = hit.first
            if (hit.second != null && country == null) country = hit.second
        }
        return ServerPresentation(title = city ?: raw, country = country)
    }
}
