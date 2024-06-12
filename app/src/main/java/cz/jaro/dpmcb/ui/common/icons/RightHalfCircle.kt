@file:Suppress("ObjectPropertyName", "UnusedReceiverParameter")

package cz.jaro.dpmcb.ui.common.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


val Icons.Filled.RightHalfDisk: ImageVector
    get() {
        if (_rightHalfDisk != null) {
            return _rightHalfDisk!!
        }
        _rightHalfDisk = ImageVector.Builder(
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
        return _rightHalfDisk!!
    }

private var _rightHalfDisk: ImageVector? = null