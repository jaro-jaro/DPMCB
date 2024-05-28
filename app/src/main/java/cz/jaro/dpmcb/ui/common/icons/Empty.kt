@file:Suppress("ObjectPropertyName", "UnusedReceiverParameter")

package cz.jaro.dpmcb.ui.common.icons

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


val Icons.Filled.Empty: ImageVector
    get() {
        if (_empty != null) {
            return _empty!!
        }
        _empty = ImageVector.Builder(
            name = "Filled.Empty",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24F,
            viewportHeight = 24F,
        ).build()
        return _empty!!
    }

private var _empty: ImageVector? = null