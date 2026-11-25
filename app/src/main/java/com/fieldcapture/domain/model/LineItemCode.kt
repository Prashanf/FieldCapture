package com.fieldcapture.domain.model

/**
 * Standard line item codes for restoration/insurance estimates.
 *
 * Unit prices are PLACEHOLDER values based on general industry ranges
 * (Xactimate-style pricing). These are NOT real pricing — they are draft
 * estimates to demonstrate the pipeline works. Real pricing would come
 * from an Xactimate integration or pricing database.
 */
enum class LineItemCode(
    val displayName: String,
    val description: String,
    val unit: String,
    val unitPrice: Double,
    val category: LineItemCategory
) {
    WATER_EXTRACTION(
        displayName = "Water Extraction",
        description = "Extract standing water using truck-mounted or portable extraction equipment",
        unit = "sq ft",
        unitPrice = 3.50,
        category = LineItemCategory.EMERGENCY
    ),
    REMOVE_REPLACE_DRYWALL(
        displayName = "Remove & Replace Drywall",
        description = "Remove damaged drywall, dispose, install new drywall with tape and finish",
        unit = "sq ft",
        unitPrice = 4.75,
        category = LineItemCategory.REPAIR
    ),
    REMOVE_REPLACE_INSULATION(
        displayName = "Remove & Replace Insulation",
        description = "Remove wet/contaminated insulation, install new batt or blown-in insulation",
        unit = "sq ft",
        unitPrice = 2.50,
        category = LineItemCategory.REPAIR
    ),
    MOLD_REMEDIATION(
        displayName = "Mold Remediation",
        description = "Professional mold removal including containment, HEPA filtration, and treatment",
        unit = "sq ft",
        unitPrice = 15.00,
        category = LineItemCategory.REMEDIATION
    ),
    PAINT_PRIME_WALLS(
        displayName = "Paint & Prime Walls",
        description = "Prime and apply two coats of paint to repaired wall surfaces",
        unit = "sq ft",
        unitPrice = 3.00,
        category = LineItemCategory.FINISH
    ),
    REMOVE_REPLACE_FLOORING(
        displayName = "Remove & Replace Flooring",
        description = "Remove damaged floor covering, prepare subfloor, install new flooring",
        unit = "sq ft",
        unitPrice = 8.50,
        category = LineItemCategory.REPAIR
    ),
    REMOVE_REPLACE_BASEBOARD(
        displayName = "Remove & Replace Baseboard",
        description = "Remove damaged baseboard trim, install and paint new baseboard",
        unit = "linear ft",
        unitPrice = 6.00,
        category = LineItemCategory.REPAIR
    ),
    STRUCTURAL_DRY_OUT(
        displayName = "Structural Dry-Out",
        description = "Monitor and manage drying of structural materials to target moisture levels",
        unit = "day",
        unitPrice = 75.00,
        category = LineItemCategory.DRYING
    ),
    DEHUMIDIFIER_RENTAL(
        displayName = "Dehumidifier Rental",
        description = "Commercial dehumidifier rental for moisture control during drying",
        unit = "day",
        unitPrice = 85.00,
        category = LineItemCategory.EQUIPMENT
    ),
    AIR_MOVER_RENTAL(
        displayName = "Air Mover Rental",
        description = "High-velocity air mover rental for evaporation acceleration",
        unit = "day",
        unitPrice = 55.00,
        category = LineItemCategory.EQUIPMENT
    ),
    ANTIMICROBIAL_TREATMENT(
        displayName = "Antimicrobial Treatment",
        description = "Apply EPA-registered antimicrobial solution to prevent mold/bacterial growth",
        unit = "sq ft",
        unitPrice = 2.00,
        category = LineItemCategory.REMEDIATION
    ),
    CONTENT_MANIPULATION(
        displayName = "Content Manipulation",
        description = "Move, protect, or store furniture and personal property to access damage",
        unit = "room",
        unitPrice = 150.00,
        category = LineItemCategory.GENERAL
    ),
    CEILING_REPAIR(
        displayName = "Ceiling Repair",
        description = "Repair or replace damaged ceiling material including texture matching",
        unit = "sq ft",
        unitPrice = 6.50,
        category = LineItemCategory.REPAIR
    ),
    PLUMBING_REPAIR(
        displayName = "Plumbing Repair",
        description = "Repair or replace damaged plumbing fixtures, pipes, or fittings",
        unit = "each",
        unitPrice = 350.00,
        category = LineItemCategory.REPAIR
    ),
    APPLIANCE_REPLACEMENT(
        displayName = "Appliance Replacement",
        description = "Replace damaged appliance (actual cost varies by appliance type)",
        unit = "each",
        unitPrice = 800.00,
        category = LineItemCategory.REPLACEMENT
    );

    companion object {
        fun fromString(code: String): LineItemCode? {
            return entries.find { it.name.equals(code.trim(), ignoreCase = true) }
        }
    }
}

enum class LineItemCategory(val displayName: String) {
    EMERGENCY("Emergency Services"),
    DRYING("Drying & Monitoring"),
    EQUIPMENT("Equipment Rental"),
    REPAIR("Repair & Replacement"),
    REMEDIATION("Remediation"),
    FINISH("Finishing"),
    REPLACEMENT("Full Replacement"),
    GENERAL("General")
}
