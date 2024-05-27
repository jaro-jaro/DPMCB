package cz.jaro.dpmcb.data.realtions

enum class StopType {
    Normal, GetOnOnly, GetOffOnly;
    companion object
}
operator fun StopType.Companion.invoke(stopFixedCodes: String) = when {
    stopFixedCodes.contains("22") -> StopType.GetOnOnly
    stopFixedCodes.contains("21") -> StopType.GetOffOnly
    else -> StopType.Normal
}
