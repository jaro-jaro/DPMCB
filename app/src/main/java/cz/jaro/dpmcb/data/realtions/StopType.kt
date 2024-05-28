package cz.jaro.dpmcb.data.realtions

enum class StopType {
    Normal, GetOnOnly, GetOffOnly;
    companion object
}
operator fun StopType.Companion.invoke(connStopFixedCodes: String) = when {
    connStopFixedCodes.contains(")") -> StopType.GetOnOnly
    connStopFixedCodes.contains("(") -> StopType.GetOffOnly
    else -> StopType.Normal
}
