package vsu.nikolnikova.routebuddy.activity

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
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vsu.nikolnikova.routebuddy.R

class UserActivity : AppCompatActivity() {

    private lateinit var last: ImageButton
    private lateinit var exit: ImageButton
    private lateinit var name: TextView

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private lateinit var myRoutesTextNo: TextView
    private lateinit var linearLayoutRoutes: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        name = findViewById(R.id.user_name)

        val userEmail = auth.currentUser?.email

        db.collection("user").whereEqualTo("email", userEmail.toString())
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    name.text = document.getString("name")
                }
            }

        last = findViewById(R.id.last)

        last.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }

        exit = findViewById(R.id.exit)

        exit.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        myRoutesTextNo = findViewById(R.id.my_routes_text_no)
        linearLayoutRoutes = findViewById(R.id.linear_layout_routes)

        val userId = auth.currentUser?.uid

        db.collection("route").whereEqualTo("id user", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.size() != 0) {

                    myRoutesTextNo.isVisible = false

                    for (document in documents) {

                        val linearLayout = LinearLayout(this)
                        val param =
                            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 100)
                        linearLayout.layoutParams = param
                        param.topMargin = 10
                        linearLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                        linearLayout.orientation = LinearLayout.HORIZONTAL

                        val textView = TextView(this)
                        textView.text = document.getString("name")
                        textView.layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 20.0f
                        )
                        textView.setPadding(10, 20, 0, 0)
                        textView.setTextColor(ContextCompat.getColor(this, R.color.black))
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

                        val routeId = document.id
                        val textRating = TextView(this)

                        db.collection("feedback").whereEqualTo("id route", routeId)
                            .get()
                            .addOnSuccessListener { docs ->
                                var totalRating = 0.0
                                for (doc in docs) {
                                    val r = doc.getDouble("estimation")
                                    if (r != null) {
                                        totalRating += r
                                    }
                                }
                                totalRating = if (docs.size() > 0) (totalRating / docs.size())
                                else 0.0

                                textRating.text = if (totalRating.toString().endsWith(".0"))
                                    totalRating.toInt().toString() else totalRating.toString()
                            }

                        textRating.layoutParams = LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 2.0f
                        )

                        textRating.gravity = Gravity.END
                        textRating.setPadding(0, 20, 10, 0)
                        textRating.setTextColor(ContextCompat.getColor(this, R.color.black))
                        textRating.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)

                        linearLayout.addView(textView)
                        linearLayout.addView(textRating)
                        linearLayout.addView(imageButton)

                        linearLayoutRoutes.addView(linearLayout)

                        linearLayout.setOnClickListener {
                            lifecycleScope.launch {
                                val intent = Intent(
                                    this@UserActivity,
                                    CreateRouteActivity::class.java
                                )
                                val routePoints =
                                    withContext(Dispatchers.IO) { collectPoints(routeId) }
                                intent.putExtra(
                                    "coordinateX",
                                    if (routePoints.size != 0) routePoints[0].latitude.toString() else "51.656680042276435"
                                )
                                intent.putExtra(
                                    "coordinateY",
                                    if (routePoints.size != 0) routePoints[0].longitude.toString() else "39.206041019902685"
                                )

                                intent.putExtra("routePoints", ArrayList(routePoints))
                                intent.putExtra("routeId", routeId)
                                intent.putExtra("category", "Готовые маршруты")
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                } else {
                    myRoutesTextNo.isVisible = true
                }
            }
    }

    private suspend fun collectPoints(routeId: String): MutableList<LatLng> {

        val routePoints = mutableListOf<LatLng>()
        val routeRandomPoints = mutableListOf<GeoPoint>()

        db.collection("waypoint")
            .whereEqualTo("id route", routeId)
            .get()
            .await()
            .forEach { document ->
                val coordinate = document.getGeoPoint("point")!!
                routeRandomPoints.add(coordinate)
                routePoints.add(LatLng(coordinate.latitude, coordinate.longitude))
            }

        for (point in routeRandomPoints) {
            db.collection("waypoint")
                .whereEqualTo("id route", routeId)
                .whereEqualTo("point", point)
                .get()
                .await()
                .forEach { doc ->
                    routePoints[doc.getDouble("order of the visit")!!.toInt() - 1] =
                        LatLng(point.latitude, point.longitude)
                }
        }

        return routePoints
    }
}