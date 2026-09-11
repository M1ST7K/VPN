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

    fun boolValue(obj: String, key: String): Boolean? =
        when (raw(obj, key)?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }

    /**
     * Extracts JSON objects from `"key":[{...},{...}]` without a JSON library.
     * Strings are skipped so braces inside values do not change depth.
     */
    fun objectArray(obj: String, key: String): List<String> {
        val needle = "\"$key\""
        val idx = obj.indexOf(needle)
        if (idx < 0) return emptyList()
        val colon = obj.indexOf(':', idx + needle.length)
        if (colon < 0) return emptyList()
        var i = colon + 1
        while (i < obj.length && obj[i].isWhitespace()) i++
        if (i >= obj.length || obj[i] != '[') return emptyList()
        i++
        val result = ArrayList<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escape = false
        while (i < obj.length) {
            val ch = obj[i]
            if (inString) {
                when {
                    escape -> escape = false
                    ch == '\\' -> escape = true
                    ch == '"' -> inString = false
                }
                i++
                continue
            }
            when (ch) {
                '"' -> inString = true
                '{' -> {
                    if (depth == 0) start = i
                    depth++
                }
                '}' -> {
                    depth--
                    if (depth == 0 && start >= 0) {
                        result.add(obj.substring(start, i + 1))
                        start = -1
                    }
                }
                ']' -> if (depth == 0) break
            }
            i++
        }
        return result
    }
}
