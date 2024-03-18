package com.nikichxp.warehouse.entity

import com.nikichxp.warehouse.data.SKUDao
import com.nikichxp.warehouse.util.distinct
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Service

@OptIn(ExperimentalCoroutinesApi::class)
@Service
class StorageTagsService(
    private val dao: SKUDao
) {
    suspend fun listPossibleTags(userId: String): Flow<String> {
        return dao.findAll().map { it.tags.asFlow() }.flattenConcat().distinct()
    }
}