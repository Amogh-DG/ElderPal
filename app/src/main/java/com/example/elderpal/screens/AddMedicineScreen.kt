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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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

    var showError by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

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
                        onClick = { timesPerDay = count },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Food Option Toggle
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

        // 5. Big Save Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(TealPrimary)
                .clickable(
                    onClick = {
                        if (medicineName.isBlank()) {
                            showError = true
                        } else {
                            val resolvedDose = dose.ifBlank { "1 tablet" }.trim()
                            val newMedicine = Medicine(
                                name = medicineName.trim(),
                                dose = resolvedDose,
                                timesPerDay = timesPerDay,
                                doseTimes = Medicine.defaultDoseTimes(timesPerDay),
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
