package frb.axeron.manager.ui.component

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun AnimatedCounter(
    count: Int,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    modifier: Modifier = Modifier
) {
    val animatedCount by animateIntAsState(
        targetValue = count,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 180f),
        label = "animatedCounter"
    )
    Text(
        text = animatedCount.toString(),
        style = style,
        modifier = modifier
    )
}
