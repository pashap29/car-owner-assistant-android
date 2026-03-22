package com.carownerassistant.feature.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.carownerassistant.core.model.AnalyticsCalculator
import com.carownerassistant.core.model.AnalyticsChartMode
import com.carownerassistant.core.model.AnalyticsDashboardSummary
import com.carownerassistant.core.model.AnalyticsMetric
import com.carownerassistant.core.model.AnalyticsPeriodFactory
import com.carownerassistant.core.model.AnalyticsPeriodPreset
import com.carownerassistant.core.model.AnalyticsScopeMode
import com.carownerassistant.core.model.AnalyticsTimeRange
import com.carownerassistant.core.model.AnalyticsTrendPoint
import com.carownerassistant.core.model.CategorySpendSummary
import com.carownerassistant.core.model.ExpenseEntrySummary
import com.carownerassistant.core.model.FuelEntrySummary
import com.carownerassistant.core.model.MileageEntrySummary
import com.carownerassistant.core.model.ServiceEntrySummary
import com.carownerassistant.core.model.repository.ExpenseRepository
import com.carownerassistant.core.model.repository.FuelRepository
import com.carownerassistant.core.model.repository.MileageRepository
import com.carownerassistant.core.model.repository.ServiceRepository
import com.carownerassistant.core.model.repository.VehicleRepository
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

private val amountFormat = DecimalFormat("0.00")
private val percentFormat = DecimalFormat("0.0")

