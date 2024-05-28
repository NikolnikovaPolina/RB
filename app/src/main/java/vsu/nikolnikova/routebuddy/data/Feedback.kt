package vsu.nikolnikova.routebuddy.data

import com.google.android.libraries.places.api.model.LocalDate
import com.google.firebase.firestore.PropertyName

data class Feedback(
    @get:PropertyName("uid user")
    val uidUser: String,
    @get:PropertyName("uid point of interest")
    val uidPointOfInterest: String?,
    @get:PropertyName("uid route")
    val uidRoute: String?,
    val estimation: Double,
    val comment: String?,
    @get:PropertyName("date of create")
    val dateOfCreate: LocalDate,
    @get:PropertyName("date of update")
    val dateOfUpdate: LocalDate?
)