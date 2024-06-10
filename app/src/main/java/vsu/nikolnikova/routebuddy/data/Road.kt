package vsu.nikolnikova.routebuddy.data

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class Road(
    val type: String,
    val distance: Double,
    @get:PropertyName("start point")
    val startPoint: GeoPoint,
    @get:PropertyName("finish point")
    val finishPoint: GeoPoint
) {
    val travelTime: Int
        get() {
            return when (type) {
                "pedestrian", -> (distance / 8).toInt()
                "footway" -> (distance / 6).toInt()
                "steps" -> (distance / 4).toInt()
                else -> (distance / 2).toInt()
            }
        }
}