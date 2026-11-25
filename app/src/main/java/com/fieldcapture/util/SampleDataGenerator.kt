package com.fieldcapture.util

import android.content.Context
import com.fieldcapture.domain.model.DamageAssessmentEntry
import com.fieldcapture.domain.model.EquipmentEntry
import com.fieldcapture.domain.model.JobMetadata
import com.fieldcapture.domain.model.JobRecord
import com.fieldcapture.domain.model.JobSummary
import com.fieldcapture.domain.model.LineItemEntry
import com.fieldcapture.domain.model.NoteEntry
import com.fieldcapture.domain.model.QualityScores
import java.io.File

/**
 * Generates 3 sample job records representing realistic restoration scenarios.
 */
class SampleDataGenerator(private val context: Context) {

    private val jsonExporter = JsonExporter(context)

    fun generateAll(): List<File> {
        return buildSampleRecords().map { jsonExporter.exportJobRecord(it) }
    }

    fun generateToDirectory(outputDir: File): List<File> {
        outputDir.mkdirs()
        return buildSampleRecords().mapIndexed { index, record ->
            val file = File(outputDir, "job_record_${index + 1}.json")
            file.writeText(jsonExporter.toJsonString(record))
            file
        }
    }

    fun buildSampleRecords(): List<JobRecord> {
        return listOf(
            generateWaterDamageJob(),
            generateMoldRemediationJob(),
            generateMultiRoomWaterDamageJob()
        )
    }

