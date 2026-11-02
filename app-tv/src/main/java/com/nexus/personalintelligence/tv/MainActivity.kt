package com.nexus.personalintelligence.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.nexus.personalintelligence.tv.ui.NexusTvApp

class MainActivity : ComponentActivity() {
    private val viewModel: TvViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexusTvApp(viewModel, startedAsHome = intent?.hasCategory(Intent.CATEGORY_HOME) == true) }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}
