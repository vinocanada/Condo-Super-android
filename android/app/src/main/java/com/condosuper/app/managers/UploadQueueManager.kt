package com.condosuper.app.managers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.condosuper.app.data.models.PendingUpload
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class UploadQueueManager private constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: UploadQueueManager? = null
        
        fun getInstance(context: Context): UploadQueueManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UploadQueueManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val MAX_RETRIES = 3
    }

    private val prefs: SharedPreferences = 
        context.getSharedPreferences("upload_queue", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _pendingUploads = MutableStateFlow<List<PendingUpload>>(emptyList())
    val pendingUploads: StateFlow<List<PendingUpload>> = _pendingUploads.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _uploadProgress = MutableStateFlow<Map<String, Double>>(emptyMap())
    val uploadProgress: StateFlow<Map<String, Double>> = _uploadProgress.asStateFlow()

    private val firebaseManager = FirebaseManager.getInstance()
    private val networkMonitor = NetworkMonitor.getInstance(context)

    init {
        loadQueue()
    }

    fun addToQueue(upload: PendingUpload) {
        val current = _pendingUploads.value.toMutableList()
        current.add(upload)
        _pendingUploads.value = current
        saveQueue()
        Log.d("UploadQueueManager", "Added to upload queue: ${upload.photoId}")
        
        if (networkMonitor.isConnected.value) {
            scope.launch {
                processQueue()
            }
        }
    }

    fun removeFromQueue(uploadId: String) {
        val current = _pendingUploads.value.toMutableList()
        current.removeAll { it.id == uploadId }
        _pendingUploads.value = current
        saveQueue()
    }

    private fun saveQueue() {
        val json = gson.toJson(_pendingUploads.value)
        prefs.edit().putString("pending_uploads", json).apply()
    }

    private fun loadQueue() {
        val json = prefs.getString("pending_uploads", null) ?: return
        val type = object : TypeToken<List<PendingUpload>>() {}.type
        val uploads = gson.fromJson<List<PendingUpload>>(json, type) ?: emptyList()
        _pendingUploads.value = uploads.filter { it.status != PendingUpload.UploadStatus.COMPLETED }
        Log.d("UploadQueueManager", "Loaded ${_pendingUploads.value.size} pending uploads")
    }

    suspend fun processQueue() {
        if (_isProcessing.value) {
            Log.w("UploadQueueManager", "Upload queue already processing")
            return
        }

        if (!networkMonitor.isConnected.value) {
            Log.w("UploadQueueManager", "No network connection, skipping queue processing")
            return
        }

        if (_pendingUploads.value.isEmpty()) {
            Log.d("UploadQueueManager", "Upload queue is empty")
            return
        }

        _isProcessing.value = true
        Log.d("UploadQueueManager", "Processing upload queue (${_pendingUploads.value.size} items)")

        val uploads = _pendingUploads.value.filter { 
            it.status == PendingUpload.UploadStatus.PENDING || 
            it.status == PendingUpload.UploadStatus.FAILED 
        }

        for (upload in uploads) {
            if (upload.uploadAttempts >= MAX_RETRIES) {
                Log.e("UploadQueueManager", "Max retries reached for upload: ${upload.photoId}")
                updateUploadStatus(upload.id, PendingUpload.UploadStatus.FAILED)
                continue
            }

            uploadPendingItem(upload)
            kotlinx.coroutines.delay(500) // Small delay between uploads
        }

        _isProcessing.value = false
        Log.d("UploadQueueManager", "Finished processing upload queue")
    }

    private suspend fun uploadPendingItem(upload: PendingUpload) {
        updateUploadStatus(upload.id, PendingUpload.UploadStatus.UPLOADING)
        
        try {
            val file = File(upload.localFilePath)
            if (!file.exists()) {
                Log.e("UploadQueueManager", "Local file not found: ${file.path}")
                updateUploadStatus(upload.id, PendingUpload.UploadStatus.FAILED)
                return
            }

            val downloadURL: String? = if (upload.isVideo) {
                firebaseManager.uploadVideo(file, upload.photoId) { progress ->
                    _uploadProgress.value = _uploadProgress.value + (upload.id to progress)
                }
            } else {
                val imageData = file.readBytes()
                firebaseManager.uploadImage(imageData, upload.photoId) { progress ->
                    _uploadProgress.value = _uploadProgress.value + (upload.id to progress)
                }
            }

            if (downloadURL != null) {
                val photo = com.condosuper.app.data.models.JobPhoto(
                    id = upload.photoId,
                    companyId = upload.companyId,
                    sessionId = upload.sessionId,
                    siteId = upload.siteId,
                    employeeId = upload.employeeId,
                    imageURL = if (upload.isVideo) null else downloadURL,
                    videoURL = if (upload.isVideo) downloadURL else null,
                    isVideo = upload.isVideo,
                    comment = upload.comment,
                    tags = upload.tags,
                    date = upload.date,
                    latitude = upload.latitude,
                    longitude = upload.longitude
                )

                firebaseManager.savePhoto(photo)
                
                // Delete local file
                file.delete()
                
                // Remove from queue
                removeFromQueue(upload.id)
                val progressMap = _uploadProgress.value.toMutableMap()
                progressMap.remove(upload.id)
                _uploadProgress.value = progressMap
                
                Log.d("UploadQueueManager", "Successfully uploaded: ${upload.photoId}")
            } else {
                throw Exception("Upload failed")
            }
        } catch (e: Exception) {
            Log.e("UploadQueueManager", "Upload failed for ${upload.photoId}", e)
            incrementRetryCount(upload.id)
        }
    }

    private fun updateUploadStatus(uploadId: String, status: PendingUpload.UploadStatus) {
        val current = _pendingUploads.value.toMutableList()
        val index = current.indexOfFirst { it.id == uploadId }
        if (index >= 0) {
            current[index] = current[index].copy(
                status = status,
                lastAttemptDate = if (status == PendingUpload.UploadStatus.UPLOADING) 
                    System.currentTimeMillis() else current[index].lastAttemptDate
            )
            _pendingUploads.value = current
            saveQueue()
        }
    }

    private fun incrementRetryCount(uploadId: String) {
        val current = _pendingUploads.value.toMutableList()
        val index = current.indexOfFirst { it.id == uploadId }
        if (index >= 0) {
            current[index] = current[index].copy(
                uploadAttempts = current[index].uploadAttempts + 1,
                status = PendingUpload.UploadStatus.FAILED,
                lastAttemptDate = System.currentTimeMillis()
            )
            _pendingUploads.value = current
            saveQueue()
        }
    }

    fun saveLocalFile(data: ByteArray, isVideo: Boolean, context: Context): String? {
        val fileName = "${UUID.randomUUID()}.${if (isVideo) "mp4" else "jpg"}"
        val file = File(context.filesDir, "PendingUploads").apply { mkdirs() }
        val filePath = File(file, fileName)
        
        return try {
            filePath.writeBytes(data)
            Log.d("UploadQueueManager", "Saved local file: $fileName")
            filePath.absolutePath
        } catch (e: Exception) {
            Log.e("UploadQueueManager", "Failed to save local file", e)
            null
        }
    }

    fun saveLocalFile(sourceFile: File, isVideo: Boolean, context: Context): String? {
        val fileName = "${UUID.randomUUID()}.${if (isVideo) "mp4" else "jpg"}"
        val destDir = File(context.filesDir, "PendingUploads").apply { mkdirs() }
        val destFile = File(destDir, fileName)
        
        return try {
            sourceFile.copyTo(destFile, overwrite = true)
            Log.d("UploadQueueManager", "Saved local file: $fileName")
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e("UploadQueueManager", "Failed to save local file", e)
            null
        }
    }

    fun cleanupFailedUploads(olderThanDays: Int = 7) {
        val cutoffDate = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        val toRemove = _pendingUploads.value.filter { upload ->
            upload.status == PendingUpload.UploadStatus.FAILED &&
            upload.uploadAttempts >= MAX_RETRIES &&
            (upload.lastAttemptDate ?: upload.date) < cutoffDate
        }

        toRemove.forEach { upload ->
            File(upload.localFilePath).delete()
            removeFromQueue(upload.id)
        }

        if (toRemove.isNotEmpty()) {
            Log.d("UploadQueueManager", "Cleaned up ${toRemove.size} old failed uploads")
        }
    }
}


