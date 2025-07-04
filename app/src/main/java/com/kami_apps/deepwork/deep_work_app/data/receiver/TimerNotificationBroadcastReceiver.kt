package com.kami_apps.deepwork.deep_work_app.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kami_apps.deepwork.deep_work_app.data.manager.TimerManager
import com.kami_apps.deepwork.deep_work_app.data.manager.WorkRequestManager
import com.kami_apps.deepwork.deep_work_app.util.helper.TimerNotificationHelper
import com.kami_apps.deepwork.deep_work_app.data.workManager.worker.TIMER_COMPLETED_TAG
import com.kami_apps.deepwork.deep_work_app.util.safeLet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerNotificationBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerManager: TimerManager

    @Inject
    lateinit var timerNotificationHelper: TimerNotificationHelper

    @Inject
    lateinit var workRequestManager: WorkRequestManager

    private val broadcastReceiverScope = CoroutineScope(SupervisorJob())

    override fun onReceive(p0: Context?, intent: Intent?) {
        val pendingResult: PendingResult = goAsync()
        broadcastReceiverScope.launch(Dispatchers.Default) {
            try {
                val isPlaying = intent?.getBooleanExtra(TIMER_RUNNING_IS_PLAYING, false)
                val timeText = intent?.getStringExtra(TIMER_RUNNING_TIME_TEXT)
                val action = intent?.action

                action?.let {
                    when (it) {
                        TIMER_RUNNING_CANCEL_ACTION -> {
                            timerManager.reset()
                            timerNotificationHelper.removeTimerRunningNotification()
                        }
                        TIMER_COMPLETED_DISMISS_ACTION -> {
                            workRequestManager.cancelWorker(TIMER_COMPLETED_TAG)
                            timerNotificationHelper.removeTimerCompletedNotification()
                        }
                        TIMER_COMPLETED_RESTART_ACTION -> {
                            workRequestManager.cancelWorker(TIMER_COMPLETED_TAG)
                            timerNotificationHelper.removeTimerCompletedNotification()
                            timerManager.start()
                        }
                    }
                }

                safeLet(isPlaying, timeText) { safeIsPlaying, safeTime ->
                    if (safeIsPlaying) {
                        timerManager.pause()
                    } else {
                        timerManager.start()
                    }
                }
            } finally {
                pendingResult.finish()
                broadcastReceiverScope.cancel()
            }
        }
    }
}

const val TIMER_RUNNING_TIME_TEXT = "TIMER_RUNNING_TIME_TEXT"
const val TIMER_RUNNING_IS_PLAYING = "TIMER_RUNNING_IS_PLAYING"
const val TIMER_RUNNING_CANCEL_ACTION = "TIMER_RUNNING_CANCEL_ACTION"
const val TIMER_COMPLETED_DISMISS_ACTION = "TIMER_COMPLETED_DISMISS_ACTION"
const val TIMER_COMPLETED_RESTART_ACTION = "TIMER_COMPLETED_RESTART_ACTION"