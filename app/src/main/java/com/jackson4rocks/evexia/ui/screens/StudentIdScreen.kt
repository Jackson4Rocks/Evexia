package com.jackson4rocks.evexia.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Emergency
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.data.AppPreferences
import com.jackson4rocks.evexia.data.IdentityField
import com.jackson4rocks.evexia.data.SubjectEntity
import com.jackson4rocks.evexia.ui.components.AppTextField
import com.jackson4rocks.evexia.ui.components.GlassSheet
import com.jackson4rocks.evexia.ui.components.GlassSurface
import com.jackson4rocks.evexia.ui.components.Icon
import com.jackson4rocks.evexia.ui.components.InfoRow
import com.jackson4rocks.evexia.ui.components.PagePadding
import com.jackson4rocks.evexia.ui.components.PersonAvatar
import com.jackson4rocks.evexia.ui.components.PrimaryButton
import com.jackson4rocks.evexia.ui.components.ScreenHeader
import com.jackson4rocks.evexia.ui.components.StudentIdCard
import com.jackson4rocks.evexia.ui.components.SecondaryButton
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.util.FileStore
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun StudentIdScreen(
    preferences: AppPreferences,
    subjects: List<SubjectEntity>,
    onBack: () -> Unit,
    onSaveFields: (Map<IdentityField, String>) -> Unit,
    onSaveProfileImage: (String?) -> Unit,
    onSaveSchoolLogo: (String?) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    var editing by remember { mutableStateOf(false) }
    val hasDetails = listOf(
        preferences.schoolName,
        preferences.identityNumber,
        preferences.program,
        preferences.year,
        preferences.schoolEmail,
        preferences.bloodGroup,
        preferences.emergencyContact,
    ).any { it.isNotBlank() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp),
    ) {
        ScreenHeader(
            title = "Student ID",
            onBack = onBack,
            eyebrow = "Optional · stays on this device",
            actions = {
                SecondaryButton(
                    label = "Edit",
                    onClick = { editing = true },
                    icon = Icons.Filled.Edit,
                    modifier = Modifier.width(88.dp),
                )
            },
        )
        Spacer(Modifier.height(14.dp))
        StudentIdCard(
            preferences = preferences,
            modifier = Modifier.fillMaxWidth().padding(horizontal = PagePadding),
        )
        Spacer(Modifier.height(13.dp))
        if (!hasDetails) {
            GlassSurface(
                Modifier.fillMaxWidth().padding(horizontal = PagePadding),
                accentTint = true,
                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Badge, null, tint = colors.accentInk, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            "Make it yours",
                            style = type.cardTitle.copy(color = colors.textPrimary),
                        )
                        BasicText(
                            "Add your name, school and favourite subjects. Nothing leaves your phone.",
                            style = type.caption.copy(color = colors.textSecondary),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                "Your details",
                style = type.sectionTitle.copy(color = colors.textPrimary),
            )
            Spacer(Modifier.weight(1f))
            BasicText(
                "PRIVATE",
                style = type.label.copy(color = colors.textTertiary, fontSize = 8.sp),
            )
        }
        Spacer(Modifier.height(7.dp))
        GlassSurface(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
        ) {
            Column {
                InfoRow(
                    icon = Icons.Filled.School,
                    title = "School",
                    subtitle = preferences.schoolName.ifBlank { "Not added" },
                )
                InfoRow(
                    icon = Icons.Outlined.MenuBook,
                    title = "Favourite Subjects",
                    subtitle = preferences.program.ifBlank { "Not added" },
                )
                InfoRow(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "Year",
                    subtitle = preferences.year.ifBlank { "Not added" },
                )
                InfoRow(
                    icon = Icons.Outlined.Badge,
                    title = "Student number",
                    subtitle = preferences.identityNumber.ifBlank { "Not added" },
                )
                InfoRow(
                    icon = Icons.Outlined.Email,
                    title = "School email",
                    subtitle = preferences.schoolEmail.ifBlank { "Not added" },
                )
                InfoRow(
                    icon = Icons.Outlined.Bloodtype,
                    title = "Blood group",
                    subtitle = preferences.bloodGroup.ifBlank { "Not added" },
                )
                InfoRow(
                    icon = Icons.Outlined.Phone,
                    title = "Emergency contact",
                    subtitle = preferences.emergencyContact.ifBlank { "Not added" },
                )
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = PagePadding, vertical = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Outlined.Emergency,
                null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp).padding(top = 1.dp),
            )
            Spacer(Modifier.width(7.dp))
            BasicText(
                text = "This card is a personal digital copy, not an official institutional credential. It is never uploaded or shared automatically.",
                style = type.caption.copy(color = colors.textTertiary, fontSize = 10.5.sp),
            )
        }
        Spacer(Modifier.height(18.dp))
    }

    if (editing) {
        StudentIdEditSheet(
            preferences = preferences,
            onDismiss = { editing = false },
            onSaveFields = {
                editing = false
                onSaveFields(it)
            },
            onSaveProfileImage = onSaveProfileImage,
            onSaveSchoolLogo = onSaveSchoolLogo,
        )
    }
}

