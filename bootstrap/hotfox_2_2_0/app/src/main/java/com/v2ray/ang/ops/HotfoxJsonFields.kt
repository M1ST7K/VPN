package com.v2ray.ang.ops

internal object HotfoxJsonFields {
    fun string(obj: String, key: String): String? {
        val needle = "\"$key\""
        val idx = obj.indexOf(needle)
        if (idx < 0) return null
        val colon = obj.indexOf(':', idx + needle.length)
        if (colon < 0) return null
        var i = colon + 1
        while (i < obj.length && obj[i].isWhitespace()) i++
        if (i >= obj.length) return null
        if (obj[i] != '"') return null
        i++
        val sb = StringBuilder()
        while (i < obj.length) {
            val ch = obj[i]
            if (ch == '\\' && i + 1 < obj.length) {
                sb.append(obj[i + 1])
                i += 2
                continue
            }
            if (ch == '"') break
            sb.append(ch)
            i++
        }
        return sb.toString()
    }

    fun raw(obj: String, key: String): String? {
        val needle = "\"$key\""
        val idx = obj.indexOf(needle)
        if (idx < 0) return null
        val colon = obj.indexOf(':', idx + needle.length)
        if (colon < 0) return null
        var i = colon + 1
        while (i < obj.length && obj[i].isWhitespace()) i++
        if (i >= obj.length) return null
        if (obj[i] == '"') return string(obj, key)
        if (obj[i] == '[') {
            val end = obj.indexOf(']', i)
            if (end < 0) return null
            return obj.substring(i, end + 1)
        }
        val start = i
        while (i < obj.length && obj[i] != ',' && obj[i] != '}' && obj[i] != ']') i++
        return obj.substring(start, i).trim()
    }

    fun intValue(obj: String, key: String): Int? = raw(obj, key)?.toIntOrNull()

    fun longValue(obj: String, key: String): Long? = raw(obj, key)?.toLongOrNull()

    fun stringList(obj: String, key: String): Set<String> {
        val raw = raw(obj, key) ?: return emptySet()
        val body = raw.trim().removePrefix("[").removeSuffix("]")
        if (body.isBlank()) return emptySet()
        return body.split(',').mapNotNull { token ->
            token.trim().trim('"').takeIf { it.isNotBlank() }
        }.toSet()
    }

    fun intList(obj: String, key: String): Set<Int> =
        stringList(obj, key).mapNotNull { it.toIntOrNull() }.toSet()
}
