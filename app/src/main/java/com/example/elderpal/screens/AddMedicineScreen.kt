// Screen for entering and saving a new medicine schedule with dosage and frequency options.
package com.example.elderpal.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderpal.R
import com.example.elderpal.data.Medicine
import com.example.elderpal.data.MedicineRepository
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderInput
import com.example.elderpal.ui.theme.DangerRed
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onSosClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { MedicineRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var medicineName by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var timesPerDay by remember { mutableIntStateOf(1) }
    var takesWithFood by remember { mutableStateOf(false) }

    var doseTimes by remember { mutableStateOf(Medicine.defaultDoseTimes(1)) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var editingTimeIndex by remember { mutableIntStateOf(0) }
    var timePickerInitialHour by remember { mutableIntStateOf(8) }
    var timePickerInitialMinute by remember { mutableIntStateOf(0) }

    var showError by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    if (showTimePickerDialog) {
        Material3TimePickerDialog(
            initialHour = timePickerInitialHour,
            initialMinute = timePickerInitialMinute,
            onConfirm = { hour, minute ->
                val formatted = format12Hour(hour, minute)
                if (editingTimeIndex in doseTimes.indices) {
                    val updated = doseTimes.toMutableList()
                    updated[editingTimeIndex] = formatted
                    doseTimes = updated
                } else {
                    val updated = doseTimes + formatted
                    doseTimes = updated
                    timesPerDay = updated.size
                }
                showTimePickerDialog = false
            },
            onDismiss = { showTimePickerDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top header with Back and SOS buttons
        InnerScreenHeader(
            onBack = onBack,
            onSosClick = onSosClick
        )

        // Title
        Text(
            text = "Add medicine",
            style = ElderPalTypography.display,
            color = TextPrimary
        )

        // 1. Medicine Name Field
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Medicine name",
                style = ElderPalTypography.label,
                color = TextPrimary
            )
            ElderPalInputField(
                value = medicineName,
                onValueChange = {
                    medicineName = it
                    if (showError && it.isNotBlank()) showError = false
                },
                placeholder = "e.g. Metformin",
                isError = showError && medicineName.isBlank()
            )
            if (showError && medicineName.isBlank()) {
                Text(
                    text = "Please enter a medicine name",
                    style = ElderPalTypography.small.copy(color = DangerRed)
                )
            }
        }

        // 2. Dose Field
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Dose",
                style = ElderPalTypography.label,
                color = TextPrimary
            )
            ElderPalInputField(
                value = dose,
                onValueChange = { dose = it },
                placeholder = "1 tablet"
            )
        }

        // 3. Times Per Day Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Times per day",
                style = ElderPalTypography.label,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf(1, 2, 3, 4).forEach { count ->
                    TimesPerDayButton(
                        number = count,
                        isSelected = timesPerDay == count,
                        onClick = {
                            timesPerDay = count
                            doseTimes = Medicine.defaultDoseTimes(count)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Dose Times Selection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Time of dose",
                style = ElderPalTypography.label,
                color = TextPrimary
            )

            doseTimes.forEachIndexed { index, timeStr ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tappable time field opening Material 3 time picker
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .clip(ElderPalShapes.input)
                            .background(SurfaceWhite)
                            .border(
                                width = 3.dp,
                                color = BorderInput,
                                shape = ElderPalShapes.input
                            )
                            .clickable(
                                onClick = {
                                    editingTimeIndex = index
                                    val (h, m) = parse12Hour(timeStr)
                                    timePickerInitialHour = h
                                    timePickerInitialMinute = m
                                    showTimePickerDialog = true
                                },
                                role = Role.Button
                            )
                            .padding(horizontal = 20.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Time ${index + 1}: $timeStr. Tap to change time"
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_clock),
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = timeStr,
                                style = ElderPalTypography.title.copy(color = TextPrimary)
                            )
                        }
                    }

                    // Remove button for extra times
                    if (doseTimes.size > 1) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(ElderPalShapes.button)
                                .background(SurfaceWhite)
                                .border(
                                    width = 3.dp,
                                    color = BorderInput,
                                    shape = ElderPalShapes.button
                                )
                                .clickable(
                                    onClick = {
                                        val updated = doseTimes.toMutableList()
                                        updated.removeAt(index)
                                        doseTimes = updated
                                        timesPerDay = updated.size
                                    },
                                    role = Role.Button
                                )
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Remove time $timeStr"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_delete),
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Button to add an extra time
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(ElderPalShapes.button)
                    .background(SurfaceWhite)
                    .border(
                        width = 3.dp,
                        color = TealPrimary,
                        shape = ElderPalShapes.button
                    )
                    .clickable(
                        onClick = {
                            editingTimeIndex = -1 // -1 means add new time
                            val (h, m) = if (doseTimes.isNotEmpty()) {
                                val (lastH, lastM) = parse12Hour(doseTimes.last())
                                Pair((lastH + 4) % 24, lastM)
                            } else {
                                Pair(12, 0)
                            }
                            timePickerInitialHour = h
                            timePickerInitialMinute = m
                            showTimePickerDialog = true
                        },
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Add another dose time"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add another time",
                        style = ElderPalTypography.title.copy(
                            color = TealPrimary,
                            fontSize = 20.sp
                        )
                    )
                }
            }

            if (showError && doseTimes.isEmpty()) {
                Text(
                    text = "Please set at least one time",
                    style = ElderPalTypography.small.copy(color = DangerRed)
                )
            }
        }

        // 5. Food Option Toggle
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Food",
                style = ElderPalTypography.label,
                color = TextPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FoodOptionButton(
                    label = "With food",
                    isSelected = takesWithFood,
                    onClick = { takesWithFood = true },
                    modifier = Modifier.weight(1f)
                )
                FoodOptionButton(
                    label = "No food",
                    isSelected = !takesWithFood,
                    onClick = { takesWithFood = false },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Big Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(TealPrimary)
                .clickable(
                    onClick = {
                        if (medicineName.isBlank() || doseTimes.isEmpty()) {
                            showError = true
                        } else {
                            val resolvedDose = dose.ifBlank { "1 tablet" }.trim()
                            val newMedicine = Medicine(
                                name = medicineName.trim(),
                                dose = resolvedDose,
                                timesPerDay = doseTimes.size,
                                doseTimes = doseTimes,
                                takesWithFood = takesWithFood
                            )
                            coroutineScope.launch {
                                repository.saveMedicine(newMedicine)
                                com.example.elderpal.data.DoseReminderScheduler.rescheduleAll(context)
                                onSaved()
                            }
                        }
                    },
                    role = Role.Button
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Save medicine"
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Save",
                    style = ElderPalTypography.title.copy(color = Color.White)
                )
            }
        }
    }
}

