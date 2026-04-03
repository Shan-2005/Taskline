package com.example.chattaskai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chattaskai.data.profile.TrackingRules
import com.example.chattaskai.data.profile.UserProfile
import com.example.chattaskai.ui.theme.LocalLiquidColors
import com.example.chattaskai.util.ContactsProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSourcesForm(
    profile: UserProfile,
    rules: TrackingRules,
    knownWhatsAppSources: List<String>,
    onProfileChange: (UserProfile) -> Unit,
    onRulesChange: (TrackingRules) -> Unit,
    onSave: () -> Unit,
    saveLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalLiquidColors.current
    val context = LocalContext.current

    var phoneNumber by remember(profile.phoneNumber) { mutableStateOf(profile.phoneNumber) }
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var email by remember(profile.email) { mutableStateOf(profile.email) }
    var organization by remember(profile.organization) { mutableStateOf(profile.organization) }
    var trackWhatsApp by remember(rules.trackWhatsApp) { mutableStateOf(rules.trackWhatsApp) }
    var trackGmail by remember(rules.trackGmail) { mutableStateOf(rules.trackGmail) }
    var trackOutlook by remember(rules.trackOutlook) { mutableStateOf(rules.trackOutlook) }
    var gmailFilters by remember(rules.gmailFilters) { mutableStateOf(rules.gmailFilters) }
    var whatsappFilters by remember(rules.whatsappFilters) { mutableStateOf(rules.whatsappFilters) }
    var messageKeywords by remember(rules.messageKeywords) { mutableStateOf(rules.messageKeywords) }
    var whatsappSelectorExpanded by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        val contacts = ContactsProvider.getPhoneContacts(context).map { it.name }
        deviceContacts = contacts
    }

    LaunchedEffect(phoneNumber, displayName, email, organization, trackWhatsApp, trackGmail, trackOutlook, gmailFilters, whatsappFilters, messageKeywords) {
        onProfileChange(
            profile.copy(
                phoneNumber = phoneNumber,
                displayName = displayName,
                email = email,
                organization = organization
            )
        )
        onRulesChange(
            rules.copy(
                trackWhatsApp = trackWhatsApp,
                trackGmail = trackGmail,
                trackOutlook = trackOutlook,
                gmailFilters = gmailFilters,
                whatsappFilters = whatsappFilters,
                messageKeywords = messageKeywords
            )
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TrackingFieldCard(
            title = "Account Profile",
            icon = Icons.Default.Person,
            description = "This local profile will later become the cloud account identity for mobile and desktop sync."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Mobile Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = organization,
                    onValueChange = { organization = it },
                    label = { Text("Organization") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        TrackingFieldCard(
            title = "Sources to Track",
            icon = Icons.Default.CheckCircle,
            description = "Choose which apps can create tasks. You can still narrow them further with filters."
        ) {
            TrackingToggleRow(
                label = "WhatsApp",
                checked = trackWhatsApp,
                onCheckedChange = { trackWhatsApp = it },
                colors = colors
            )
            TrackingToggleRow(
                label = "Gmail",
                checked = trackGmail,
                onCheckedChange = { trackGmail = it },
                colors = colors
            )
            TrackingToggleRow(
                label = "Outlook",
                checked = trackOutlook,
                onCheckedChange = { trackOutlook = it },
                colors = colors
            )
        }

        TrackingFieldCard(
            title = "Gmail Filters",
            icon = Icons.Default.Email,
            description = "Enter sender emails, account names, or keywords. Separate multiple values with commas or new lines."
        ) {
            OutlinedTextField(
                value = gmailFilters,
                onValueChange = { gmailFilters = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("boss@company.com, finance, hr@company.com") }
            )
        }

        TrackingFieldCard(
            title = "WhatsApp Filters",
            icon = Icons.Default.CheckCircle,
            description = "Select device contacts, detected groups, or enter group names manually."
        ) {
            // Device Contacts Picker
            var deviceSelectorExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = deviceSelectorExpanded,
                onExpandedChange = { deviceSelectorExpanded = !deviceSelectorExpanded }
            ) {
                OutlinedTextField(
                    value = "Select device contacts",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("Device contacts") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceSelectorExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = deviceSelectorExpanded,
                    onDismissRequest = { deviceSelectorExpanded = false }
                ) {
                    if (deviceContacts.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No contacts found (check READ_CONTACTS permission)") },
                            onClick = { deviceSelectorExpanded = false }
                        )
                    } else {
                        deviceContacts.forEach { contact ->
                            val selected = parseFilterValues(whatsappFilters).any { it.equals(contact, ignoreCase = true) }
                            DropdownMenuItem(
                                text = { Text(contact) },
                                onClick = {
                                    whatsappFilters = toggleFilterValue(whatsappFilters, contact)
                                },
                                trailingIcon = {
                                    androidx.compose.material3.Checkbox(
                                        checked = selected,
                                        onCheckedChange = null
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Detected WhatsApp sources dropdown
            ExposedDropdownMenuBox(
                expanded = whatsappSelectorExpanded,
                onExpandedChange = { whatsappSelectorExpanded = !whatsappSelectorExpanded }
            ) {
                OutlinedTextField(
                    value = selectedWhatsAppSummary(whatsappFilters),
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text("Detected WhatsApp contacts/groups") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = whatsappSelectorExpanded)
                    }
                )

                ExposedDropdownMenu(
                    expanded = whatsappSelectorExpanded,
                    onDismissRequest = { whatsappSelectorExpanded = false }
                ) {
                    if (knownWhatsAppSources.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No WhatsApp contacts/groups detected yet") },
                            onClick = { whatsappSelectorExpanded = false }
                        )
                    } else {
                        knownWhatsAppSources.forEach { source ->
                            val selected = parseFilterValues(whatsappFilters).any { it.equals(source, ignoreCase = true) }
                            DropdownMenuItem(
                                text = { Text(source) },
                                onClick = {
                                    whatsappFilters = toggleFilterValue(whatsappFilters, source)
                                },
                                trailingIcon = {
                                    androidx.compose.material3.Checkbox(
                                        checked = selected,
                                        onCheckedChange = null
                                    )
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = whatsappFilters,
                onValueChange = { whatsappFilters = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                label = { Text("Manual WhatsApp filter") },
                placeholder = { Text("Project Team, Family, Client A") }
            )
        }

        TrackingFieldCard(
            title = "Message Keywords",
            icon = Icons.Default.CheckCircle,
            description = "If you only want very specific task-like messages, add the keywords here."
        ) {
            OutlinedTextField(
                value = messageKeywords,
                onValueChange = { messageKeywords = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                placeholder = { Text("deadline, submit, call, meeting") }
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.purple)
        ) {
            Text(saveLabel, fontWeight = FontWeight.Bold)
        }
    }
}

private fun parseFilterValues(raw: String): List<String> {
    return raw.split("\n", ",", ";")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

private fun selectedWhatsAppSummary(raw: String): String {
    val values = parseFilterValues(raw)
    return when {
        values.isEmpty() -> "No selection"
        values.size == 1 -> values.first()
        values.size == 2 -> "${values[0]}, ${values[1]}"
        else -> "${values[0]}, ${values[1]} +${values.size - 2} more"
    }
}

private fun toggleFilterValue(raw: String, value: String): String {
    val items = parseFilterValues(raw).toMutableList()
    val existingIndex = items.indexOfFirst { it.equals(value, ignoreCase = true) }
    if (existingIndex >= 0) {
        items.removeAt(existingIndex)
    } else {
        items.add(value)
    }
    return items.joinToString(", ")
}

@Composable
private fun TrackingFieldCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    content: @Composable () -> Unit
) {
    val colors = LocalLiquidColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.55f))
            content()
        }
    }
}

@Composable
private fun TrackingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: com.example.chattaskai.ui.theme.LiquidColors
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Medium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.cyan,
                checkedTrackColor = colors.cyan.copy(alpha = 0.3f)
            )
        )
    }
}
