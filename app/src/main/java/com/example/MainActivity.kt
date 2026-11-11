package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.local.entity.UserProfileEntity
import com.example.ui.StudyBloomApp
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.StudyBloomTheme
import com.example.ui.viewmodel.StudyBloomViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge with transparent system status bar and navigation bar with dark icons
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            StudyBloomTheme {
                StudyBloomNavApp()
            }
        }
    }
}

@Composable
fun StudyBloomNavApp() {
    val navController = rememberNavController()
    val viewModel: StudyBloomViewModel = viewModel()
    val userProfile by viewModel.userProfile.collectAsState()

    val startDestination = if (userProfile?.hasCompletedOnboarding == true) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onContinueAsGuest = {
                    val guestProfile = (userProfile ?: UserProfileEntity()).copy(
                        isGuest = true,
                        hasCompletedOnboarding = true
                    )
                    viewModel.completeOnboarding(guestProfile)
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onCreateAccount = {
                    navController.navigate("signup")
                },
                onSignIn = {
                    navController.navigate("signin")
                }
            )
        }

        composable("home") {
            StudyBloomApp(viewModel = viewModel)
        }

        composable("signup") {
            StudyBloomApp(
                viewModel = viewModel,
                openAuthOnStart = true,
                initialIsRegister = true
            )
        }

        composable("signin") {
            StudyBloomApp(
                viewModel = viewModel,
                openAuthOnStart = true,
                initialIsRegister = false
            )
        }
    }
}
