package vsu.nikolnikova.routebuddy.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import vsu.nikolnikova.routebuddy.R

class FeedbackActivity : AppCompatActivity() {

    private lateinit var last: ImageButton
    private lateinit var name: TextView

    private lateinit var createFeedback: Button
    private lateinit var auth: FirebaseAuth

    private lateinit var thereAreNoReviews: TextView

    private lateinit var linearLayoutReviews: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_feedback)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.feedback)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        name = findViewById(R.id.name)
        name.text = intent.getStringExtra("name")

        last = findViewById(R.id.last)
        last.setOnClickListener {
            val intent = Intent(
                this,
                if (this.intent.getStringExtra("category") != "Готовые маршруты")
                    PlaceForPhotoActivity::class.java else ReadyMadeRoutesActivity::class.java
            )
            intent.putExtra("category", this.intent.getStringExtra("category"))
            intent.putExtra("name", this.intent.getStringExtra("name"))
            intent.putExtra("id", this.intent.getStringExtra("id"))
            intent.putExtra("pointOrRoute", this.intent.getStringExtra("pointOrRoute"))
            startActivity(intent)
            finish()
        }

        createFeedback = findViewById(R.id.create_feedback)
        createFeedback.setOnClickListener {
            val intent = Intent(this, CreateFeedbackActivity::class.java)
            intent.putExtra("category", this.intent.getStringExtra("category"))
            intent.putExtra("name", this.intent.getStringExtra("name"))
            intent.putExtra("id", this.intent.getStringExtra("id"))
            intent.putExtra("pointOrRoute", this.intent.getStringExtra("pointOrRoute"))
            startActivity(intent)
            finish()
        }

        val db = Firebase.firestore
        val pointOrRoute = intent.getStringExtra("pointOrRoute")

        auth = Firebase.auth

        val userId = auth.currentUser?.uid.toString()
        val id = intent.getStringExtra("id")

        thereAreNoReviews = findViewById(R.id.there_are_no_reviews)

        linearLayoutReviews = findViewById(R.id.linear_layout_reviews)

        db.collection("feedback").whereEqualTo("id $pointOrRoute", id)
            .whereEqualTo("id user", userId)
            .get()
            .addOnSuccessListener { documents ->
                createFeedback.text = if (!documents.isEmpty) "Обновить отзыв" else "Создать отзыв"
            }

        db.collection("feedback").whereEqualTo("id $pointOrRoute", id)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {

                    thereAreNoReviews.isVisible = false

                    for (document in documents) {

                        val linearLayoutVertical = LinearLayout(this)
                        val paramVertical =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                        linearLayoutVertical.layoutParams = paramVertical
                        paramVertical.topMargin = 10
                        linearLayoutVertical.setBackgroundColor(
                            ContextCompat.getColor(
                                this,
                                R.color.white
                            )
                        )
                        linearLayoutVertical.orientation = LinearLayout.VERTICAL

                        val linearLayoutHorizontal = LinearLayout(this)
                        val paramHorizontal =
                            LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                100
                            )

                        linearLayoutHorizontal.layoutParams = paramHorizontal
                        linearLayoutHorizontal.orientation = LinearLayout.HORIZONTAL

                        val textView = TextView(this)
                        db.collection("user")
                            .document(document.getString("id user").toString())
                            .get()
                            .addOnSuccessListener { docs ->
                                textView.text = docs.getString("name").toString()
                            }

                        textView.layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 20.0f
                        )
                        textView.setPadding(20, 20, 0, 0)
                        textView.setTextColor(
                            ContextCompat.getColor(
                                this,
                                R.color.black
                            )
                        )

                        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                        textView.setTypeface(null, Typeface.BOLD)

                        val imageButton = ImageButton(this)
                        val params = LinearLayout.LayoutParams(0, 80, 1.0f)
                        params.marginEnd = 30
                        imageButton.layoutParams = params
                        params.gravity = Gravity.START
                        imageButton.setBackgroundColor(
                            ContextCompat.getColor(
                                this,
                                R.color.transparent
                            )
                        )
                        imageButton.setImageResource(R.drawable.star)
                        imageButton.setPadding(5, 20, 5, 0)
                        imageButton.scaleType = ImageView.ScaleType.CENTER_INSIDE

                        val textRating = TextView(this)

                        textRating.text = document.getDouble("estimation")!!.toInt().toString()

                        textRating.layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
                        )

                        textRating.gravity = Gravity.END
                        textRating.setPadding(0, 20, 10, 0)
                        textRating.setTextColor(
                            ContextCompat.getColor(
                                this,
                                R.color.black
                            )
                        )

                        textRating.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)

                        linearLayoutHorizontal.addView(textView)
                        linearLayoutHorizontal.addView(textRating)
                        linearLayoutHorizontal.addView(imageButton)

                        linearLayoutVertical.addView(linearLayoutHorizontal)

                        if (document.getString("comment") != null) {

                            val textDescription = TextView(this)
                            textDescription.text = document.getString("comment")
                            textDescription.layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            textDescription.setTextColor(
                                ContextCompat.getColor(
                                    this,
                                    R.color.black
                                )
                            )
                            textDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            textDescription.setPadding(20, 0, 45, 0)

                            val scrollText = ScrollView(this)

                            val paramScroll = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                125
                            )

                            paramScroll.bottomMargin = 30
                            scrollText.layoutParams = paramScroll

                            scrollText.addView(textDescription)

                            linearLayoutVertical.addView(scrollText)
                        }

                        linearLayoutReviews.addView(linearLayoutVertical)
                    }
                } else {
                    thereAreNoReviews.isVisible = true
                }
            }
    }
}