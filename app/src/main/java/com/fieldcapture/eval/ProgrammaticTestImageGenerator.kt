package com.fieldcapture.eval

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.fieldcapture.domain.model.CaptureType
import java.io.ByteArrayOutputStream

/**
 * Generates synthetic JPEG/Bitmap test frames using Android [Canvas] and [Paint].
 *
 * Used for automated unit and instrumented pipeline verification when real field
 * photographs are not bundled in the repository.
 */
object ProgrammaticTestImageGenerator {

    private const val DEFAULT_WIDTH = 640
    private const val DEFAULT_HEIGHT = 480

    /**
     * Render a synthetic equipment nameplate frame as JPEG bytes.
     */
    fun generateNameplateJpeg(
        manufacturer: String = "Rheem",
        equipmentType: String = "WATER HEATER",
        modelNumber: String = "XG40T06EC36U1",
        serialNumber: String = "MP1234567890",
        capacity: String = "40 GAL",
        dimLighting: Boolean = false
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = if (dimLighting) Color.rgb(42, 42, 46) else Color.rgb(225, 228, 232)
        val plateColor = if (dimLighting) Color.rgb(68, 70, 74) else Color.rgb(248, 249, 250)
        val textColor = if (dimLighting) Color.rgb(190, 192, 196) else Color.rgb(20, 24, 30)
        val accentColor = if (dimLighting) Color.rgb(80, 95, 115) else Color.rgb(22, 78, 145)

        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = accentColor
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 24f
        }

        // Outer background
        fillPaint.color = bgColor
        canvas.drawRect(Rect(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT), fillPaint)

        // Nameplate metal plate
        fillPaint.color = plateColor
        val plateRect = Rect(48, 56, DEFAULT_WIDTH - 48, DEFAULT_HEIGHT - 56)
        canvas.drawRect(plateRect, fillPaint)
        canvas.drawRect(plateRect, strokePaint)

        // Header banner
        fillPaint.color = accentColor
        canvas.drawRect(Rect(48, 56, DEFAULT_WIDTH - 48, 124), fillPaint)
        canvas.drawText("$manufacturer — $equipmentType", 72f, 100f, titlePaint)

        // Spec rows
        canvas.drawText("MOD: $modelNumber", 72f, 185f, bodyPaint)
        canvas.drawText("SER: $serialNumber", 72f, 240f, bodyPaint)
        canvas.drawText("CAP: $capacity", 72f, 295f, bodyPaint)
        canvas.drawText("VOLTS: 240V AC   60Hz", 72f, 350f, bodyPaint)

        return bitmapToJpeg(bitmap)
    }

    /**
     * Render a synthetic property damage frame (wall/floor panel with staining patch).
     */
    fun generateDamageJpeg(
        room: String = "Kitchen",
        surface: String = "North Wall",
        damageLabel: String = "WATER STAIN",
        severity: String = "medium",
        isMold: Boolean = false
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val wallPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(236, 232, 224)
        }
        val stainPaint = Paint().apply {
            style = Paint.Style.FILL
            color = if (isMold) Color.rgb(48, 54, 42) else Color.rgb(156, 114, 72)
        }
        val baseboardPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(205, 198, 188)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30)
            textSize = 24f
            isFakeBoldText = true
        }

        // Drywall field
        canvas.drawRect(Rect(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT), wallPaint)

        // Affected damage area near lower wall/baseboard
        val stainTop = when (severity.lowercase()) {
            "critical", "high" -> 150
            "medium" -> 230
            else -> 310
        }
        canvas.drawRect(Rect(90, stainTop, DEFAULT_WIDTH - 90, 410), stainPaint)

        // Baseboard strip
        canvas.drawRect(Rect(0, 410, DEFAULT_WIDTH, DEFAULT_HEIGHT), baseboardPaint)

        // Context annotation bar
        canvas.drawText("$room ($surface) — $damageLabel [$severity]", 36f, 52f, labelPaint)

        return bitmapToJpeg(bitmap)
    }

    /**
     * Render a synthetic context photo accompanying a technician voice note.
     */
    fun generateNoteContextJpeg(
        category: String = "observation",
        caption: String = "Supply Line Inspection"
    ): ByteArray {
        val bitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(212, 220, 228)
        }
        val boxPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.rgb(64, 110, 142)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            isFakeBoldText = true
        }

        canvas.drawRect(Rect(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT), bgPaint)
        canvas.drawRect(Rect(60, 140, DEFAULT_WIDTH - 60, 340), boxPaint)
        canvas.drawText("NOTE [$category]", 90f, 215f, textPaint)
        canvas.drawText(caption, 90f, 270f, textPaint)

        return bitmapToJpeg(bitmap)
    }

    /**
     * Generate a small representative set of programmatic frames keyed by ground truth filename
     * for automated end-to-end evaluation pipeline tests.
     */
    fun generateSamplePipelineSet(): Map<String, ByteArray> = mapOf(
        "nameplate_rheem_water_heater.jpg" to generateNameplateJpeg(
            manufacturer = "Rheem",
            equipmentType = "Water Heater",
            modelNumber = "XG40T06EC36U1",
            serialNumber = "MP1234567890",
            capacity = "40 GAL"
        ),
        "damage_water_drywall_kitchen.jpg" to generateDamageJpeg(
            room = "kitchen",
            surface = "north wall",
            damageLabel = "water_damage",
            severity = "medium"
        ),
        "note_supply_line_connection.jpg" to generateNoteContextJpeg(
            category = "homeowner_statement",
            caption = "Corroded supply line near dishwasher"
        )
    )

    /**
     * Dispatch image generation by [CaptureType].
     */
    fun generateForType(captureType: CaptureType, label: String): ByteArray = when (captureType) {
        CaptureType.NAMEPLATE -> generateNameplateJpeg(manufacturer = label)
        CaptureType.DAMAGE -> generateDamageJpeg(damageLabel = label)
        CaptureType.NOTE -> generateNoteContextJpeg(caption = label)
    }

    private fun bitmapToJpeg(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }
}
