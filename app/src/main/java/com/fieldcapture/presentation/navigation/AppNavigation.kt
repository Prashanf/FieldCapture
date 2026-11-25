package com.fieldcapture.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fieldcapture.presentation.jobrecord.JobRecordScreen
import com.fieldcapture.presentation.review.ReviewScreen
import com.fieldcapture.presentation.session.SessionScreen

object Routes {
    const val SESSION = "session"
    const val REVIEW = "review/{jobId}"
    const val JOB_RECORD = "job_record/{jobId}"

    fun review(jobId: String) = "review/$jobId"
    fun jobRecord(jobId: String) = "job_record/$jobId"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.SESSION
    ) {
        composable(Routes.SESSION) {
            SessionScreen(
                onNavigateToReview = { jobId ->
                    navController.navigate(Routes.review(jobId))
                }
            )
        }

        composable(
            route = Routes.REVIEW,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) {
            ReviewScreen(
                onNavigateToJobRecord = { jobId ->
                    navController.navigate(Routes.jobRecord(jobId))
                }
            )
        }

        composable(
            route = Routes.JOB_RECORD,
            arguments = listOf(navArgument("jobId") { type = NavType.StringType })
        ) {
            JobRecordScreen()
        }
    }
}
