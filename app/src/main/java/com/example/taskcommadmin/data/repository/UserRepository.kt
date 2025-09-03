package com.example.taskcommadmin.data.repository

import com.example.taskcommadmin.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // Try multiple probable collection names (adjust order if you know the exact name)
    private val candidateCollections = listOf(
        "users", "userProfiles", "profiles", "Users"
    )

    // Debug function to test Firebase connection
    suspend fun testFirebaseConnection(): Boolean {
        return try {
            println("DEBUG: Testing Firebase connection...")
            withTimeout(8000) {
                val testDoc = firestore.collection("test").document("connection")
                testDoc.set(mapOf("timestamp" to com.google.firebase.Timestamp.now())).await()
            }
            println("DEBUG: Firebase connection successful!")
            true
        } catch (e: Exception) {
            println("DEBUG: Firebase connection failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    // Function to test if we can read from the users collection
    suspend fun testUsersCollection(): Boolean {
        return try {
            println("DEBUG: Testing users collection access...")
            val ok = withTimeout(8000) {
                val snapshot = firestore.collection("users").limit(1).get().await()
                println("DEBUG: Users collection access successful! Found ${snapshot.documents.size} documents")
                if (snapshot.documents.isNotEmpty()) {
                    val doc = snapshot.documents.first()
                    println("DEBUG: First document ID: ${doc.id}")
                    println("DEBUG: First document data: ${doc.data}")
                }
                true
            }
            ok
        } catch (e: Exception) {
            println("DEBUG: Users collection access failed: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun getAllUsers(): Flow<List<User>> = flow {
        try {
            println("DEBUG: Fetching users from Firebase (multi-collection fallback)...")

            var loadedUsers: List<User> = emptyList()
            var usedCollection: String? = null

            for (collection in candidateCollections) {
                val baseQuery = firestore.collection(collection)
                val allSnapshot = withTimeout(10000) { baseQuery.get().await() }
                println("DEBUG: [$collection] total docs (no filter): ${allSnapshot.size()}")

                // Prefer filtered by isActive if field exists in docs
                val filteredSnapshot = try {
                    withTimeout(10000) {
                        baseQuery.whereEqualTo("isActive", true)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .get()
                            .await()
                    }
                } catch (t: Throwable) {
                    // If orderBy/where fails due to missing fields, fall back to unfiltered
                    allSnapshot
                }

                val users = filteredSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(userId = doc.id)
                }

                println("DEBUG: [$collection] parsed users count: ${users.size}")
                if (users.isNotEmpty()) {
                    loadedUsers = users
                    usedCollection = collection
                    break
                }

                // If filtered yielded none but unfiltered had docs, try parsing unfiltered
                if (users.isEmpty() && allSnapshot.isEmpty.not()) {
                    val unfilteredUsers = allSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(userId = doc.id)
                    }
                    if (unfilteredUsers.isNotEmpty()) {
                        loadedUsers = unfilteredUsers
                        usedCollection = collection
                        break
                    }
                }
            }

            if (usedCollection == null) {
                println("DEBUG: No users found across candidate collections: $candidateCollections")
            } else {
                println("DEBUG: Loaded ${loadedUsers.size} users from collection '$usedCollection'")
            }

            emit(loadedUsers)
        } catch (e: Exception) {
            println("DEBUG: Error fetching users: ${e.message}")
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            // Try each candidate collection until found
            for (collection in candidateCollections) {
                val doc = withTimeout(8000) { firestore.collection(collection).document(userId).get().await() }
                val user = doc.toObject(User::class.java)?.copy(userId = doc.id)
                if (user != null) return user
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateUser(user: User): Boolean {
        return try {
            // Default to first collection; adjust if you know the exact one
            withTimeout(8000) {
                firestore.collection(candidateCollections.first())
                    .document(user.userId)
                    .set(user)
                    .await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteUser(userId: String): Boolean {
        return try {
            // Soft delete by isActive=false; try first collection
            withTimeout(8000) {
                firestore.collection(candidateCollections.first())
                    .document(userId)
                    .update("isActive", false)
                    .await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun searchUsers(query: String): List<User> {
        return try {
            for (collection in candidateCollections) {
                val snapshot = withTimeout(8000) { firestore.collection(collection).get().await() }
                val matches = snapshot.documents.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)?.copy(userId = doc.id)
                    if (user?.name?.contains(query, ignoreCase = true) == true ||
                        user?.businessField?.contains(query, ignoreCase = true) == true ||
                        user?.email?.contains(query, ignoreCase = true) == true) {
                        user
                    } else null
                }
                if (matches.isNotEmpty()) return matches
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
