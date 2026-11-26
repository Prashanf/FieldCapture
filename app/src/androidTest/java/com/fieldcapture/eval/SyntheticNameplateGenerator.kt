package com.fieldcapture.eval

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream

/**
 * Generates synthetic nameplate images for pipeline testing.
 * These simulate what a camera would capture when photographing equipment labels.
 */
object SyntheticNameplateGenerator {

    data class NameplateData(
        val id: String,
        val manufacturer: String,
        val modelNumber: String,
        val serialNumber: String,
        val equipmentType: String,
        val capacity: String?,
        val manufactureDate: String?
    )

    val TEST_NAMEPLATES = listOf(
        NameplateData(
            id = "SYN-NP-001",
            manufacturer = "RHEEM",
            modelNumber = "XG40T06EC36U1",
            serialNumber = "MP0921234567",
            equipmentType = "WATER HEATER",
            capacity = "40 GAL",
            manufactureDate = "MFG: 09/2021"
        ),
        NameplateData(
            id = "SYN-NP-002",
            manufacturer = "CARRIER",
            modelNumber = "24ACC636A003",
            serialNumber = "3219E45678",
            equipmentType = "AIR CONDITIONER",
            capacity = "3 TON",
            manufactureDate = "MFG: 2019"
        ),
        NameplateData(
            id = "SYN-NP-003",
            manufacturer = "GOODMAN",
            modelNumber = "GMVM970803BNA",
            serialNumber = "2007388842",
            equipmentType = "GAS FURNACE",
            capacity = "80000 BTU",
            manufactureDate = "MFG: 2020"
        )
    )

    /**
     * Generate a synthetic nameplate bitmap.
     * Creates a metallic gray plate with black text resembling an equipment data plate.
     */
    fun generateNameplateBitmap(data: NameplateData): Bitmap {
        val width = 800
        val height = 500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(Color.rgb(200, 200, 205))

        val borderPaint = Paint().apply {
            color = Color.rgb(80, 80, 80)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(10f, 10f, (width - 10).toFloat(), (height - 10).toFloat(), borderPaint)

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = Color.rgb(60, 60, 60)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val valuePaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }

        var y = 60f

        val titleWidth = titlePaint.measureText(data.manufacturer)
        canvas.drawText(data.manufacturer, (width - titleWidth) / 2, y, titlePaint)
        y += 50f

        val typeWidth = labelPaint.measureText(data.equipmentType)
        canvas.drawText(data.equipmentType, (width - typeWidth) / 2, y, labelPaint)
        y += 50f

        canvas.drawLine(30f, y, (width - 30).toFloat(), y, borderPaint)
        y += 35f

        canvas.drawText("MODEL NO.", 30f, y, labelPaint)
        y += 32f
        canvas.drawText(data.modelNumber, 30f, y, valuePaint)
        y += 45f

        canvas.drawText("SERIAL NO.", 30f, y, labelPaint)
        y += 32f
        canvas.drawText(data.serialNumber, 30f, y, valuePaint)
        y += 45f

        data.capacity?.let {
            canvas.drawText("CAPACITY", 30f, y, labelPaint)
            canvas.drawText(it, 300f, y, valuePaint)
            y += 40f
        }

        data.manufactureDate?.let {
            canvas.drawText(it, 30f, y, labelPaint)
        }

        return bitmap
    }

    /**
     * Convert bitmap to JPEG byte array (same format the pipeline uses).
     */
    fun bitmapToJpegBytes(bitmap: Bitmap, quality: Int = 90): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }
}
