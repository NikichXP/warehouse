package com.nikichxp.warehouse.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.data.mongodb.core.mapping.FieldType

data class SKU(
    var name: String,
    var quantity: Int = 0,
    var tags: Set<String> = setOf()
) {

    @Id
    @Field(targetType = FieldType.OBJECT_ID)
    lateinit var id: String

    var userIds: Set<String> = setOf()

}