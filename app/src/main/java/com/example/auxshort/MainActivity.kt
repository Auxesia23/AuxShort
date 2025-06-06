package com.example.auxshort

import JwtPayload
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.content.edit
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import decodeJwtPayload


class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var urlAdapter: UrlAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyTextView: TextView
    private lateinit var apiService: ApiService
    private lateinit var fabAddUrl: FloatingActionButton
    private lateinit var avatarImageView: ShapeableImageView
    private lateinit var profile : JwtPayload

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views and Retrofit service
        progressBar = findViewById(R.id.progressBar)
        emptyTextView = findViewById(R.id.textViewEmpty)
        recyclerView = findViewById(R.id.recyclerViewUrls)
        fabAddUrl = findViewById(R.id.fabAddUrl)
        avatarImageView = findViewById(R.id.avatarImageView)
        apiService = RetrofitClient.instance

        setupAvatarClick()
        setupRecyclerView()

        val token = getToken()
        if (token != null) {
            profile = decodeJwtPayload(token)
            loadAvatar(profile.picture)
            fetchUrls(token)
        } else {
            navigateToLogin()
        }

        fabAddUrl.setOnClickListener {
            showAddUrlDialog()
        }
    }

    private fun setupAvatarClick() {
        avatarImageView.setOnClickListener { view ->
            showAvatarDropdownMenu(view)
        }
    }

    private fun showAvatarDropdownMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.avatar_menu, popup.menu)

        // Set email di menu item
        val emailItem = popup.menu.findItem(R.id.action_user_email)
        emailItem.title = profile.email

        // Set listener untuk item menu
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun loadAvatar(imageUrl: String?) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_logout)
                .error(R.drawable.ic_logout)
                .into(avatarImageView)
        } else {
            avatarImageView.setImageResource(R.drawable.ic_logout)
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerView() {
        urlAdapter = UrlAdapter(mutableListOf(),
            onDeleteClick = { urlItem, position ->
                showDeleteConfirmationDialog(urlItem, position)
            },
            onAnalyticsClick = { urlItem->
                val urlId : String = urlItem.shortened.toString().substringAfter("/")
                val intent = Intent(this, AnalyticActivity::class.java)
                intent.putExtra("URL_ID", urlId)
                startActivity(intent)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = urlAdapter
    }

    private fun fetchUrls(token: String) {
        showLoading(true)
        val call = apiService.getUrls("Bearer $token")

        call.enqueue(object : Callback<UrlResponse> {
            override fun onResponse(call: Call<UrlResponse>, response: Response<UrlResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    val urls = response.body()?.urls
                    if (!urls.isNullOrEmpty()) {
                        urlAdapter.updateUrls(urls)
                        recyclerView.visibility = View.VISIBLE
                        emptyTextView.visibility = View.GONE
                    } else {
                        recyclerView.visibility = View.GONE
                        emptyTextView.visibility = View.VISIBLE
                    }
                } else {
                    handleApiError(response)
                }
            }

            override fun onFailure(call: Call<UrlResponse>, t: Throwable) {
                showLoading(false)
                handleApiFailure("API call failed", t)
            }
        })
    }

    private fun showAddUrlDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_url, null)
        val urlOriginal = dialogView.findViewById<EditText>(R.id.editTextOriginalUrl)
        val urlShortened = dialogView.findViewById<EditText>(R.id.editTextShortenedUrl)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val token = getToken()
                val originalUrl = urlOriginal.text.toString().trim()
                val shortUrl = urlShortened.text.toString().trim()
                if (token != null &&originalUrl.isNotEmpty() && Patterns.WEB_URL.matcher(originalUrl).matches()){
                    createNewUrl(token,originalUrl,shortUrl)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this, "Error: Invalid url format.", Toast.LENGTH_SHORT).show()
                }

            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewUrl(token: String, originalUrl: String, shortenedUrl: String) {
        showLoading(true)
        val request = CreateUrlRequest(original = originalUrl, shortened = shortenedUrl)
        val call = apiService.createUrl("Bearer $token", request)

        call.enqueue(object: Callback<UrlItem> {
            override fun onResponse(call: Call<UrlItem>, response: Response<UrlItem>) {
                showLoading(false)
                if(response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "URL added succesfuly!", Toast.LENGTH_SHORT).show()
                    fetchUrls(token)
                } else {
                    handleApiError(response)
                }
            }

            override fun onFailure(call: Call<UrlItem>, t: Throwable) {
                showLoading(false)
                handleApiFailure("Failed to connect to the server", t)
            }
        })
    }


    private fun showDeleteConfirmationDialog(urlItem: UrlItem, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete URL")
            .setMessage("Are you sure you want to delete this URL? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val token = getToken()
                val shortUrlId = urlItem.shortened?.substringAfterLast('/') // Assuming ID is the last part of the path
                if (token != null && shortUrlId != null) {
                    deleteUrl(token, shortUrlId, position)
                } else {
                    Toast.makeText(this, "Error: Missing token or URL ID.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteUrl(token: String, shortUrlId: String, position: Int) {
        showLoading(true)
        val call = apiService.deleteUrl("Bearer $token", shortUrlId)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                showLoading(false)
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "URL deleted successfully", Toast.LENGTH_SHORT).show()
                    urlAdapter.removeItem(position)
                } else {
                    handleApiError(response)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showLoading(false)
                handleApiFailure("Delete call failed", t)
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun handleApiError(response: Response<*>) {
        val errorBody = response.errorBody()?.string()
        var errorMessage = "Error: ${response.code()}"

        if (!errorBody.isNullOrEmpty()) {
            try {
                val errorResponse = Gson().fromJson(errorBody, ApiErrorResponse::class.java)
                if (!errorResponse.error.isNullOrEmpty()) {
                    errorMessage = errorResponse.error
                    Toast.makeText(this, "Error: ${errorMessage}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                errorMessage = errorBody
            }

            if (recyclerView.adapter?.itemCount == 0) {
                emptyTextView.setText(errorMessage)
                emptyTextView.visibility = View.VISIBLE
            }
        }

    }

    private fun handleApiFailure(logMessage: String, t: Throwable) {
        Log.e(TAG, "$logMessage: ${t.message}", t)
        Toast.makeText(this, "Connection failed: ${t.message}", Toast.LENGTH_LONG).show()
        emptyTextView.visibility = View.VISIBLE
        emptyTextView.text = "Connection failed."
    }

    private fun getToken(): String? {
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    private fun logout() {
        deleteToken()
        navigateToLogin()
    }

    private fun deleteToken() {
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit() { remove(KEY_ACCESS_TOKEN) }
        Log.d(TAG, "Access token has been deleted.")
    }
}