    private fun generateWaterDamageJob(): JobRecord {
        return JobRecord(
            jobId = "SAMPLE-WD-001",
            createdAt = "2024-11-15T09:00:00.000Z",
            completedAt = "2024-11-15T09:45:00.000Z",
            technicianName = "John Smith",
            jobAddress = "123 Main St, Apt 4B, Springfield, IL 62704",
            equipment = listOf(
                EquipmentEntry(
                    captureId = "cap-001",
                    imagePath = "captures/nameplate_001.jpg",
                    capturedAt = "2024-11-15T09:05:00.000Z",
                    equipmentType = "Water Heater",
                    manufacturer = "Rheem",
                    modelNumber = "XG40T06EC36U1",
                    serialNumber = "MP1234567890",
                    manufactureDate = "2019",
                    capacity = "40 GAL",
                    confidence = 0.87,
                    qualityScores = QualityScores(blurScore = 450.0, brightnessScore = 140, edgeDensity = 12.5)
                )
            ),
            damageAssessments = listOf(
                DamageAssessmentEntry(
                    captureId = "cap-002",
                    imagePath = "captures/damage_001.jpg",
                    capturedAt = "2024-11-15T09:12:00.000Z",
                    damageType = "water_damage",
                    severity = "high",
                    description = "Extensive water damage on kitchen floor. Vinyl flooring is buckled and lifting. Water staining extends approximately 8 feet from the base of the wall near the dishwasher supply line.",
                    affectedMaterials = listOf("vinyl flooring", "baseboard", "drywall"),
                    affectedArea = "approximately 64 sq ft",
                    room = "kitchen",
                    surface = "floor",
                    moistureVisible = true,
                    moldVisible = false,
                    safetyConcerns = null,
                    confidence = 0.85,
                    qualityScores = QualityScores(blurScore = 320.0, brightnessScore = 155, edgeDensity = 8.3)
                ),
                DamageAssessmentEntry(
                    captureId = "cap-003",
                    imagePath = "captures/damage_002.jpg",
                    capturedAt = "2024-11-15T09:20:00.000Z",
                    damageType = "water_damage",
                    severity = "medium",
                    description = "Water staining on bathroom wall adjacent to kitchen. Lower 18 inches of drywall shows moisture wicking. No visible mold yet but conditions are favorable.",
                    affectedMaterials = listOf("drywall", "baseboard"),
                    affectedArea = "approximately 24 sq ft",
                    room = "bathroom",
                    surface = "west wall",
                    moistureVisible = true,
                    moldVisible = false,
                    safetyConcerns = "Electrical outlet within 2 feet of moisture — recommend de-energizing circuit",
                    confidence = 0.80,
                    qualityScores = QualityScores(blurScore = 280.0, brightnessScore = 95, edgeDensity = 6.1)
                )
            ),
            notes = listOf(
                NoteEntry(
                    captureId = "cap-004",
                    imagePath = "captures/note_001.jpg",
                    capturedAt = "2024-11-15T09:30:00.000Z",
                    transcribedText = "Homeowner reports the leak started approximately 48 hours ago when they noticed water pooling near the dishwasher. They turned off the water supply valve yesterday morning.",
                    category = "homeowner_statement",
                    imageContext = "Photo shows the dishwasher supply line connection point with visible corrosion on the fitting",
                    keyFacts = listOf(
                        "Leak started ~48 hours ago",
                        "Water noticed near dishwasher",
                        "Supply valve shut off yesterday morning",
                        "Corrosion visible on supply line fitting"
                    ),
                    confidence = 0.88
                )
            ),
            lineItems = listOf(
                LineItemEntry(1, "WATER_EXTRACTION", "Water Extraction", "Extract standing water using truck-mounted or portable extraction equipment", "Emergency Services", "sq ft", 3.50, 64.0, 224.00, "Standing water on kitchen floor from burst supply line", "high", "cap-002", 0.85, true),
                LineItemEntry(2, "REMOVE_REPLACE_FLOORING", "Remove & Replace Flooring", "Remove damaged floor covering, prepare subfloor, install new flooring", "Repair & Replacement", "sq ft", 8.50, 64.0, 544.00, "Vinyl flooring buckled and lifting across kitchen", "high", "cap-002", 0.85, true),
                LineItemEntry(3, "REMOVE_REPLACE_DRYWALL", "Remove & Replace Drywall", "Remove damaged drywall, dispose, install new drywall with tape and finish", "Repair & Replacement", "sq ft", 4.75, 24.0, 114.00, "Lower 18 inches of bathroom drywall shows moisture wicking", "medium", "cap-003", 0.80, true),
                LineItemEntry(4, "REMOVE_REPLACE_BASEBOARD", "Remove & Replace Baseboard", "Remove damaged baseboard trim, install and paint new baseboard", "Repair & Replacement", "linear ft", 6.00, 20.0, 120.00, "Baseboard warped in kitchen and bathroom", "medium", "cap-002", 0.85, true),
                LineItemEntry(5, "STRUCTURAL_DRY_OUT", "Structural Dry-Out", "Monitor and manage drying of structural materials to target moisture levels", "Drying & Monitoring", "day", 75.00, 3.0, 225.00, "Materials still damp, need monitored drying", "high", "cap-002", 0.80, true),
                LineItemEntry(6, "PLUMBING_REPAIR", "Plumbing Repair", "Repair or replace damaged plumbing fixtures, pipes, or fittings", "Repair & Replacement", "each", 350.00, 1.0, 350.00, "Corroded dishwasher supply line fitting needs replacement", "high", "cap-004", 0.88, true)
            ),
            summary = JobSummary(
                totalCaptures = 4,
                nameplateCaptures = 1,
                damageCaptures = 2,
                noteCaptures = 1,
                rejectedCaptures = 1,
                totalLineItems = 6,
                estimateSubtotal = 1577.00,
                averageExtractionConfidence = 0.85,
                unknownLineItemCodes = emptyList(),
                durationSeconds = 2700
            ),
            metadata = JobMetadata()
        )
    }

