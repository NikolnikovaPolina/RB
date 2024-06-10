package vsu.nikolnikova.routebuddy.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import vsu.nikolnikova.routebuddy.R
import vsu.nikolnikova.routebuddy.data.Feedback

@Suppress("UNCHECKED_CAST")
class CreateFeedbackActivity : AppCompatActivity() {

    private lateinit var last: ImageButton

    private lateinit var name: TextView
    private var rating: Int = 0
    private lateinit var textReview: TextView

    private lateinit var saveFeedback: Button

    private lateinit var auth: FirebaseAuth

    private lateinit var star1: ImageView
    private lateinit var star2: ImageView
    private lateinit var star3: ImageView
    private lateinit var star4: ImageView
    private lateinit var star5: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_feedback)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.create_feedback)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        last = findViewById(R.id.last)

        last.setOnClickListener {
            val intent = Intent(this, FeedbackActivity::class.java)
            intent.putExtra("category", this.intent.getStringExtra("category"))
            intent.putExtra("name", this.intent.getStringExtra("name"))
            intent.putExtra("id", this.intent.getStringExtra("id"))
            intent.putExtra("pointOrRoute", this.intent.getStringExtra("pointOrRoute"))
            startActivity(intent)
            finish()
        }

        name = findViewById(R.id.name)
        name.text = intent.getStringExtra("name")
        val pointOrRoute = intent.getStringExtra("pointOrRoute")
        val id = intent.getStringExtra("id")

        star1 = findViewById(R.id.star1)
        star2 = findViewById(R.id.star2)
        star3 = findViewById(R.id.star3)
        star4 = findViewById(R.id.star4)
        star5 = findViewById(R.id.star5)

        star1.setOnClickListener {
            star1.setImageResource(R.drawable.star_white)
            star2.setImageResource(R.drawable.empty_star)
            star3.setImageResource(R.drawable.empty_star)
            star4.setImageResource(R.drawable.empty_star)
            star5.setImageResource(R.drawable.empty_star)
            rating = 1
        }

        star2.setOnClickListener {
            star1.setImageResource(R.drawable.star_white)
            star2.setImageResource(R.drawable.star_white)
            star3.setImageResource(R.drawable.empty_star)
            star4.setImageResource(R.drawable.empty_star)
            star5.setImageResource(R.drawable.empty_star)
            rating = 2
        }

        star3.setOnClickListener {
            star1.setImageResource(R.drawable.star_white)
            star2.setImageResource(R.drawable.star_white)
            star3.setImageResource(R.drawable.star_white)
            star4.setImageResource(R.drawable.empty_star)
            star5.setImageResource(R.drawable.empty_star)
            rating = 3
        }

        star4.setOnClickListener {
            star1.setImageResource(R.drawable.star_white)
            star2.setImageResource(R.drawable.star_white)
            star3.setImageResource(R.drawable.star_white)
            star4.setImageResource(R.drawable.star_white)
            star5.setImageResource(R.drawable.empty_star)
            rating = 4
        }

        star5.setOnClickListener {
            star1.setImageResource(R.drawable.star_white)
            star2.setImageResource(R.drawable.star_white)
            star3.setImageResource(R.drawable.star_white)
            star4.setImageResource(R.drawable.star_white)
            star5.setImageResource(R.drawable.star_white)
            rating = 5
        }

        textReview = findViewById(R.id.text_of_the_review)

        saveFeedback = findViewById(R.id.save_feedback)
        saveFeedback.setOnClickListener {

            val db = Firebase.firestore
            auth = Firebase.auth

            val userId = auth.currentUser?.uid.toString()
            db.collection("feedback").whereEqualTo("id user", userId)
                .whereEqualTo("id $pointOrRoute", id)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        db.collection("feedback").add(
                            Feedback(
                                userId,
                                if (pointOrRoute != "route") id else null,
                                if (pointOrRoute == "route") id else null,
                                rating,
                                textReview.text.toString(),
                                Timestamp.now(),
                                null
                            )
                        )
                    } else {
                        for (document in documents) {
                            db.collection("feedback").document(document.id).update(
                                hashMapOf(
                                    "id point of interest" to if (pointOrRoute != "route") id else null,
                                    "id route" to if (pointOrRoute == "route") id else null,
                                    "estimation" to rating,
                                    "comment" to if (textReview.text.isEmpty()) textReview.text.toString() else null,
                                    "date of update" to Timestamp.now()
                                ) as Map<String, Any>
                            )
                        }
                    }
                }

            val intent = Intent(this, FeedbackActivity::class.java)
            intent.putExtra("category", this.intent.getStringExtra("category"))
            intent.putExtra("name", this.intent.getStringExtra("name"))
            intent.putExtra("pointOrRoute", this.intent.getStringExtra("pointOrRoute"))
            intent.putExtra("id", this.intent.getStringExtra("id"))
            startActivity(intent)
            finish()
        }
    }
}