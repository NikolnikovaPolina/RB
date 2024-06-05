package vsu.nikolnikova.routebuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
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

    private lateinit var description: EditText

    private lateinit var mMap: GoogleMap

    private lateinit var createRoute: Button

    private lateinit var last: ImageButton

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private lateinit var point: GeoPoint
    private lateinit var routeId: String

    private lateinit var userId: String

    private val routePoints = mutableListOf<LatLng>()
    private val pointsSave = mutableListOf<LatLng>()
    private val pointsSaveId = mutableListOf<String>()

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

        userId = auth.currentUser?.uid.toString()

        title = findViewById(R.id.title)
        description = findViewById(R.id.description)

        createRoute = findViewById(R.id.create_route)

        createRoute.setOnClickListener {
            db.collection("route").add(
                Route(
                    title.text.toString(),
                    if (description.text.isEmpty()) null else description.text.toString(),
                    0.0,
                    Timestamp.now(),
                    null,
                    userId
                )
            )
                .addOnSuccessListener { document ->
                    routeId = document.id
                    addFirestore()
                    Toast.makeText(this, routePoints.toString(), Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MapsActivity::class.java)
                    intent.putExtra("routeId", routeId)
                    startActivity(intent)
                    finish()
                }
        }

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

        db.collection("point of interest").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val coordinate = document.getGeoPoint("coordinate")
                val marker = MarkerOptions()
                    .position(LatLng(coordinate!!.latitude, coordinate.longitude))
                    .title(document.getString("name").toString())
                mMap.addMarker(marker)
            }
        }

        mMap.setOnMarkerClickListener { marker ->
            showMarkerDialog(marker)
            true
        }

        mMap.setOnMapClickListener { latLng ->
            routePoints.add(latLng)

            mMap.addMarker(
                MarkerOptions().position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )

        }
    }

    private fun showMarkerDialog(marker: Marker) {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogStyle)
        if (marker.title != null) {
            builder.setTitle(marker.title)
        }

        builder.setMessage("Что вы хотите сделать с этой точкой?")
            .setPositiveButton("Добавить") { _, _ ->
                if (routePoints.indexOf(marker.position) != -1) {
                    routePoints.remove(marker.position)
                    Toast.makeText(
                        this@CreateRouteActivity,
                        "Точка добавлена в конец маршрута",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@CreateRouteActivity,
                        "Точка добавлена в машрут",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                routePoints.add(marker.position)
            }
            .setNegativeButton("Удалить") { _, _ ->
                if (marker.title == null) {
                    marker.remove()
                    Toast.makeText(
                        this@CreateRouteActivity,
                        "Точка удалена",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    if (routePoints.indexOf(marker.position) == -1) {
                        Toast.makeText(
                            this@CreateRouteActivity,
                            "Точка не может быть удалена с карты",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@CreateRouteActivity,
                            "Точка удалена",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                routePoints.remove(marker.position)
            }

            .setNeutralButton("Отмена")
            { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addFirestore() {
        db.collection("point of interest")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val coordinate = document.getGeoPoint("coordinate")
                    pointsSave.add(LatLng(coordinate!!.latitude, coordinate.longitude))
                    pointsSaveId.add(document.id)
                }

                for (p in routePoints) {
                    db.collection("waypoint").document().set(
                        Waypoint(
                            routeId,
                            if (pointsSave.indexOf(p) != -1) pointsSaveId[pointsSave.indexOf(p)] else null,
                            GeoPoint(p.latitude, p.longitude),
                            routePoints.indexOf(p) + 1
                        )
                    )
                }
            }
    }
}