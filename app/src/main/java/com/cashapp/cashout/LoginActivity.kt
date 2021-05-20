package com.cashapp.cashout

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.content.SharedPreferences  
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_login)

        SharedPreferences preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
        val checkbox = preferences.getval("remember",'')

        val username = null
        val password = null

        if(checkbox.equals('true')){
            username = preferences.getString('username', MODE_PRIVATE)
            pwd= preferences.getString('password', MODE_PRIVATE)

            if(username != null && password != null){
                attemptLogin(username, pwd)
            }

        }

        private fun attemptLogin(val email, val password){
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

        register_new.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }

        save_login.setOnClickListener{
            if(save_login.isChecked()){
                SharedPreferences preferences = getSharedPreferences('checkbox', MODE_PRIVATE)
                SharedPreferences.Editor editor = preferences.edit()
                editor.putString('remember', 'true')
                editor.apply()
            }else{
                SharedPreferences preferences = getSharedPreferences('checkbox', MODE_PRIVATE)
                SharedPreferences.Editor editor = preferences.edit()
                editor.putString('remember', 'false')
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