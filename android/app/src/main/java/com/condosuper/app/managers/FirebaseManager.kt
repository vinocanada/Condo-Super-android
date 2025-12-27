package com.condosuper.app.managers

import android.util.Log
import com.condosuper.app.data.models.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseManager private constructor() {
    companion object {
        @Volatile
        private var INSTANCE: FirebaseManager? = null
        
        fun getInstance(): FirebaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseManager().also { INSTANCE = it }
            }
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val _currentCompany = MutableStateFlow<Company?>(null)
    val currentCompany: StateFlow<Company?> = _currentCompany.asStateFlow()
    
    private val _currentEmployee = MutableStateFlow<Employee?>(null)
    val currentEmployee: StateFlow<Employee?> = _currentEmployee.asStateFlow()
    
    private val _companies = MutableStateFlow<List<Company>>(emptyList())
    val companies: StateFlow<List<Company>> = _companies.asStateFlow()
    
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()
    
    private val _jobSites = MutableStateFlow<List<JobSite>>(emptyList())
    val jobSites: StateFlow<List<JobSite>> = _jobSites.asStateFlow()
    
    private val _timeEntries = MutableStateFlow<List<TimeEntry>>(emptyList())
    val timeEntries: StateFlow<List<TimeEntry>> = _timeEntries.asStateFlow()
    
    private val _photos = MutableStateFlow<List<JobPhoto>>(emptyList())
    val photos: StateFlow<List<JobPhoto>> = _photos.asStateFlow()
    
    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()
    
    private val _locationPoints = MutableStateFlow<List<LocationPoint>>(emptyList())
    val locationPoints: StateFlow<List<LocationPoint>> = _locationPoints.asStateFlow()
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _tasks = MutableStateFlow<List<TaskTicket>>(emptyList())
    val tasks: StateFlow<List<TaskTicket>> = _tasks.asStateFlow()
    
    private val _taskComments = MutableStateFlow<List<TaskComment>>(emptyList())
    val taskComments: StateFlow<List<TaskComment>> = _taskComments.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private var listeners: MutableList<ListenerRegistration> = mutableListOf()
    private var currentUserId: String? = null
    private val previousMessageIds = mutableSetOf<String>()

    fun setCompany(company: Company) {
        _currentCompany.value = company
        setupListeners()
    }
    
    fun setCurrentEmployee(employee: Employee) {
        _currentEmployee.value = employee
    }

    private fun setupListeners() {
        val companyId = _currentCompany.value?.id ?: return
        
        // Clear existing listeners
        listeners.forEach { it.remove() }
        listeners.clear()
        
        Log.d("FirebaseManager", "Setting up listeners for company: ${_currentCompany.value?.name}")

        // Employees listener
        listeners.add(
            db.collection("employees")
                .whereEqualTo("companyId", companyId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Error listening to employees", error)
                        return@addSnapshotListener
                    }
                    val employeesList = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Employee::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseManager", "Error parsing employee", e)
                            null
                        }
                    } ?: emptyList()
                    _employees.value = employeesList
                    Log.d("FirebaseManager", "Loaded ${employeesList.size} employees")
                }
        )

        // Job sites listener
        listeners.add(
            db.collection("jobSites")
                .whereEqualTo("companyId", companyId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Error listening to job sites", error)
                        return@addSnapshotListener
                    }
                    val sitesList = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(JobSite::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseManager", "Error parsing job site", e)
                            null
                        }
                    } ?: emptyList()
                    _jobSites.value = sitesList
                    Log.d("FirebaseManager", "Loaded ${sitesList.size} job sites")
                }
        )

        // Time entries listener
        listeners.add(
            db.collection("timeEntries")
                .whereEqualTo("companyId", companyId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Error listening to time entries", error)
                        return@addSnapshotListener
                    }
                    val entriesList = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(TimeEntry::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseManager", "Error parsing time entry", e)
                            null
                        }
                    } ?: emptyList()
                    _timeEntries.value = entriesList
                    Log.d("FirebaseManager", "Loaded ${entriesList.size} time entries")
                }
        )

        // Photos listener
        listeners.add(
            db.collection("photos")
                .whereEqualTo("companyId", companyId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Error listening to photos", error)
                        return@addSnapshotListener
                    }
                    val photosList = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(JobPhoto::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseManager", "Error parsing photo", e)
                            null
                        }
                    } ?: emptyList()
                    _photos.value = photosList
                    Log.d("FirebaseManager", "Loaded ${photosList.size} photos")
                }
        )

        // Tags listener
        listeners.add(
            db.collection("tags")
                .whereEqualTo("companyId", companyId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Error listening to tags", error)
                        return@addSnapshotListener
                    }
                    val tagsList = snapshot?.documents?.mapNotNull { doc ->
                        doc.getString("name")
                    } ?: emptyList()
                    _tags.value = tagsList
                    Log.d("FirebaseManager", "Loaded ${tagsList.size} tags")
                }
        )

        // Location points listener
        listeners.add(
            db.collection("locationPoints")
                .whereEqualTo("companyId", companyId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Error listening to location points", error)
                        return@addSnapshotListener
                    }
                    val pointsList = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(LocationPoint::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseManager", "Error parsing location point", e)
                            null
                        }
                    } ?: emptyList()
                    _locationPoints.value = pointsList
                    Log.d("FirebaseManager", "Loaded ${pointsList.size} location points")
                }
        )

        // Tasks listener
        listeners.add(
            db.collection("tasks")
                .whereEqualTo("companyId", companyId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Error listening to tasks", error)
                        return@addSnapshotListener
                    }
                    val tasksList = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(TaskTicket::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseManager", "Error parsing task", e)
                            null
                        }
                    } ?: emptyList()
                    _tasks.value = tasksList
                    Log.d("FirebaseManager", "Loaded ${tasksList.size} tasks")
                }
        )

        // Task comments listener
        listeners.add(
            db.collection("taskComments")
                .whereEqualTo("companyId", companyId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Error listening to task comments", error)
                        return@addSnapshotListener
                    }
                    val commentsList = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(TaskComment::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseManager", "Error parsing task comment", e)
                            null
                        }
                    } ?: emptyList()
                    _taskComments.value = commentsList
                    Log.d("FirebaseManager", "Loaded ${commentsList.size} task comments")
                }
        )

        // Messages listener
        startMessagesListener()
    }

    suspend fun createCompany(name: String, ownerEmail: String): Company? {
        return try {
            val company = Company(
                name = name,
                subscriptionStatus = "trial",
                subscriptionEndDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000), // 30 days
                createdDate = System.currentTimeMillis(),
                ownerEmail = ownerEmail
            )
            db.collection("companies").document(company.id).set(company).await()
            Log.d("FirebaseManager", "Created company: ${company.name}")
            company
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error creating company", e)
            null
        }
    }

    suspend fun fetchCompany(byId: String): Company? {
        return try {
            val doc = db.collection("companies").document(byId).get().await()
            doc.toObject(Company::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error fetching company", e)
            null
        }
    }

    suspend fun saveEmployee(employee: Employee) {
        try {
            db.collection("employees").document(employee.id).set(employee).await()
            Log.d("FirebaseManager", "Saved employee: ${employee.name}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving employee", e)
        }
    }

    suspend fun deleteEmployee(employee: Employee) {
        try {
            db.collection("employees").document(employee.id).delete().await()
            Log.d("FirebaseManager", "Deleted employee: ${employee.name}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error deleting employee", e)
        }
    }

    suspend fun saveJobSite(site: JobSite) {
        try {
            db.collection("jobSites").document(site.id).set(site).await()
            Log.d("FirebaseManager", "Saved job site: ${site.name}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving job site", e)
        }
    }

    suspend fun deleteJobSite(site: JobSite) {
        try {
            db.collection("jobSites").document(site.id).delete().await()
            Log.d("FirebaseManager", "Deleted job site: ${site.name}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error deleting job site", e)
        }
    }

    suspend fun saveTimeEntry(entry: TimeEntry) {
        try {
            Log.d("FirebaseManager", "Saving time entry to Firestore...")
            Log.d("FirebaseManager", "Entry ID: ${entry.id}")
            Log.d("FirebaseManager", "Company ID: ${entry.companyId}")
            Log.d("FirebaseManager", "Employee ID: ${entry.employeeId}")
            Log.d("FirebaseManager", "Site ID: ${entry.siteId}")
            Log.d("FirebaseManager", "Clock In Time: ${entry.clockInTime}")
            Log.d("FirebaseManager", "Clock Out Time: ${entry.clockOutTime}")
            
            db.collection("timeEntries").document(entry.id).set(entry).await()
            Log.d("FirebaseManager", "Time entry saved successfully")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving time entry", e)
        }
    }

    suspend fun saveLocationPoint(locationPoint: LocationPoint) {
        try {
            Log.d("FirebaseManager", "Saving location point...")
            Log.d("FirebaseManager", "Employee ID: ${locationPoint.employeeId}")
            Log.d("FirebaseManager", "Coordinates: ${locationPoint.latitude}, ${locationPoint.longitude}")
            Log.d("FirebaseManager", "Accuracy: ${locationPoint.accuracy}m")
            
            db.collection("locationPoints").document(locationPoint.id).set(locationPoint).await()
            Log.d("FirebaseManager", "Location point saved successfully")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving location point", e)
        }
    }

    suspend fun savePhoto(photo: JobPhoto) {
        try {
            db.collection("photos").document(photo.id).set(photo).await()
            Log.d("FirebaseManager", "Photo saved to Firestore")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving photo", e)
        }
    }

    suspend fun uploadImage(
        imageData: ByteArray,
        photoId: String,
        onProgress: ((Double) -> Unit)? = null
    ): String? {
        return try {
            val storageRef = storage.reference.child("photos/${photoId}.jpg")
            val uploadTask = storageRef.putBytes(imageData)
            
            // Monitor progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                onProgress?.invoke(progress / 100.0)
            }
            
            val snapshot = uploadTask.await()
            val downloadURL = snapshot.storage.downloadUrl.await()
            Log.d("FirebaseManager", "Image uploaded: ${downloadURL.toString()}")
            downloadURL.toString()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error uploading image", e)
            null
        }
    }

    suspend fun uploadVideo(
        videoFile: java.io.File,
        photoId: String,
        onProgress: ((Double) -> Unit)? = null
    ): String? {
        return try {
            val storageRef = storage.reference.child("videos/${photoId}.mp4")
            val uploadTask = storageRef.putFile(android.net.Uri.fromFile(videoFile))
            
            // Monitor progress
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                onProgress?.invoke(progress / 100.0)
            }
            
            val snapshot = uploadTask.await()
            val downloadURL = snapshot.storage.downloadUrl.await()
            Log.d("FirebaseManager", "Video uploaded: ${downloadURL.toString()}")
            downloadURL.toString()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error uploading video", e)
            null
        }
    }

    suspend fun saveTag(tag: String) {
        val companyId = _currentCompany.value?.id ?: return
        val cleanTag = tag.trim()
        if (cleanTag.isEmpty()) return
        
        try {
            db.collection("tags").document("${companyId}_$cleanTag").set(
                mapOf("companyId" to companyId, "name" to cleanTag)
            ).await()
            Log.d("FirebaseManager", "Saved tag: $cleanTag")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving tag", e)
        }
    }

    suspend fun deleteTag(tag: String) {
        val companyId = _currentCompany.value?.id ?: return
        try {
            db.collection("tags").document("${companyId}_$tag").delete().await()
            Log.d("FirebaseManager", "Deleted tag: $tag")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error deleting tag", e)
        }
    }

    fun setCurrentUser(userId: String) {
        currentUserId = userId
        previousMessageIds.clear()
        previousMessageIds.addAll(_messages.value.map { it.id })
    }

    private fun startMessagesListener() {
        val companyId = _currentCompany.value?.id ?: return
        
        listeners.add(
            db.collection("messages")
                .whereEqualTo("companyId", companyId)
                .orderBy("timestamp")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("FirebaseManager", "Error listening to messages", error)
                        return@addSnapshotListener
                    }
                    
                    val newMessages = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(Message::class.java)
                        } catch (e: Exception) {
                            Log.e("FirebaseManager", "Error parsing message", e)
                            null
                        }
                    } ?: emptyList()
                    
                    // Check for new messages and send notifications
                    for (message in newMessages) {
                        if (!previousMessageIds.contains(message.id) &&
                            message.senderId != currentUserId &&
                            message.recipientId == currentUserId &&
                            !message.isRead) {
                            // Send notification (handled by NotificationManager)
                        }
                    }
                    
                    previousMessageIds.clear()
                    previousMessageIds.addAll(newMessages.map { it.id })
                    
                    _messages.value = newMessages
                    Log.d("FirebaseManager", "Loaded ${newMessages.size} messages")
                }
        )
    }

    suspend fun sendMessage(
        content: String,
        recipientId: String?,
        imageURL: String?,
        videoURL: String?,
        senderId: String,
        senderName: String
    ) {
        val companyId = _currentCompany.value?.id ?: return
        
        // Encrypt content (handled by EncryptionManager)
        val encryptedContent = EncryptionManager.getInstance().encrypt(content, companyId) ?: content
        val encryptedImageURL = imageURL?.let { EncryptionManager.getInstance().encryptURL(it, companyId) }
        val encryptedVideoURL = videoURL?.let { EncryptionManager.getInstance().encryptURL(it, companyId) }
        
        val message = Message(
            companyId = companyId,
            senderId = senderId,
            senderName = senderName,
            recipientId = recipientId,
            chatType = if (recipientId == null) Message.ChatType.GROUP else Message.ChatType.DIRECT,
            content = encryptedContent,
            imageURL = encryptedImageURL,
            videoURL = encryptedVideoURL,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )
        
        try {
            db.collection("messages").document(message.id).set(message).await()
            Log.d("FirebaseManager", "Sent encrypted message")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error sending message", e)
        }
    }

    suspend fun markMessageAsRead(messageId: String) {
        try {
            db.collection("messages").document(messageId).update("isRead", true).await()
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error marking message as read", e)
        }
    }

    suspend fun deleteMessage(messageId: String) {
        try {
            db.collection("messages").document(messageId).delete().await()
            Log.d("FirebaseManager", "Deleted message")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error deleting message", e)
        }
    }

    suspend fun createTask(task: TaskTicket) {
        try {
            db.collection("tasks").document(task.id).set(task).await()
            Log.d("FirebaseManager", "Created task: ${task.title}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error creating task", e)
        }
    }

    suspend fun updateTask(task: TaskTicket) {
        try {
            db.collection("tasks").document(task.id).set(task).await()
            Log.d("FirebaseManager", "Updated task: ${task.title}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating task", e)
        }
    }

    suspend fun updateTaskStatus(taskId: String, status: TaskTicket.TaskStatus, completedAt: Long? = null) {
        try {
            val updates = mutableMapOf<String, Any>("status" to status.name)
            completedAt?.let { updates["completedAt"] = it }
            db.collection("tasks").document(taskId).update(updates).await()
            Log.d("FirebaseManager", "Updated task status to: ${status.name}")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error updating task status", e)
        }
    }

    suspend fun assignTask(taskId: String, employeeId: String, employeeName: String) {
        try {
            db.collection("tasks").document(taskId).update(
                mapOf("assignedTo" to employeeId, "assignedToName" to employeeName)
            ).await()
            Log.d("FirebaseManager", "Assigned task to: $employeeName")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error assigning task", e)
        }
    }

    suspend fun unassignTask(taskId: String) {
        try {
            val updates = mapOf(
                "assignedTo" to com.google.firebase.firestore.FieldValue.delete(),
                "assignedToName" to com.google.firebase.firestore.FieldValue.delete()
            )
            db.collection("tasks").document(taskId).update(updates).await()
            Log.d("FirebaseManager", "Unassigned task")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error unassigning task", e)
        }
    }

    suspend fun deleteTask(taskId: String) {
        try {
            // Delete all comments for this task
            val comments = db.collection("taskComments")
                .whereEqualTo("taskId", taskId)
                .get().await()
            
            comments.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            
            // Delete the task
            db.collection("tasks").document(taskId).delete().await()
            Log.d("FirebaseManager", "Deleted task and its comments")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error deleting task", e)
        }
    }

    suspend fun addTaskComment(comment: TaskComment) {
        try {
            db.collection("taskComments").document(comment.id).set(comment).await()
            Log.d("FirebaseManager", "Added comment to task")
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error adding comment", e)
        }
    }

    fun getCommentsForTask(taskId: String): List<TaskComment> {
        return _taskComments.value.filter { it.taskId == taskId }
            .sortedBy { it.timestamp }
    }
}


