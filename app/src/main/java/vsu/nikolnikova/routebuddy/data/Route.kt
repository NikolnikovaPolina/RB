package vsu.nikolnikova.routebuddy.data

import com.google.android.libraries.places.api.model.LocalDate
import com.google.firebase.firestore.PropertyName

data class Route(
    val name: String,
    val description: String?,
    val distance: Double,
    val cost: Double?,
    @get:PropertyName("date of create")
    val dateOfCreate: LocalDate,
    @get:PropertyName("date of update")
    val dateOfUpdate: LocalDate?,
    @get:PropertyName("id user")
    val idUser: String,
)