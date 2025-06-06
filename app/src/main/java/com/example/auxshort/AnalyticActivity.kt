package com.example.auxshort

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class AnalyticActivity : AppCompatActivity() {

    private lateinit var urlId: String
    private lateinit var apiService: ApiService
    private lateinit var progressBar: ProgressBar

    // View di dalam layout utama
    private lateinit var totalClicksTextView: TextView
    private lateinit var clicksPerCountryLayout: LinearLayout
    private lateinit var clicksPerUserAgentLayout: LinearLayout

    // View di dalam header card yang di-include
    private lateinit var headerOriginalUrlTextView: TextView
    private lateinit var headerShortenedUrlTextView: TextView
    private lateinit var headerCreatedAtTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytic)

        // Inisialisasi Views
        progressBar = findViewById(R.id.progressBar)
        totalClicksTextView = findViewById(R.id.textViewTotalClicks)
        clicksPerCountryLayout = findViewById(R.id.layoutClicksPerCountry)
        clicksPerUserAgentLayout = findViewById(R.id.layoutClicksPerUserAgent)

        // Inisialisasi view dari header card
        val headerView = findViewById<View>(R.id.urlHeaderCard)
        headerOriginalUrlTextView = headerView.findViewById(R.id.textViewOriginalUrl)
        headerShortenedUrlTextView = headerView.findViewById(R.id.textViewShortenedUrl)
        headerCreatedAtTextView = headerView.findViewById(R.id.textViewCreatedAt)


        apiService = RetrofitClient.instance

        val id = intent.getStringExtra("URL_ID")
        if (id != null) {
            urlId = id
            fetchAnalytics()
        } else {
            Toast.makeText(this, "ID URL tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchAnalytics() {
        showLoading(true)
        val token = getToken()
        if (token == null) {
            Toast.makeText(this, "Sesi tidak valid, harap login kembali.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val call = apiService.getUrlAndAnalytic("Bearer $token", urlId)
        call.enqueue(object : Callback<AnalyticResponse> {
            override fun onResponse(call: Call<AnalyticResponse>, response: Response<AnalyticResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    val analyticResponse = response.body()
                    analyticResponse?.let { displayData(it) }
                } else {
                    Toast.makeText(this@AnalyticActivity, "Gagal mengambil data: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AnalyticResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@AnalyticActivity, "Koneksi gagal: ${t.message}", Toast.LENGTH_LONG).show()
                Log.e("AnalyticActivity", "API call failed", t)
            }
        })
    }

    private fun displayData(data: AnalyticResponse) {
        // Tampilkan detail URL di header card
        headerOriginalUrlTextView.text = data.url?.original ?: "N/A"
        headerShortenedUrlTextView.text = data.url?.shortened ?: "N/A"
        headerCreatedAtTextView.text = formatDisplayDate(data.url?.createdAt)


        // Tampilkan data analitik
        val analyticData = data.analytic
        totalClicksTextView.text = analyticData?.totalClick?.toString() ?: "0"

        // Kosongkan layout sebelum menambahkan data baru
        clicksPerCountryLayout.removeAllViews()
        clicksPerUserAgentLayout.removeAllViews()

        // Tampilkan data klik per negara
        analyticData?.clicksPerCountry?.forEach {
            addItemToLayout(clicksPerCountryLayout, it.name ?: "Unknown", it.count ?: 0)
        }

        // Tampilkan data klik per user agent
        analyticData?.clicksPerUserAgent?.forEach {
            addItemToLayout(clicksPerUserAgentLayout, it.name ?: "Unknown", it.count ?: 0)
        }
    }

    private fun addItemToLayout(layout: LinearLayout, name: String, count: Int) {
        val inflater = LayoutInflater.from(this)
        val itemView = inflater.inflate(R.layout.item_analytic_detail, layout, false)

        val nameTextView = itemView.findViewById<TextView>(R.id.textViewName)
        val countTextView = itemView.findViewById<TextView>(R.id.textViewCount)

        nameTextView.text = name
        countTextView.text = count.toString()

        layout.addView(itemView)
    }

    private fun formatDisplayDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "N/A"
        return try {
            val inputFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
            val outputFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault())
            val zonedDateTime = ZonedDateTime.parse(dateString, inputFormatter)
            "Created: ${zonedDateTime.format(outputFormatter)}"
        } catch (e: Exception) {
            "Invalid date"
        }
    }


    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun getToken(): String? {
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }
}
