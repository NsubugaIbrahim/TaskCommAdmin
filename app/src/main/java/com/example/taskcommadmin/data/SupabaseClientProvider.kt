package com.example.taskcommadmin.data

import android.content.Context
import com.example.taskcommadmin.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseClientProvider {
	@Volatile private var client: SupabaseClient? = null

	fun getClient(context: Context): SupabaseClient {
		val existing = client
		if (existing != null) return existing
		val created = createSupabaseClient(
			BuildConfig.SUPABASE_URL,
			BuildConfig.SUPABASE_ANON_KEY
		) {
			httpEngine = OkHttp.create()
			install(Auth)
			install(Postgrest)
		}
		client = created
		return created
	}
}
