package com.fieldcapture.domain.usecase

import com.fieldcapture.domain.model.Capture
import com.fieldcapture.domain.model.LineItem
import javax.inject.Inject

class MapToLineItemsUseCase @Inject constructor() {

    operator fun invoke(captures: List<Capture>): List<LineItem> {
        // TODO: Map DamageData and NameplateData extraction fields to LineItemCode entries with quantities & confidence
        return emptyList()
    }
}
