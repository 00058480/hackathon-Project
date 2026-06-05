package com.example.hackathonproject.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hackathonproject.ui.billsplitter.BillSplitterScreen
import com.example.hackathonproject.ui.billsplitter.BillSplitterViewModel
import com.example.hackathonproject.AppViewModelProvider
import com.example.hackathonproject.ui.scan.ReceiptScanScreen
import com.example.hackathonproject.ui.history.BillDetailScreen
import com.example.hackathonproject.ui.history.HistoryScreen
import com.example.hackathonproject.ui.people.PeopleScreen
import com.example.hackathonproject.ui.people.PersonEditScreen
import com.example.hackathonproject.ui.welcome.WelcomeScreen

private data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.SPLITTER, "Счёт", Icons.Default.Calculate),
    TopLevelDestination(Routes.HISTORY, "История", Icons.AutoMirrored.Filled.ReceiptLong),
    TopLevelDestination(Routes.PEOPLE, "Люди", Icons.Default.Group)
)

@Composable
fun BillSplitterApp() {
    var showWelcome by rememberSaveable { mutableStateOf(true) }
    if (showWelcome) {
        WelcomeScreen(onFinished = { showWelcome = false })
    } else {
        MainScaffold()
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topLevelDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.navigateTopLevel(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLITTER,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.SPLITTER) {
                BillSplitterScreen(
                    onBillSaved = { navController.navigateTopLevel(Routes.HISTORY) },
                    onScanReceipt = { navController.navigate(Routes.SCAN) }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onOpenBill = { navController.navigate(Routes.billDetail(it)) })
            }
            composable(Routes.PEOPLE) {
                PeopleScreen(
                    onAddPerson = { navController.navigate(Routes.personEdit(0)) },
                    onEditPerson = { navController.navigate(Routes.personEdit(it)) }
                )
            }
            composable(
                route = Routes.PERSON_EDIT,
                arguments = listOf(navArgument("personId") { type = NavType.LongType })
            ) { entry ->
                PersonEditScreen(
                    personId = entry.arguments?.getLong("personId") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.BILL_DETAIL,
                arguments = listOf(navArgument("billId") { type = NavType.LongType })
            ) { entry ->
                BillDetailScreen(
                    billId = entry.arguments?.getLong("billId") ?: 0L,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SCAN) { entry ->
                // Share the splitter's ViewModel so imported items land on the bill being edited.
                val splitterEntry = remember(entry) { navController.getBackStackEntry(Routes.SPLITTER) }
                val billViewModel: BillSplitterViewModel =
                    viewModel(splitterEntry, factory = AppViewModelProvider.Factory)
                ReceiptScanScreen(
                    onBack = { navController.popBackStack() },
                    onImport = { items ->
                        billViewModel.addParsedItems(items)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
