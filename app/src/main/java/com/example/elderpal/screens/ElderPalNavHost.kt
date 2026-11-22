// Navigation host defining all application routes and transitions between screens.
package com.example.elderpal.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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
}

@Composable
fun ElderPalNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ElderPalRoutes.HOME,
        modifier = modifier
    ) {
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(ElderPalRoutes.CONTACTS) {
            ContactsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(ElderPalRoutes.SOS) {
            SosScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(ElderPalRoutes.FALL_ALERT) {
            FallAlertScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(ElderPalRoutes.PERMISSIONS) {
            PermissionsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(ElderPalRoutes.SCAN_PRESCRIPTION) {
            ScanPrescriptionScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
