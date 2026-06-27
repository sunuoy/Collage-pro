package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            // Check if already initialized by the Google Services plugin automatically
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                isInitialized = true
                Log.d(TAG, "Firebase initialized automatically.")
                return
            }

            // Programmatic fallback options to allow compiling/running safely without google-services.json
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:554238129038:android:a168b4e72c5a03d7")
                .setApiKey("AIzaSyB3v-k8eP3xR2t1q7v6u5z4w2y1b0c9")
                .setProjectId("picollage-cloud-sync")
                .build()

            FirebaseApp.initializeApp(context, options)
            isInitialized = true
            Log.d(TAG, "Firebase initialized programmatically via fallback.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during Firebase initialization: ${e.message}", e)
        }
    }

    fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Could not retrieve Firestore instance: ${e.message}")
            null
        }
    }
}

suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitTask(): T = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: RuntimeException("Firebase Task failed"))
        }
    }
}
