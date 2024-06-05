package vsu.nikolnikova.routebuddy.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Route(
    val name: String,
    val description: String?,
    val distance: Double,
    @get:PropertyName("date of create")
    val dateOfCreate: Timestamp,
    @get:PropertyName("date of update")
    val dateOfUpdate: Timestamp?,
    @get:PropertyName("id user")
    val idUser: String,
)