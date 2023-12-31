package cz.jaro.dpmcb.data.jikord


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data z mapy
 *
 * @param language
 * @param initialZoom
 * @param labelDetailMinZoomLevel
 * @param labelDetailDelayMinZoomLevel
 * @param refreshInterval
 * @param lat
 * @param lng
 * @param portal Jikord
 * @param transmitters
 * @param routeStops
 * @param stops
 * @param selectedStop
 * @param external
 */
@Serializable
@SerialName("MapData")
data class MapData(
    @SerialName("language") val language: String,
    @SerialName("initialZoom") val initialZoom: Int,
    @SerialName("labelDetailMinZoomLevel") val labelDetailMinZoomLevel: Int,
    @SerialName("labelDetailDelayMinZoomLevel") val labelDetailDelayMinZoomLevel: Int,
    @SerialName("refreshInterval") val refreshInterval: Int,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("portal") val portal: String,
    @SerialName("transmitters") val transmitters: List<SpojNaMape>,
    @SerialName("routeStops") val routeStops: Nothing?,
    @SerialName("stops") val stops: List<Nothing>,
    @SerialName("selectedStop") val selectedStop: Nothing?,
    @SerialName("external") val `external`: Nothing?,
)