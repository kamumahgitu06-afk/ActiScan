package com.example.model

sealed interface TestStatus {
    data object NotTested : TestStatus
    data object Launched : TestStatus
    data class Denied(val reason: String) : TestStatus
    data class RequiresParameters(val reason: String) : TestStatus
}

enum class RootStatus {
    ROOTED,
    NON_ROOTED,
    UNKNOWN
}

data class RootDetectionResult(
    val status: RootStatus = RootStatus.UNKNOWN,
    val indicators: List<String> = emptyList()
)

data class ExportedActivityItem(
    val id: String,
    val appName: String,
    val packageName: String,
    val activityName: String,
    val permission: String?,
    val appPermission: String?,
    val isSystemApp: Boolean,
    val isEnabled: Boolean,
    val testStatus: TestStatus = TestStatus.NotTested
) {
    val hasNoPermission: Boolean
        get() = permission.isNullOrBlank()

    val shortActivityName: String
        get() = activityName.substringAfterLast('.')
}