private data class VehicleAnalyticsRecords(
    val vehicleId: String,
    val mileage: List<MileageEntrySummary>,
    val fuel: List<FuelEntrySummary>,
    val expenses: List<ExpenseEntrySummary>,
    val services: List<ServiceEntrySummary>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsTabScreen(
    vehicleRepository: VehicleRepository,
    mileageRepository: MileageRepository,
    fuelRepository: FuelRepository,
    expenseRepository: ExpenseRepository,
    serviceRepository: ServiceRepository,
    onOpenMileage: () -> Unit,
) {
    val vehicles by vehicleRepository.observeVehicles().collectAsState(initial = emptyList())
    val activeVehicleId by vehicleRepository.activeVehicleId().collectAsState(initial = null)

    var scopeMode by remember { mutableStateOf(AnalyticsScopeMode.ACTIVE_CAR) }
    var periodPreset by remember { mutableStateOf(AnalyticsPeriodPreset.MONTH) }
    var chartMode by remember { mutableStateOf(AnalyticsChartMode.BAR) }
    var customStartDateText by remember { mutableStateOf(LocalDate.now().minusMonths(1).toString()) }
    var customEndDateText by remember { mutableStateOf(LocalDate.now().toString()) }

    val selectedVehicleIds = remember(scopeMode, vehicles, activeVehicleId) {
        when (scopeMode) {
            AnalyticsScopeMode.ACTIVE_CAR -> listOfNotNull(activeVehicleId)
            AnalyticsScopeMode.ALL_CARS -> vehicles.map { it.id }
        }
    }
    val timeRange = remember(periodPreset, customStartDateText, customEndDateText) {
        when (periodPreset) {
            AnalyticsPeriodPreset.MONTH,
            AnalyticsPeriodPreset.HALF_YEAR,
            AnalyticsPeriodPreset.YEAR -> AnalyticsPeriodFactory.forPreset(LocalDate.now(), periodPreset)
            AnalyticsPeriodPreset.CUSTOM -> parseCustomRange(customStartDateText, customEndDateText)
        }
    }

    val summaryState = produceState(
        initialValue = emptySummary(
            scopeMode = scopeMode,
            range = timeRange ?: AnalyticsPeriodFactory.forPreset(LocalDate.now(), AnalyticsPeriodPreset.MONTH),
            vehicleCount = selectedVehicleIds.size,
            message = "Loading analytics...",
        ),
        scopeMode,
        timeRange,
        selectedVehicleIds,
    ) {
        val range = timeRange
        if (range == null) {
            value = emptySummary(
                scopeMode = scopeMode,
                range = AnalyticsPeriodFactory.forPreset(LocalDate.now(), AnalyticsPeriodPreset.MONTH),
                vehicleCount = selectedVehicleIds.size,
                message = "Enter a valid custom date range in yyyy-MM-dd format.",
            )
            return@produceState
        }
        if (selectedVehicleIds.isEmpty()) {
            value = emptySummary(
                scopeMode = scopeMode,
                range = range,
                vehicleCount = 0,
                message = if (scopeMode == AnalyticsScopeMode.ACTIVE_CAR) {
                    "Select an active car to see analytics."
                } else {
                    "Create at least one car to see analytics."
                },
            )
            return@produceState
        }

        observeVehicleAnalyticsRecords(
            vehicleIds = selectedVehicleIds,
            mileageRepository = mileageRepository,
            fuelRepository = fuelRepository,
            expenseRepository = expenseRepository,
            serviceRepository = serviceRepository,
        ).collect { records ->
            value = AnalyticsCalculator.buildDashboard(
                scopeMode = scopeMode,
                range = range,
                selectedVehicleIds = selectedVehicleIds,
                mileageEntriesByVehicle = records.associate { it.vehicleId to it.mileage },
                fuelEntriesByVehicle = records.associate { it.vehicleId to it.fuel },
                expenseEntriesByVehicle = records.associate { it.vehicleId to it.expenses },
                serviceEntriesByVehicle = records.associate { it.vehicleId to it.services },
            )
        }
    }

    val summary = summaryState.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Statistics") },
                actions = {
                    TextButton(onClick = onOpenMileage) {
                        Text(text = "Mileage")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatisticsControlsCard(
                    scopeMode = scopeMode,
                    periodPreset = periodPreset,
                    chartMode = chartMode,
                    selectedVehicleCount = summary.selectedVehicleCount,
                    customStartDateText = customStartDateText,
                    customEndDateText = customEndDateText,
                    onScopeChange = { scopeMode = it },
                    onPeriodChange = { periodPreset = it },
                    onChartModeChange = { chartMode = it },
                    onCustomStartChange = { customStartDateText = it },
                    onCustomEndChange = { customEndDateText = it },
                )
            }

            item {
                MetricRow(
                    first = MetricCardData("Total expenses", summary.totalExpenses) { "${amountFormat.format(it)}" },
                    second = MetricCardData("Average check", summary.averageCheck) { "${amountFormat.format(it)}" },
                )
            }

            item {
                MetricRow(
                    first = MetricCardData("Distance", summary.totalDistanceKm) { "${amountFormat.format(it)} km" },
                    second = MetricCardData("Cost per km", summary.costPerKm) { "${amountFormat.format(it)}" },
                )
            }

            item {
                MetricRow(
                    first = MetricCardData("Verified mileage ratio", summary.verifiedMileageRatioPercent) { "${percentFormat.format(it)}%" },
                    second = MetricCardData("Trust score", summary.overallTrustScore) { "$it / 100" },
                )
            }

            item {
                MetricRow(
                    first = MetricCardData("Fuel consumption", summary.fuelConsumptionLPer100Km) { "${amountFormat.format(it)} L/100km" },
                    second = MetricCardData("Fuel cost per 100 km", summary.fuelCostPer100Km) { "${amountFormat.format(it)}" },
                )
            }

            item {
                SingleMetricCard(
                    title = "Service spend",
                    metric = summary.serviceSpend,
                    formatter = { "${amountFormat.format(it)}" },
                )
            }

            item {
                CategoryBreakdownCard(summary.categoryBreakdown)
            }

            item {
                TrendChartCard(
                    metric = summary.trend,
                    chartMode = chartMode,
                )
            }
        }
    }
}

