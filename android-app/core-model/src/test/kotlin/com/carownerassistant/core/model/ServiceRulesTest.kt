package com.carownerassistant.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceRulesTest {

    @Test
    fun `total amount sums work and parts`() {
        val total = ServiceRules.totalAmount(
            workItems = listOf(
                ServiceWorkItem(title = "Oil change", totalAmount = 1200.0),
                ServiceWorkItem(title = "Diagnostics", totalAmount = 800.0),
            ),
            partItems = listOf(
                ServicePartItem(title = "Oil filter", quantity = 1, totalAmount = 450.0),
            ),
        )

        assertEquals(2450.0, total, 0.0001)
    }

    @Test
    fun `validation requires title and at least one item`() {
        val result = ServiceRules.validateDraft(
            title = "",
            workItems = emptyList(),
            partItems = emptyList(),
            mileageValue = null,
            mileageUnit = null,
        )

        assertEquals(
            ServiceDraftValidationResult.Invalid("Service title is required."),
            result,
        )
    }

    @Test
    fun `linked mileage draft uses service context`() {
        val mileageDraft = ServiceRules.buildLinkedMileageDraft(
            ServiceEntryDraft(
                vehicleId = "vehicle-1",
                timestampEpochMillis = 1000L,
                title = "Brake service",
                notes = "",
                address = null,
                phone = null,
                contact = null,
                workItems = listOf(ServiceWorkItem("Pad replacement", 2000.0)),
                partItems = emptyList(),
                mileageValue = 45000.0,
                mileageUnit = DistanceUnit.KM,
            ),
        )

        assertNotNull(mileageDraft)
        assertEquals(MileageEntryOrigin.SERVICE_CONTEXT, mileageDraft?.origin)
        assertEquals(45000.0, mileageDraft!!.value, 0.0)
    }

    @Test
    fun `dial and maps uri helpers return platform-friendly values`() {
        assertEquals("tel:+79991234567", ServiceRules.dialUri("+7 (999) 123-45-67"))
        assertTrue(ServiceRules.mapsUri("Moscow, Tverskaya 1")!!.startsWith("geo:0,0?q="))
    }
}
