package vsu.nikolnikova.routebuddy

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

@Suppress("DEPRECATION")
class SearchActivity : AppCompatActivity() {

    private lateinit var autocomplete: AutocompleteSupportFragment
    private lateinit var textView: TextView

    private lateinit var close: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        close = findViewById(R.id.close)

        close.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

        textView = findViewById(R.id.text)

        val apiKey = BuildConfig.MAPS_API_KEY

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        autocomplete =
            supportFragmentManager.findFragmentById(R.id.autocomplete) as AutocompleteSupportFragment

        autocomplete.setPlaceFields(
            listOf(
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.OPENING_HOURS,
                Place.Field.RATING,
                Place.Field.USER_RATINGS_TOTAL
            )
        )

        autocomplete.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            @SuppressLint("SetTextI18n")
            override fun onPlaceSelected(place: Place) {
                val name = place.name
                val address = place.address
                val latlng = place.latLng
                val latitude = latlng?.latitude
                val longitude = latlng?.longitude

                val isOpenStatus: String = if (place.isOpen == true) {
                    "Open"
                } else {
                    "Closed"
                }

                val rating = place.rating
                val userRatings = place.userRatingsTotal

                textView.text =
                    "Name: $name \nAddress: $address \nLatitude, Longitude: $latitude ," +
                            "$longitude\nIs open: $isOpenStatus \n" +
                            "Rating: $rating \nUser ratings: $userRatings"
            }

            override fun onError(status: Status) {
                Toast.makeText(
                    applicationContext,
                    "Отсутсвует подключение к интернету",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        })
    }
}