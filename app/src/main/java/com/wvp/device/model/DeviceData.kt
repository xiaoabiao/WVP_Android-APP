package com.wvp.device.model

import android.util.Log

/**
 * 设备数据模型
 * 对应原始 Python 设备列表中的字段
 */
data class DeviceInfo(
    val deviceId: String = "",
    val name: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val hostAddress: String = "",
    val port: Int = 0,
    val status: String = "",
    val online: Boolean = false,
    val streamMode: String = "",
    val sdpIp: String = "",
    val extra: Map<String, Any> = emptyMap()
) {
    companion object {
        fun fromMap(map: Map<String, Any>): DeviceInfo {
            return DeviceInfo(
                deviceId = map["deviceId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                manufacturer = map["manufacturer"] as? String ?: "",
                model = map["model"] as? String ?: "",
                hostAddress = map["hostAddress"] as? String ?: "",
                port = (map["port"] as? Number)?.toInt() ?: 0,
                status = map["status"] as? String ?: "",
                online = parseNullableOnline(map["onLine"])
                    ?: parseNullableOnline(map["online"])
                    ?: parseNullableOnline(map["status"])
                    ?: false,
                streamMode = map["streamMode"] as? String ?: "",
                sdpIp = map["sdpIp"] as? String ?: "",
                extra = map
            )
        }

        private fun parseNullableOnline(value: Any?): Boolean? {
            return when (value) {
                is Boolean -> value
                is Number -> value.toInt() == 1
                is String -> when (value.uppercase()) {
                    "1", "TRUE", "ON", "YES", "ENABLE", "ENABLED" -> true
                    "0", "FALSE", "OFF", "NO", "DISABLE", "DISABLED" -> false
                    else -> null
                }
                else -> null
            }
        }
    }
}

/**
 * 通道数据模型
 */
data class ChannelInfo(
    val deviceId: String = "",
    val name: String = "",
    val manufacturer: String = "",
    val model: String = "",
    val status: String = "",
    val online: Boolean = false,
    val streamId: String = "",
    val extra: Map<String, Any> = emptyMap()
) {
    companion object {
        fun fromMap(map: Map<String, Any>): ChannelInfo {
            return ChannelInfo(
                deviceId = map["deviceId"] as? String ?: "",
                name = map["name"] as? String ?: "",
                manufacturer = map["manufacturer"] as? String ?: "",
                model = map["model"] as? String ?: "",
                status = map["status"] as? String ?: "",
                online = parseNullableOnline(map["onLine"])
                    ?: parseNullableOnline(map["online"])
                    ?: parseNullableOnline(map["status"])
                    ?: false,
                streamId = map["streamId"] as? String ?: "",
                extra = map
            )
        }

        private fun parseNullableOnline(value: Any?): Boolean? {
            return when (value) {
                is Boolean -> value
                is Number -> value.toInt() == 1
                is String -> when (value.uppercase()) {
                    "1", "TRUE", "ON", "YES", "ENABLE", "ENABLED" -> true
                    "0", "FALSE", "OFF", "NO", "DISABLE", "DISABLED" -> false
                    else -> null
                }
                else -> null
            }
        }
    }
}
