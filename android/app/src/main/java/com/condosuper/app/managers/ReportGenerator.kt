package com.condosuper.app.managers

import com.condosuper.app.data.models.*
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates PDF reports for photos and payroll
 * Similar to iOS ReportGenerator using UIGraphicsPDFRenderer
 * Android implementation uses iText7 library
 */
object ReportGenerator {

    /**
     * Creates a PDF report with photos, comments, and metadata
     * @param photos List of photos to include
     * @param employees List of employees for name lookup
     * @param sites List of job sites for name lookup
     * @param title Report title
     * @param dateRange Date range string
     * @param companyName Company name
     * @param outputFile Output file path
     * @return File path to generated PDF
     */
    suspend fun generatePhotoReport(
        photos: List<JobPhoto>,
        employees: List<Employee>,
        sites: List<JobSite>,
        title: String,
        dateRange: String,
        companyName: String,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val writer = PdfWriter(FileOutputStream(outputFile))
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument, PageSize.LETTER)
        
        try {
            // Professional Header Section
            val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(100f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginBottom(20f)
            
            // Header background (light gray)
            val headerCell = Cell()
                .setBackgroundColor(DeviceRgb(245, 245, 245))
                .setPadding(15f)
                .add(Paragraph(companyName.uppercase())
                    .setFontSize(16f)
                    .setBold()
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(5f))
                .add(Paragraph(title)
                    .setFontSize(22f)
                    .setBold()
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFontColor(DeviceRgb(0, 119, 182)))
            
            headerTable.addCell(headerCell)
            document.add(headerTable)
            
            // Summary Section
            val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginBottom(30f)
                .setBackgroundColor(DeviceRgb(250, 250, 250))
                .setBorder(com.itextpdf.layout.borders.SolidBorder(DeviceRgb(220, 220, 220), 1f))
                .setPadding(15f)
            
            val dateFormatter = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
            
            summaryTable.addCell(Cell().add(Paragraph("REPORT PERIOD")
                .setFontSize(10f)
                .setBold()
                .setFontColor(DeviceRgb(100, 100, 100))
                .setMarginBottom(5f))
                .add(Paragraph(dateRange)
                    .setFontSize(12f)))
            
            summaryTable.addCell(Cell().add(Paragraph("TOTAL PHOTOS")
                .setFontSize(10f)
                .setBold()
                .setFontColor(DeviceRgb(100, 100, 100))
                .setMarginBottom(5f))
                .add(Paragraph(photos.size.toString())
                    .setFontSize(12f)))
            
            summaryTable.addCell(Cell().add(Paragraph("GENERATED")
                .setFontSize(10f)
                .setBold()
                .setFontColor(DeviceRgb(100, 100, 100))
                .setMarginBottom(5f))
                .add(Paragraph(dateFormatter.format(Date()))
                    .setFontSize(12f)))
            
            document.add(summaryTable)
            
            // Photos Grid Layout (4 columns)
            val photosPerRow = 4
            val photoSpacing = 10f
            val columnWidth = (PageSize.LETTER.width - 100f - (photoSpacing * (photosPerRow - 1))) / photosPerRow
            
            var currentColumn = 0
            var currentRow = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f)))
                .setWidth(UnitValue.createPercentValue(100f))
                .setMarginBottom(photoSpacing)
            
            for ((index, photo) in photos.withIndex()) {
                // Check if we need a new page
                if (index > 0 && index % 16 == 0) { // 16 photos per page (4x4)
                    document.add(currentRow)
                    document.add(AreaBreak(AreaBreakType.NEXT_PAGE))
                    currentRow = Table(UnitValue.createPercentArray(floatArrayOf(25f, 25f, 25f, 25f)))
                        .setWidth(UnitValue.createPercentValue(100f))
                        .setMarginBottom(photoSpacing)
                    currentColumn = 0
                }
                
                // Photo Card
                val cardCell = Cell()
                    .setPadding(6f)
                    .setBorder(com.itextpdf.layout.borders.SolidBorder(DeviceRgb(230, 230, 230), 1f))
                    .setBackgroundColor(ColorConstants.WHITE)
                
                // Photo number badge
                val badge = Paragraph("#${index + 1}")
                    .setFontSize(7f)
                    .setBold()
                    .setBackgroundColor(DeviceRgb(0, 119, 182))
                    .setFontColor(ColorConstants.WHITE)
                    .setPadding(4f)
                    .setMarginBottom(5f)
                    .setTextAlignment(TextAlignment.LEFT)
                
                cardCell.add(badge)
                
                // Photo Image
                try {
                    if (!photo.isVideo && photo.imageURL != null) {
                        val imageUrl = URL(photo.imageURL)
                        val imageData = ImageDataFactory.create(imageUrl)
                        val image = Image(imageData)
                            .setWidth(columnWidth - 20f)
                            .setAutoScale(true)
                        cardCell.add(image)
                    } else {
                        // Placeholder
                        cardCell.add(Paragraph("📷")
                            .setFontSize(20f)
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(20f))
                    }
                } catch (e: Exception) {
                    // Placeholder on error
                    cardCell.add(Paragraph("📷")
                        .setFontSize(20f)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setPadding(20f))
                }
                
                // Photo Metadata
                val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                cardCell.add(Paragraph(dateFormat.format(Date(photo.date)))
                    .setFontSize(6.5f)
                    .setFontColor(DeviceRgb(128, 128, 128))
                    .setMarginTop(5f))
                
                if (photo.tags.isNotEmpty()) {
                    cardCell.add(Paragraph("🏷 ${photo.tags.first()}")
                        .setFontSize(6.5f)
                        .setFontColor(DeviceRgb(0, 119, 182))
                        .setMarginTop(2f))
                }
                
                currentRow.addCell(cardCell)
                currentColumn++
                
                if (currentColumn >= photosPerRow) {
                    currentColumn = 0
                }
            }
            
            // Add remaining cells if row is incomplete
            while (currentColumn < photosPerRow && currentColumn > 0) {
                currentRow.addCell(Cell().setBorder(com.itextpdf.layout.borders.SolidBorder(ColorConstants.WHITE, 0f)))
                currentColumn++
            }
            
            document.add(currentRow)
            
            // Footer
            val footer = Paragraph("Condo Super • Photo Report • ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}")
                .setFontSize(8f)
                .setFontColor(DeviceRgb(180, 180, 180))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20f)
            
            document.add(footer)
            
            document.close()
            
            android.util.Log.d("ReportGenerator", "✅ Professional photo report generated: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            android.util.Log.e("ReportGenerator", "❌ Error generating photo report: ${e.message}", e)
            document.close()
            throw e
        }
    }

    /**
     * Creates a PDF payroll report with hours worked and break details
     * @param timeEntries List of time entries
     * @param employees List of employees for name lookup
     * @param sites List of job sites for name lookup
     * @param title Report title
     * @param dateRange Date range string
     * @param outputFile Output file path
     * @return File path to generated PDF
     */
    suspend fun generatePayrollReport(
        timeEntries: List<TimeEntry>,
        employees: List<Employee>,
        sites: List<JobSite>,
        title: String,
        dateRange: String,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val writer = PdfWriter(FileOutputStream(outputFile))
        val pdfDocument = PdfDocument(writer)
        val document = Document(pdfDocument, PageSize.LETTER)
        
        try {
            var yPosition = 50f
            
            // Report Header
            document.add(Paragraph(title)
                .setFontSize(24f)
                .setBold()
                .setMarginBottom(10f))
            
            document.add(Paragraph(dateRange)
                .setFontSize(14f)
                .setFontColor(DeviceRgb(128, 128, 128))
                .setMarginBottom(20f))
            
            // Divider
            val divider = LineSeparator(com.itextpdf.layout.borders.SolidBorder(DeviceRgb(200, 200, 200), 1f))
            document.add(divider)
            document.add(Paragraph().setMarginBottom(20f))
            
            // Group entries by employee
            val groupedByEmployee = timeEntries.groupBy { it.employeeId }
            
            // Employee Summary Loop
            for ((employeeId, entries) in groupedByEmployee.toList().sortedBy { (id, _) ->
                employees.firstOrNull { it.id == id }?.name ?: ""
            }) {
                // Check if new page needed
                document.add(AreaBreak(AreaBreakType.NEXT_PAGE))
                
                // Employee name header
                val employee = employees.firstOrNull { it.id == employeeId }
                if (employee != null) {
                    document.add(Paragraph(employee.name)
                        .setFontSize(18f)
                        .setBold()
                        .setMarginBottom(15f))
                }
                
                // Calculate totals for this employee
                var totalHours: Long = 0
                var totalBreakTime: Long = 0
                
                for (entry in entries) {
                    if (entry.clockOutTime != null) {
                        val shiftDuration = entry.clockOutTime - entry.clockInTime
                        totalHours += shiftDuration
                        
                        // Calculate break time
                        val breakDuration = entry.breaks.sumOf { breakEntry ->
                            if (breakEntry.endTime != null) {
                                breakEntry.endTime - breakEntry.startTime
                            } else {
                                0L
                            }
                        }
                        totalBreakTime += breakDuration
                    }
                }
                
                val totalWorkTime = totalHours - totalBreakTime
                
                // Display totals
                document.add(Paragraph("Total Hours: ${formatTimeInterval(totalHours)}")
                    .setFontSize(12f)
                    .setBold()
                    .setMarginBottom(10f))
                
                document.add(Paragraph("Break Time: ${formatTimeInterval(totalBreakTime)}")
                    .setFontSize(12f)
                    .setFontColor(DeviceRgb(80, 80, 80))
                    .setMarginBottom(10f))
                
                document.add(Paragraph("Work Time: ${formatTimeInterval(totalWorkTime)}")
                    .setFontSize(12f)
                    .setBold()
                    .setFontColor(DeviceRgb(0, 119, 182))
                    .setMarginBottom(15f))
                
                // Detail entries for this employee
                document.add(Paragraph("Shift Details:")
                    .setFontSize(12f)
                    .setBold()
                    .setMarginBottom(10f))
                
                val dateFormatter = SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.getDefault())
                
                // Loop through each shift
                for (entry in entries.sortedByDescending { it.clockInTime }) {
                    val site = sites.firstOrNull { it.id == entry.siteId }
                    if (site != null) {
                        document.add(Paragraph("• ${site.name}")
                            .setFontSize(12f)
                            .setMarginBottom(5f)
                            .setMarginLeft(20f))
                    }
                    
                    document.add(Paragraph("  In: ${dateFormatter.format(Date(entry.clockInTime))}")
                        .setFontSize(11f)
                        .setFontColor(DeviceRgb(80, 80, 80))
                        .setMarginBottom(5f)
                        .setMarginLeft(20f))
                    
                    if (entry.clockOutTime != null) {
                        document.add(Paragraph("  Out: ${dateFormatter.format(Date(entry.clockOutTime))}")
                            .setFontSize(11f)
                            .setFontColor(DeviceRgb(80, 80, 80))
                            .setMarginBottom(5f)
                            .setMarginLeft(20f))
                        
                        val duration = entry.clockOutTime - entry.clockInTime
                        document.add(Paragraph("  Duration: ${formatTimeInterval(duration)}")
                            .setFontSize(11f)
                            .setFontColor(DeviceRgb(0, 150, 0))
                            .setMarginBottom(5f)
                            .setMarginLeft(20f))
                    } else {
                        document.add(Paragraph("  Out: (Still working)")
                            .setFontSize(11f)
                            .setFontColor(DeviceRgb(255, 140, 0))
                            .setMarginBottom(5f)
                            .setMarginLeft(20f))
                    }
                    
                    // Breaks detail
                    if (entry.breaks.isNotEmpty()) {
                        document.add(Paragraph("  Breaks:")
                            .setFontSize(11f)
                            .setFontColor(DeviceRgb(80, 80, 80))
                            .setMarginBottom(5f)
                            .setMarginLeft(20f))
                        
                        entry.breaks.forEachIndexed { index, breakEntry ->
                            if (breakEntry.endTime != null) {
                                val breakDuration = breakEntry.endTime - breakEntry.startTime
                                document.add(Paragraph("    Break ${index + 1}: ${formatTimeInterval(breakDuration)}")
                                    .setFontSize(10f)
                                    .setFontColor(DeviceRgb(150, 150, 150))
                                    .setMarginBottom(3f)
                                    .setMarginLeft(20f))
                            }
                        }
                    }
                    
                    document.add(Paragraph().setMarginBottom(10f))
                }
                
                // Divider between employees
                document.add(LineSeparator(com.itextpdf.layout.borders.SolidBorder(DeviceRgb(200, 200, 200), 0.5f)))
                document.add(Paragraph().setMarginBottom(20f))
            }
            
            // Report Footer
            document.add(Paragraph("Generated by Condo Super on ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())}")
                .setFontSize(10f)
                .setFontColor(DeviceRgb(128, 128, 128))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20f))
            
            document.close()
            
            android.util.Log.d("ReportGenerator", "✅ Payroll report generated: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            android.util.Log.e("ReportGenerator", "❌ Error generating payroll report: ${e.message}", e)
            document.close()
            throw e
        }
    }

    /**
     * Formats TimeInterval (milliseconds) to readable string
     * Example: 3665000 milliseconds → "1h 1m"
     */
    private fun formatTimeInterval(intervalMs: Long): String {
        val totalSeconds = intervalMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }
}

