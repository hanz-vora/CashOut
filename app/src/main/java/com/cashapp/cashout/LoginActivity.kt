package com.cashapp.cashout

import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_login)

        var preferences: SharedPreferences = this.getSharedPreferences("checkbox", MODE_PRIVATE)
        val checkbox = preferences.getString("remember", "")

        var username: String? = null
        var pwd: String? = null

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = Firebase.auth

        google_login_btn.setOnClickListener {
            signIn()
        }

        fun attemptLogin(email: String, password: String) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        Toast.makeText(
                            this@LoginActivity,
                            "Login Successful",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent =
                            Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        intent.putExtra(
                            "user_id",
                            FirebaseAuth.getInstance().currentUser!!.uid
                        )
                        intent.putExtra("email_id", email)
                        val editor: SharedPreferences.Editor = preferences.edit()
                        editor.putString("username", email)
                        editor.putString("password", password)
                        editor.apply()
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            task.exception!!.message.toString(),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        if (checkbox.equals("true")) {
            username = preferences.getString("username", null)
            pwd = preferences.getString("password", null)

            if (username != null && pwd != null) {

                username_et.setText(username)
                password_et.setText(pwd)
                attemptLogin(username, pwd)
            }

        }

        register_new.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }

        save_login.setOnCheckedChangeListener { _, isChecked ->
            preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
            val editor: SharedPreferences.Editor = preferences.edit()
            if (isChecked) {
                editor.putString("remember", "true")
                editor.apply()
            } else {
                editor.putString("remember", "false")
                editor.apply()
            }
        }

        login_btn.setOnClickListener {
            when {
                TextUtils.isEmpty(username_et.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter your email",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                TextUtils.isEmpty(password_et.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter your password",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    val email: String = username_et.text.toString().trim { it <= ' ' }
                    val password: String = password_et.text.toString().trim { it <= ' ' }

                    attemptLogin(email, password)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            login_with_google(currentUser, false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val isNew = task.getResult()?.additionalUserInfo?.isNewUser
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    if (user != null && isNew != null) {
                        login_with_google(user, isNew)
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
            }
    }

    private fun login_with_google(user: FirebaseUser, isNew: Boolean){
        var db = Firebase.firestore
        val email = user.email
        val intent =
            Intent(this@LoginActivity, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(
            "user_id",
            user.uid
        )

        if(isNew){
            val newUser = hashMapOf(
                "email" to email,
                "amount_paid" to 0,
                "amount_sent" to 0
            )
            db.collection("users").document(user.uid).set(newUser)
                .addOnSuccessListener {
                    Log.d(
                        ContentValues.TAG,
                        "DocumentSnapshot added"
                    )
                }
                .addOnFailureListener { e ->
                    Log.w(ContentValues.TAG, "Error adding document", e)
                }
        }

        googleSignInClient.signOut()
        intent.putExtra("email_id", email)
        startActivity(intent)
        finish()
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }
}