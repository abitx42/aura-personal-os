package com.example.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object AuraHaptics {

    /**
     * For extremely subtle micro-ticks, like scrolling, calendar switches, or light toggles.
     */
    fun triggerSubtleTick(view: View?) {
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /**
     * For soft sliding transitions or continuous adjustment feedback.
     */
    fun triggerSlideFeedback(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view?.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
        } else {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    /**
     * For major successful commitments (e.g., saving an expense, checking off a critical task).
     */
    fun triggerConfirm(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            // Fallback for older SDKs
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /**
     * For input rejection, validation failures, or cancellations.
     */
    fun triggerReject(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            // Fallback for older SDKs
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
}
