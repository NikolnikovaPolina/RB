package vsu.nikolnikova.routebuddy

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var recoverPassword: TextView
    private lateinit var enter: Button
    private lateinit var register: TextView

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        val db = Firebase.firestore

        if (auth.currentUser == null) {

            email = findViewById(R.id.email)
            password = findViewById(R.id.password)
            recoverPassword = findViewById(R.id.recover_password)
            register = findViewById(R.id.register)

            password.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    recoverPassword.isVisible = s.isNullOrBlank()
                }

                override fun afterTextChanged(s: Editable?) {
                }
            })
            enter = findViewById(R.id.login)

            enter.setOnClickListener {
                if (!TextUtils.isEmpty(email.text.toString()) &&
                    !TextUtils.isEmpty(password.text.toString())
                ) {

                    db.collection("user").whereEqualTo("email", email.text.toString())
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                auth.signInWithEmailAndPassword(
                                    email.text.toString(),
                                    password.text.toString()
                                )
                                    .addOnSuccessListener {
                                        val intent = Intent(this, MapsActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this,
                                            it.message,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Пользователя с такой почтой нет",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "Error getting documents: ", exception)
                        }
                } else if (TextUtils.isEmpty(email.text.toString()) ||
                    TextUtils.isEmpty(password.text.toString())
                ) {
                    Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
                }
            }

            register.setOnClickListener {
                val intent = Intent(this, RegisterActivity::class.java)
                startActivity(intent)
                finish()
            }

        } else {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}