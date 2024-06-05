package vsu.nikolnikova.routebuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import vsu.nikolnikova.routebuddy.data.Route
import vsu.nikolnikova.routebuddy.data.Waypoint

class CreateRouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var title: EditText

    private var description: EditText? = null

    private lateinit var mMap: GoogleMap

    private lateinit var createRoute: Button

    private lateinit var last: ImageButton

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private lateinit var point: GeoPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_route)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val pointX = intent.getStringExtra("coordinateX")
        val pointY = intent.getStringExtra("coordinateY")
        point = GeoPoint(pointX!!.toDouble(), pointY!!.toDouble())

        val userId = auth.currentUser?.uid

        title = findViewById(R.id.title)
        description = findViewById(R.id.description)

        createRoute = findViewById(R.id.create_route)

        createRoute.setOnClickListener {
            db.collection("route").document().set(
                Route(
                    title.text.toString(),
                    description?.text.toString(),
                    0.0,
                    Timestamp.now(),
                    null,
                    userId.toString()
                )
            )
        }
        // создать маршрут и добавлять в точки маршрута по номерам

        last = findViewById(R.id.last)
        last.setOnClickListener {
            val newIntent = Intent(this, PlaceForPhotoActivity::class.java)
            startActivity(newIntent)
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.isBuildingsEnabled = true
        mMap.isIndoorEnabled = true
        mMap.uiSettings.isRotateGesturesEnabled = false

        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(point.latitude, point.longitude),
                16f
            )
        )

        mMap.addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)))

        mMap.setOnMapClickListener { latLng ->
            val latitude = latLng.latitude
            val longitude = latLng.longitude

            db.collection("route").document().set(
                Waypoint(
                    "",
                    "",
                    1
                )
            )

            mMap.addMarker(MarkerOptions().position(latLng))
            point = GeoPoint(latitude, longitude)
        }
    }
}