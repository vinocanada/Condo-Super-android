package com.condosuper.app.data.models

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import java.util.UUID

// MARK: - Company Model
@Serializable
data class Company(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subscriptionStatus: String, // "trial", "active", or "expired"
    val subscriptionEndDate: Long, // Timestamp
    val createdDate: Long, // Timestamp
    val ownerEmail: String
)

// MARK: - Employee Model
@Serializable
data class Employee(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val name: String,
    val isManager: Boolean,
    val pin: String // 4-digit PIN
)

// MARK: - Job Site Model
@Serializable
data class JobSite(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Double // Geofence radius in meters
)

// MARK: - Time Entry Model
@Serializable
data class TimeEntry(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val employeeId: String,
    val siteId: String,
    val clockInTime: Long, // Timestamp
    val clockInLat: Double,
    val clockInLon: Double,
    val clockOutTime: Long? = null, // Timestamp, null if still working
    val clockOutLat: Double? = null,
    val clockOutLon: Double? = null,
    val breaks: List<BreakEntry> = emptyList()
)

// MARK: - Break Entry Model
@Serializable
data class BreakEntry(
    val id: String = UUID.randomUUID().toString(),
    val startTime: Long, // Timestamp
    val endTime: Long? = null // Timestamp, null if on break now
)

// MARK: - Location Point Model
@Serializable
data class LocationPoint(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val employeeId: String,
    val timeEntryId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long, // Timestamp
    val accuracy: Double // GPS accuracy in meters
)

// MARK: - Message Model
@Serializable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val senderId: String,
    val senderName: String,
    val recipientId: String? = null, // null = group chat
    val siteId: String? = null, // Optional: for site-specific group chats
    val chatType: ChatType,
    val content: String, // Encrypted
    val imageURL: String? = null, // Encrypted
    val videoURL: String? = null, // Encrypted
    val documentURL: String? = null, // Encrypted
    val documentName: String? = null,
    val voiceNoteURL: String? = null, // Encrypted
    val voiceNoteDuration: Double? = null, // Duration in seconds
    val timestamp: Long, // Timestamp
    val isRead: Boolean = false,
    val isDelivered: Boolean = true,
    val readAt: Long? = null, // Timestamp
    val readBy: List<String> = emptyList(), // List of user IDs who read (for group chats)
    val reactions: List<MessageReaction> = emptyList(),
    val isPinned: Boolean = false,
    val replyToId: String? = null,
    val replyToPreview: String? = null
) {
    enum class ChatType {
        DIRECT,           // One-on-one message
        GROUP,           // Company-wide group chat
        SITE,            // Job site specific chat
        ANNOUNCEMENT     // Manager announcements
    }
}

// MARK: - Message Reaction Model
@Serializable
data class MessageReaction(
    val emoji: String,                    // The emoji (e.g., "👍", "❤️")
    val userId: String,                    // Who reacted
    val userName: String,                 // Name of reactor
    val timestamp: Long                   // Timestamp
)

// MARK: - Announcement Model
@Serializable
data class Announcement(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val authorId: String,
    val authorName: String,
    val title: String,
    val content: String,
    val priority: AnnouncementPriority,
    val imageURL: String? = null,
    val createdAt: Long, // Timestamp
    val expiresAt: Long? = null, // Timestamp
    val isPinned: Boolean = false,
    val readBy: List<String> = emptyList() // Employee IDs who have read it
) {
    enum class AnnouncementPriority {
        NORMAL,
        IMPORTANT,
        URGENT
    }
}

// MARK: - Typing Indicator Model
@Serializable
data class TypingIndicator(
    val userId: String,                    // Who is typing
    val userName: String,                  // Name of typer
    val recipientId: String? = null,      // In which conversation (null = group)
    val siteId: String? = null,            // In which site chat
    val chatType: Message.ChatType,        // Type of chat
    val timestamp: Long                    // Timestamp (expires after 5 seconds)
)

