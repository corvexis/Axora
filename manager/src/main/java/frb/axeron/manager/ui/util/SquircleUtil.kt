package frb.axeron.manager.ui.util

import android.graphics.Path as AndroidPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

private const val SQUIRCLE_POWER = 4f
private const val SQUIRCLE_SEGMENTS = 360

internal fun squirclePath(size: Int): AndroidPath = squirclePath(size.toFloat(), size.toFloat())

internal fun squirclePath(width: Float, height: Float): AndroidPath {
    val halfWidth = width / 2f
    val halfHeight = height / 2f
    val exponent = 2f / SQUIRCLE_POWER
    val path = AndroidPath()

    for (index in 0..SQUIRCLE_SEGMENTS) {
        val angle = (2.0 * PI * index / SQUIRCLE_SEGMENTS).toFloat()
        val cosine = cos(angle)
        val sine = sin(angle)
        val x = halfWidth + halfWidth * sign(cosine) * abs(cosine).pow(exponent)
        val y = halfHeight + halfHeight * sign(sine) * abs(sine).pow(exponent)
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    path.close()
    return path
}

val SquircleShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline = Outline.Generic(
        squirclePath(size.width, size.height).asComposePath()
    )
}
