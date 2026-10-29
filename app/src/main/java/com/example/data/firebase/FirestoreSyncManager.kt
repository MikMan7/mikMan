package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.db.entity.AlertEntity
import com.example.data.db.entity.NetworkUserEntity
import com.example.data.db.entity.RouterEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreSyncManager(private val context: Context) {

    private val TAG = "FirestoreSync"

    private val firestore: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
    }

    suspend fun syncRouterToCloud(userId: String, router: RouterEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("routers")
                .document("router_${router.id}")

            val data = hashMapOf(
                "userId" to userId,
                "name" to router.name,
                "ip" to router.ip,
                "port" to router.port,
                "username" to router.username,
                "useSsl" to router.useSsl,
                "routerModel" to router.routerModel,
                "routerOsVersion" to router.routerOsVersion,
                "cpuLoad" to router.cpuLoad,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(data).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing router: ${e.message}", e)
            false
        }
    }

    suspend fun syncNetworkUserToCloud(userId: String, user: NetworkUserEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("network_users")
                .document("user_${user.id}")

            val data = hashMapOf(
                "userId" to userId,
                "name" to user.name,
                "ipAddress" to user.ipAddress,
                "macAddress" to user.macAddress,
                "hotspotUser" to user.hotspotUser,
                "quotaGb" to user.quotaGb,
                "usedDownloadGb" to user.usedDownloadGb,
                "usedUploadGb" to user.usedUploadGb,
                "maxDownloadMbps" to user.maxDownloadMbps,
                "maxUploadMbps" to user.maxUploadMbps,
                "alertThresholdPercent" to user.alertThresholdPercent,
                "status" to user.status.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            docRef.set(data).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user: ${e.message}", e)
            false
        }
    }

    suspend fun syncAlertToCloud(userId: String, alert: AlertEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("users")
                .document(userId)
                .collection("alerts")
                .document("alert_${alert.id}")

            val data = hashMapOf(
                "userId" to userId,
                "userName" to alert.userName,
                "title" to alert.title,
                "message" to alert.message,
                "severity" to alert.severity.name,
                "isResolved" to alert.isResolved,
                "actionTaken" to alert.actionTaken,
                "createdAt" to FieldValue.serverTimestamp()
            )

            docRef.set(data).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing alert: ${e.message}", e)
            false
        }
    }
}
