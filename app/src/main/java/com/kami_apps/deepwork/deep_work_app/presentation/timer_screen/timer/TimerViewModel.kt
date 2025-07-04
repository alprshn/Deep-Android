package com.kami_apps.deepwork.deep_work_app.presentation.timer_screen.timer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.kami_apps.deepwork.deep_work_app.data.UiState
import com.kami_apps.deepwork.deep_work_app.data.local.entities.Sessions
import com.kami_apps.deepwork.deep_work_app.data.local.entities.Tags
import com.kami_apps.deepwork.deep_work_app.data.manager.TimerManager
import com.kami_apps.deepwork.deep_work_app.data.service.AppBlockingService
import com.kami_apps.deepwork.deep_work_app.domain.usecases.AddTagUseCase
import com.kami_apps.deepwork.deep_work_app.domain.usecases.GetAllTagsUseCase
import com.kami_apps.deepwork.deep_work_app.domain.usecases.StartFocusSessionUseCase
import com.kami_apps.deepwork.deep_work_app.presentation.timer_screen.TimerUiState
import com.kami_apps.deepwork.deep_work_app.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val timerManager: TimerManager,
    private val addTag: AddTagUseCase,
    private val startFocusSession: StartFocusSessionUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel(), TimerActions {

    private val _uiState = MutableStateFlow<UiState>(UiState.Success(timerManager.timerState))
    val uiState: StateFlow<UiState> = _uiState

    private val _allTagList = MutableStateFlow(emptyList<Tags>())
    val allTagList = _allTagList.asStateFlow()

    private val _timerUIState = MutableStateFlow(TimerUiState())
    val timerUIState: StateFlow<TimerUiState> = _timerUIState.asStateFlow()

    val timerState = timerManager.timerState.asLiveData()

    override fun setHour(hour: Int) {
        timerManager.setTHour(hour)
    }

    override fun setMinute(minute: Int) {
        timerManager.setMinute(minute)
        timerManager.setCountDownTimer()
    }

    override fun setSecond(second: Int) {
        timerManager.setSecond(second)
        timerManager.setCountDownTimer()
    }

    override fun setCountDownTimer() {
        timerManager.setCountDownTimer()
    }

    override fun start() {
        viewModelScope.launch {
            val currentState = _timerUIState.value
            val currentMinutes = timerState.value?.minute ?: 0
            
            if (currentState.isStarted) {
                // This is a RESUME action
                Log.d("Timer", "Resuming paused timer")
                _timerUIState.value = currentState.copy(timerIsRunning = true)
                timerManager.start()
                
                // Resume app blocking if permissions are granted
                if (PermissionHelper.hasAllPermissions(context)) {
                    startAppBlocking()
                }
            } else {
                // This is a NEW START action
                if (currentMinutes <= 0) {
                    Log.e("Timer", "Cannot start timer with 0 minutes")
                    return@launch
                }
                
                // CountDownTimer'ı tekrar kur
                timerManager.setCountDownTimer()
                
                val now = Date()
                Log.e("Timer Start", "Starting NEW timer with $currentMinutes minutes at $now")
                _timerUIState.value = _timerUIState.value.copy(
                    startTime = now,
                    isStarted = true,
                    timerIsRunning = true
                )
                timerManager.start()
                
                // Start app blocking if permissions are granted
                if (PermissionHelper.hasAllPermissions(context)) {
                    startAppBlocking()
                }
            }
        }
    }

    override fun pause() {
        timerManager.pause()
        _timerUIState.value = _timerUIState.value.copy(
            timerIsRunning = false
        )
        
        // Stop app blocking when pausing
        stopAppBlocking()
    }

    override fun reset() {
        timerManager.reset()
        _timerUIState.value = _timerUIState.value.copy(
            startTime = null,
            finishTime = null,
            isStarted = false,
            timerIsRunning = false
        )
        
        // Stop app blocking when resetting
        stopAppBlocking()
    }

    fun completeSession() {
        val finish = Date()
        val currentState = _timerUIState.value
        val updatedState = currentState.copy(
            finishTime = finish,
            isStarted = false,
            timerIsRunning = false
        )
        _timerUIState.value = updatedState
        
        val tagId = updatedState.tagId
        val startTime = updatedState.startTime
        val finishTime = updatedState.finishTime
        val selectedTagEmoji = updatedState.selectedTagEmoji
        
        // Timer için süreyi hesapla
        val totalMinutes = timerState.value?.minute ?: 0
        val remainingMinutes = timerState.value?.minute ?: 0
        val remainingSeconds = timerState.value?.second ?: 0
        val completedMinutes = totalMinutes - remainingMinutes
        val completedSeconds = if (remainingSeconds > 0) 60 - remainingSeconds else 0
        val duration = "$completedMinutes:${if (completedSeconds < 10) "0$completedSeconds" else completedSeconds}"
        
        viewModelScope.launch(Dispatchers.IO) {
            timerManager.reset()
            
            // Stop app blocking when session completes
            stopAppBlocking()
            
            startFocusSession.invoke(
                Sessions(
                    tagId = tagId,
                    startTime = startTime,
                    finishTime = finishTime,
                    duration = duration,
                    sessionEmoji = selectedTagEmoji
                )
            )
            Log.e("Timer Duration", duration)
        }
    }

    fun addTag(tagName: String, tagColor: String, tagEmoji: String) {
        viewModelScope.launch {
            addTag.invoke(
                Tags(
                    tagName = tagName,
                    tagColor = tagColor,
                    tagEmoji = tagEmoji
                )
            )
        }
    }

    fun getAllTag() {
        viewModelScope.launch {
            getAllTagsUseCase.invoke().collectLatest {
                _allTagList.tryEmit(it)
            }
        }
    }

    fun setTimerFromMinutes(minutes: Int) {
        setMinute(minutes)
        setSecond(0)
        setHour(0)
        setCountDownTimer()
    }
    
    // App Blocking Helper Methods
    private fun startAppBlocking() {
        try {
            val intent = Intent(context, AppBlockingService::class.java).apply {
                action = AppBlockingService.ACTION_START_BLOCKING
            }
            context.startForegroundService(intent)
            Log.d("TimerViewModel", "App blocking service started")
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Failed to start app blocking service", e)
        }
    }
    
    private fun stopAppBlocking() {
        try {
            val intent = Intent(context, AppBlockingService::class.java).apply {
                action = AppBlockingService.ACTION_STOP_BLOCKING
            }
            context.startService(intent)
            Log.d("TimerViewModel", "App blocking service stopped")
        } catch (e: Exception) {
            Log.e("TimerViewModel", "Failed to stop app blocking service", e)
        }
    }
}