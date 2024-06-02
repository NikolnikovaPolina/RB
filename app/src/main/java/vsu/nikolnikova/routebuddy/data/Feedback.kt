package vsu.nikolnikova.routebuddy.data

import com.google.android.libraries.places.api.model.LocalDate
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
    val dateOfCreate: LocalDate,
    @get:PropertyName("date of update")
    val dateOfUpdate: LocalDate?
)