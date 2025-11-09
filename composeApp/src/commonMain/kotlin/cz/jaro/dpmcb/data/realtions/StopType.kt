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
val StopType.canGetOn get() = this != StopType.GetOffOnly
val StopType.canGetOff get() = this != StopType.GetOnOnly