// MARK: - Task/Ticket Model
@Serializable
data class TaskTicket(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val siteId: String? = null,
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val status: TaskStatus,
    val createdBy: String,
    val createdByName: String,
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val createdAt: Long, // Timestamp
    val dueDate: Long? = null, // Timestamp
    val completedAt: Long? = null, // Timestamp
    val imageURLs: List<String> = emptyList(),
    val videoURLs: List<String> = emptyList()
) {
    enum class TaskPriority(val displayName: String, val color: String, val icon: String) {
        LOW("Low", "green", "arrow.down.circle.fill"),
        MEDIUM("Medium", "blue", "minus.circle.fill"),
        HIGH("High", "orange", "arrow.up.circle.fill"),
        URGENT("Urgent", "red", "exclamationmark.circle.fill")
    }

    enum class TaskStatus(val displayName: String, val color: String, val icon: String) {
        OPEN("Open", "gray", "circle"),
        IN_PROGRESS("In Progress", "blue", "play.circle.fill"),
        PENDING_REVIEW("Pending Review", "orange", "clock.fill"),
        COMPLETED("Completed", "green", "checkmark.circle.fill"),
        CLOSED("Closed", "purple", "xmark.circle.fill")
    }
}

// MARK: - Task Comment Model
@Serializable
data class TaskComment(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val companyId: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val imageURLs: List<String> = emptyList(),
    val videoURLs: List<String> = emptyList(),
    val timestamp: Long, // Timestamp
    val isStatusChange: Boolean = false,
    val oldStatus: String? = null,
    val newStatus: String? = null
)

// MARK: - Job Photo Model
@Serializable
data class JobPhoto(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val sessionId: String, // Groups photos taken together
    val siteId: String,
    val employeeId: String,
    val imageURL: String? = null, // Firebase Storage URL
    val videoURL: String? = null, // Firebase Storage URL
    val isVideo: Boolean,
    val comment: String,
    val tags: List<String> = emptyList(),
    val date: Long, // Timestamp
    val latitude: Double? = null,
    val longitude: Double? = null
)

// MARK: - Pending Upload Model
@Serializable
data class PendingUpload(
    val id: String = UUID.randomUUID().toString(),
    val photoId: String,
    val companyId: String,
    val sessionId: String,
    val siteId: String,
    val employeeId: String,
    val isVideo: Boolean,
    val comment: String,
    val tags: List<String> = emptyList(),
    val date: Long, // Timestamp
    val latitude: Double? = null,
    val longitude: Double? = null,
    val localFilePath: String,
    val uploadAttempts: Int = 0,
    val lastAttemptDate: Long? = null, // Timestamp
    val status: UploadStatus = UploadStatus.PENDING
) {
    enum class UploadStatus {
        PENDING,
        UPLOADING,
        FAILED,
        COMPLETED
    }
}

// MARK: - Scheduled Shift Model
@Serializable
data class ScheduledShift(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val employeeId: String,
    val employeeName: String,
    val siteId: String? = null,
    val siteName: String? = null,
    val startTime: Long, // Timestamp
    val endTime: Long, // Timestamp
    val notes: String? = null,
    val isRecurring: Boolean = false,
    val recurringPattern: RecurringPattern? = null,
    val createdBy: String,
    val createdAt: Long, // Timestamp
    val status: ShiftStatus
) {
    enum class ShiftStatus {
        SCHEDULED,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        MISSED,
        CANCELLED
    }

    enum class RecurringPattern {
        DAILY,
        WEEKDAYS,
        WEEKENDS,
        WEEKLY,
        BIWEEKLY,
        MONTHLY
    }
}

// MARK: - Shift Swap Request Model
@Serializable
data class ShiftSwapRequest(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val originalShiftId: String,
    val requesterId: String,
    val requesterName: String,
    val targetEmployeeId: String? = null, // null = open request
    val targetEmployeeName: String? = null,
    val targetShiftId: String? = null, // Their shift (for direct swap)
    val reason: String? = null,
    val status: SwapStatus,
    val createdAt: Long, // Timestamp
    val respondedAt: Long? = null, // Timestamp
    val managerApproval: Boolean? = null,
    val managerApprovedBy: String? = null
) {
    enum class SwapStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        CANCELLED,
        MANAGER_PENDING,
        APPROVED
    }
}

// MARK: - Geofence Event Model
@Serializable
data class GeofenceEvent(
    val id: String = UUID.randomUUID().toString(),
    val siteId: String,
    val eventType: EventType,
    val timestamp: Long, // Timestamp
    val latitude: Double,
    val longitude: Double
) {
    enum class EventType {
        ENTER,
        EXIT
    }
}

