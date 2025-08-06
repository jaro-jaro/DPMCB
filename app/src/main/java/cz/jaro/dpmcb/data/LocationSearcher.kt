
import cz.jaro.dpmcb.data.entities.BusName
import cz.jaro.dpmcb.data.entities.bus
import cz.jaro.dpmcb.data.entities.line
import cz.jaro.dpmcb.data.jikord.MapData
import cz.jaro.dpmcb.data.jikord.Transmitter
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

object LocationSearcher {

    private val LineSegment.length get() = start from end

    @Serializable
    private data class Point(val x: Double, val y: Double) {
        override fun toString() = "${x}N, ${y}E"
    }

    private data class Vector(val x: Double, val y: Double)

    // dot product
    private infix fun Vector.dot(other: Vector) = x * other.x + y * other.y
    private infix operator fun Point.minus(other: Point) = Vector(x - other.x, y - other.y)
    private operator fun Point.plus(vector: Vector) = Point(x = x + vector.x, y = y + vector.y)
    private operator fun Vector.times(multiplier: Double) = Vector(x = x * multiplier, y = y * multiplier)
    private operator fun Double.times(multiplier: Vector) = multiplier * this

    private fun closestPointOnSegment(segment: LineSegment, point: Point): Double {
        val segmentVector = segment.end minus segment.start
        val pointVector = point minus segment.start

        val segmentLengthSquared = segmentVector dot segmentVector
        if (segmentLengthSquared == 0.0) return 1.0 // The segment is a point

        // Projection factor
        val t = (pointVector dot segmentVector) / segmentLengthSquared

        // Clamp t between 0 and 1 to ensure the projection lies on the segment
        val tClamped = t.coerceIn(0.0, 1.0)

        return tClamped
    }

    private infix fun Point.from(other: Point): Double {
        return sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
    }

    @Serializable
    private data class Result(
        val point: Point,
        val distance: Double,
        val segment: LineSegment,
        val stopSegment: StopSegment,
        val stopsFromStart: Double,
    )

    @Serializable
    sealed interface SearchResult {
        data class FoundOne(
            val stopsFromStart: Double,
            val nextStop: String,
        ) : SearchResult

        data class FoundMore(
            val options: List<FoundOne>,
        ) : SearchResult

        data object NotFound : SearchResult
        data object NoData : SearchResult
        data object NoTransmitters : SearchResult
    }

    fun search(
        busName: BusName,
        onDownload: () -> Unit = {},
    ): SearchResult {
        val mapData = getMapDataPerConnName(busName)
        onDownload()
        if (mapData == null) {
            return SearchResult.NoData
        }
        if (mapData.transmitters.isEmpty()) {
            return SearchResult.NoTransmitters
        }
        val transmitter = mapData.transmitters.single()
        val targetPoint = transmitter.toPoint()

        return (mapData.routeStops ?: emptyList())
            .asSequence()
            .map {
                Point(it.x, it.y) to it.n
            }
            .fold(emptyList<StopSegmentLine>()) { result, (point, name) ->
                val r = result.toMutableList()
                if (name != null) {
                    if (r.isNotEmpty()) r[r.lastIndex] = r[r.lastIndex].copy(
                        segment = r[r.lastIndex].segment.addEndStop(name),
                    )
                    r += StopSegmentLine(
                        segment = StopSegment(name),
                        lines = listOf(LineSegment(point)),
                    )
                }
                if (r.isNotEmpty()) {
                    r[r.lastIndex] = r.addSegment(LineSegment(r.lastPoint(), point))
                }
                r
            }
            .map { it.copy(lines = it.lines.drop(1)) }
            .dropLast(1)
            .mapIndexed { i, stopSegmentLine ->
                val totalPathLength = stopSegmentLine.lines.sumOf { it.length }
                stopSegmentLine.lines
                    .fold(emptyList<Result>() to .0) { (resultList, pathLength), segment ->
                        val t = closestPointOnSegment(segment, targetPoint)
                        val projectedPoint = segment.pointIn(t)
                        val dist = projectedPoint from targetPoint

                        resultList + Result(
                            distance = dist,
                            point = projectedPoint,
                            segment = segment,
                            stopSegment = stopSegmentLine.segment,
                            stopsFromStart = i + (pathLength + t * segment.length) / totalPathLength
                        ) to pathLength + segment.length
                    }.first
                    .minBy { it.distance }
            }
            .sortedBy { it.distance }.let { list ->
                list.map {
                    it to list.first().distance / it.distance
                }.filter {
                    it.second > .99
                }.map {
                    it.first
                }
            }
            .let { list ->
                if (list.isEmpty()) SearchResult.NotFound
                else if (list.size == 1) list.single().toSearchResult()
                else if (transmitter.angle == null) SearchResult.FoundMore(list.map { it.toSearchResult() })
                else list.minBy {
                    val angle1 = transmitter.angle!!
                    val toNextPoint = it.segment.end - it.point
                    val angle2 = atan(
                        toNextPoint.x / toNextPoint.y
                    ) / 2 * PI + (if (toNextPoint.y > 0) 360 else 180) % 360
                    (angle2 - angle1 + 360) % 360
                }.toSearchResult()
            }
    }

    /**
     * * From north
     * * Clocwise
     * * In degrees
     */
    private val Transmitter.angle get() = ico.substringAfter("a_").substringBefore(".png").toIntOrNull()

    private fun LineSegment.pointIn(fraction: Double) = start + (end - start) * fraction
    private fun Transmitter.toPoint() = Point(x, y)

    private fun List<StopSegmentLine>.addSegment(segment: LineSegment) = last().addSegment(segment)
    private fun StopSegmentLine.addSegment(segment: LineSegment) =
        copy(lines = lines + segment)

    @JvmName("LastPointOnStopSegment")
    private fun List<StopSegmentLine>.lastPoint() = last().lines.lastPoint()
    private fun List<LineSegment>.lastPoint() = last().end

    private data class StopSegmentLine(
        val segment: StopSegment,
        val lines: List<LineSegment>,
    )

    @Serializable
    private data class StopSegment(val startStop: String, val endStop: String)

    private fun StopSegment(startStop: String) = StopSegment(startStop, "")

    @Serializable
    private data class LineSegment(val start: Point, val end: Point)

    private fun LineSegment(end: Point) = LineSegment(Point(.0, .0), end)

    private fun StopSegment.addEndStop(endStop: String) = StopSegment(
        startStop = startStop,
        endStop = endStop,
    )

    private val client = HttpClient()

    private val json = Json {
        explicitNulls = false
    }

    private fun getMapDataPerConnName(busName: BusName): MapData? {

        return null
        val data = runBlocking {
            client.post("https://mpvnet.cz/jikord/map/getRoute") {
                header("accept", "application/json, text/javascript, */*; q=0.01")
                header("accept-language", "en-US,en;q=0.9,cs-CZ;q=0.8,cs;q=0.7")
                header("content-type", "application/json; charset=UTF-8")
                header("Referer", "https://mpvnet.cz/jikord/map")
                setBody("""{"num1":"${busName.line()}","num2":"${busName.bus()}","carrier":0,"cat":2,"trajectory":true}""")
            }.bodyAsText()
        }
        if (data == "null") return null
        return json.decodeFromString(data)
    }

    private fun Result.toSearchResult() = SearchResult.FoundOne(
        stopsFromStart = stopsFromStart,
        nextStop = stopSegment.endStop,
    )
}
