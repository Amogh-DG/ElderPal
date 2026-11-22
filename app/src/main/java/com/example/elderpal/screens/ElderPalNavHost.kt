// Navigation host defining all application routes and transitions between screens.
package com.example.elderpal.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.elderpal.ai.CapturedImageHolder
import com.example.elderpal.ai.CheckResultType
import com.example.elderpal.data.DoseReminderScheduler
import com.example.elderpal.data.MedicineRepository
import com.example.elderpal.data.PermissionManager
import kotlinx.coroutines.launch

object ElderPalRoutes {
    const val HOME = "home"
    const val SCHEDULE = "schedule"
    const val ADD_MEDICINE = "add_medicine"
    const val CHECK_MEDICINE = "check_medicine"
    const val CONTACTS = "contacts"
    const val SOS = "sos"
    const val FALL_ALERT = "fall_alert"
    const val PERMISSIONS = "permissions"
    const val SCAN_PRESCRIPTION = "scan_prescription"
    const val REMINDER = "reminder"
    const val CHECK_MEDICINE_LOADING = "check_medicine_loading"
    const val RESULT_OK = "result_ok"
    const val RESULT_WRONG = "result_wrong"
    const val RESULT_NOT_DUE = "result_not_due"
    const val RESULT_NOT_DUE_CONFIRM = "result_not_due_confirm"
    const val RESULT_UNSURE = "result_unsure"
    const val ADD_CONTACT = "add_contact"
    const val ALERT_PREVIEW = "alert_preview"
    const val DEMO_TOOLS = "demo_tools"
    const val FIRST_RUN_PERMISSIONS = "first_run_permissions"
}

