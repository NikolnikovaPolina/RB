package vsu.nikolnikova.routebuddy.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import vsu.nikolnikova.routebuddy.R
import vsu.nikolnikova.routebuddy.data.OptimizedRoute
import vsu.nikolnikova.routebuddy.data.Road
import vsu.nikolnikova.routebuddy.data.RoadGraph
import vsu.nikolnikova.routebuddy.data.Waypoint
import java.util.Locale
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


@Suppress("DEPRECATION")
class MapsRouteActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap

    private var db = Firebase.firestore

    private lateinit var traffic: ImageButton

    private lateinit var optimization: Button

    private lateinit var plus: ImageButton
    private lateinit var minus: ImageButton

    private lateinit var last: ImageButton

    private lateinit var mapView: MapView

    private lateinit var routeId: String

    private var existingPolyline: PolylineOptions? = null

    private var routePoints = mutableListOf<LatLng>()

    private lateinit var location: FusedLocationProviderClient
    private lateinit var locationButton: ImageButton

    private var currentLocation: LatLng = LatLng(51.656680042276435, 39.206041019902685)

    private var permissionId = 42

    private var country = ""
    private var city = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps_route)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_route) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mapView = MapView(this)
        mapView.setTileSource(TileSourceFactory.MAPNIK)

        routeId = intent.getStringExtra("routeId").toString()


        val list = intent.getSerializableExtra("routePoints") as? ArrayList<LatLng>

        if (list!!.size != 0) {
            routePoints = list.toMutableList()

            for (point in routePoints) {
                val country = getCountryByCoordinates(point)
                val city = getCityFromCoordinates(point)

                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        addFirestoreInformation(country, city)

                    }
                }
            }
        }

        optimization = findViewById(R.id.optimization)
        optimization.setOnClickListener {
            showDialog()
        }

        traffic = findViewById(R.id.traffic)
        traffic.alpha = 0.4f

        plus = findViewById(R.id.plus)
        minus = findViewById(R.id.minus)

        last = findViewById(R.id.last)

        location = LocationServices.getFusedLocationProviderClient(this)
        locationButton = findViewById(R.id.location)

        locationButton.setOnClickListener {
            getLastLocation()
        }

        last.setOnClickListener {
            val intent = Intent(this, ReadyMadeRoutesActivity::class.java)
            intent.putExtra("category", this.intent.getStringExtra("category"))
            startActivity(intent)
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        traffic.setOnClickListener {
            if (!mMap.isTrafficEnabled) {
                mMap.isTrafficEnabled = true
                traffic.alpha = 0.9f
            } else {
                mMap.isTrafficEnabled = false
                traffic.alpha = 0.4f
            }
        }

        plus.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomIn())
        }

        minus.setOnClickListener {
            mMap.animateCamera(CameraUpdateFactory.zoomOut())
        }

        mMap.isBuildingsEnabled = true
        mMap.isIndoorEnabled = true

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(routePoints[0], 16f))

        firstShowRoute()
    }

    private fun firstShowRoute() {
        for (point in routePoints) {
            mMap.addMarker(
                MarkerOptions().position(point)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                location.lastLocation.addOnCompleteListener(this) { task ->
                    val location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        mMap.clear()
                        mMap.addMarker(MarkerOptions().position(currentLocation))
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                currentLocation,
                                16F
                            )
                        )
                    }
                }
            } else {
                Toast.makeText(this, "Отключена геопозиция", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
                finish()
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        location = LocationServices.getFusedLocationProviderClient(this)
        location.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            if (mLastLocation != null) {
                currentLocation = LatLng(mLastLocation.latitude, mLastLocation.longitude)
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    private fun getCountryByCoordinates(coordinates: LatLng): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address> =
            geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)!!

        return addresses[0].countryName
    }

    private fun getCityFromCoordinates(coordinates: LatLng): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address> =
            geocoder.getFromLocation(coordinates.latitude, coordinates.longitude, 1)!!

        return addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea
    }

    private suspend fun addFirestoreInformation(country: String, city: String) {

        var countryId: String
        var cityId: String
        val listRoads = getOSMRoadData(city)

        db.collection("country").whereEqualTo("name", country)
            .get().addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    db.collection("country").add(mapOf("name" to country))
                        .addOnSuccessListener { document ->
                            countryId = document.id
                            db.collection("country").document(countryId)
                                .collection("city")
                                .add(mapOf("name" to city)).addOnSuccessListener { doc ->
                                    cityId = doc.id
                                    for (road in listRoads) {
                                        db.collection("country").document(countryId)
                                            .collection("city").document(cityId)
                                            .collection("road").add(road)
                                    }
                                }
                        }
                } else {
                    countryId = documents.first().id
                    db.collection("country").document(countryId).collection("city")
                        .whereEqualTo("name", city)
                        .get().addOnSuccessListener { docs ->
                            if (docs.isEmpty) {
                                db.collection("country").document(countryId)
                                    .collection("city").add(mapOf("name" to city))
                                    .addOnSuccessListener { doc ->
                                        cityId = doc.id
                                        for (road in listRoads) {
                                            db.collection("country").document(countryId)
                                                .collection("city").document(cityId)
                                                .collection("road").add(road)
                                        }
                                    }
                            }
                        }
                }
            }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogStyle)

        builder.setTitle("Выберите параметр оптимизации")

        builder.setPositiveButton("время") { _, _ ->
            db.collection("optimized route")
                .whereEqualTo("id route", intent.getStringExtra("routeId").toString())
                .whereEqualTo("id type of optimization", "KbO3hILnYdlvi7hCGmmM")
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        db.collection("optimized route").add(
                            OptimizedRoute(
                                "9rcB2TVtXXaTbSUyDysq",
                                routeId,
                                0.0
                            )
                        )
                            .addOnSuccessListener {
                                CoroutineScope(Dispatchers.Main).launch {
                                    withContext(Dispatchers.IO) {
                                        createRoute(it.id, "time")
                                        showRoute(it.id)
                                    }
                                }
                            }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            withContext(Dispatchers.IO) {
                                showRoute(documents.first().id)
                            }
                        }
                    }
                }
        }

        builder.setNegativeButton("расстояние") { _, _ ->
            db.collection("optimized route")
                .whereEqualTo("id route", intent.getStringExtra("routeId").toString())
                .whereEqualTo("id type of optimization", "9rcB2TVtXXaTbSUyDysq")
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        db.collection("optimized route").add(
                            OptimizedRoute(
                                "9rcB2TVtXXaTbSUyDysq",
                                routeId,
                                0.0
                            )
                        )
                            .addOnSuccessListener {
                                CoroutineScope(Dispatchers.Main).launch {
                                    withContext(Dispatchers.IO) {
                                        createRoute(it.id, "distance")
                                        showRoute(it.id)
                                    }
                                }
                            }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            withContext(Dispatchers.IO) {
                                showRoute(documents.first().id)
                            }
                        }
                    }
                }
        }

        builder.setNeutralButton("Отмена")
        { dialog, _ ->
            dialog.dismiss()
        }
            .show()
    }

    private suspend fun showRoute(routeId: String) {
        val route = collectPoints(routeId)
        withContext(Dispatchers.Main) {
            for (point in routePoints) {
                mMap.addMarker(
                    MarkerOptions().position(point)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
                )
            }

            if (existingPolyline != null) {
                existingPolyline = null
            }

            val pathOptions = PolylineOptions()

            for (point in route) {
                pathOptions.add(LatLng(point.latitude, point.longitude))
            }

            val distance = SphericalUtil.computeLength(pathOptions.points)

            db.collection("optimized route")
                .document(routeId)
                .update(hashMapOf("distance" to distance) as Map<String, Any>)

            mMap.addPolyline(pathOptions)
            existingPolyline = pathOptions
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
                .whereEqualTo("route", routeId)
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

    private fun getRoads(): RoadGraph {

        val graph = RoadGraph()
        var countryId: String
        var cityId: String

        db.collection("country").whereEqualTo("name", country)
            .get().addOnSuccessListener { document ->
                countryId = document.first().id
                db.collection("country").document(countryId)
                    .collection("city")
                    .add(mapOf("name" to city)).addOnSuccessListener { doc ->
                        cityId = doc.id

                        db.collection("country").document(countryId)
                            .collection("city").document(cityId)
                            .collection("road").get().addOnSuccessListener { documents ->
                                for (d in documents) {
                                    val road = Road(
                                        d.getString("type")!!,
                                        d.getDouble("distance")!!,
                                        d.getGeoPoint("start point")!!,
                                        d.getGeoPoint("finish point")!!
                                    )
                                    graph.addRoad(road)
                                }
                            }
                    }
            }
        return graph
    }

    private suspend fun createRoute(routeId: String, type: String) {

        val routeNew = mutableListOf<GeoPoint>()

        val graph = getRoads()

        var start = GeoPoint(routePoints[0].latitude, routePoints[0].longitude)

        val c1 =
            graph.findNearestRoadPoint(GeoPoint(routePoints[0].latitude, routePoints[0].longitude))

        if (c1 != start) {
            routeNew.add(start)
            start = c1!!
        }

        for (i in 1..<routePoints.size) {

            val end = graph.findNearestRoadPoint(
                GeoPoint(routePoints[i].latitude, routePoints[i].longitude)
            )

            if (end != null) {
                val path: List<GeoPoint>? = if (type == "time") {
                    graph.findPathTime(start, end)
                } else {
                    graph.findPathDistance(start, end)
                }

                if (path != null) {
                    routeNew.addAll(path)
                } else {
                    break
                }

                if (end != GeoPoint(routePoints[i].latitude, routePoints[i].longitude)) {
                    routeNew.add(GeoPoint(routePoints[i].latitude, routePoints[i].longitude))
                    if (i != routePoints.size - 1) {
                        routeNew.add(end)
                    }
                }

                start = end
            } else {
                break
            }
        }

        if (routeNew[routeNew.size - 1] == GeoPoint(
                routePoints[routePoints.size - 1].latitude,
                routePoints[routePoints.size - 1].longitude
            )
        ) {
            addFirestore(routeNew, routeId)
        }
    }

    private suspend fun addFirestore(routeNew: List<GeoPoint>, routeId: String) {

        val pointsSave = mutableListOf<GeoPoint>()
        val pointsSaveId = mutableListOf<String>()

        db.collection("point of interest")
            .get()
            .await()
            .forEach { document ->
                val coordinate = document.getGeoPoint("coordinate")!!
                pointsSave.add(coordinate)
                pointsSaveId.add(document.id)
            }

        for (point in routeNew) {
            db.collection("waypoint").add(
                Waypoint(
                    routeId,
                    if (pointsSave.indexOf(point) != -1) pointsSaveId[pointsSave.indexOf(
                        point
                    )] else null,
                    point,
                    routeNew.indexOf(point) + 1
                )
            )
        }
    }

    private suspend fun getOSMRoadData(city: String): List<Road> {
        return withContext(Dispatchers.IO) {
            val overpassQuery = """
           [out:json];
            area["name"="$city"]->.searchArea;
            (
            way["highway"~"^(footway|path|pedestrian|steps)$"](area.searchArea);
            );
            out body;
            >;
            out skel qt;
        """.trimIndent()

            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .post(overpassQuery.toRequestBody())
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonData = response.body?.string()
                val osmData = Gson().fromJson(jsonData, JsonObject::class.java)

                return@withContext processOSMData(osmData)
            } else {
                throw Exception("Error fetching data from Overpass API: ${response.message}")
            }
        }
    }

    private fun processOSMData(osmData: JsonObject): List<Road> {
        val list = mutableListOf<Road>()
        val elements = osmData.getAsJsonArray("elements")

        for (element in elements) {
            val wayObject = element.asJsonObject
            val roadType = wayObject.getAsJsonObject("tags")?.getAsJsonObject()
                ?.get("highway")?.asString!!

            val nodes = wayObject.getAsJsonArray("nodes")
            var start = GeoPoint(0.0, 0.0)
            var end: GeoPoint

            for (node in nodes) {
                val nodeObject = elements.find { it.asJsonObject["id"] == node }?.asJsonObject!!
                if (start == GeoPoint(0.0, 0.0)) {
                    start = GeoPoint(
                        nodeObject["lat"].asDouble,
                        nodeObject["lon"].asDouble
                    )
                } else {
                    end = start
                    start = GeoPoint(
                        nodeObject["lat"].asDouble,
                        nodeObject["lon"].asDouble
                    )
                    val length = calculateDistance(start, end)
                    list.add(Road(roadType, length, start, end))
                }
            }
        }
        return list
    }

    private fun calculateDistance(start: GeoPoint, end: GeoPoint): Double {
        val earthRadius = 6371000

        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * acos(sqrt(1 - a))

        return earthRadius * c
    }
}