// MARK: - Checklist Template Model
@Serializable
data class ChecklistTemplate(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val name: String,
    val description: String,
    val category: ChecklistCategory,
    val items: List<ChecklistItemTemplate>,
    val requiresSignature: Boolean = false,
    val requiresPhoto: Boolean = false,
    val isActive: Boolean = true,
    val createdBy: String,
    val createdAt: Long, // Timestamp
    val siteIds: List<String>? = null // Optional: specific sites only
) {
    enum class ChecklistCategory {
        SAFETY,
        INSPECTION,
        MAINTENANCE,
        CLEANING,
        INVENTORY,
        QUALITY,
        CUSTOM
    }
}

// MARK: - Checklist Item Template Model
@Serializable
data class ChecklistItemTemplate(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String? = null,
    val itemType: ItemType,
    val isRequired: Boolean = false,
    val options: List<String>? = null, // Options for multiple choice
    val order: Int
) {
    enum class ItemType {
        CHECKBOX,
        YES_NO,
        TEXT,
        NUMBER,
        MULTIPLE_CHOICE,
        PHOTO,
        SIGNATURE,
        RATING,
        DATE
    }
}

// MARK: - Completed Checklist Model
@Serializable
data class CompletedChecklist(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val templateId: String,
    val templateName: String,
    val employeeId: String,
    val employeeName: String,
    val siteId: String? = null,
    val siteName: String? = null,
    val responses: List<ChecklistResponse>,
    val signatureImageURL: String? = null,
    val photoURLs: List<String> = emptyList(),
    val notes: String? = null,
    val status: SubmissionStatus,
    val startedAt: Long, // Timestamp
    val completedAt: Long? = null, // Timestamp
    val submittedAt: Long? = null, // Timestamp
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null, // Timestamp
    val reviewNotes: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    enum class SubmissionStatus {
        IN_PROGRESS,
        COMPLETED,
        SUBMITTED,
        APPROVED,
        REJECTED,
        NEEDS_REVISION
    }
}

// MARK: - Checklist Response Model
@Serializable
data class ChecklistResponse(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val itemTitle: String,
    val itemType: ChecklistItemTemplate.ItemType,
    val checkboxValue: Boolean? = null,
    val yesNoValue: YesNoNA? = null,
    val textValue: String? = null,
    val numberValue: Double? = null,
    val selectedOption: String? = null,
    val photoURL: String? = null,
    val signatureURL: String? = null,
    val ratingValue: Int? = null, // 1-5
    val dateValue: Long? = null, // Timestamp
    val isCompleted: Boolean = false,
    val notes: String? = null
) {
    enum class YesNoNA {
        YES,
        NO,
        NA
    }
}

// MARK: - Equipment Model
@Serializable
data class Equipment(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val name: String,
    val description: String? = null,
    val category: EquipmentCategory,
    val serialNumber: String? = null,
    val barcode: String? = null,
    val purchaseDate: Long? = null, // Timestamp
    val purchasePrice: Double? = null,
    val currentValue: Double? = null,
    val status: EquipmentStatus,
    val condition: EquipmentCondition,
    val assignedTo: String? = null,
    val assignedToSite: String? = null,
    val lastMaintenanceDate: Long? = null, // Timestamp
    val nextMaintenanceDate: Long? = null, // Timestamp
    val warrantyExpiry: Long? = null, // Timestamp
    val photoURL: String? = null,
    val notes: String? = null,
    val createdAt: Long, // Timestamp
    val updatedAt: Long // Timestamp
) {
    enum class EquipmentCategory {
        TOOLS,
        MACHINERY,
        VEHICLES,
        ELECTRONICS,
        SAFETY,
        CLEANING,
        LANDSCAPING,
        OTHER
    }

    enum class EquipmentStatus {
        AVAILABLE,
        IN_USE,
        MAINTENANCE,
        REPAIR,
        RETIRED,
        LOST
    }

    enum class EquipmentCondition {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        NEEDS_REPAIR
    }
}