    private fun generateMoldRemediationJob(): JobRecord {
        return JobRecord(
            jobId = "SAMPLE-MR-002",
            createdAt = "2024-11-16T10:00:00.000Z",
            completedAt = "2024-11-16T10:50:00.000Z",
            technicianName = "Sarah Johnson",
            jobAddress = "456 Oak Avenue, Basement, Springfield, IL 62704",
            equipment = listOf(
                EquipmentEntry(
                    captureId = "cap-010",
                    imagePath = "captures/nameplate_010.jpg",
                    capturedAt = "2024-11-16T10:05:00.000Z",
                    equipmentType = "Dehumidifier",
                    manufacturer = "Dri-Eaz",
                    modelNumber = "F413",
                    serialNumber = "DE2024-00891",
                    manufactureDate = "2024",
                    capacity = "29 GPD",
                    confidence = 0.92,
                    qualityScores = QualityScores(blurScore = 520.0, brightnessScore = 130, edgeDensity = 15.2)
                )
            ),
            damageAssessments = listOf(
                DamageAssessmentEntry(
                    captureId = "cap-011",
                    imagePath = "captures/damage_010.jpg",
                    capturedAt = "2024-11-16T10:15:00.000Z",
                    damageType = "mold",
                    severity = "high",
                    description = "Active mold colony visible on basement north wall behind removed drywall. Black and green growth pattern covering approximately 20 square feet. Insulation behind drywall is saturated and contaminated.",
                    affectedMaterials = listOf("drywall", "insulation", "framing"),
                    affectedArea = "approximately 20 sq ft",
                    room = "basement",
                    surface = "north wall",
                    moistureVisible = true,
                    moldVisible = true,
                    safetyConcerns = "Active mold — recommend respiratory protection for occupants",
                    confidence = 0.90,
                    qualityScores = QualityScores(blurScore = 380.0, brightnessScore = 85, edgeDensity = 11.7)
                )
            ),
            notes = listOf(
                NoteEntry(
                    captureId = "cap-012",
                    imagePath = "captures/note_010.jpg",
                    capturedAt = "2024-11-16T10:25:00.000Z",
                    transcribedText = "Previous water intrusion from foundation crack was repaired 3 months ago. Homeowner reports musty smell persisted after repair. Air quality test recommended.",
                    category = "homeowner_statement",
                    imageContext = "Foundation repair patch visible at base of wall",
                    keyFacts = listOf("Foundation crack repaired 3 months ago", "Musty smell persisted", "Air quality test recommended"),
                    confidence = 0.85
                ),
                NoteEntry(
                    captureId = "cap-013",
                    imagePath = null,
                    capturedAt = "2024-11-16T10:35:00.000Z",
                    transcribedText = "Moisture readings: north wall 45% WME, east wall 18% WME, floor slab near wall 32% WME. Drying target is below 15% WME.",
                    category = "measurement",
                    imageContext = null,
                    keyFacts = listOf("North wall: 45% WME", "East wall: 18% WME", "Floor slab: 32% WME", "Target: below 15% WME"),
                    confidence = 0.92
                )
            ),
            lineItems = listOf(
                LineItemEntry(1, "MOLD_REMEDIATION", "Mold Remediation", "Professional mold removal including containment, HEPA filtration, and treatment", "Remediation", "sq ft", 15.00, 20.0, 300.00, "Active mold colony on basement north wall", "high", "cap-011", 0.90, true),
                LineItemEntry(2, "REMOVE_REPLACE_DRYWALL", "Remove & Replace Drywall", "Remove damaged drywall, dispose, install new drywall with tape and finish", "Repair & Replacement", "sq ft", 4.75, 20.0, 95.00, "Drywall contaminated with mold must be removed", "high", "cap-011", 0.90, true),
                LineItemEntry(3, "REMOVE_REPLACE_INSULATION", "Remove & Replace Insulation", "Remove wet/contaminated insulation, install new batt or blown-in insulation", "Repair & Replacement", "sq ft", 2.50, 20.0, 50.00, "Insulation behind drywall is saturated and contaminated", "high", "cap-011", 0.90, true),
                LineItemEntry(4, "ANTIMICROBIAL_TREATMENT", "Antimicrobial Treatment", "Apply EPA-registered antimicrobial solution to prevent mold/bacterial growth", "Remediation", "sq ft", 2.00, 40.0, 80.00, "Treat exposed framing and surrounding area after mold removal", "medium", "cap-011", 0.85, true),
                LineItemEntry(5, "DEHUMIDIFIER_RENTAL", "Dehumidifier Rental", "Commercial dehumidifier rental for moisture control during drying", "Equipment Rental", "day", 85.00, 5.0, 425.00, "High moisture readings require extended drying", "medium", "cap-011", 0.85, true)
            ),
            summary = JobSummary(
                totalCaptures = 4,
                nameplateCaptures = 1,
                damageCaptures = 1,
                noteCaptures = 2,
                rejectedCaptures = 0,
                totalLineItems = 5,
                estimateSubtotal = 950.00,
                averageExtractionConfidence = 0.90,
                unknownLineItemCodes = emptyList(),
                durationSeconds = 3000
            ),
            metadata = JobMetadata()
        )
    }

