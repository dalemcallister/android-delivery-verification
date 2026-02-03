package com.connexi.deliveryverification.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.connexi.deliveryverification.ui.delivery.DeliveryVerificationScreen
import com.connexi.deliveryverification.ui.login.LoginScreen
import com.connexi.deliveryverification.ui.route_detail.RouteDetailScreen
import com.connexi.deliveryverification.ui.routes.RoutesScreen
import com.connexi.deliveryverification.ui.sync.SyncScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Routes : Screen("routes")
    object RouteDetail : Screen("route_detail/{routeId}") {
        fun createRoute(routeId: String) = "route_detail/$routeId"
    }
    object DeliveryVerification : Screen("delivery_verification/{deliveryId}") {
        fun createRoute(deliveryId: String) = "delivery_verification/$deliveryId"
    }
    object Sync : Screen("sync")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Routes.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Routes.route) {
            RoutesScreen(
                onRouteClick = { routeId ->
                    navController.navigate(Screen.RouteDetail.createRoute(routeId))
                },
                onSyncClick = {
                    navController.navigate(Screen.Sync.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Routes.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.RouteDetail.route,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments?.getString("routeId") ?: return@composable
            RouteDetailScreen(
                routeId = routeId,
                onBackClick = { navController.popBackStack() },
                onDeliveryClick = { deliveryId ->
                    navController.navigate(Screen.DeliveryVerification.createRoute(deliveryId))
                }
            )
        }

        composable(
            route = Screen.DeliveryVerification.route,
            arguments = listOf(navArgument("deliveryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId") ?: return@composable
            DeliveryVerificationScreen(
                deliveryId = deliveryId,
                onBackClick = { navController.popBackStack() },
                onVerificationComplete = { navController.popBackStack() }
            )
        }

        composable(Screen.Sync.route) {
            SyncScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
