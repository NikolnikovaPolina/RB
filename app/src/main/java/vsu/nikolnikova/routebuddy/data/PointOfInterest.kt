package vsu.nikolnikova.routebuddy.data

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName

data class PointOfInterest(
    val name: String,
    val description: String?,
    val coordinate: GeoPoint,
    val cost: Double?,
    @get:PropertyName("id user")
    val idUser: String,
    @get:PropertyName("id category")
    val idCategory: String
)