@Composable
private fun StatisticsControlsCard(
    scopeMode: AnalyticsScopeMode,
    periodPreset: AnalyticsPeriodPreset,
    chartMode: AnalyticsChartMode,
    selectedVehicleCount: Int,
    customStartDateText: String,
    customEndDateText: String,
    onScopeChange: (AnalyticsScopeMode) -> Unit,
    onPeriodChange: (AnalyticsPeriodPreset) -> Unit,
    onChartModeChange: (AnalyticsChartMode) -> Unit,
    onCustomStartChange: (String) -> Unit,
    onCustomEndChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Analytics dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = "Vehicles in scope: $selectedVehicleCount")
            ToggleRow(
                title = "Scope",
                options = listOf(
                    Triple("Active car", scopeMode == AnalyticsScopeMode.ACTIVE_CAR, { onScopeChange(AnalyticsScopeMode.ACTIVE_CAR) }),
                    Triple("All cars", scopeMode == AnalyticsScopeMode.ALL_CARS, { onScopeChange(AnalyticsScopeMode.ALL_CARS) }),
                ),
            )
            ToggleRow(
                title = "Period",
                options = listOf(
                    Triple("Month", periodPreset == AnalyticsPeriodPreset.MONTH, { onPeriodChange(AnalyticsPeriodPreset.MONTH) }),
                    Triple("Half-year", periodPreset == AnalyticsPeriodPreset.HALF_YEAR, { onPeriodChange(AnalyticsPeriodPreset.HALF_YEAR) }),
                    Triple("Year", periodPreset == AnalyticsPeriodPreset.YEAR, { onPeriodChange(AnalyticsPeriodPreset.YEAR) }),
                    Triple("Custom", periodPreset == AnalyticsPeriodPreset.CUSTOM, { onPeriodChange(AnalyticsPeriodPreset.CUSTOM) }),
                ),
            )
            if (periodPreset == AnalyticsPeriodPreset.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customStartDateText,
                        onValueChange = onCustomStartChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(text = "Start date") },
                    )
                    OutlinedTextField(
                        value = customEndDateText,
                        onValueChange = onCustomEndChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(text = "End date") },
                    )
                }
                Text(
                    text = "Use yyyy-MM-dd format for custom range.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            ToggleRow(
                title = "Chart mode",
                options = listOf(
                    Triple("Bar", chartMode == AnalyticsChartMode.BAR, { onChartModeChange(AnalyticsChartMode.BAR) }),
                    Triple("Line", chartMode == AnalyticsChartMode.LINE, { onChartModeChange(AnalyticsChartMode.LINE) }),
                ),
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    options: List<Triple<String, Boolean, () -> Unit>>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (label, selected, action) ->
                Button(
                    onClick = action,
                    enabled = !selected,
                ) {
                    Text(text = label)
                }
            }
        }
    }
}

private data class MetricCardData<T>(
    val title: String,
    val metric: AnalyticsMetric<T>,
    val formatter: (T) -> String,
)

@Composable
private fun <A, B> MetricRow(
    first: MetricCardData<A>,
    second: MetricCardData<B>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            SingleMetricCard(first.title, first.metric, first.formatter)
        }
        Box(modifier = Modifier.weight(1f)) {
            SingleMetricCard(second.title, second.metric, second.formatter)
        }
    }
}

