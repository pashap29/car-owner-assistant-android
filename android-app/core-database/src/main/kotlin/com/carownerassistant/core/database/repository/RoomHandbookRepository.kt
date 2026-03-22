package com.carownerassistant.core.database.repository

import com.carownerassistant.core.database.AppDatabase
import com.carownerassistant.core.database.entity.HandbookDocumentEntity
import com.carownerassistant.core.database.entity.HandbookSectionEntity
import com.carownerassistant.core.database.entity.VehicleHandbookEntity
import com.carownerassistant.core.model.HandbookRules
import com.carownerassistant.core.model.HandbookSectionId
import com.carownerassistant.core.model.HandbookSectionSummary
import com.carownerassistant.core.model.VehicleDocumentSummary
import com.carownerassistant.core.model.VehicleHandbookSummary
import com.carownerassistant.core.model.repository.HandbookRepository
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class RoomHandbookRepository(
    database: AppDatabase,
) : HandbookRepository {

    private val handbookDao = database.handbookDao()

    override fun observeHandbook(vehicleId: String): Flow<VehicleHandbookSummary> {
        return combine(
            handbookDao.observeHandbook(vehicleId),
            handbookDao.observeSections(vehicleId),
            handbookDao.observeDocuments(vehicleId),
        ) { handbook, sections, documents ->
            VehicleHandbookSummary(
                vehicleId = vehicleId,
                vin = handbook?.vin.orEmpty(),
                sections = HandbookRules.mergeSections(
                    sections.map { it.toSummary() },
                ),
                documents = HandbookRules.sortDocuments(
                    documents.map { it.toSummary() },
                ),
            )
        }
    }

    override suspend fun updateVin(vehicleId: String, vin: String) {
        handbookDao.upsertHandbook(
            VehicleHandbookEntity(
                vehicleId = vehicleId,
                vin = HandbookRules.sanitizeVin(vin),
            ),
        )
    }

    override suspend fun updateSection(
        vehicleId: String,
        sectionId: HandbookSectionId,
        content: String,
    ) {
        handbookDao.upsertSection(
            HandbookSectionEntity(
                handbookSectionId = "$vehicleId:${sectionId.name}",
                vehicleId = vehicleId,
                sectionId = sectionId.name,
                content = content,
            ),
        )
    }

    override suspend fun addDocument(
        vehicleId: String,
        displayName: String,
        mimeType: String,
        filePath: String,
    ): String {
        val documentId = UUID.randomUUID().toString()
        handbookDao.insertDocument(
            HandbookDocumentEntity(
                documentId = documentId,
                vehicleId = vehicleId,
                displayName = displayName,
                mimeType = mimeType,
                filePath = filePath,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return documentId
    }

    private fun HandbookSectionEntity.toSummary(): HandbookSectionSummary {
        val section = enumValueOf<HandbookSectionId>(sectionId)
        return HandbookSectionSummary(
            id = section,
            title = section.name,
            content = content,
        )
    }

    private fun HandbookDocumentEntity.toSummary(): VehicleDocumentSummary {
        return VehicleDocumentSummary(
            id = documentId,
            vehicleId = vehicleId,
            displayName = displayName,
            mimeType = mimeType,
            filePath = filePath,
            createdAtEpochMillis = createdAtEpochMillis,
        )
    }
}
