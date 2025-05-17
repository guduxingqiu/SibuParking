package sibu.parking.model

enum class ReportType {
    PARKING_ISSUE,  // 停车位问题
    APP_BUG        // 应用bug
}

enum class ReportStatus {
    PENDING,    // 待处理
    IN_PROGRESS,// 处理中
    RESOLVED,    // 已解决
    REJECTED
}

data class Report(
    val id: String = "",
    val userId: String = "",
    val type: ReportType = ReportType.PARKING_ISSUE,
    val title: String = "",
    val description: String = "",
    val parkingArea: String = "",  // 如果是停车位问题，记录具体位置
    val parkingLotNumber: String = "", // 如果是停车位问题，记录车位号
    val timestamp: Long = System.currentTimeMillis(),
    val status: ReportStatus = ReportStatus.PENDING,
    val imageUrls: List<String> = emptyList() // 可选的照片URL列表
) 