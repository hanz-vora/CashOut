package com.cashapp.cashout


import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.content.ContentValues.TAG
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_register.*


class RegisterActivity : AppCompatActivity() {
    var db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_register)

        login_new.setOnClickListener {
            onBackPressed()
        }

        register_btn.setOnClickListener {
            when {
                TextUtils.isEmpty(username_re.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please enter your email",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                TextUtils.isEmpty(password_re.text.toString().trim { it <= ' ' }) -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Please enter your password",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                password_re.text.toString().trim { it <= ' ' } != password_re_2.text.toString().trim { it <= ' ' } -> {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Passwords do no match",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    val email: String = username_re.text.toString().trim { it <= ' ' }
                    val password: String = password_re.text.toString().trim { it <= ' ' }

                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(
                            OnCompleteListener<AuthResult> { task ->

                                if (task.isSuccessful) {
                                    val firebaseUser: FirebaseUser = task.result!!.user!!

                                    Toast.makeText(
                                        this@RegisterActivity,
                                        "Registration Successful",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    val intent =
                                        Intent(this@RegisterActivity, MainActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    intent.putExtra("user_id", firebaseUser.uid)
                                    intent.putExtra("email_id", email)

                                    val user = hashMapOf(
                                        "email" to email,
                                        "amount_paid" to 0,
                                        "amount_sent" to 0
                                    )
                                    db.collection("users").document(firebaseUser.uid).set(user)
                                        .addOnSuccessListener { documentReference ->
                                            Log.d(
                                                TAG,
                                                "DocumentSnapshot added"
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            Log.w(TAG, "Error adding document", e)
                                        }

                                    startActivity(intent)
                                    finish()

                                } else {
                                    Toast.makeText(
                                        this@RegisterActivity,
                                        task.exception!!.message.toString(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                }
            }
        }

    }
}