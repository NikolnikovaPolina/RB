package vsu.nikolnikova.routebuddy.data

import com.google.firebase.firestore.PropertyName

data class Waypoint(
    @get:PropertyName("uid route")
    val uidRoute: String,
    @get:PropertyName("uid point of interest")
    val uidPointOfInterest: String,
    @get:PropertyName("order of the visit")
    val orderOfTheVisit: Int,
    val cost: Double?
)