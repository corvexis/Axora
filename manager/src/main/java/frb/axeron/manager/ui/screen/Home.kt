package frb.axeron.manager.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import frb.axeron.manager.ui.navigation.LocalTabSelected
import frb.axeron.manager.ui.viewmodel.ViewModelGlobal

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun HomeScreen(navigator: DestinationsNavigator, viewModelGlobal: ViewModelGlobal) {
    HomeCircleScreen(
        navigator = navigator,
        activateViewModel = viewModelGlobal.activateViewModel,
        pluginViewModel = viewModelGlobal.pluginViewModel,
        privilegeViewModel = viewModelGlobal.privilegeViewModel,
        bannerDrawable = viewModelGlobal.settingsViewModel.bannerDrawable,
        onTabSelected = LocalTabSelected.current
    )
}
