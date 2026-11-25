package com.fieldcapture.domain.model

enum class LineItemCode(val description: String, val unit: String) {
    WATER_EXTRACTION("Water extraction", "per sq ft"),
    REMOVE_REPLACE_DRYWALL("Remove and replace drywall", "per sq ft"),
    REMOVE_REPLACE_INSULATION("Remove and replace insulation", "per sq ft"),
    MOLD_REMEDIATION("Mold remediation treatment", "per sq ft"),
    PAINT_PRIME_WALLS("Paint and prime walls", "per sq ft"),
    REMOVE_REPLACE_FLOORING("Remove and replace flooring", "per sq ft"),
    REMOVE_REPLACE_BASEBOARD("Remove and replace baseboard", "per linear ft"),
    STRUCTURAL_DRY_OUT("Structural dry-out equipment", "per day"),
    DEHUMIDIFIER_RENTAL("Dehumidifier rental", "per day"),
    AIR_MOVER_RENTAL("Air mover rental", "per day"),
    ANTIMICROBIAL_TREATMENT("Antimicrobial treatment", "per sq ft"),
    CONTENT_MANIPULATION("Content manipulation / move-out", "per room"),
    CEILING_REPAIR("Ceiling repair / replacement", "per sq ft"),
    PLUMBING_REPAIR("Plumbing repair", "per unit"),
    APPLIANCE_REPLACEMENT("Appliance replacement", "per unit"),
}
