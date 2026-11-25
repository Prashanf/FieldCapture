package com.fieldcapture.domain

import com.fieldcapture.domain.usecase.MapToLineItemsUseCase
import org.junit.Assert.assertTrue
import org.junit.Test

class MapToLineItemsUseCaseTest {

    private val useCase = MapToLineItemsUseCase()

    @Test
    fun `empty captures produce empty line items`() {
        val result = useCase(emptyList())
        assertTrue(result.isEmpty())
    }

    // TODO: Add unit tests verifying mapping from DamageData & NameplateData to LineItemCode entries
}