    private fun generateMultiRoomWaterDamageJob(): JobRecord {
        return JobRecord(
            jobId = "SAMPLE-WD-003",
            createdAt = "2024-11-17T14:00:00.000Z",
            completedAt = "2024-11-17T15:10:00.000Z",
            technicianName = "Mike Chen",
            jobAddress = "789 Elm Drive, Springfield, IL 62704",
            equipment = listOf(
                EquipmentEntry(
                    captureId = "cap-020",
                    imagePath = "captures/nameplate_020.jpg",
                    capturedAt = "2024-11-17T14:08:00.000Z",
                    equipmentType = "Washing Machine",
                    manufacturer = "Samsung",
                    modelNumber = "WF45R6100AW",
                    serialNumber = "0B4N5CDJ200??1",
                    manufactureDate = "2020",
                    capacity = "4.5 cu ft",
                    confidence = 0.72,
                    qualityScores = QualityScores(blurScore = 180.0, brightnessScore = 110, edgeDensity = 9.4)
                ),
                EquipmentEntry(
                    captureId = "cap-021",
                    imagePath = "captures/nameplate_021.jpg",
                    capturedAt = "2024-11-17T14:15:00.000Z",
                    equipmentType = "Furnace",
                    manufacturer = "Carrier",
                    modelNumber = "59SC5A080S17--14",
                    serialNumber = null,
                    manufactureDate = "2018",
                    capacity = "80000 BTU",
                    confidence = 0.65,
                    qualityScores = QualityScores(blurScore = 140.0, brightnessScore = 68, edgeDensity = 7.1)
                )
            ),
            damageAssessments = listOf(
                DamageAssessmentEntry(
                    captureId = "cap-022",
                    imagePath = "captures/damage_020.jpg",
                    capturedAt = "2024-11-17T14:22:00.000Z",
                    damageType = "water_damage",
                    severity = "critical",
                    description = "Washing machine supply hose burst. Standing water approximately 1 inch deep across laundry room floor. Water has migrated under wall into adjacent hallway.",
                    affectedMaterials = listOf("vinyl flooring", "drywall", "baseboard", "subfloor"),
                    affectedArea = "approximately 80 sq ft",
                    room = "laundry room",
                    surface = "floor",
                    moistureVisible = true,
                    moldVisible = false,
                    safetyConcerns = "Standing water near electrical outlets — circuits should be de-energized",
                    confidence = 0.92,
                    qualityScores = QualityScores(blurScore = 410.0, brightnessScore = 160, edgeDensity = 10.8)
                ),
                DamageAssessmentEntry(
                    captureId = "cap-023",
                    imagePath = "captures/damage_021.jpg",
                    capturedAt = "2024-11-17T14:30:00.000Z",
                    damageType = "water_damage",
                    severity = "medium",
                    description = "Water has migrated from laundry room through hallway. Carpet is saturated along approximately 15 linear feet. Baseboard showing early signs of wicking.",
                    affectedMaterials = listOf("carpet", "carpet pad", "baseboard"),
                    affectedArea = "approximately 45 sq ft",
                    room = "hallway",
                    surface = "floor",
                    moistureVisible = true,
                    moldVisible = false,
                    safetyConcerns = null,
                    confidence = 0.88,
                    qualityScores = QualityScores(blurScore = 350.0, brightnessScore = 125, edgeDensity = 5.9)
                ),
                DamageAssessmentEntry(
                    captureId = "cap-024",
                    imagePath = "captures/damage_022.jpg",
                    capturedAt = "2024-11-17T14:40:00.000Z",
                    damageType = "water_damage",
                    severity = "low",
                    description = "Minor water migration into living room. Carpet damp along wall shared with hallway, approximately 3 feet into the room. No visible damage to walls or baseboard yet.",
                    affectedMaterials = listOf("carpet"),
                    affectedArea = "approximately 12 sq ft",
                    room = "living room",
                    surface = "floor",
                    moistureVisible = true,
                    moldVisible = false,
                    safetyConcerns = null,
                    confidence = 0.78,
                    qualityScores = QualityScores(blurScore = 290.0, brightnessScore = 145, edgeDensity = 4.2)
                )
            ),
            notes = listOf(
                NoteEntry(
                    captureId = "cap-025",
                    imagePath = "captures/note_020.jpg",
                    capturedAt = "2024-11-17T14:50:00.000Z",
                    transcribedText = "Safety concern: water is within 6 inches of a floor-level electrical outlet in the laundry room. Recommended homeowner contact electrician before restoration work begins. Breaker for laundry circuit has been turned off.",
                    category = "safety_concern",
                    imageContext = "Photo shows electrical outlet near floor level with visible water line approaching",
                    keyFacts = listOf(
                        "Water within 6 inches of electrical outlet",
                        "Electrician recommended before restoration",
                        "Laundry circuit breaker turned off"
                    ),
                    confidence = 0.91
                )
            ),
            lineItems = listOf(
                LineItemEntry(1, "WATER_EXTRACTION", "Water Extraction", "Extract standing water using truck-mounted or portable extraction equipment", "Emergency Services", "sq ft", 3.50, 80.0, 280.00, "Standing water across laundry room floor", "high", "cap-022", 0.92, true),
                LineItemEntry(2, "REMOVE_REPLACE_FLOORING", "Remove & Replace Flooring", "Remove damaged floor covering, prepare subfloor, install new flooring", "Repair & Replacement", "sq ft", 8.50, 80.0, 680.00, "Vinyl flooring buckled in laundry room", "high", "cap-022", 0.92, true),
                LineItemEntry(3, "REMOVE_REPLACE_BASEBOARD", "Remove & Replace Baseboard", "Remove damaged baseboard trim, install and paint new baseboard", "Repair & Replacement", "linear ft", 6.00, 30.0, 180.00, "Baseboard damaged in laundry and hallway", "medium", "cap-022", 0.90, true),
                LineItemEntry(4, "REMOVE_REPLACE_FLOORING", "Remove & Replace Flooring", "Remove damaged floor covering, prepare subfloor, install new flooring", "Repair & Replacement", "sq ft", 8.50, 45.0, 382.50, "Carpet saturated in hallway", "medium", "cap-023", 0.88, true),
                LineItemEntry(5, "STRUCTURAL_DRY_OUT", "Structural Dry-Out", "Monitor and manage drying of structural materials to target moisture levels", "Drying & Monitoring", "day", 75.00, 4.0, 300.00, "Multi-room drying needed", "high", "cap-022", 0.90, true),
                LineItemEntry(6, "DEHUMIDIFIER_RENTAL", "Dehumidifier Rental", "Commercial dehumidifier rental for moisture control during drying", "Equipment Rental", "day", 85.00, 4.0, 340.00, "Humidity control for multi-room drying", "high", "cap-022", 0.90, true),
                LineItemEntry(7, "AIR_MOVER_RENTAL", "Air Mover Rental", "High-velocity air mover rental for evaporation acceleration", "Equipment Rental", "day", 55.00, 4.0, 220.00, "Air circulation needed across 3 rooms", "medium", "cap-022", 0.85, true),
                LineItemEntry(8, "CONTENT_MANIPULATION", "Content Manipulation", "Move, protect, or store furniture and personal property to access damage", "General", "room", 150.00, 2.0, 300.00, "Move furniture in hallway and living room to access wet carpet", "medium", "cap-023", 0.85, true)
            ),
            summary = JobSummary(
                totalCaptures = 6,
                nameplateCaptures = 2,
                damageCaptures = 3,
                noteCaptures = 1,
                rejectedCaptures = 2,
                totalLineItems = 8,
                estimateSubtotal = 2682.50,
                averageExtractionConfidence = 0.84,
                unknownLineItemCodes = emptyList(),
                durationSeconds = 4200
            ),
            metadata = JobMetadata()
        )
    }
}
