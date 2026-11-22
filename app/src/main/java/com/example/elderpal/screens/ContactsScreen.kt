// Displays emergency family contacts, the empty state, and access to add or edit contacts.
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.elderpal.R
import com.example.elderpal.data.Contact
import com.example.elderpal.data.ContactsRepository
import com.example.elderpal.ui.theme.BackgroundWarmCream
import com.example.elderpal.ui.theme.BorderCard
import com.example.elderpal.ui.theme.ElderPalShapes
import com.example.elderpal.ui.theme.ElderPalTypography
import com.example.elderpal.ui.theme.SurfaceWhite
import com.example.elderpal.ui.theme.TealPrimary
import com.example.elderpal.ui.theme.TealPrimaryTint
import com.example.elderpal.ui.theme.TextPrimary
import com.example.elderpal.ui.theme.TextSecondary

@Composable
fun ContactsScreen(
    onBack: () -> Unit,
    onAddContactClick: () -> Unit,
    onEditContactClick: (contactId: String) -> Unit,
    onPreviewAlertClick: () -> Unit = {},
    onSosClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ContactsRepository(context) }
    val contacts by repository.contacts.collectAsState(initial = emptyList())

    if (contacts.isEmpty()) {
        ContactsEmptyState(
            onBack = onBack,
            onSosClick = onSosClick,
            onAddContactClick = onAddContactClick,
            onPreviewAlertClick = onPreviewAlertClick,
            modifier = modifier
        )
    } else {
        ContactsListState(
            contacts = contacts,
            onBack = onBack,
            onSosClick = onSosClick,
            onContactClick = onEditContactClick,
            onAddContactClick = onAddContactClick,
            onPreviewAlertClick = onPreviewAlertClick,
            modifier = modifier
        )
    }
}

// State matching design/contacts.png with existing contacts and notification note
@Composable
private fun ContactsListState(
    contacts: List<Contact>,
    onBack: () -> Unit,
    onSosClick: () -> Unit,
    onContactClick: (contactId: String) -> Unit,
    onAddContactClick: () -> Unit,
    onPreviewAlertClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Back and SOS
            InnerScreenHeader(
                onBack = onBack,
                onSosClick = onSosClick
            )

            // Title
            Text(
                text = "Emergency contacts",
                style = ElderPalTypography.display,
                color = TextPrimary
            )

            // Contacts list cards
            contacts.forEach { contact ->
                ContactCard(
                    contact = contact,
                    onClick = { onContactClick(contact.id) }
                )
            }

            // Explanatory note about what emergency contacts receive, with preview button
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ElderPalShapes.card)
                    .clickable(
                        onClick = onPreviewAlertClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "See example text that family will receive"
                    },
                shape = ElderPalShapes.card,
                colors = CardDefaults.cardColors(containerColor = TealPrimaryTint),
                border = BorderStroke(2.dp, TealPrimary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            text = "Your family will get a text with your location and your recent medicines.",
                            style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 28.sp),
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = ElderPalShapes.button,
                        color = SurfaceWhite,
                        border = BorderStroke(2.dp, TealPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "See example text",
                                style = ElderPalTypography.title.copy(
                                    color = TealPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bottom Add Contact Button (72dp tall, teal background, white text)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(TealPrimary)
                .clickable(
                    onClick = onAddContactClick,
                    role = Role.Button
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Add contact"
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
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add contact",
                    style = ElderPalTypography.title.copy(
                        color = Color.White,
                        fontSize = 24.sp
                    )
                )
            }
        }
    }
}

// Single contact card matching the design specification
@Composable
private fun ContactCard(
    contact: Contact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(ElderPalShapes.card)
            .clickable(
                onClick = onClick,
                role = Role.Button
            )
            .semantics {
                role = Role.Button
                contentDescription = "Contact: ${contact.name}, ${contact.relationship}, ${contact.phoneNumber}. Tap to edit."
            },
        shape = ElderPalShapes.card,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(2.dp, BorderCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle avatar with the first letter of the name
            val initial = contact.name.trim().firstOrNull()?.uppercase() ?: "C"
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(TealPrimaryTint)
                    .border(2.5.dp, TealPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = ElderPalTypography.heading2.copy(
                        color = TealPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Contact details column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = ElderPalTypography.title.copy(fontSize = 24.sp),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val subtitle = if (contact.relationship.isNotBlank()) {
                    "${contact.relationship} · ${contact.phoneNumber}"
                } else {
                    contact.phoneNumber
                }
                Text(
                    text = subtitle,
                    style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 26.sp),
                    color = TextPrimary
                )

                // Call First indicator pill
                if (contact.isCallFirst) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = TealPrimaryTint,
                        border = BorderStroke(1.5.dp, TealPrimary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_phone),
                                contentDescription = null,
                                tint = TealPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Call first",
                                style = ElderPalTypography.small.copy(
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
            }

            // Edit icon button indicating card is editable
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Empty state matching design/contacts-empty.png
@Composable
private fun ContactsEmptyState(
    onBack: () -> Unit,
    onSosClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onPreviewAlertClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundWarmCream)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Back and SOS
            InnerScreenHeader(
                onBack = onBack,
                onSosClick = onSosClick
            )

            // Title
            Text(
                text = "Emergency contacts",
                style = ElderPalTypography.display,
                color = TextPrimary
            )
        }

        // Center empty state illustration and explanatory text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Big circular illustration with group contact icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(TealPrimaryTint)
                    .border(3.dp, TealPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_contacts),
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No contacts yet",
                style = ElderPalTypography.heading2,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Add someone who can help you.\nYour family will get a text with your\nlocation and your recent\nmedicines.",
                style = ElderPalTypography.body.copy(fontSize = 20.sp, lineHeight = 30.sp),
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = ElderPalShapes.button,
                color = SurfaceWhite,
                border = BorderStroke(2.dp, TealPrimary),
                modifier = Modifier
                    .clip(ElderPalShapes.button)
                    .clickable(
                        onClick = onPreviewAlertClick,
                        role = Role.Button
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "See example text that family will receive"
                    }
                    .padding(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chat),
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "See example text",
                        style = ElderPalTypography.title.copy(
                            color = TealPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // Bottom Add Contact Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(ElderPalShapes.button)
                .background(TealPrimary)
                .clickable(
                    onClick = onAddContactClick,
                    role = Role.Button
                )
                .semantics {
                    role = Role.Button
                    contentDescription = "Add contact"
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
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add contact",
                    style = ElderPalTypography.title.copy(
                        color = Color.White,
                        fontSize = 24.sp
                    )
                )
            }
        }
    }
}
