package vsu.nikolnikova.routebuddy

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class PlaceForPhotoActivity : AppCompatActivity() {

    private lateinit var linearLayoutPoints: LinearLayout

    private lateinit var last: ImageButton

    private lateinit var name: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_place_for_photo)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.place_for_photo)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        last = findViewById(R.id.last)

        last.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
            finish()
        }

        val category = intent.getStringExtra("category").toString()

        name = findViewById(R.id.name)
        name.text = category

        linearLayoutPoints = findViewById(R.id.linear_layout_points)

        val db = Firebase.firestore

        db.collection("category").whereEqualTo("name", category)
            .get().addOnSuccessListener { documents ->
                for (document in documents) {
                    val categoryId = document.id
                    db.collection("point of interest")
                        .whereEqualTo("id category", categoryId)
                        .get()
                        .addOnSuccessListener { docs ->
                            for (doc in docs) {

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
                                textView.text = doc.getString("name")
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

                                val pointOfInterestId = doc.id

                                val feedback = TextView(this)

                                feedback.setTextColor(
                                    ContextCompat.getColor(
                                        this,
                                        R.color.black
                                    )
                                )
                                feedback.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                feedback.gravity = Gravity.END
                                feedback.setPadding(0, 0, 40, 10)

                                feedback.setOnClickListener {
                                    val intent = Intent(this, FeedbackActivity::class.java)

                                    intent.putExtra("name", doc.getString("name"))
                                    intent.putExtra("pointOrRoute", "point of interest")
                                    intent.putExtra("id", pointOfInterestId)
                                    intent.putExtra("category", category)

                                    startActivity(intent)
                                    finish()
                                }

                                val textRating = TextView(this)

                                db.collection("feedback")
                                    .whereEqualTo("id point of interest", pointOfInterestId)
                                    .get()
                                    .addOnSuccessListener { ds ->
                                        var totalRating = 0.0
                                        for (d in ds) {
                                            val r = d.getDouble("estimation")
                                            if (r != null) {
                                                totalRating += r
                                            }
                                        }
                                        totalRating =
                                            if (ds.size() > 0) (totalRating / ds.size())
                                            else 0.0

                                        textRating.text =
                                            if (totalRating.toString().endsWith(".0"))
                                                totalRating.toInt()
                                                    .toString() else totalRating.toString()

                                        feedback.text =
                                            if (ds.size() != 0) "Посмотреть отзывы" else "Оставить отзыв"
                                    }

                                textRating.layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f
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

                                if (doc.getString("description") != null) {

                                    val textDescription = TextView(this)
                                    textDescription.text = doc.getString("description")
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
                                        315
                                    )
                                    paramScroll.bottomMargin = 30
                                    scrollText.layoutParams = paramScroll

                                    scrollText.addView(textDescription)

                                    linearLayoutVertical.addView(scrollText)
                                }

                                linearLayoutVertical.addView(feedback)

                                linearLayoutPoints.addView(linearLayoutVertical)

                                linearLayoutVertical.setOnClickListener {
                                    val intent = Intent(this, CreateRouteActivity::class.java)

                                    intent.putExtra(
                                        "coordinateX",
                                        doc.getGeoPoint("coordinate")!!.latitude.toString()
                                    )
                                    intent.putExtra(
                                        "coordinateY",
                                        doc.getGeoPoint("coordinate")!!.longitude.toString()
                                    )
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        }
                }
            }
    }
}