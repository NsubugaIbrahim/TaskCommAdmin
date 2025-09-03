package com.example.taskcommadmin

import android.app.Application
import com.google.firebase.FirebaseApp

class TaskCommAdminApplication : Application() {
	override fun onCreate() {
		super.onCreate()
		// Ensure Firebase is initialized early
		try {
			FirebaseApp.initializeApp(this)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
}
