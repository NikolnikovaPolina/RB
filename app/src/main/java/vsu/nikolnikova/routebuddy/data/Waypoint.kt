package vsu.nikolnikova.routebuddy.data

import com.google.firebase.firestore.PropertyName

data class Waypoint(
    @get:PropertyName("id route")
    val idRoute: String,
    @get:PropertyName("id point of interest")
    val idPointOfInterest: String,
    @get:PropertyName("order of the visit")
    val orderOfTheVisit: Int
)