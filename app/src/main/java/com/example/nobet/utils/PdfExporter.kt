package com.example.nobet.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.nobet.ui.calendar.MonthlyStatistics
import com.example.nobet.ui.calendar.ShiftType
import com.example.nobet.ui.calendar.YearlyStatistics
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

class PdfExporter(private val context: Context) {
    
    companion object {
        private const val FONT_SIZE_TITLE = 16f
        private const val FONT_SIZE_HEADER = 14f
        private const val FONT_SIZE_NORMAL = 12f
        private const val FONT_SIZE_SMALL = 10f
    }
    
    private val locale = Locale.forLanguageTag("tr-TR")
    
    fun exportMonthlyStatistics(
        monthlyStats: MonthlyStatistics,
        holidays: List<TurkishHolidays.Holiday>,
        arifeDayAdjustments: List<Pair<java.time.LocalDate, Int>>,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val fileName = "Nobet_Istatistik_${monthlyStats.month.year}_${monthlyStats.month.monthValue.toString().padStart(2, '0')}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val pdfWriter = PdfWriter(FileOutputStream(file))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Title
            val monthName = monthlyStats.month.month.getDisplayName(TextStyle.FULL, locale)
            val title = "${monthName.replaceFirstChar { it.titlecase(locale) }} ${monthlyStats.month.year} Nöbet İstatistikleri"
            document.add(
                Paragraph(title)
                    .setFontSize(FONT_SIZE_TITLE)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
            )
            
            // Summary Statistics
            addSummarySection(document, monthlyStats)
            
            // Shift Types and Counts
            addShiftTypesSection(document, monthlyStats.shiftCounts)
            
            // Working Day Distribution
            addWorkingDayDistributionSection(document, monthlyStats.workingDayDistribution)
            
            // Holidays
            if (holidays.isNotEmpty()) {
                addHolidaysSection(document, holidays)
            }
            
            // Arife Day Adjustments
            if (arifeDayAdjustments.isNotEmpty()) {
                addArifeDayAdjustmentsSection(document, arifeDayAdjustments)
            }
            
            document.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            onSuccess(uri)
            
        } catch (e: Exception) {
            onError("PDF oluşturulurken hata: ${e.message}")
        }
    }
    
    fun exportYearlyStatistics(
        yearlyStats: YearlyStatistics,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val fileName = "Nobet_Yillik_Istatistik_${yearlyStats.year}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val pdfWriter = PdfWriter(FileOutputStream(file))
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Title
            val title = "${yearlyStats.year} Yılı Nöbet İstatistikleri"
            document.add(
                Paragraph(title)
                    .setFontSize(FONT_SIZE_TITLE)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)
            )
            
            // Yearly Summary
            addYearlySummarySection(document, yearlyStats)
            
            // Yearly Shift Types
            addShiftTypesSection(document, yearlyStats.yearlyShiftCounts)
            
            // Yearly Working Day Distribution
            addWorkingDayDistributionSection(document, yearlyStats.yearlyWorkingDayDistribution)
            
            // Monthly Breakdown
            addMonthlyBreakdownSection(document, yearlyStats.monthlyData)
            
            document.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            onSuccess(uri)
            
        } catch (e: Exception) {
            onError("PDF oluşturulurken hata: ${e.message}")
        }
    }
    
    private fun addSummarySection(document: Document, monthlyStats: MonthlyStatistics) {
        document.add(
            Paragraph("Özet")
                .setFontSize(FONT_SIZE_HEADER)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f)
        )
        
        val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        table.addCell(createCell("Toplam Çalışılan Saat:", false))
        table.addCell(createCell("${monthlyStats.totalHours} saat", false))
        
        table.addCell(createCell("Beklenen Saat:", false))
        table.addCell(createCell("${monthlyStats.overtimeResult.totalExpectedHours} saat", false))
        
        val overtimeHours = monthlyStats.overtimeResult.overtimeHours
        if (overtimeHours > 0) {
            table.addCell(createCell("Fazla Mesai:", false))
            table.addCell(createCell("$overtimeHours saat", false, DeviceRgb(255, 0, 0)))
        }
        
        val missingHours = kotlin.math.max(0, monthlyStats.overtimeResult.totalExpectedHours - monthlyStats.totalHours)
        if (missingHours > 0) {
            table.addCell(createCell("Eksik Saat:", false))
            table.addCell(createCell("$missingHours saat", false, DeviceRgb(255, 165, 0)))
        }
        
        document.add(table)
    }
    
    private fun addYearlySummarySection(document: Document, yearlyStats: YearlyStatistics) {
        document.add(
            Paragraph("Yıllık Özet")
                .setFontSize(FONT_SIZE_HEADER)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f)
        )
        
        val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        table.addCell(createCell("Toplam Çalışılan Saat:", false))
        table.addCell(createCell("${yearlyStats.totalWorkedHours} saat", false))
        
        table.addCell(createCell("Beklenen Saat:", false))
        table.addCell(createCell("${yearlyStats.totalExpectedHours} saat", false))
        
        if (yearlyStats.totalOvertimeHours > 0) {
            table.addCell(createCell("Toplam Fazla Mesai:", false))
            table.addCell(createCell("${yearlyStats.totalOvertimeHours} saat", false, DeviceRgb(255, 0, 0)))
        }
        
        val yearlyMissingHours = kotlin.math.max(0, yearlyStats.totalExpectedHours - yearlyStats.totalWorkedHours)
        if (yearlyMissingHours > 0) {
            table.addCell(createCell("Toplam Eksik Saat:", false))
            table.addCell(createCell("$yearlyMissingHours saat", false, DeviceRgb(255, 165, 0)))
        }
        
        document.add(table)
    }
    
    private fun addShiftTypesSection(document: Document, shiftCounts: Map<ShiftType, Int>) {
        document.add(
            Paragraph("Nöbet Tipleri ve Sayıları")
                .setFontSize(FONT_SIZE_HEADER)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f)
        )
        
        val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        shiftCounts.forEach { (type, count) ->
            table.addCell(createCell(type.label, false))
            table.addCell(createCell("$count adet", false))
        }
        
        document.add(table)
    }
    
    private fun addWorkingDayDistributionSection(document: Document, distribution: Map<DayOfWeek, Int>) {
        document.add(
            Paragraph("Günlere Göre Dağılım")
                .setFontSize(FONT_SIZE_HEADER)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f)
        )
        
        val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        distribution.forEach { (dayOfWeek, count) ->
            val dayName = when (dayOfWeek) {
                DayOfWeek.MONDAY -> "Pazartesi"
                DayOfWeek.TUESDAY -> "Salı"
                DayOfWeek.WEDNESDAY -> "Çarşamba"
                DayOfWeek.THURSDAY -> "Perşembe"
                DayOfWeek.FRIDAY -> "Cuma"
                DayOfWeek.SATURDAY -> "Cumartesi"
                DayOfWeek.SUNDAY -> "Pazar"
            }
            
            table.addCell(createCell(dayName, false))
            table.addCell(createCell("$count nöbet", false))
        }
        
        document.add(table)
    }

    // PdfExporter.kt dosyasındaki eski addHolidaysSection'ı bununla değiştirin.

    private fun addHolidaysSection(document: Document, holidays: List<TurkishHolidays.Holiday>) {
        document.add(
            Paragraph("Bu Aydaki Tatiller")            .setFontSize(FONT_SIZE_HEADER)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f)
        )

        val table = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
            .setWidth(UnitValue.createPercentValue(100f))

        holidays.forEach { holiday ->
            table.addCell(createCell(holiday.name, false))
            table.addCell(createCell("${holiday.date.dayOfMonth}/${holiday.date.monthValue}", false))

            // --- DEĞİŞİKLİK BURADA ---
            // Artık holiday.workingHours yerine, o günün zorunlu mesaisini
            // WorkHourCalculator'dan alıyoruz.
            val expectedWork = WorkHourCalculator.getExpectedWorkHours(holiday.date)

            // Sadece zorunlu mesaisi olan kısmi tatiller (28 Ekim, Arifeler) için bu detayı PDF'e ekle.
            if (expectedWork > 0) {
                // İki hücreli bir tabloya tek sütun eklemek düzeni bozacağı için,
                // bunu ayrı bir paragraf olarak eklemek daha güvenlidir.
                val detailParagraph = Paragraph("   → Zorunlu Mesai: ${expectedWork} saat")
                    .setFontSize(FONT_SIZE_SMALL)
                    .setFontColor(DeviceRgb(100, 100, 100))

                val detailCell = Cell(1, 2).add(detailParagraph) // İki sütunu kaplayan tek bir hücre
                detailCell.setBorder(null) // Hücre kenarlıklarını kaldır
                table.addCell(detailCell)
            }
        }

        document.add(table)
    }


    private fun addArifeDayAdjustmentsSection(document: Document, adjustments: List<Pair<java.time.LocalDate, Int>>) {
        document.add(
            Paragraph("Arife Günü Saat Ayarları")
                .setFontSize(FONT_SIZE_HEADER)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f)
        )
        
        val table = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        adjustments.forEach { (date, effectiveHours) ->
            table.addCell(createCell("${date.dayOfMonth}/${date.monthValue}", false))
            table.addCell(createCell("→ ${effectiveHours}h", false))
        }
        
        document.add(table)
        
        document.add(
            Paragraph("Arife günlerinde nöbetler maksimum 5 saat olarak hesaplanır.")
                .setFontSize(FONT_SIZE_SMALL)
                .setMarginTop(5f)
        )
    }
    
    private fun addMonthlyBreakdownSection(document: Document, monthlyData: List<MonthlyStatistics>) {
        document.add(
            Paragraph("Aylık Detay")
                .setFontSize(FONT_SIZE_HEADER)
                .setBold()
                .setMarginTop(15f)
                .setMarginBottom(10f)
        )
        
        val table = Table(UnitValue.createPercentArray(floatArrayOf(20f, 20f, 20f, 20f, 20f)))
            .setWidth(UnitValue.createPercentValue(100f))
        
        // Header
        table.addHeaderCell(createCell("Ay", true))
        table.addHeaderCell(createCell("Çalışılan", true))
        table.addHeaderCell(createCell("Beklenen", true))
        table.addHeaderCell(createCell("Fazla Mesai", true))
        table.addHeaderCell(createCell("Eksik", true))
        
        monthlyData.forEach { monthly ->
            val monthName = monthly.month.month.getDisplayName(TextStyle.SHORT, locale)
            table.addCell(createCell(monthName, false))
            table.addCell(createCell("${monthly.totalHours}h", false))
            table.addCell(createCell("${monthly.overtimeResult.totalExpectedHours}h", false))
            
            val overtime = monthly.overtimeResult.overtimeHours
            table.addCell(createCell(
                if (overtime > 0) "${overtime}h" else "-", 
                false,
                if (overtime > 0) DeviceRgb(255, 0, 0) else null
            ))
            
            val missing = kotlin.math.max(0, monthly.overtimeResult.totalExpectedHours - monthly.totalHours)
            table.addCell(createCell(
                if (missing > 0) "${missing}h" else "-", 
                false,
                if (missing > 0) DeviceRgb(255, 165, 0) else null
            ))
        }
        
        document.add(table)
    }
    
    private fun createCell(text: String, isHeader: Boolean, color: DeviceRgb? = null): Cell {
        val cell = Cell().add(Paragraph(text).setFontSize(if (isHeader) FONT_SIZE_HEADER else FONT_SIZE_NORMAL))
        
        if (isHeader) {
            cell.setBold()
            cell.setBackgroundColor(DeviceRgb(230, 230, 230))
        }
        
        color?.let { cell.setFontColor(it) }
        
        return cell.setTextAlignment(TextAlignment.CENTER)
    }
    
    fun sharePdf(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "PDF'yi Paylaş")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}