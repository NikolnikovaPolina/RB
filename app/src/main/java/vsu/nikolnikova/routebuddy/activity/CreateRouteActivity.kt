package vsu.nikolnikova.routebuddy.activity

import android.annotation.SuppressLint
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
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vsu.nikolnikova.routebuddy.R
import vsu.nikolnikova.routebuddy.data.Route
import vsu.nikolnikova.routebuddy.data.Waypoint

@Suppress("UNCHECKED_CAST", "DEPRECATION")
class CreateRouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var title: EditText

    private lateinit var description: EditText

    private lateinit var mMap: GoogleMap

    private lateinit var createRoute: Button

    private lateinit var last: ImageButton

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    private var existingPolyline: PolylineOptions? = null

    private lateinit var point: GeoPoint
    private lateinit var routeId: String
    private lateinit var routeAllId: String

    private lateinit var userId: String

    private var routePoints = mutableListOf<LatLng>()
    private var oldRoutePoints = mutableListOf<LatLng>()
    private var newRoutePoints = mutableListOf<LatLng>()

    private var distance = 0.0

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

        val category = intent.getStringExtra("category").toString()

        if (category == "Готовые маршруты") {
            routeId = intent.getStringExtra("routeId").toString()
            createRoute.text = "Обновить маршрут"

            CoroutineScope(Dispatchers.IO).launch {

                val it = db.collection("route")
                    .document(routeId)
                    .get()
                    .await()
                if (it.exists()) {

                    val titleText = it.getString("name")
                    val descriptionText = it.getString("description")
                    withContext(Dispatchers.Main) {
                        title.setText(titleText.toString())
                        if (descriptionText != null) {
                            description.setText(descriptionText)
                        }
                    }
                }
            }

            val list = intent.getSerializableExtra("routePoints") as? ArrayList<LatLng>

            if (list!!.size != 0) {
                routePoints = list.toMutableList()
                oldRoutePoints = routePoints
            }
        }

        createRoute.setOnClickListener {
            if (category != "Готовые маршруты") {
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
                        CoroutineScope(Dispatchers.IO).launch {
                            routeId = document.id
                            withContext(Dispatchers.IO) { addFirestore() }
                            val intent =
                                Intent(this@CreateRouteActivity, MapsRouteActivity::class.java)
                            intent.putExtra("routeId", routeId)
                            intent.putExtra("category", category)
                            intent.putExtra("routePoints", ArrayList(routePoints))
                            startActivity(intent)
                            finish()
                        }
                    }
            } else {
                db.collection("route")
                    .document(intent.getStringExtra("routeId").toString())
                    .update(
                        hashMapOf(
                            "name" to title.text.toString(),
                            "description" to if (description.text.isEmpty()) null else description.text.toString(),
                            "date of update" to Timestamp.now(),
                        ) as Map<String, Any>
                    )
                    .addOnSuccessListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            val intent =
                                Intent(this@CreateRouteActivity, MapsRouteActivity::class.java)
                            withContext(Dispatchers.IO) { updateFirestore() }
                            intent.putExtra("routeId", routeId)
                            intent.putExtra("category", category)
                            intent.putExtra("routePoints", ArrayList(routePoints))
                            startActivity(intent)
                            finish()
                        }
                    }
            }
        }

        last = findViewById(R.id.last)
        last.setOnClickListener {
            val intent = Intent(
                this,
                if (category != "Готовые маршруты")
                    PlaceForPhotoActivity::class.java else UserActivity::class.java
            )
            intent.putExtra("category", category)
            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
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

        if (routePoints.size != 0) {
            showRoutePoints()
        }

        db.collection("point of interest").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val coordinate = document.getGeoPoint("coordinate")
                val marker = MarkerOptions()
                    .position(LatLng(coordinate!!.latitude, coordinate.longitude))
                    .title(document.getString("name").toString())

                if (coordinate == point) {
                    marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                    routePoints.add(marker.position)
                }

                if (routePoints.size == 1 || checkMarker(marker)) {
                    mMap.addMarker(marker)
                }
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

    private fun checkMarker(marker: MarkerOptions): Boolean {
        for (point in routePoints) {
            if (point == marker.position) {
                return false
            }
        }
        return true
    }

    private fun showRoutePoints() {
        for (point in routePoints) {
            mMap.addMarker(
                MarkerOptions().position(point)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }

        if (existingPolyline != null) {
            existingPolyline = null
        }

        val pathOptions = PolylineOptions()

        distance = SphericalUtil.computeLength(pathOptions.points)

        db.collection("route")
            .document(intent.getStringExtra("routeId").toString())
            .update(hashMapOf("distance" to distance) as Map<String, Any>)

        mMap.addPolyline(pathOptions)
        existingPolyline = pathOptions
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
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            }
            .setNegativeButton("Удалить") { _, _ ->
                if (marker.title == null) {
                    var p: Boolean
                    val pos = GeoPoint(marker.position.latitude, marker.position.longitude)

                    CoroutineScope(Dispatchers.Main).launch {
                        withContext(Dispatchers.IO) {
                            p = check(pos)
                        }

                        if (p) {
                            withContext(Dispatchers.Main) {
                                marker.remove()
                            }
                        } else {
                            withContext(Dispatchers.IO) {
                                val title = newTitle(pos)
                                withContext(Dispatchers.Main) {
                                    marker.title = title
                                    marker.setIcon(
                                        BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_RED
                                        )
                                    )
                                }
                            }
                        }
                    }

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
                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                }
                routePoints.remove(marker.position)
            }

            .setNeutralButton("Отмена")
            { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private suspend fun check(location: GeoPoint): Boolean {
        return db.collection("point of interest").whereEqualTo("coordinate", location)
            .get()
            .await().isEmpty
    }

    private suspend fun newTitle(location: GeoPoint): String {

        var title = ""

        db.collection("point of interest").whereEqualTo("coordinate", location)
            .get()
            .await()
            .forEach {
                title = it.getString("name").toString()
            }

        return title
    }

    private suspend fun addFirestore() {

        val pointsSave = mutableListOf<LatLng>()
        val pointsSaveId = mutableListOf<String>()

        db.collection("point of interest")
            .get()
            .await()
            .forEach { document ->
                val coordinate = document.getGeoPoint("coordinate")
                pointsSave.add(LatLng(coordinate!!.latitude, coordinate.longitude))
                pointsSaveId.add(document.id)
            }

        for (p in routePoints) {
            db.collection("waypoint").add(
                Waypoint(
                    routeId,
                    if (pointsSave.indexOf(p) != -1) pointsSaveId[pointsSave.indexOf(p)] else null,
                    GeoPoint(p.latitude, p.longitude),
                    routePoints.indexOf(p) + 1
                )
            )
                .await()
        }
    }

    private suspend fun updateFirestore() {

        val pointsSave = mutableListOf<LatLng>()
        val pointsSaveId = mutableListOf<String>()

        db.collection("point of interest")
            .get()
            .await()
            .forEach { document ->
                val coordinate = document.getGeoPoint("coordinate")
                pointsSave.add(LatLng(coordinate!!.latitude, coordinate.longitude))
                pointsSaveId.add(document.id)
            }

        db.collection("optimized route")
            .whereEqualTo("id route", routeId)
            .get()
            .await()
            .forEach { document ->
                db.collection("waypoint")
                    .whereEqualTo("id route", document.id)
                    .get()
                    .await()
                    .forEach { p ->
                        p.reference.delete()
                    }
                document.reference.delete()
            }


        for (p in oldRoutePoints) {
            db.collection("waypoint").whereEqualTo("id route", routeId)
                .get()
                .await()
                .forEach { document ->
                    document.reference.delete()
                }
        }

        for (p in routePoints) {
            db.collection("waypoint").add(
                Waypoint(
                    routeId,
                    if (pointsSave.indexOf(p) != -1) pointsSaveId[pointsSave.indexOf(p)] else null,
                    GeoPoint(p.latitude, p.longitude),
                    routePoints.indexOf(p) + 1
                )
            )
                .await()
        }

        for (p in newRoutePoints) {
            db.collection("waypoint").add(
                Waypoint(
                    routeAllId,
                    if (pointsSave.indexOf(p) != -1) pointsSaveId[pointsSave.indexOf(
                        p
                    )] else null,
                    GeoPoint(p.latitude, p.longitude),
                    newRoutePoints.indexOf(p) + 1
                )
            )
                .await()
        }
    }
}