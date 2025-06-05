package com.example.auxshort

import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val token = getToken()
        if (token != null){
            fetchUrls(token)
        }else{
            Toast.makeText(this,"Token nya gaada",Toast.LENGTH_LONG).show()
        }

    }

    private fun fetchUrls(token: String) {
        val apiService = RetrofitClient.instance
         val call = apiService.getUrls("Bearer $token") // Tidak perlu mengirim token di sini jika menggunakan AuthInterceptor

        call.enqueue(object : Callback<UrlResponse> { // Pastikan UrlResponse adalah tipe yang benar
            override fun onResponse(call: Call<UrlResponse>, response: Response<UrlResponse>) {
                if (response.isSuccessful) {
                    val urlsResponse = response.body()
                    if (urlsResponse?.urls != null) {
                        urlsResponse.urls.forEach { urlItem ->
                            Log.d(TAG, "Original: ${urlItem.original}, Shortened: ${urlItem.shortened}")
                        }
                        if (urlsResponse.urls.isEmpty()) {
                            Toast.makeText(this@MainActivity, "Tidak ada URL yang ditemukan.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.w(TAG, "Respons berhasil tetapi body kosong atau tidak ada list URLs.")
                        Toast.makeText(this@MainActivity, "Tidak ada data URL diterima.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Gagal mendapatkan URLs. Kode: ${response.code()}, Pesan: ${response.message()}, Error Body: $errorBody")
                    Toast.makeText(this@MainActivity, "Gagal memuat data: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<UrlResponse>, t: Throwable) {
                Log.e(TAG, "Panggilan API gagal: ${t.message}", t)
                Toast.makeText(this@MainActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }


    private fun getToken(): String? {
        val sharedPreferences : SharedPreferences = getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_ACCESS_TOKEN,null)
    }
}