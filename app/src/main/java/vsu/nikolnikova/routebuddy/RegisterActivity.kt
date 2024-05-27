package vsu.nikolnikova.routebuddy

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import at.favre.lib.crypto.bcrypt.BCrypt
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import vsu.nikolnikova.routebuddy.data.User

class RegisterActivity : AppCompatActivity() {

    private lateinit var name: EditText
    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var passwordRepeat: EditText

    private lateinit var create: Button

    private lateinit var auth: FirebaseAuth

    private lateinit var last: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = Firebase.firestore
        auth = FirebaseAuth.getInstance()

        create = findViewById(R.id.create)

        create.setOnClickListener {

            name = findViewById(R.id.username)
            email = findViewById(R.id.email)
            password = findViewById(R.id.password)
            passwordRepeat = findViewById(R.id.password_repeat)

            if (!TextUtils.isEmpty(name.text.toString()) &&
                !TextUtils.isEmpty(email.text.toString()) &&
                !TextUtils.isEmpty(password.text.toString()) &&
                passwordRepeat.text.toString() == password.text.toString() &&
                password.text.length >= 6
            ) {
                db.collection("user").whereEqualTo("email", email.text.toString())
                    .get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            Toast.makeText(
                                this,
                                "Пользователь с данной почтой уже существует",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        } else {
                            auth.createUserWithEmailAndPassword(
                                email.text.toString(),
                                password.text.toString()
                            ).addOnSuccessListener {
                                db.collection("user").document(auth.currentUser!!.uid)
                                    .set(
                                        User(
                                            name.text.toString(),
                                            email.text.toString(),
                                            hashPassword(password.text.toString())
                                        )
                                    )
                                    .addOnSuccessListener {
                                        /*val intent = Intent(this, MapsActivity::class.java)
                                        startActivity(intent)
                                        finish()*/
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            this,
                                            it.message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        it.message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w(TAG, "Error getting documents: ", exception)
                    }
            } else if (TextUtils.isEmpty(name.text.toString()) ||
                TextUtils.isEmpty(email.text.toString()) ||
                TextUtils.isEmpty(password.text.toString()) ||
                TextUtils.isEmpty(passwordRepeat.text.toString())
            ) {
                Toast.makeText(this, "Не все поля заполнены", Toast.LENGTH_LONG).show()
            } else if (passwordRepeat.text.toString() != password.text.toString()) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_LONG).show()
            } else if (passwordRepeat.text.length < 6) {
                Toast.makeText(this, "Пароль содержит меньше 6 символов", Toast.LENGTH_LONG)
                    .show()
            }
        }

        last = findViewById(R.id.last)

        last.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun hashPassword(password: String): String {
        val bcrypt = BCrypt.withDefaults()
        return bcrypt.hashToString(12, password.toCharArray())
    }
}