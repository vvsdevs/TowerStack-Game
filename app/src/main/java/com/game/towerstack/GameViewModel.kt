package com.game.towerstack

import android.app.Application
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.IOException

private val Application.settings by preferencesDataStore("tower_stack")
class GameViewModel(application: Application) : AndroidViewModel(application) {
    val engine = GameEngine()
    var revision by mutableIntStateOf(0); private set
    var phase by mutableStateOf(Phase.HOME); private set
    var score by mutableIntStateOf(0); private set
    var best by mutableIntStateOf(0); private set
    var sound by mutableStateOf(true); private set
    var vibration by mutableStateOf(true); private set
    var newRecord by mutableStateOf(false); private set
    private val bestKey = intPreferencesKey("best")
    private val soundKey = booleanPreferencesKey("sound")
    private val vibrationKey = booleanPreferencesKey("vibration")
    init {
        viewModelScope.launch {
            application.settings.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.collect {
                best = maxOf(best, it[bestKey] ?: 0)
                sound = it[soundKey] ?: true; vibration = it[vibrationKey] ?: true
            }
        }
    }
    private fun sync() { phase = engine.phase; score = engine.score; revision++ }
    fun frame(dt: Float) { engine.tick(dt); revision++ }
    fun start() { newRecord = false; engine.reset(); sync() }
    fun pause() { engine.pause(); sync() }
    fun resume() { engine.resume(); sync() }
    fun home() { engine.home(); sync() }
    fun place(): Cut? {
        val result = engine.place() ?: return null
        if (engine.score > best) {
            best = engine.score; newRecord = true
            val value = best
            viewModelScope.launch { getApplication<Application>().settings.edit { it[bestKey] = maxOf(it[bestKey] ?: 0, value) } }
        }
        sync(); return result
    }
    fun toggleSound() { sound = !sound; val value = sound; viewModelScope.launch { getApplication<Application>().settings.edit { it[soundKey] = value } } }
    fun toggleVibration() { vibration = !vibration; val value = vibration; viewModelScope.launch { getApplication<Application>().settings.edit { it[vibrationKey] = value } } }
}