@Composable
private fun StudentIdEditSheet(
    preferences: AppPreferences,
    onDismiss: () -> Unit,
    onSaveFields: (Map<IdentityField, String>) -> Unit,
    onSaveProfileImage: (String?) -> Unit,
    onSaveSchoolLogo: (String?) -> Unit,
) {
    val colors = DF.colors
    val type = DF.type
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(preferences.displayName) }
    var school by rememberSaveable { mutableStateOf(preferences.schoolName) }
    var idNumber by rememberSaveable { mutableStateOf(preferences.identityNumber) }
    var program by rememberSaveable { mutableStateOf(preferences.program) }
    var year by rememberSaveable { mutableStateOf(preferences.year) }
    var email by rememberSaveable { mutableStateOf(preferences.schoolEmail) }
    var blood by rememberSaveable { mutableStateOf(preferences.bloodGroup) }
    var emergency by rememberSaveable { mutableStateOf(preferences.emergencyContact) }
    var profilePath by rememberSaveable { mutableStateOf(preferences.profileImagePath) }
    var logoPath by rememberSaveable { mutableStateOf(preferences.schoolLogoPath) }
    var uploading by remember { mutableStateOf(false) }

    val profilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                uploading = true
                val file = FileStore.copyProfileImage(context, uri, "student")
                uploading = false
                if (file != null) {
                    profilePath = file.absolutePath
                    onSaveProfileImage(file.absolutePath)
                }
            }
        }
    }
    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                uploading = true
                val file = FileStore.copyProfileImage(context, uri, "school")
                uploading = false
                if (file != null) {
                    logoPath = file.absolutePath
                    onSaveSchoolLogo(file.absolutePath)
                }
            }
        }
    }

    GlassSheet(
        title = "Edit student card",
        subtitle = "All fields are optional. Everything stays in local storage.",
        onDismiss = onDismiss,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonAvatar(name = name, path = profilePath, size = 54.dp, fill = colors.accent)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                BasicText("Your photo", style = type.cardTitle.copy(color = colors.textPrimary))
                BasicText("Used only on this card", style = type.caption.copy(color = colors.textSecondary))
            }
            SecondaryButton(
                label = if (profilePath == null) "Add" else "Change",
                onClick = { profilePicker.launch("image/*") },
                modifier = Modifier.width(86.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        AppTextField(
            value = name,
            onValueChange = { if (it.length <= 60) name = it },
            label = "Display name",
            placeholder = "Your name",
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = school,
            onValueChange = { if (it.length <= 80) school = it },
            label = "School",
            placeholder = "School or university",
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = idNumber,
            onValueChange = { if (it.length <= 40) idNumber = it },
            label = "Student number",
            placeholder = "e.g. 2026-00142",
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = program,
            onValueChange = { if (it.length <= 80) program = it },
            label = "Favourite Subjects",
            placeholder = "e.g. Mathematics, Physics",
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = year,
            onValueChange = { if (it.length <= 30) year = it },
            label = "Year / level",
            placeholder = "e.g. Year 2",
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = email,
            onValueChange = { if (it.length <= 100) email = it },
            label = "School email",
            placeholder = "you@school.edu",
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = blood,
            onValueChange = { if (it.length <= 8) blood = it },
            label = "Blood group (optional)",
            placeholder = "e.g. O+",
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = emergency,
            onValueChange = { if (it.length <= 40) emergency = it },
            label = "Emergency contact (optional)",
            placeholder = "Name and number",
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done,
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonAvatar(name = school, path = logoPath, size = 38.dp, fill = colors.accentSoft)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                BasicText("School logo", style = type.cardTitle.copy(color = colors.textPrimary, fontSize = 13.sp))
                BasicText(
                    if (logoPath == null) "Optional, shown small on the card" else "Logo added",
                    style = type.caption.copy(color = colors.textSecondary),
                )
            }
            if (logoPath != null) {
                SecondaryButton(
                    label = "Remove",
                    onClick = {
                        logoPath = null
                        onSaveSchoolLogo(null)
                    },
                    modifier = Modifier.width(80.dp),
                    danger = true,
                )
                Spacer(Modifier.width(6.dp))
            }
            SecondaryButton(
                label = if (logoPath == null) "Add" else "Change",
                onClick = { logoPicker.launch("image/*") },
                modifier = Modifier.width(80.dp),
            )
        }
        if (uploading) {
            Spacer(Modifier.height(8.dp))
            BasicText("Preparing image…", style = type.caption.copy(color = colors.textTertiary))
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            label = "Save card",
            onClick = {
                onSaveFields(
                    mapOf(
                        IdentityField.NAME to name.trim(),
                        IdentityField.SCHOOL to school.trim(),
                        IdentityField.ID_NUMBER to idNumber.trim(),
                        IdentityField.PROGRAM to program.trim(),
                        IdentityField.YEAR to year.trim(),
                        IdentityField.SCHOOL_EMAIL to email.trim(),
                        IdentityField.BLOOD_GROUP to blood.trim(),
                        IdentityField.EMERGENCY_CONTACT to emergency.trim(),
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (preferences.profileImagePath != null) {
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                label = "Remove profile photo",
                onClick = {
                    profilePath = null
                    onSaveProfileImage(null)
                },
                modifier = Modifier.fillMaxWidth(),
                danger = true,
            )
        }
        Spacer(Modifier.height(4.dp))
        BasicText(
            text = "Your card data is included in Evexia's local backup only if you enable device backup.",
            style = type.caption.copy(color = colors.textTertiary, fontSize = 9.5.sp),
        )
    }
}
