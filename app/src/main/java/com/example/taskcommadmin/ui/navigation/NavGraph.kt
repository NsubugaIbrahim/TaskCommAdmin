package com.example.taskcommadmin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.taskcommadmin.ui.screen.auth.LoginScreen
import com.example.taskcommadmin.ui.screen.chat.ChatScreen
import com.example.taskcommadmin.ui.screen.dashboard.DashboardScreen
import com.example.taskcommadmin.ui.screen.task.TaskDetailScreen
import com.example.taskcommadmin.ui.screen.task.TaskListScreen
import com.example.taskcommadmin.ui.screen.user.UserDetailScreen
import com.example.taskcommadmin.ui.screen.user.UserListScreen
import com.example.taskcommadmin.ui.screen.instruction.InstructionListScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController)
        }
        
        composable(Screen.UserList.route) {
            UserListScreen(navController = navController)
        }
        
        composable(
            route = Screen.UserDetail.route + "/{userId}"
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            UserDetailScreen(
                navController = navController,
                userId = userId
            )
        }
        
        composable(
            route = Screen.TaskList.route + "/{userId}"
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            TaskListScreen(
                navController = navController,
                instructionId = userId
            )
        }

        composable(
            route = Screen.InstructionList.route + "/{userId}"
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            InstructionListScreen(
                navController = navController,
                userId = userId
            )
        }
        
        composable(
            route = Screen.TaskDetail.route + "/{taskId}"
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailScreen(
                navController = navController,
                taskId = taskId
            )
        }
        
        composable(
            route = Screen.Chat.route + "/{taskId}"
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            ChatScreen(
                navController = navController,
                taskId = taskId
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object UserList : Screen("user_list")
    object UserDetail : Screen("user_detail")
    object TaskList : Screen("task_list")
    object TaskDetail : Screen("task_detail")
    object Chat : Screen("chat")
    object InstructionList : Screen("instruction_list")
}
