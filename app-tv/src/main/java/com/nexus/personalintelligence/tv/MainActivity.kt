package com.nexus.personalintelligence.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.nexus.personalintelligence.tv.data.TvLauncherLogic
import com.nexus.personalintelligence.tv.ui.NexusTvApp

class MainActivity : ComponentActivity() {
    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isHome = intent?.let { TvLauncherLogic.isHomeIntent(it.action, it.categories) } == true ||
                intent?.hasCategory(Intent.CATEGORY_HOME) == true
        setContent { NexusTvApp(viewModel, startedAsHome = isHome) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (TvLauncherLogic.isHomeIntent(intent.action, intent.categories) ||
            intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.navigateToHome()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
