@file:Suppress("ObjectPropertyName", "UnusedReceiverParameter")

package cz.jaro.dpmcb.ui.common.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


val Icons.Filled.LeftHalfDisk: ImageVector
    get() {
        if (_leftHalfDisk != null) {
            return _leftHalfDisk!!
        }
        _leftHalfDisk = ImageVector.Builder(
            name = "Filled.LeftHalfCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24F,
            viewportHeight = 24F,
        ).materialPath {
            moveTo(x = 12F, y = 2F)
            arcToRelative(a = 10F, b = 10F, theta = 180F, isMoreThanHalf = false, isPositiveArc = false, dx1 = 0F, dy1 = 20F)
            close()
        }.build()
        return _leftHalfDisk!!
    }

private var _leftHalfDisk: ImageVector? = null