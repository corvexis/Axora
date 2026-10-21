package frb.axeron.manager.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

val LocalTabSelected = staticCompositionLocalOf<(BottomBarDestination) -> Unit> { {} }
