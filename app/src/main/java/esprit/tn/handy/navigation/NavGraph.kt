package esprit.tn.handy.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import esprit.tn.handy.data.Animal
import esprit.tn.handy.ui.screens.LearnScreen
import esprit.tn.handy.ui.screens.MainMenuScreen
import esprit.tn.handy.ui.screens.RanQuizScreen
import esprit.tn.handy.ui.screens.TestScreen
import esprit.tn.handy.ui.screens.TrainScreen

sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object Learn : Screen("learn")
    object Test : Screen("test")
    object Train : Screen("train")
    object RanQuiz : Screen("ran_quiz")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route
    ) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                onLearnClick = { navController.navigate(Screen.Learn.route) },
                onTestClick = { navController.navigate(Screen.Test.route) },
                onTrainClick = { navController.navigate(Screen.Train.route) },
                onRanQuizClick = { navController.navigate(Screen.RanQuiz.route) }
            )
        }
        
        composable(Screen.Learn.route) {
            LearnScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Test.route) {
            TestScreen(
                onBack = { navController.popBackStack() },
                onComplete = { animalsToReview ->
                    // Ne pas naviguer automatiquement - l'utilisateur reste sur les résultats
                    // La navigation vers Train se fera manuellement depuis le menu si nécessaire
                }
            )
        }
        
        composable(Screen.Train.route) {
            TrainScreen(
                animalsToTrain = emptyList(), // Sera rempli depuis Test si nécessaire
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.RanQuiz.route) {
            RanQuizScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

