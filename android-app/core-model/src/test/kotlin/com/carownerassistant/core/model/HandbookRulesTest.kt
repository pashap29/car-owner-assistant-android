package com.carownerassistant.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandbookRulesTest {

    @Test
    fun `default sections are always present in canonical order`() {
        val merged = HandbookRules.mergeSections(
            storedSections = listOf(
                HandbookSectionSummary(
                    id = HandbookSectionId.GENERAL_NOTES,
                    title = "Ignored custom title",
                    content = "Custom content",
                ),
            ),
        )

        assertEquals(HandbookSectionId.entries.toList(), merged.map { it.id })
        assertEquals("Custom content", merged.last().content)
        assertEquals("General Notes", merged.last().title)
    }

    @Test
    fun `vin is trimmed uppercased and capped at 17 chars`() {
        val vin = HandbookRules.sanitizeVin("  wp0zzz99zts392124-extra ")

        assertEquals("WP0ZZZ99ZTS392124", vin)
    }

    @Test
    fun `documents are sorted newest first`() {
        val sorted = HandbookRules.sortDocuments(
            listOf(
                document(id = "1", createdAt = 10L, name = "b.pdf"),
                document(id = "2", createdAt = 20L, name = "a.pdf"),
                document(id = "3", createdAt = 20L, name = "c.pdf"),
            ),
        )

        assertEquals(listOf("2", "3", "1"), sorted.map { it.id })
    }

    @Test
    fun `defaults include identification section for manual vin`() {
        assertTrue(HandbookRules.defaultSections().any { it.id == HandbookSectionId.IDENTIFICATION })
    }

    private fun document(
        id: String,
        createdAt: Long,
        name: String,
    ): VehicleDocumentSummary {
        return VehicleDocumentSummary(
            id = id,
            vehicleId = "vehicle-1",
            displayName = name,
            mimeType = "application/pdf",
            filePath = "/tmp/$name",
            createdAtEpochMillis = createdAt,
        )
    }
}
