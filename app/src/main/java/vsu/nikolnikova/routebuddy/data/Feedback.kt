package vsu.nikolnikova.routebuddy.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Feedback(
    @get:PropertyName("id user")
    val idUser: String,
    @get:PropertyName("id point of interest")
    val idPointOfInterest: String?,
    @get:PropertyName("id route")
    val idRoute: String?,
    val estimation: Int,
    val comment: String?,
    @get:PropertyName("date of create")
    val dateOfCreate: Timestamp,
    @get:PropertyName("date of update")
    val dateOfUpdate: Timestamp?
)