// MARK: - Equipment Log Model
@Serializable
data class EquipmentLog(
    val id: String = UUID.randomUUID().toString(),
    val equipmentId: String,
    val equipmentName: String,
    val action: LogAction,
    val employeeId: String,
    val employeeName: String,
    val siteId: String? = null,
    val siteName: String? = null,
    val notes: String? = null,
    val timestamp: Long // Timestamp
) {
    enum class LogAction {
        CHECKED_OUT,
        CHECKED_IN,
        MAINTENANCE,
        REPAIR,
        REPAIRED,
        RETIRED,
        LOST,
        FOUND,
        TRANSFERRED,
        INSPECTED
    }
}

// MARK: - Inventory Item Model
@Serializable
data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val name: String,
    val description: String? = null,
    val category: InventoryCategory,
    val sku: String? = null,
    val barcode: String? = null,
    val unit: String, // Unit of measure (each, box, gallon, etc.)
    val currentStock: Int,
    val minimumStock: Int,
    val reorderQuantity: Int,
    val costPerUnit: Double? = null,
    val supplier: String? = null,
    val storageLocation: String? = null,
    val siteId: String? = null, // Site-specific stock
    val photoURL: String? = null,
    val isActive: Boolean = true,
    val lastRestocked: Long? = null, // Timestamp
    val createdAt: Long // Timestamp
) {
    enum class InventoryCategory {
        CLEANING,
        SAFETY,
        OFFICE,
        MAINTENANCE,
        LANDSCAPING,
        ELECTRICAL,
        PLUMBING,
        HARDWARE,
        OTHER
    }

    val isLowStock: Boolean
        get() = currentStock <= minimumStock

    val stockStatus: StockStatus
        get() = when {
            currentStock == 0 -> StockStatus.OUT_OF_STOCK
            currentStock <= minimumStock -> StockStatus.LOW
            else -> StockStatus.IN_STOCK
        }

    enum class StockStatus {
        IN_STOCK,
        LOW,
        OUT_OF_STOCK
    }
}

// MARK: - Inventory Transaction Model
@Serializable
data class InventoryTransaction(
    val id: String = UUID.randomUUID().toString(),
    val itemId: String,
    val itemName: String,
    val transactionType: TransactionType,
    val quantity: Int, // Amount changed (+ or -)
    val previousStock: Int,
    val newStock: Int,
    val employeeId: String,
    val employeeName: String,
    val siteId: String? = null,
    val siteName: String? = null,
    val notes: String? = null,
    val timestamp: Long // Timestamp
) {
    enum class TransactionType {
        STOCK_IN,
        STOCK_OUT,
        ADJUSTMENT,
        TRANSFER,
        DAMAGED,
        RETURNED
    }
}

// MARK: - Client Model
@Serializable
data class Client(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val company: String? = null, // Client's company name
    val siteIds: List<String>, // Sites they can access
    val isActive: Boolean = true,
    val accessCode: String, // PIN for portal access
    val canViewPhotos: Boolean = true,
    val canViewReports: Boolean = true,
    val canSubmitRequests: Boolean = true,
    val canViewSchedule: Boolean = false,
    val lastLogin: Long? = null, // Timestamp
    val createdAt: Long, // Timestamp
    val createdBy: String
)

// MARK: - Work Request Model
@Serializable
data class WorkRequest(
    val id: String = UUID.randomUUID().toString(),
    val companyId: String,
    val clientId: String,
    val clientName: String,
    val siteId: String,
    val siteName: String,
    val title: String,
    val description: String,
    val priority: RequestPriority,
    val category: RequestCategory,
    val status: RequestStatus,
    val photoURLs: List<String> = emptyList(),
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val estimatedCompletion: Long? = null, // Timestamp
    val completedAt: Long? = null, // Timestamp
    val completionNotes: String? = null,
    val completionPhotoURLs: List<String> = emptyList(),
    val clientRating: Int? = null, // 1-5 satisfaction rating
    val createdAt: Long, // Timestamp
    val updatedAt: Long // Timestamp
) {
    enum class RequestPriority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    enum class RequestCategory {
        MAINTENANCE,
        REPAIR,
        CLEANING,
        INSPECTION,
        EMERGENCY,
        OTHER
    }

    enum class RequestStatus {
        SUBMITTED,
        ASSIGNED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        ON_HOLD
    }
}


