package com.cashapp.cashout

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login)

        var preferences:SharedPreferences  = this.getSharedPreferences("checkbox", MODE_PRIVATE)
        val checkbox = preferences.getString("remember","")

        var username: String? = null
        var pwd: String? = null

        fun attemptLogin(email: String, password: String){
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

        if(checkbox.equals("true")){
            username = preferences.getString("username", null)
            pwd = preferences.getString("password", null)

            if(username != null  && pwd != null){

                username_et.setText(username)
                password_et.setText(pwd)
                attemptLogin(username, pwd)
            }

        }

        register_new.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }

        save_login.setOnCheckedChangeListener{ _, isChecked ->
            preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
            val editor: SharedPreferences.Editor  = preferences.edit()
            if(isChecked){
                editor.putString("remember", "true")
                editor.apply()
            }else{
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
}