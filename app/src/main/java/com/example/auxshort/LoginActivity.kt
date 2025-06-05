package com.example.auxshort

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.gson.annotations.SerializedName
import com.example.auxshort.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.content.edit


data class SimpleTokenResponse(
    @SerializedName("acces_token")
    val accessToken: String?
)

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val TAG = "LoginActivity"

        private const val WEB_CLIENT_ID = "250867793265-emn6aqfrloqgoh6j9gqip1uo48r8b2mp.apps.googleusercontent.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val token = getTokenFromPreferences()
        if (token != null){
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestServerAuthCode(WEB_CLIENT_ID)
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        signInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            } else{
                Log.w(TAG, "Sign-in failed or cancelled. Result code: ${result.resultCode}")
            }
        }

        val signInButton: Button = findViewById(R.id.signInButton)
        signInButton.setOnClickListener {
            signIn()
        }


    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            val authCode = account?.serverAuthCode

            if (authCode != null) {
                sendAuthCodeToServer(authCode)
            } else {
                Toast.makeText(this,"Error Signing in", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            Toast.makeText(this,"signInResult:failed code=" + e.statusCode, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendAuthCodeToServer(authCode: String) {
        val apiService = RetrofitClient.instance
        val call = apiService.getAccessToken(authCode)

        call.enqueue(object : Callback<SimpleTokenResponse> {
            override fun onResponse(call: Call<SimpleTokenResponse>, response: Response<SimpleTokenResponse>) {
                if (response.isSuccessful) {
                    val tokenResponse = response.body()
                    val receivedAccessToken = tokenResponse?.accessToken

                    if (receivedAccessToken != null) {
                        saveTokenToPreferences(receivedAccessToken)
                        runOnUiThread {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error from server: ${response.code()} - $errorBody")
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, "Failed connecting to server: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<SimpleTokenResponse>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Failed connecting to server", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun getTokenFromPreferences(): String? {
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    private fun saveTokenToPreferences(token: String) {
        val sharedPreferences: SharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit() {
            putString(KEY_ACCESS_TOKEN, token)
        }
    }
}