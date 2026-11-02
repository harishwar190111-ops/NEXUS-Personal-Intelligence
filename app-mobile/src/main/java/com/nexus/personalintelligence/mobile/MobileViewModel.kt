package com.nexus.personalintelligence.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.personalintelligence.mobile.ai.ProviderDraft
import com.nexus.personalintelligence.mobile.data.AssistantLanguage
import com.nexus.personalintelligence.mobile.data.MobileSettings
import com.nexus.personalintelligence.mobile.data.MobileSettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MobileViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MobileSettingsRepository(app)

    val settings: StateFlow<MobileSettings?> = repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setDisplayName(name: String) {
        viewModelScope.launch { repo.setDisplayName(name.trim().take(40)) }
    }

    fun setLanguage(language: AssistantLanguage) {
        viewModelScope.launch { repo.setLanguage(language) }
    }

    /** [onResult] receives null on success or an error message. */
    fun saveProvider(draft: ProviderDraft, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(repo.saveProvider(draft)) }
    }

    fun removeApiKey() {
        viewModelScope.launch { repo.removeApiKey() }
    }
}
