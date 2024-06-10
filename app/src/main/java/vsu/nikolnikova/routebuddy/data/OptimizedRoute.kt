package vsu.nikolnikova.routebuddy.data

import com.google.firebase.firestore.PropertyName

data class OptimizedRoute(
    @get:PropertyName("id type of optimization")
    val idTypeOfOptimization: String?,
    @get:PropertyName("id route")
    val idRoute: String,
    val distance: Double
)