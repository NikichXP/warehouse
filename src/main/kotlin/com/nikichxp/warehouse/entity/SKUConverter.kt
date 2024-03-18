package com.nikichxp.warehouse.entity

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class SKUConverter : Converter<SKU, SKUDTO> {

    override fun convert(sku: SKU): SKUDTO {
        return SKUDTO(
            id = sku.id,
            name = sku.name,
            quantity = sku.quantity,
            tags = sku.tags
        )
    }
}