package com.example.nettool.utils

object PingUtils {
    /**
     * 处理用户输入的ping次数，添加0值作为无限ping标识
     * - "inf"、"∞"或"0"表示无限ping
     * - 空值自动转为默认4次测试
     * - 负数转为最小有效值1
     * - 超大值限制为999999
     */
    fun processPingCount(userInput: String): Int {
        val trimmed = userInput.trim()
        return when {
            trimmed.equals("inf", ignoreCase = true) || 
            trimmed == "∞" || 
            trimmed == "0" -> -1  // 统一转换为-1表示无限ping
            trimmed.isEmpty() -> 4
            trimmed.toIntOrNull() != null -> {
                val num = trimmed.toInt()
                when {
                    num < 0 -> 1
                    num > 999999 -> 999999
                    else -> num
                }
            }
            else -> 4
        }
    }

    /**
     * 验证是否为安全的无限ping请求
     * - 检查用户是否明确表示需要无限ping
     * - 防止意外输入0导致无限ping
     */
    fun isSafeInfinityPing(userInput: String): Boolean {
        val trimmed = userInput.trim()
        return trimmed.equals("inf", ignoreCase = true) || 
               trimmed == "∞" || 
               trimmed == "0"
    }
}
