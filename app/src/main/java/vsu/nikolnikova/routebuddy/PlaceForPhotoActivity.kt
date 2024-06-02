package vsu.nikolnikova.routebuddy

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_place_for_photo)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
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

        linearLayoutPoints = findViewById(R.id.linear_layout_points)

        val db = Firebase.firestore

        db.collection("category").whereEqualTo("name", "Места для фото")
            .get().addOnSuccessListener { documents ->
                for (document in documents) {
                    val categoryId = document.id
                    db.collection("point of interest")
                        .whereEqualTo("id category", categoryId)
                        .get()
                        .addOnSuccessListener { docs ->
                            for (doc in docs) {

                                val linearLayout = LinearLayout(this)
                                val param =
                                    LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        100
                                    )
                                linearLayout.layoutParams = param
                                param.topMargin = 10
                                linearLayout.setBackgroundColor(
                                    ContextCompat.getColor(
                                        this,
                                        R.color.white
                                    )
                                )
                                linearLayout.orientation = LinearLayout.HORIZONTAL

                                val textView = TextView(this)
                                textView.text = doc.getString("name")
                                textView.layoutParams = LinearLayout.LayoutParams(
                                    0,
                                    LinearLayout.LayoutParams.WRAP_CONTENT, 20.0f
                                )
                                textView.setPadding(10, 20, 0, 0)
                                textView.setTextColor(
                                    ContextCompat.getColor(
                                        this,
                                        R.color.black
                                    )
                                )
                                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)

                                val imageButton = ImageButton(this)
                                val params = LinearLayout.LayoutParams(0, 80, 1.0f)
                                params.marginEnd = 20
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
                                textRating.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)

                                linearLayout.addView(textView)
                                linearLayout.addView(textRating)
                                linearLayout.addView(imageButton)

                                linearLayoutPoints.addView(linearLayout)
                            }
                        }
                }
            }
    }
}