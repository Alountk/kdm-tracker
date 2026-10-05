package io.kdmtracker.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kdmtracker.app.ui.HuntScreen
import io.kdmtracker.app.ui.SettlementScreen
import io.kdmtracker.app.ui.ShowdownScreen

@Composable
fun KdmTrackerApp(viewModel: LanternYearViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

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
                            onAdvance = viewModel::advance,
                            onPrevious = viewModel::previous,
                            onSelectStep = viewModel::selectStep,
                        )
                    Screen.Hunt ->
                        HuntScreen(
                            state = state,
                            onAdvance = viewModel::advance,
                            onFail = viewModel::failHunt,
                        )
                    Screen.Showdown ->
                        ShowdownScreen(
                            state = state,
                            onAdvance = viewModel::advance,
                        )
                }
            }
        }
    }
}
