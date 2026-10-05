package io.kdmtracker.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.kdmtracker.app.ui.HuntScreen
import io.kdmtracker.app.ui.SettlementScreen
import io.kdmtracker.app.ui.ShowdownScreen

@Composable
fun KdmTrackerApp() {
    val controller = remember { LanternYearController(SettlementRepository()) }
    val state by controller.state.collectAsState()

    MaterialTheme {
        Scaffold { innerPadding ->
            Surface(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
            ) {
                when (state.screen) {
                    Screen.Settlement ->
                        SettlementScreen(
                            state = state,
                            onAdvance = controller::advance,
                            onPrevious = controller::previous,
                            onSelectStep = controller::selectStep,
                        )
                    Screen.Hunt ->
                        HuntScreen(
                            state = state,
                            onAdvance = controller::advance,
                        )
                    Screen.Showdown ->
                        ShowdownScreen(
                            state = state,
                            onAdvance = controller::advance,
                        )
                }
            }
        }
    }
}
