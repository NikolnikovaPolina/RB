package vsu.nikolnikova.routebuddy.data

import com.google.firebase.firestore.PropertyName

data class PointOfInterest(
    val name: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val height: Double,
    @get:PropertyName("uid user")
    val uidUser: String
)