@Composable
fun ElderPalNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val startDestination = remember {
        val activity = context as? android.app.Activity
        if (activity?.intent?.action == com.example.elderpal.data.FallDetectionService.ACTION_FALL_ALERT) {
            ElderPalRoutes.FALL_ALERT
        } else if (!PermissionManager.isFirstRunCompleted(context)) {
            ElderPalRoutes.FIRST_RUN_PERMISSIONS
        } else {
            ElderPalRoutes.HOME
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(ElderPalRoutes.FIRST_RUN_PERMISSIONS) {
            FirstRunPermissionsScreen(
                onComplete = {
                    navController.navigate(ElderPalRoutes.HOME) {
                        popUpTo(ElderPalRoutes.FIRST_RUN_PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }

        composable(ElderPalRoutes.HOME) {
            HomeScreen(
                onScheduleClick = {
                    navController.navigate(ElderPalRoutes.SCHEDULE)
                },
                onScanPrescriptionClick = {
                    navController.navigate(ElderPalRoutes.SCAN_PRESCRIPTION)
                },
                onCheckMedicineClick = {
                    navController.navigate(ElderPalRoutes.CHECK_MEDICINE)
                },
                onEmergencyContactsClick = {
                    navController.navigate(ElderPalRoutes.CONTACTS)
                },
                onSosClick = {
                    navController.navigate(ElderPalRoutes.SOS)
                },
                onLogoClick = {
                    navController.navigate(ElderPalRoutes.PERMISSIONS)
                }
            )
        }

        composable(ElderPalRoutes.SCHEDULE) {
            ScheduleScreen(
                onBack = { navController.popBackStack() },
                onAddMedicineClick = {
                    navController.navigate(ElderPalRoutes.ADD_MEDICINE)
                },
                onSosClick = {
                    navController.navigate(ElderPalRoutes.SOS)
                }
            )
        }

        composable(ElderPalRoutes.ADD_MEDICINE) {
            AddMedicineScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onSosClick = {
                    navController.navigate(ElderPalRoutes.SOS)
                }
            )
        }

        composable(ElderPalRoutes.CHECK_MEDICINE) {
            CheckMedicineScreen(
                onBack = { navController.popBackStack() },
                onUsePhotoClick = { _ ->
                    navController.navigate(ElderPalRoutes.CHECK_MEDICINE_LOADING)
                }
            )
        }

        composable(ElderPalRoutes.CHECK_MEDICINE_LOADING) {
            CheckMedicineLoadingScreen(
                onResultReady = { result ->
                    when (result.type) {
                        CheckResultType.OK -> {
                            navController.navigate(ElderPalRoutes.RESULT_OK) {
                                popUpTo(ElderPalRoutes.CHECK_MEDICINE) { inclusive = true }
                            }
                        }
                        CheckResultType.WRONG -> {
                            navController.navigate(ElderPalRoutes.RESULT_WRONG) {
                                popUpTo(ElderPalRoutes.CHECK_MEDICINE) { inclusive = true }
                            }
                        }
                        CheckResultType.NOT_DUE -> {
                            navController.navigate(ElderPalRoutes.RESULT_NOT_DUE) {
                                popUpTo(ElderPalRoutes.CHECK_MEDICINE) { inclusive = true }
                            }
                        }
                        CheckResultType.UNSURE -> {
                            navController.navigate(ElderPalRoutes.RESULT_UNSURE) {
                                popUpTo(ElderPalRoutes.CHECK_MEDICINE) { inclusive = true }
                            }
                        }
                    }
                },
                onCancelClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(ElderPalRoutes.RESULT_OK) {
            val context = LocalContext.current
            val repository = remember { MedicineRepository(context) }
            val coroutineScope = rememberCoroutineScope()
            val result = CapturedImageHolder.latestResult
            val medName = result?.matchedMedicine?.name ?: result?.detectedLabelName ?: "Medicine"
            val dose = result?.matchedMedicine?.dose ?: "1 tablet"
            val scheduledTime = result?.matchedScheduledTime ?: ""

            ResultOkScreen(
                medicineName = medName,
                dose = dose,
                onBack = {
                    CapturedImageHolder.clear()
                    navController.popBackStack()
                },
                onMarkAsTakenClick = {
                    val medId = result?.matchedMedicine?.id
                    if (medId != null && scheduledTime.isNotBlank()) {
                        coroutineScope.launch {
                            repository.markDoseTaken(medId, medName, scheduledTime)
                            CapturedImageHolder.clear()
                            navController.navigate(ElderPalRoutes.SCHEDULE) {
                                popUpTo(ElderPalRoutes.HOME)
                            }
                        }
                    } else {
                        CapturedImageHolder.clear()
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(ElderPalRoutes.RESULT_WRONG) {
            ResultWrongScreen(
                onBack = {
                    CapturedImageHolder.clear()
                    navController.popBackStack()
                },
                onCallFamilyClick = {
                    navController.navigate(ElderPalRoutes.CONTACTS)
                },
                onCheckAnotherClick = {
                    CapturedImageHolder.clear()
                    navController.navigate(ElderPalRoutes.CHECK_MEDICINE) {
                        popUpTo(ElderPalRoutes.HOME)
                    }
                }
            )
        }

        composable(ElderPalRoutes.RESULT_NOT_DUE) {
            val result = CapturedImageHolder.latestResult
            val medName = result?.matchedMedicine?.name ?: result?.detectedLabelName ?: "Medicine"
            val dose = result?.matchedMedicine?.dose ?: "1 tablet"
            val scheduledTime = result?.matchedScheduledTime ?: ""

            ResultNotDueScreen(
                medicineName = medName,
                dose = dose,
                scheduledTime = scheduledTime,
                onBack = {
                    CapturedImageHolder.clear()
                    navController.popBackStack()
                },
                onWaitClick = {
                    CapturedImageHolder.clear()
                    navController.navigate(ElderPalRoutes.SCHEDULE) {
                        popUpTo(ElderPalRoutes.HOME)
                    }
                },
                onTakeAnywayClick = {
                    navController.navigate(ElderPalRoutes.RESULT_NOT_DUE_CONFIRM)
                }
            )
        }

        composable(ElderPalRoutes.RESULT_NOT_DUE_CONFIRM) {
            val context = LocalContext.current
            val repository = remember { MedicineRepository(context) }
            val coroutineScope = rememberCoroutineScope()
            val result = CapturedImageHolder.latestResult
            val medName = result?.matchedMedicine?.name ?: result?.detectedLabelName ?: "Medicine"
            val scheduledTime = result?.matchedScheduledTime ?: ""

            ResultNotDueConfirmScreen(
                medicineName = medName,
                scheduledTime = scheduledTime,
                onBack = { navController.popBackStack() },
                onConfirmTakeClick = {
                    val medId = result?.matchedMedicine?.id
                    if (medId != null && scheduledTime.isNotBlank()) {
                        coroutineScope.launch {
                            repository.markDoseTaken(medId, medName, scheduledTime)
                            CapturedImageHolder.clear()
                            navController.navigate(ElderPalRoutes.SCHEDULE) {
                                popUpTo(ElderPalRoutes.HOME)
                            }
                        }
                    } else {
                        CapturedImageHolder.clear()
                        navController.popBackStack()
                    }
                }
            )
        }

        composable(ElderPalRoutes.RESULT_UNSURE) {
            val result = CapturedImageHolder.latestResult
            ResultUnsureScreen(
                reason = result?.reason ?: "",
                onBack = {
                    CapturedImageHolder.clear()
                    navController.popBackStack()
                },
                onTryAgainClick = {
                    CapturedImageHolder.clear()
                    navController.navigate(ElderPalRoutes.CHECK_MEDICINE) {
                        popUpTo(ElderPalRoutes.HOME)
                    }
                }
            )
        }

        composable(ElderPalRoutes.CONTACTS) {
            ContactsScreen(
                onBack = { navController.popBackStack() },
                onAddContactClick = {
                    navController.navigate(ElderPalRoutes.ADD_CONTACT)
                },
                onEditContactClick = { contactId ->
                    navController.navigate("${ElderPalRoutes.ADD_CONTACT}?contactId=$contactId")
                },
                onPreviewAlertClick = {
                    navController.navigate(ElderPalRoutes.ALERT_PREVIEW)
                },
                onSosClick = {
                    navController.navigate(ElderPalRoutes.SOS)
                }
            )
        }

        composable(ElderPalRoutes.ALERT_PREVIEW) {
            AlertPreviewScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${ElderPalRoutes.ADD_CONTACT}?contactId={contactId}",
            arguments = listOf(
                navArgument("contactId") {
                    defaultValue = ""
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val contactId = backStackEntry.arguments?.getString("contactId")
            AddContactScreen(
                contactId = contactId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
                onSosClick = {
                    navController.navigate(ElderPalRoutes.SOS)
                }
            )
        }

        composable(ElderPalRoutes.SOS) {
            SosScreen(
                onBack = { navController.popBackStack() },
                onNavigateToContacts = { navController.navigate(ElderPalRoutes.CONTACTS) }
            )
        }

        composable(ElderPalRoutes.FALL_ALERT) {
            FallAlertScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(ElderPalRoutes.HOME) {
                            popUpTo(0)
                        }
                    }
                },
                onNavigateToContacts = { navController.navigate(ElderPalRoutes.CONTACTS) }
            )
        }

        composable(ElderPalRoutes.PERMISSIONS) {
            PermissionsScreen(
                onBack = { navController.popBackStack() },
                onDemoToolsClick = { navController.navigate(ElderPalRoutes.DEMO_TOOLS) }
            )
        }

        composable(ElderPalRoutes.DEMO_TOOLS) {
            DemoToolsScreen(
                onBack = { navController.popBackStack() },
                onSimulateFallClick = {
                    navController.navigate(ElderPalRoutes.FALL_ALERT)
                }
            )
        }

        composable(ElderPalRoutes.SCAN_PRESCRIPTION) {
            ScanPrescriptionScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.navigate(ElderPalRoutes.SCHEDULE) {
                        popUpTo(ElderPalRoutes.HOME)
                    }
                }
            )
        }

        composable(
            route = "${ElderPalRoutes.REMINDER}?medicineId={medicineId}&medicineName={medicineName}&dose={dose}&time={time}",
            arguments = listOf(
                navArgument("medicineId") { defaultValue = "" },
                navArgument("medicineName") { defaultValue = "Medicine" },
                navArgument("dose") { defaultValue = "" },
                navArgument("time") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("medicineId") ?: ""
            val medicineName = backStackEntry.arguments?.getString("medicineName") ?: "Medicine"
            val dose = backStackEntry.arguments?.getString("dose") ?: ""
            val time = backStackEntry.arguments?.getString("time") ?: ""

            val context = LocalContext.current
            ReminderScreen(
                medicineName = medicineName,
                dose = dose,
                scheduledTime = time,
                onCheckMedicineClick = {
                    navController.navigate(ElderPalRoutes.CHECK_MEDICINE)
                },
                onSnoozeClick = {
                    DoseReminderScheduler.scheduleSnooze(
                        context = context,
                        medicineId = medicineId,
                        medicineName = medicineName,
                        dose = dose,
                        scheduledTime = time,
                        delayMinutes = 10
                    )
                    navController.popBackStack()
                }
            )
        }
    }
}
