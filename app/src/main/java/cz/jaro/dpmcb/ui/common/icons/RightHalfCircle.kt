@file:Suppress("ObjectPropertyName", "UnusedReceiverParameter")

package cz.jaro.dpmcb.ui.common.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


val Icons.Filled.RightHalfCircle: ImageVector
    get() {
        if (_rightHalfCircle != null) {
            return _rightHalfCircle!!
        }
        _rightHalfCircle = ImageVector.Builder(
            name = "Filled.RightHalfCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24F,
            viewportHeight = 24F,
        ).materialPath {
            moveTo(x = 12F, y = 2F)
            arcToRelative(a = 10F, b = 10F, theta = 180F, isMoreThanHalf = false, isPositiveArc = true, dx1 = 0F, dy1 = 20F)
            close()
        }.build()
        return _rightHalfCircle!!
    }

private var _rightHalfCircle: ImageVector? = null