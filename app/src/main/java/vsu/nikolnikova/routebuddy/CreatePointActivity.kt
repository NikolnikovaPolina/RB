package vsu.nikolnikova.routebuddy

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.MultiAutoCompleteTextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import vsu.nikolnikova.routebuddy.data.PointOfInterest
import java.util.LinkedList

class CreatePointActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var title: EditText
    private lateinit var description: EditText
    private lateinit var cost: EditText
    private lateinit var category: MultiAutoCompleteTextView

    private lateinit var mMap: GoogleMap

    private lateinit var createPoint: Button

    private lateinit var auth: FirebaseAuth

    private lateinit var point: GeoPoint
    private var currentMarker: Marker? = null
    private var currentLocation: LatLng = LatLng(51.656680042276435, 39.206041019902685)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_point)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        auth = Firebase.auth

        val db = Firebase.firestore
        val userId = auth.currentUser?.uid

        title = findViewById(R.id.title)
        description = findViewById(R.id.description)
        cost = findViewById(R.id.cost)
        category = findViewById(R.id.category)

        val items = LinkedList<String>()

        db.collection("category")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.getString("name")?.let { items.add(it) }
                }
            }

        category.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, items))
        category.threshold = 1
        category.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())

        createPoint = findViewById(R.id.create_point)

        createPoint.setOnClickListener {
            db.collection("point of interest").whereEqualTo("coordinate", point)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        var currentCategory = category.text.toString()

                        var cc = currentCategory

                        if (currentCategory.contains(",")) {
                            cc = currentCategory.substring(currentCategory.indexOf(",") + 2)
                            currentCategory =
                                currentCategory.substring(0, currentCategory.indexOf(","))
                        }

                        do {
                            db.collection("category")
                                .whereEqualTo("name", currentCategory)
                                .get()
                                .addOnSuccessListener { docs ->
                                    for (doc in docs) {
                                        db.collection("point of interest").document().set(
                                            PointOfInterest(
                                                title.text.toString(),
                                                description.text.toString(),
                                                point,
                                                cost.text.toString().toDoubleOrNull(),
                                                userId.toString(),
                                                doc.id
                                            )
                                        )
                                    }
                                }

                            if (cc.contains(",")) {
                                currentCategory = cc.substring(0, cc.indexOf(","))
                                cc = cc.substring(cc.indexOf(",") + 2)
                            } else {
                                currentCategory = cc
                                cc = ""
                            }


                        } while (currentCategory != "")
                    }
                }

            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.isBuildingsEnabled = true
        mMap.isIndoorEnabled = true
        mMap.uiSettings.isRotateGesturesEnabled = false

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))

        mMap.setOnMapClickListener { latLng ->
            val latitude = latLng.latitude
            val longitude = latLng.longitude

            currentMarker?.remove()
            currentMarker = mMap.addMarker(MarkerOptions().position(latLng))
            point = GeoPoint(latitude, longitude)
        }
    }
}