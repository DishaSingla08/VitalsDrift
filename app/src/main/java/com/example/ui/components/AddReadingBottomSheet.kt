package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActivityContext
import com.example.data.MeasurementArm
import com.example.data.TimeOfDay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddReadingBottomSheet(
    onDismiss: () -> Unit,
    onSaveReading: (
        systolic: Int,
        diastolic: Int,
        heartRate: Int,
        activityContext: ActivityContext,
        timeOfDay: TimeOfDay,
        arm: MeasurementArm,
        notes: String
    ) -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    var systolicInput by remember { mutableStateOf("120") }
    var diastolicInput by remember { mutableStateOf("80") }
    var heartRateInput by remember { mutableStateOf("72") }

    // Automatic default time of day according to current system hour
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val initialTimeOfDay = remember {
        when (currentHour) {
            in 5..11 -> TimeOfDay.MORNING
            in 12..16 -> TimeOfDay.MIDDAY
            in 17..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }

    var selectedTimeOfDay by remember { mutableStateOf(initialTimeOfDay) }
    var selectedActivityContext by remember { mutableStateOf(ActivityContext.RESTING) }
    var selectedArm by remember { mutableStateOf(MeasurementArm.LEFT) }
    var notesInput by remember { mutableStateOf("") }

    val sysInt = systolicInput.toIntOrNull()
    val diaInt = diastolicInput.toIntOrNull()
    val hrInt = heartRateInput.toIntOrNull()

    val pulsePressure = remember(sysInt, diaInt) {
        if (sysInt != null && diaInt != null) sysInt - diaInt else null
    }

    val meanArterialPressure = remember(sysInt, diaInt) {
        if (sysInt != null && diaInt != null && sysInt > diaInt) {
            diaInt + ((sysInt - diaInt) / 3.0)
        } else null
    }

    // Validation logic
    val validationError by remember(sysInt, diaInt, hrInt) {
        derivedStateOf {
            when {
                sysInt == null || diaInt == null || hrInt == null -> "Please enter all vital values"
                sysInt !in 50..250 -> "Systolic must be between 50 and 250 mmHg"
                diaInt !in 40..150 -> "Diastolic must be between 40 and 150 mmHg"
                sysInt <= diaInt -> "Systolic must be strictly greater than Diastolic"
                (sysInt - diaInt) < 10 -> "Pulse pressure is unnaturally narrow (< 10 mmHg)"
                hrInt !in 35..220 -> "Heart rate must be between 35 and 220 bpm"
                else -> null
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("add_reading_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Log Blood Pressure",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AHA/ACC Standard Protocol",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_sheet_btn")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vital Numeric Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = systolicInput,
                    onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) systolicInput = it },
                    label = { Text("Systolic") },
                    placeholder = { Text("120") },
                    suffix = { Text("mmHg", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("systolic_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = diastolicInput,
                    onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) diastolicInput = it },
                    label = { Text("Diastolic") },
                    placeholder = { Text("80") },
                    suffix = { Text("mmHg", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("diastolic_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = heartRateInput,
                    onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) heartRateInput = it },
                    label = { Text("Pulse") },
                    placeholder = { Text("72") },
                    suffix = { Text("bpm", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pulse_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Auto-calculated Pulse Pressure & MAP pill
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Pulse Pressure (PP)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (pulsePressure != null) "$pulsePressure mmHg" else "--",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Mean Arterial (MAP)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (meanArterialPressure != null) "${String.format("%.1f", meanArterialPressure)} mmHg" else "--",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Error notice
            AnimatedVisibility(visible = validationError != null) {
                validationError?.let { err ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contextual Tags: Activity State
            Text(
                text = "Activity State & Context",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Baseline drift calculation prefers resting seated logs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ActivityContext.values().forEach { ctx ->
                    FilterChip(
                        selected = selectedActivityContext == ctx,
                        onClick = { selectedActivityContext = ctx },
                        label = { Text(ctx.displayName, fontSize = 12.sp) },
                        modifier = Modifier.testTag("context_chip_${ctx.name}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (ctx.isIdealForBaseline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            selectedLabelColor = if (ctx.isIdealForBaseline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        )
                    )
                }
            }

            // Non-resting warning card
            AnimatedVisibility(visible = !selectedActivityContext.isIdealForBaseline) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Acute physical exertion or stress elevates BP transiently. This entry will be saved but quarantined from long-term baseline moving averages.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timestamp Auto-detected from clock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log Timestamp",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Auto-synced with device clock",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Arm selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Measurement Arm",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeasurementArm.values().forEach { arm ->
                        FilterChip(
                            selected = selectedArm == arm,
                            onClick = { selectedArm = arm },
                            label = { Text(arm.displayName, fontSize = 12.sp) },
                            modifier = Modifier.testTag("arm_chip_${arm.name}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Notes
            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Clinical Notes / Symptoms (Optional)") },
                placeholder = { Text("e.g. Mild headache, took lisinopril 10mg, poor sleep") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Save Action
            Button(
                onClick = {
                    if (validationError == null && sysInt != null && diaInt != null && hrInt != null) {
                        onSaveReading(
                            sysInt,
                            diaInt,
                            hrInt,
                            selectedActivityContext,
                            selectedTimeOfDay,
                            selectedArm,
                            notesInput.trim()
                        )
                    }
                },
                enabled = validationError == null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_reading_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm & Record Reading", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
