package sibu.parking.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import sibu.parking.model.User
import sibu.parking.model.UserType
import sibu.parking.model.Report
import sibu.parking.model.ReportType
import sibu.parking.model.ReportStatus
import java.util.UUID

class FirebaseReportService {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    // 创建新报告
    suspend fun createReport(
        type: ReportType,
        title: String,
        description: String,
        parkingArea: String = "",
        parkingLotNumber: String = "",
        imageUrls: List<String> = emptyList()
    ): Result<Report> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val report = Report(
                id = UUID.randomUUID().toString(),
                userId = userId,
                type = type,
                title = title,
                description = description,
                parkingArea = parkingArea,
                parkingLotNumber = parkingLotNumber,
                timestamp = System.currentTimeMillis(),
                imageUrls = imageUrls
            )
            
            firestore.collection("reports")
                .document(report.id)
                .set(report)
                .await()
                
            Result.success(report)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseReportService", "Error creating report: ${e.message}")
            Result.failure(e)
        }
    }
    
    // 获取报告列表
    suspend fun getUserReports(): Flow<List<Report>> = flow {
        try {
            val userId = auth.currentUser?.uid ?: return@flow
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userType = userDoc.getString("userType")
            
            android.util.Log.d("FirebaseReportService", "Getting reports for user $userId with type $userType")
            
            val query = if (userType == UserType.STAFF.name) {
                android.util.Log.d("FirebaseReportService", "Building query for staff user")
                // 员工可以看到所有报告
                firestore.collection("reports")
            } else {
                android.util.Log.d("FirebaseReportService", "Building query for regular user")
                // 普通用户只能看到自己的报告
                firestore.collection("reports")
                    .whereEqualTo("userId", userId)
            }
            
            val snapshot = query.get().await()
            val reports = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Report::class.java)
            }.sortedByDescending { it.timestamp }  // 在内存中排序
            
            android.util.Log.d("FirebaseReportService", "Found ${reports.size} reports")
            reports.forEach { report ->
                android.util.Log.d("FirebaseReportService", "Report: id=${report.id}, userId=${report.userId}, title=${report.title}")
            }
            
            emit(reports)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseReportService", "Error getting user reports: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }
    
    // 更新报告状态（仅员工可用）
    suspend fun updateReportStatus(reportId: String, newStatus: ReportStatus): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userType = userDoc.getString("userType")
            
            if (userType != UserType.STAFF.name) {
                return Result.failure(Exception("Only staff can update report status"))
            }
            
            firestore.collection("reports")
                .document(reportId)
                .update("status", newStatus.name)
                .await()
                
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("FirebaseReportService", "Error updating report status: ${e.message}")
            Result.failure(e)
        }
    }
} 