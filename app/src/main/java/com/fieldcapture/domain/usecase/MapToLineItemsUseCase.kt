package com.fieldcapture.domain.usecase

import com.fieldcapture.domain.model.LineItemCode
import com.fieldcapture.extraction.DamageExtraction
import javax.inject.Inject

/**
 * Maps damage extraction results (AI-suggested line items) to priced estimate line items.
 */
class MapToLineItemsUseCase @Inject constructor() {

    data class PricedLineItem(
        val code: String,
        val displayName: String,
        val description: String,
        val unit: String,
        val unitPrice: Double,
        val quantity: Double,
        val total: Double,
        val reason: String,
        val priority: String,
        val sourceCapture: String,
        val confidence: Double,
        val isValidCode: Boolean
    )

    data class LineItemSummary(
        val lineItems: List<PricedLineItem>,
        val subtotal: Double,
        val unknownCodes: List<String>,
        val lineItemCount: Int
    )

    fun execute(
        damageExtractions: List<Pair<String, DamageExtraction>>
    ): LineItemSummary {
        val allLineItems = mutableListOf<PricedLineItem>()
        val unknownCodes = mutableListOf<String>()

        for ((captureId, extraction) in damageExtractions) {
            for (recommended in extraction.recommendedLineItems) {
                val lineItemCode = LineItemCode.fromString(recommended.code)

                if (lineItemCode != null) {
                    val quantity = parseQuantity(recommended.estimatedQuantity, lineItemCode)
                    allLineItems.add(
                        PricedLineItem(
                            code = lineItemCode.name,
                            displayName = lineItemCode.displayName,
                            description = lineItemCode.description,
                            unit = lineItemCode.unit,
                            unitPrice = lineItemCode.unitPrice,
                            quantity = quantity,
                            total = lineItemCode.unitPrice * quantity,
                            reason = recommended.reason,
                            priority = recommended.priority,
                            sourceCapture = captureId,
                            confidence = extraction.confidence.lineItems,
                            isValidCode = true
                        )
                    )
                } else {
                    unknownCodes.add(recommended.code)
                    allLineItems.add(
                        PricedLineItem(
                            code = recommended.code,
                            displayName = recommended.code.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            description = recommended.reason,
                            unit = "each",
                            unitPrice = 0.0,
                            quantity = 1.0,
                            total = 0.0,
                            reason = recommended.reason,
                            priority = recommended.priority,
                            sourceCapture = captureId,
                            confidence = extraction.confidence.lineItems,
                            isValidCode = false
                        )
                    )
                }
            }
        }

        val deduped = deduplicateLineItems(allLineItems)

        return LineItemSummary(
            lineItems = deduped,
            subtotal = deduped.sumOf { it.total },
            unknownCodes = unknownCodes.distinct(),
            lineItemCount = deduped.size
        )
    }

    private fun parseQuantity(estimatedQuantity: String?, lineItemCode: LineItemCode): Double {
        if (estimatedQuantity.isNullOrBlank()) {
            return defaultQuantity(lineItemCode)
        }
        val numberRegex = Regex("""(\d+\.?\d*)""")
        val match = numberRegex.find(estimatedQuantity)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: defaultQuantity(lineItemCode)
    }

    private fun defaultQuantity(code: LineItemCode): Double {
        return when (code) {
            LineItemCode.WATER_EXTRACTION -> 50.0
            LineItemCode.REMOVE_REPLACE_DRYWALL -> 32.0
            LineItemCode.REMOVE_REPLACE_INSULATION -> 32.0
            LineItemCode.MOLD_REMEDIATION -> 10.0
            LineItemCode.PAINT_PRIME_WALLS -> 32.0
            LineItemCode.REMOVE_REPLACE_FLOORING -> 50.0
            LineItemCode.REMOVE_REPLACE_BASEBOARD -> 12.0
            LineItemCode.STRUCTURAL_DRY_OUT -> 3.0
            LineItemCode.DEHUMIDIFIER_RENTAL -> 3.0
            LineItemCode.AIR_MOVER_RENTAL -> 3.0
            LineItemCode.ANTIMICROBIAL_TREATMENT -> 50.0
            LineItemCode.CONTENT_MANIPULATION -> 1.0
            LineItemCode.CEILING_REPAIR -> 20.0
            LineItemCode.PLUMBING_REPAIR -> 1.0
            LineItemCode.APPLIANCE_REPLACEMENT -> 1.0
        }
    }

    private fun deduplicateLineItems(items: List<PricedLineItem>): List<PricedLineItem> {
        return items.groupBy { "${it.code}:${it.sourceCapture}" }
            .map { (_, group) ->
                if (group.size == 1) {
                    group.first()
                } else {
                    group.maxByOrNull { it.confidence } ?: group.first()
                }
            }
            .sortedWith(
                compareBy<PricedLineItem> { priorityOrder(it.priority) }
                    .thenBy { it.code }
            )
    }

    private fun priorityOrder(priority: String): Int = when (priority.lowercase()) {
        "high" -> 0
        "medium" -> 1
        "low" -> 2
        else -> 3
    }
}
