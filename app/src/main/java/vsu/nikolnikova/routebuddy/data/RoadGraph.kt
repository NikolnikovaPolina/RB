package vsu.nikolnikova.routebuddy.data

import com.google.firebase.firestore.GeoPoint
import java.util.PriorityQueue
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RoadGraph {
    private val roads = mutableListOf<Road>()

    fun addRoad(road: Road) {
        roads.add(road)
    }

    private fun buildGraphTime(): Map<GeoPoint, Map<GeoPoint, Int>> {
        val graph = mutableMapOf<GeoPoint, MutableMap<GeoPoint, Int>>()
        for (road in roads) {
            val start = road.startPoint
            val finish = road.finishPoint
            val travelTime = road.travelTime
            graph.getOrPut(start) { mutableMapOf() }[finish] = travelTime
            graph.getOrPut(finish) { mutableMapOf() }[start] = travelTime
        }
        return graph.toMap()
    }

    fun findPathTime(start: GeoPoint, end: GeoPoint): List<GeoPoint>? {
        val graph = buildGraphTime()
        val distances = mutableMapOf<GeoPoint, Int>()
        val previous = mutableMapOf<GeoPoint, GeoPoint?>()
        val queue = PriorityQueue<Pair<Int, GeoPoint>> { (d1, _), (d2, _) -> d1.compareTo(d2) }

        distances[start] = 0
        previous[start] = null
        queue.offer(0 to start)

        while (queue.isNotEmpty()) {
            val (_, current) = queue.poll()!!
            if (current == end) {
                return reconstructPath(previous, end)
            }
            for ((neighbor, weight) in graph[current] ?: emptyMap()) {
                val newDistance = distances[current]!! + weight
                if (neighbor !in distances || newDistance < distances[neighbor]!!) {
                    distances[neighbor] = newDistance
                    previous[neighbor] = current
                    queue.offer(newDistance to neighbor)
                }
            }
        }

        return null
    }

    private fun buildGraphDistance(): Map<GeoPoint, Map<GeoPoint, Double>> {
        val graph = mutableMapOf<GeoPoint, MutableMap<GeoPoint, Double>>()
        for (road in roads) {
            val start = road.startPoint
            val finish = road.finishPoint
            val distance = road.distance
            graph.getOrPut(start) { mutableMapOf() }[finish] = distance
            graph.getOrPut(finish) { mutableMapOf() }[start] = distance
        }
        return graph.toMap()
    }

    fun findPathDistance(start: GeoPoint, end: GeoPoint): List<GeoPoint>? {
        val graph = buildGraphDistance()
        val distances = mutableMapOf<GeoPoint, Double>()
        val previous = mutableMapOf<GeoPoint, GeoPoint?>()
        val queue = PriorityQueue<Pair<Double, GeoPoint>> { (d1, _), (d2, _) -> d1.compareTo(d2) }

        distances[start] = 0.0
        previous[start] = null
        queue.offer(0.0 to start)

        while (queue.isNotEmpty()) {
            val (_, current) = queue.poll()!!
            if (current == end) {
                return reconstructPath(previous, end)
            }
            for ((neighbor, weight) in graph[current] ?: emptyMap()) {
                val newDistance = distances[current]!! + weight
                if (neighbor !in distances || newDistance < distances[neighbor]!!) {
                    distances[neighbor] = newDistance
                    previous[neighbor] = current
                    queue.offer(newDistance to neighbor)
                }
            }
        }

        return null
    }

    private fun reconstructPath(previous: Map<GeoPoint, GeoPoint?>, end: GeoPoint): List<GeoPoint> {
        val path = mutableListOf<GeoPoint>()
        var current: GeoPoint? = end
        while (current != null) {
            path.add(0, current)
            current = previous[current]
        }
        return path
    }

    fun findNearestRoadPoint(currentPoint: GeoPoint): GeoPoint? {
        var nearestPoint: GeoPoint? = null
        var minDistance = 50.0

        for (road in roads) {
            val startPoint = road.startPoint
            val endPoint = road.finishPoint

            val distanceStart = calculateDistance(currentPoint, startPoint)
            val distanceEnd = calculateDistance(currentPoint, endPoint)

            if (distanceStart < minDistance) {
                minDistance = distanceStart
                nearestPoint = startPoint
            }
            if (distanceEnd < minDistance) {
                minDistance = distanceEnd
                nearestPoint = endPoint
            }
        }
        return nearestPoint
    }

    private fun calculateDistance(start: GeoPoint, end: GeoPoint): Double {
        val earthRadius = 6371000
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLon = Math.toRadians(end.longitude - start.longitude)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * acos(sqrt(1 - a))

        return earthRadius * c
    }
}