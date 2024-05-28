package vsu.nikolnikova.routebuddy.data

import com.google.android.libraries.places.api.model.LocalDate
import com.google.firebase.firestore.PropertyName

data class OptimizedRoute(
    val name: String,
    @get:PropertyName("uid type of optimization")
    val uidTypeOfOptimization: String,
    @get:PropertyName("uid route")
    val uidRoute: String,
    val distance: Double,
    val cost: Double?,
    @get:PropertyName("date of create")
    val dateOfCreate: LocalDate
)