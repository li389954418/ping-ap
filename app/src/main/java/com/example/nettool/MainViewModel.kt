// 智能解析：根据所有启用模板提取信息，支持指定分类
fun autoParseAndPreview(text: String, targetCategory: String = "默认"): List<IpEntry> {
    val enabledTemplates = runBlocking { db.templateDao().getEnabledTemplates().firstOrNull() ?: emptyList() }
    if (enabledTemplates.isEmpty()) return emptyList()

    val addressCandidates = mutableListOf<String>()
    val nameCandidates = mutableListOf<String>()
    val remarkMap = mutableMapOf<String, MutableList<String>>()

    for (template in enabledTemplates) {
        try {
            val rulesArray = JSONArray(template.rulesJson)
            for (i in 0 until rulesArray.length()) {
                val rule = rulesArray.getJSONObject(i)
                val keyword = rule.getString("keyword")
                val targetField = rule.getString("targetField")
                val extractUntil = rule.optString("extractUntil", "line")

                val keywordIndex = text.indexOf(keyword, ignoreCase = true)
                if (keywordIndex >= 0) {
                    val startIndex = keywordIndex + keyword.length
                    val remaining = text.substring(startIndex)
                    val endIndex = when (extractUntil) {
                        "line" -> remaining.indexOfAny(charArrayOf('\n', '\r'))
                        else -> remaining.indexOf(' ')
                    }
                    val extracted = if (endIndex > 0) {
                        remaining.substring(0, endIndex).trim()
                    } else {
                        remaining.trim()
                    }

                    if (extracted.isNotEmpty()) {
                        when {
                            targetField == "address" -> {
                                val ips = extracted.split(Regex("[,，、\\s]+")).filter { it.isNotBlank() }
                                ips.forEach { ip ->
                                    if (!addressCandidates.contains(ip)) {
                                        addressCandidates.add(ip)
                                    }
                                }
                            }
                            targetField == "name" -> {
                                if (!nameCandidates.contains(extracted)) {
                                    nameCandidates.add(extracted)
                                }
                            }
                            targetField.startsWith("remark_") -> {
                                val key = targetField.removePrefix("remark_")
                                remarkMap.getOrPut(key) { mutableListOf() }.add(extracted)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略解析错误
        }
    }

    if (addressCandidates.isEmpty()) return emptyList()

    val primaryAddress = addressCandidates.first()
    val primaryName = nameCandidates.firstOrNull() ?: primaryAddress

    val extraJson = JSONObject()
    addressCandidates.drop(1).forEachIndexed { index, ip ->
        extraJson.put("IP${index + 2}", ip)
    }
    remarkMap.forEach { (key, values) ->
        extraJson.put(key, values.firstOrNull() ?: "")
    }

    return listOf(
        IpEntry(
            name = primaryName.ifBlank { "未命名" },
            address = primaryAddress,
            extraRemarks = extraJson.toString(),
            category = targetCategory
        )
    )
}