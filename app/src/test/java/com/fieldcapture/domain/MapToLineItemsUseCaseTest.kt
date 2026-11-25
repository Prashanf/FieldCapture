package com.fieldcapture.domain

import com.fieldcapture.domain.model.LineItemCode
import com.fieldcapture.domain.usecase.MapToLineItemsUseCase
import com.fieldcapture.extraction.DamageConfidence
import com.fieldcapture.extraction.DamageExtraction
import com.fieldcapture.extraction.RecommendedLineItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapToLineItemsUseCaseTest {

    private val useCase = MapToLineItemsUseCase()

    @Test
    fun `maps valid damage extraction to priced line items`() {
        val extraction = DamageExtraction(
            damageType = "water_damage",
            severity = "high",
            description = "Water damage on wall",
            affectedMaterials = listOf("drywall"),
            affectedAreaEstimate = "16 sq ft",
            room = "kitchen",
            surface = "north wall",
            moistureVisible = true,
            moldVisible = false,
            recommendedLineItems = listOf(
                RecommendedLineItem(
                    code = "REMOVE_REPLACE_DRYWALL",
                    reason = "Drywall water damaged",
                    priority = "high",
                    estimatedQuantity = "16 sq ft"
                )
            ),
            confidence = DamageConfidence(0.9, 0.8, 0.85, 0.75),
            overallConfidence = 0.82
        )

        val result = useCase.execute(listOf("cap-001" to extraction))

        assertEquals(1, result.lineItems.size)
        assertEquals("REMOVE_REPLACE_DRYWALL", result.lineItems[0].code)
        assertEquals(4.75, result.lineItems[0].unitPrice, 0.01)
        assertEquals(16.0, result.lineItems[0].quantity, 0.01)
        assertEquals(76.0, result.lineItems[0].total, 0.01)
        assertTrue(result.lineItems[0].isValidCode)
        assertEquals(76.0, result.subtotal, 0.01)
    }

    @Test
    fun `handles unknown line item codes gracefully`() {
        val extraction = DamageExtraction(
            damageType = "water_damage",
            severity = "medium",
            description = "Some damage",
            affectedMaterials = listOf("wood"),
            recommendedLineItems = listOf(
                RecommendedLineItem("CUSTOM_REPAIR_CODE", "Custom repair", "medium", null)
            ),
            overallConfidence = 0.5
        )

        val result = useCase.execute(listOf("cap-002" to extraction))

        assertEquals(1, result.lineItems.size)
        assertFalse(result.lineItems[0].isValidCode)
        assertEquals(0.0, result.lineItems[0].unitPrice, 0.01)
        assertEquals(listOf("CUSTOM_REPAIR_CODE"), result.unknownCodes)
    }

    @Test
    fun `parses quantity from various formats`() {
        val makeExtraction = { qty: String? ->
            DamageExtraction(
                damageType = "water_damage",
                severity = "medium",
                description = "test",
                recommendedLineItems = listOf(
                    RecommendedLineItem("WATER_EXTRACTION", "test", "medium", qty)
                ),
                overallConfidence = 0.5
            )
        }

        var result = useCase.execute(listOf("c1" to makeExtraction("~16 sq ft")))
        assertEquals(16.0, result.lineItems[0].quantity, 0.01)

        result = useCase.execute(listOf("c2" to makeExtraction("approximately 8 sq ft")))
        assertEquals(8.0, result.lineItems[0].quantity, 0.01)

        result = useCase.execute(listOf("c3" to makeExtraction(null)))
        assertEquals(50.0, result.lineItems[0].quantity, 0.01)
    }

    @Test
    fun `multiple damage captures produce multiple line items`() {
        val extraction1 = DamageExtraction(
            damageType = "water_damage",
            severity = "high",
            description = "Kitchen floor",
            affectedMaterials = listOf("flooring"),
            affectedAreaEstimate = "64 sq ft",
            room = "kitchen",
            surface = "floor",
            moistureVisible = true,
            recommendedLineItems = listOf(
                RecommendedLineItem("REMOVE_REPLACE_FLOORING", "Floor damaged", "high", "64 sq ft"),
                RecommendedLineItem("WATER_EXTRACTION", "Standing water", "high", "64 sq ft")
            ),
            confidence = DamageConfidence(0.9, 0.85, 0.85, 0.80),
            overallConfidence = 0.85
        )

        val extraction2 = DamageExtraction(
            damageType = "water_damage",
            severity = "medium",
            description = "Bathroom wall",
            affectedMaterials = listOf("drywall"),
            affectedAreaEstimate = "24 sq ft",
            room = "bathroom",
            surface = "wall",
            moistureVisible = true,
            recommendedLineItems = listOf(
                RecommendedLineItem("REMOVE_REPLACE_DRYWALL", "Drywall damaged", "medium", "24 sq ft")
            ),
            confidence = DamageConfidence(0.8, 0.75, 0.80, 0.70),
            overallConfidence = 0.76
        )

        val result = useCase.execute(
            listOf(
                "cap-001" to extraction1,
                "cap-002" to extraction2
            )
        )

        assertEquals(3, result.lineItems.size)
        assertTrue(result.subtotal > 0)
    }

    @Test
    fun `empty damage list produces empty line items`() {
        val result = useCase.execute(emptyList())
        assertEquals(0, result.lineItems.size)
        assertEquals(0.0, result.subtotal, 0.01)
    }

    @Test
    fun `all 15 LineItemCode values are valid`() {
        val allCodes = listOf(
            "WATER_EXTRACTION", "REMOVE_REPLACE_DRYWALL", "REMOVE_REPLACE_INSULATION",
            "MOLD_REMEDIATION", "PAINT_PRIME_WALLS", "REMOVE_REPLACE_FLOORING",
            "REMOVE_REPLACE_BASEBOARD", "STRUCTURAL_DRY_OUT", "DEHUMIDIFIER_RENTAL",
            "AIR_MOVER_RENTAL", "ANTIMICROBIAL_TREATMENT", "CONTENT_MANIPULATION",
            "CEILING_REPAIR", "PLUMBING_REPAIR", "APPLIANCE_REPLACEMENT"
        )
        assertEquals(15, LineItemCode.entries.size)
        for (code in allCodes) {
            assertNotNull("LineItemCode.fromString($code) should not be null", LineItemCode.fromString(code))
        }
    }
}