@Composable
private fun <T> SingleMetricCard(
    title: String,
    metric: AnalyticsMetric<T>,
    formatter: (T) -> String,
) {
    val value = metric.value
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (value != null) {
                Text(
                    text = formatter(value),
                    style = MaterialTheme.typography.headlineSmall,
                )
            } else {
                Text(
                    text = metric.emptyStateMessage ?: "No data",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Text(
                text = metric.trace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    metric: AnalyticsMetric<List<CategorySpendSummary>>,
) {
    val value = metric.value
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Category breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (value == null) {
                Text(text = metric.emptyStateMessage ?: "No data")
            } else {
                value.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = item.category.name)
                        Text(text = "${amountFormat.format(item.totalAmount)} • ${percentFormat.format(item.percentageOfTotal)}%")
                    }
                }
            }
            Text(
                text = metric.trace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TrendChartCard(
    metric: AnalyticsMetric<List<AnalyticsTrendPoint>>,
    chartMode: AnalyticsChartMode,
) {
    val value = metric.value
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Trend chart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (value == null) {
                Text(text = metric.emptyStateMessage ?: "No trend data")
            } else {
                TrendChart(
                    points = value,
                    chartMode = chartMode,
                )
                value.forEach { point ->
                    Text(
                        text = "${point.label}: spend ${amountFormat.format(point.combinedSpendAmount)}, distance ${amountFormat.format(point.distanceKm)} km",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                text = metric.trace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TrendChart(
    points: List<AnalyticsTrendPoint>,
    chartMode: AnalyticsChartMode,
) {
    val maxValue = points.maxOfOrNull { it.combinedSpendAmount }.takeIf { (it ?: 0.0) > 0.0 } ?: 1.0
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPadding = 40f
            val bottomPadding = 24f
            val chartWidth = size.width - leftPadding
            val chartHeight = size.height - bottomPadding
            drawLine(
                color = Color.Gray,
                start = Offset(leftPadding, 0f),
                end = Offset(leftPadding, chartHeight),
                strokeWidth = 2f,
            )
            drawLine(
                color = Color.Gray,
                start = Offset(leftPadding, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 2f,
            )

            if (chartMode == AnalyticsChartMode.BAR) {
                val barWidth = (chartWidth / points.size) * 0.6f
                points.forEachIndexed { index, point ->
                    val ratio = (point.combinedSpendAmount / maxValue).toFloat()
                    val barHeight = chartHeight * ratio
                    val x = leftPadding + (index + 0.2f) * (chartWidth / points.size)
                    drawRect(
                        color = Color(0xFF1D3557),
                        topLeft = Offset(x, chartHeight - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    )
                }
            } else {
                val stepX = if (points.size == 1) 0f else chartWidth / (points.size - 1)
                points.zipWithNext().forEachIndexed { index, (left, right) ->
                    val start = Offset(
                        x = leftPadding + index * stepX,
                        y = chartHeight - ((left.combinedSpendAmount / maxValue).toFloat() * chartHeight),
                    )
                    val end = Offset(
                        x = leftPadding + (index + 1) * stepX,
                        y = chartHeight - ((right.combinedSpendAmount / maxValue).toFloat() * chartHeight),
                    )
                    drawLine(
                        color = Color(0xFFE76F51),
                        start = start,
                        end = end,
                        strokeWidth = 4f,
                    )
                    drawCircle(color = Color(0xFFE76F51), radius = 6f, center = start)
                    if (index == points.lastIndex - 1) {
                        drawCircle(color = Color(0xFFE76F51), radius = 6f, center = end)
                    }
                }
            }
        }
    }
}

private fun observeVehicleAnalyticsRecords(
    vehicleIds: List<String>,
    mileageRepository: MileageRepository,
    fuelRepository: FuelRepository,
    expenseRepository: ExpenseRepository,
    serviceRepository: ServiceRepository,
): Flow<List<VehicleAnalyticsRecords>> {
    if (vehicleIds.isEmpty()) {
        return flowOf(emptyList())
    }

    val perVehicleFlows = vehicleIds.map { vehicleId ->
        combine(
            mileageRepository.observeMileage(vehicleId),
            fuelRepository.observeFuel(vehicleId),
            expenseRepository.observeExpenses(vehicleId),
            serviceRepository.observeServices(vehicleId),
        ) { mileage, fuel, expenses, services ->
            VehicleAnalyticsRecords(
                vehicleId = vehicleId,
                mileage = mileage,
                fuel = fuel,
                expenses = expenses,
                services = services,
            )
        }
    }

    return combine(perVehicleFlows) { it.toList() }
}

private fun parseCustomRange(
    startDateText: String,
    endDateText: String,
): AnalyticsTimeRange? {
    return try {
        val startDate = LocalDate.parse(startDateText.trim())
        val endDate = LocalDate.parse(endDateText.trim())
        AnalyticsPeriodFactory.custom(startDate, endDate)
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun emptySummary(
    scopeMode: AnalyticsScopeMode,
    range: AnalyticsTimeRange,
    vehicleCount: Int,
    message: String,
): AnalyticsDashboardSummary {
    fun <T> emptyMetric(): AnalyticsMetric<T> = AnalyticsMetric(
        value = null,
        trace = message,
        emptyStateMessage = message,
    )

    return AnalyticsDashboardSummary(
        scopeMode = scopeMode,
        range = range,
        selectedVehicleCount = vehicleCount,
        totalExpenses = emptyMetric(),
        categoryBreakdown = emptyMetric(),
        averageCheck = emptyMetric(),
        totalDistanceKm = emptyMetric(),
        verifiedMileageRatioPercent = emptyMetric(),
        overallTrustScore = emptyMetric(),
        costPerKm = emptyMetric(),
        fuelConsumptionLPer100Km = emptyMetric(),
        fuelCostPer100Km = emptyMetric(),
        serviceSpend = emptyMetric(),
        trend = emptyMetric(),
        validFuelChains = 0,
    )
}
