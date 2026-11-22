// Screen for adding or editing an emergency contact with input validation and delete confirmation.
package com.example.elderpal.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderpal.R
import com.example.elderpal.data.Contact
import com.example.elderpal.data.ContactsRepository
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderCard
import com.example.elderpal.ui.theme.BorderInput
import com.example.elderpal.ui.theme.DangerRed
import com.example.elderpal.ui.theme.DangerRedTint
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AddContactScreen(
    contactId: String? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit = onBack,
    onSosClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ContactsRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val allContacts by repository.contacts.collectAsState(initial = emptyList())

    val isEditMode = !contactId.isNullOrBlank()

    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isCallFirst by remember { mutableStateOf(false) }

    var validationErrorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // In edit mode, pre-populate existing contact details
    LaunchedEffect(contactId, allContacts) {
        if (!contactId.isNullOrBlank()) {
            val existing = allContacts.firstOrNull { it.id == contactId }
            if (existing != null) {
                name = existing.name
                relationship = existing.relationship
                phoneNumber = existing.phoneNumber
                isCallFirst = existing.isCallFirst
            }
        }
    }

    // Identify if another contact is currently set as call-first
    val currentCallFirstContact = allContacts.firstOrNull { it.isCallFirst && it.id != contactId }

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
            text = if (isEditMode) "Edit contact" else "Add contact",
            style = ElderPalTypography.display,
            color = TextPrimary
        )

        // 1. Name Field
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Name",
                style = ElderPalTypography.label,
                color = TextPrimary
            )
            ElderPalContactInputField(
                value = name,
                onValueChange = {
                    name = it
                    validationErrorMessage = null
                },
                placeholder = "e.g. Anita",
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                isError = validationErrorMessage != null && name.trim().isBlank()
            )
        }

        // 2. Relationship Field
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Relationship",
                style = ElderPalTypography.label,
                color = TextPrimary
            )
            ElderPalContactInputField(
                value = relationship,
                onValueChange = { relationship = it },
                placeholder = "e.g. Sister, Son, Doctor",
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            )
        }

        // 3. Phone Number Field
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Phone number",
                style = ElderPalTypography.label,
                color = TextPrimary
            )
            ElderPalContactInputField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it
                    validationErrorMessage = null
                },
                placeholder = "e.g. 98765 43210",
                keyboardType = KeyboardType.Phone,
                isError = validationErrorMessage != null && ContactsRepository.validate(name, phoneNumber) != null
            )
        }

        // 4. "Call first" Toggle Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ElderPalShapes.card)
                .clickable(
                    onClick = { isCallFirst = !isCallFirst },
                    role = Role.Checkbox
                )
                .semantics {
                    role = Role.Checkbox
                    contentDescription = "Call this person first in an emergency"
                },
            shape = ElderPalShapes.card,
            colors = CardDefaults.cardColors(
                containerColor = if (isCallFirst) TealPrimaryTint else SurfaceWhite
            ),
            border = BorderStroke(
                width = 3.dp,
                color = if (isCallFirst) TealPrimary else BorderInput
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox indicator box
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCallFirst) TealPrimary else SurfaceWhite)
                        .border(
                            width = 2.5.dp,
                            color = if (isCallFirst) TealPrimary else BorderInput,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCallFirst) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Call this person first",
                        style = ElderPalTypography.title.copy(fontSize = 22.sp),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ElderPal will call them first if you press SOS.",
                        style = ElderPalTypography.small.copy(fontSize = 18.sp),
                        color = TextSecondary
                    )
                    if (currentCallFirstContact != null && !isCallFirst) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Currently set to ${currentCallFirstContact.name}.",
                            style = ElderPalTypography.small.copy(
                                fontSize = 16.sp,
                                color = TealPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // 5. Informational Card (matching design/contacts-add.png)
        val displayName = if (name.isNotBlank()) name.trim() else "Your family"
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ElderPalShapes.card,
            colors = CardDefaults.cardColors(containerColor = TealPrimaryTint),
            border = BorderStroke(2.dp, TealPrimary)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chat),
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "$displayName will get a text with your location and your recent medicines.",
                    style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 28.sp),
                    color = TextPrimary
                )
            }
        }

        // Validation Error Alert Box if any
        if (validationErrorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = ElderPalShapes.card,
                colors = CardDefaults.cardColors(containerColor = DangerRedTint),
                border = BorderStroke(2.dp, DangerRed)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_warning),
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = validationErrorMessage ?: "",
                        style = ElderPalTypography.body.copy(
                            fontSize = 18.sp,
                            color = DangerRed,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save Contact Button (72dp tall, teal background, white text)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(TealPrimary)
                .clickable(
                    onClick = {
                        val error = ContactsRepository.validate(name, phoneNumber)
                        if (error != null) {
                            validationErrorMessage = error
                        } else {
                            coroutineScope.launch {
                                if (!contactId.isNullOrBlank()) {
                                    repository.updateContact(
                                        Contact(
                                            id = contactId,
                                            name = name.trim(),
                                            relationship = relationship.trim(),
                                            phoneNumber = phoneNumber.trim(),
                                            isCallFirst = isCallFirst
                                        )
                                    )
                                } else {
                                    repository.saveContact(
                                        Contact(
                                            name = name.trim(),
                                            relationship = relationship.trim(),
                                            phoneNumber = phoneNumber.trim(),
                                            isCallFirst = isCallFirst
                                        )
                                    )
                                }
                                onSaved()
                            }
                        }
                    },
                    role = Role.Button
                )
                .semantics {
                    role = Role.Button
                    contentDescription = if (isEditMode) "Save changes" else "Save contact"
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
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEditMode) "Save changes" else "Save contact",
                    style = ElderPalTypography.title.copy(
                        color = Color.White,
                        fontSize = 24.sp
                    )
                )
            }
        }

        // Delete Contact Button (visible only in Edit Mode)
        if (!contactId.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(ElderPalShapes.button)
                    .background(SurfaceWhite)
                    .border(3.dp, DangerRed, ElderPalShapes.button)
                    .clickable(
                        onClick = { showDeleteConfirmation = true },
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Delete contact"
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Delete contact",
                        style = ElderPalTypography.title.copy(
                            color = DangerRed,
                            fontSize = 22.sp
                        )
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation && !contactId.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = BackgroundWarmCream,
            shape = ElderPalShapes.card,
            title = {
                Text(
                    text = "Delete contact?",
                    style = ElderPalTypography.display.copy(fontSize = 28.sp),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${name.ifBlank { "this contact" }}? They will no longer receive emergency alerts.",
                    style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 28.sp),
                    color = TextPrimary
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .clip(ElderPalShapes.button)
                        .background(DangerRed)
                        .clickable(
                            onClick = {
                                coroutineScope.launch {
                                    repository.deleteContact(contactId)
                                    showDeleteConfirmation = false
                                    onDeleted()
                                }
                            },
                            role = Role.Button
                        )
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete",
                            style = ElderPalTypography.title.copy(
                                color = Color.White,
                                fontSize = 20.sp
                            )
                        )
                    }
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .height(64.dp)
                        .clip(ElderPalShapes.button)
                        .background(SurfaceWhite)
                        .border(2.dp, BorderInput, ElderPalShapes.button)
                        .clickable(
                            onClick = { showDeleteConfirmation = false },
                            role = Role.Button
                        )
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = ElderPalTypography.title.copy(
                            color = TextPrimary,
                            fontSize = 20.sp
                        )
                    )
                }
            }
        )
    }
}

@Composable
private fun ElderPalContactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
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
                keyboardType = keyboardType,
                capitalization = capitalization,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
        )
    }
}
