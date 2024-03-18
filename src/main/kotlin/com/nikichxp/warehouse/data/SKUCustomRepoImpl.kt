package com.nikichxp.warehouse.data

import com.mongodb.client.result.UpdateResult
import com.nikichxp.warehouse.entity.ListOptions
import com.nikichxp.warehouse.entity.SKU
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.mongodb.core.update
import org.springframework.stereotype.Repository

@Repository
class SKUCustomRepoImpl(
    private val mongoTemplate: ReactiveMongoTemplate
) : SKUCustomRepo {

    override fun customFind(parameters: ListOptions): Flow<SKU> {
        val query = parameters.toQuery()
        return mongoTemplate.find<SKU>(query).asFlow()
    }

    override suspend fun updateQuantity(id: ObjectId, quantity: Int, userId: String): UpdateResult {
        return mongoTemplate.update<SKU>().matching(
            Criteria.where(SKU::id.name).`is`(id)
                .and(SKU::userIds.name).`is`(userId)
        )
            .apply(Update.update(SKU::quantity.name, quantity))
            .first()
            .awaitSingle()
    }
}