@Composable
private fun ElderPalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(ElderPalShapes.input)
            .background(SurfaceWhite)
            .border(
                width = 3.dp,
                color = when {
                    isError -> DangerRed
                    isFocused -> TealPrimary
                    else -> BorderInput
                },
                shape = ElderPalShapes.input
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = ElderPalTypography.title.copy(color = TextSecondary.copy(alpha = 0.5f))
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = ElderPalTypography.title.copy(color = TextPrimary),
            cursorBrush = SolidColor(TealPrimary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        )
    }
}

@Composable
private fun TimesPerDayButton(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(ElderPalShapes.button)
            .background(if (isSelected) TealPrimary else SurfaceWhite)
            .border(
                width = 3.dp,
                color = if (isSelected) TealPrimary else BorderInput,
                shape = ElderPalShapes.button
            )
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                role = Role.Button
                contentDescription = "$number times per day"
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = "$number",
                style = ElderPalTypography.title.copy(
                    color = if (isSelected) Color.White else TextPrimary
                )
            )
        }
    }
}

@Composable
private fun FoodOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(ElderPalShapes.button)
            .background(if (isSelected) TealPrimary else SurfaceWhite)
            .border(
                width = 3.dp,
                color = if (isSelected) TealPrimary else BorderInput,
                shape = ElderPalShapes.button
            )
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                role = Role.Button
                contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = ElderPalTypography.title.copy(
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 22.sp
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Material3TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                }
            ) {
                Text(
                    text = "OK",
                    style = ElderPalTypography.title.copy(color = TealPrimary, fontSize = 20.sp)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = ElderPalTypography.title.copy(color = TextSecondary, fontSize = 20.sp)
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        containerColor = SurfaceWhite,
        shape = ElderPalShapes.card
    )
}

private fun format12Hour(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when (val h = hour % 12) {
        0 -> 12
        else -> h
    }
    return String.format(java.util.Locale.US, "%d:%02d %s", hour12, minute, amPm)
}

private fun parse12Hour(timeStr: String): Pair<Int, Int> {
    return try {
        val parts = timeStr.trim().split(" ")
        val timeParts = parts[0].split(":")
        var hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        val isPm = parts.getOrNull(1)?.equals("PM", ignoreCase = true) == true
        if (isPm && hour < 12) hour += 12
        if (!isPm && hour == 12) hour = 0
        Pair(hour, minute)
    } catch (e: Exception) {
        Pair(8, 0)
    }
}
