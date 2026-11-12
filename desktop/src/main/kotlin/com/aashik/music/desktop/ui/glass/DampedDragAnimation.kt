package com.aashik.music.desktop.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class DampedDragAnimation(
    private val animationScope: CoroutineScope,
    val initialValue: Float,
    val valueRange: ClosedRange<Float>,
    val visibilityThreshold: Float,
    val initialScale: Float = 1f,
    val pressedScale: Float = 78f / 56f,
    val pressedScaleX: Float = pressedScale,
    val pressedScaleY: Float = pressedScale,
    val onDragStarted: (DampedDragAnimation.(position: Offset) -> Unit)? = null,
    val onDragStopped: (DampedDragAnimation.() -> Unit)? = null,
    val onDrag: (DampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit)? = null,
    /** When true for a drag event the gesture is ended early (lens drops onto the nearest value). */
    val shouldRelease: ((PointerInputChange) -> Boolean)? = null,
) {
    private var endedEarly = false

    var isDragging: Boolean by mutableStateOf(false)
        private set

    private val valueAnimationSpec =
        spring<Float>(1f, 1000f, visibilityThreshold)
    private val velocityAnimationSpec =
        spring<Float>(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec =
        spring<Float>(1f, 1000f, 0.001f)
    private val scaleXAnimationSpec =
        spring<Float>(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec =
        spring<Float>(0.7f, 250f, 0.001f)

    private val valueAnimation =
        Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation =
        Animatable(0f, 5f)
    private val pressProgressAnimation =
        Animatable(0f, 0.001f)
    private val scaleXAnimation =
        Animatable(initialScale, 0.001f)
    private val scaleYAnimation =
        Animatable(initialScale, 0.001f)

    private val mutatorMutex = MutatorMutex()

    private val velocityTracker = VelocityTracker()

    val value: Float get() = valueAnimation.value
    val progress: Float get() = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        inspectDragGestures(
            onDragStart = { down ->
                endedEarly = false
                onDragStarted?.invoke(this@DampedDragAnimation, down.position)
                press()
            },
            onDragEnd = {
                if (!endedEarly) {
                    onDragStopped?.invoke(this@DampedDragAnimation)
                    release()
                }
            },
            onDragCancel = {
                if (!endedEarly) {
                    onDragStopped?.invoke(this@DampedDragAnimation)
                    release()
                }
            }
        ) { change, dragAmount ->
            if (endedEarly) return@inspectDragGestures
            if (shouldRelease?.invoke(change) == true) {
                endedEarly = true
                onDragStopped?.invoke(this@DampedDragAnimation)
                release()
                return@inspectDragGestures
            }
            onDrag?.invoke(this@DampedDragAnimation, size, dragAmount)
        }
    }

    fun press() {
        isDragging = true
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScaleX, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScaleY, scaleYAnimationSpec) }
        }
    }

    fun release(onFinished: (() -> Unit)? = null) {
        isDragging = false
        onFinished?.invoke()
        animationScope.launch {
            // Canonical: keep the lens pressed until the value has (almost) settled on its target.
            withFrameNanos { }
            if (value != targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(initialScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(initialScale, scaleYAnimationSpec) }
            if (velocity != 0f) {
                launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
            }
        }
    }

    fun snapToValue(value: Float) {
        val target = value.coerceIn(valueRange)
        animationScope.launch {
            valueAnimation.snapTo(target)
            updateVelocity()
        }
    }

    fun updateValue(value: Float) {
        val targetValue = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) { updateVelocity() } }
        }
    }

    fun animateToValue(value: Float, onFinished: (() -> Unit)? = null) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val targetValue = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(targetValue, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release(onFinished)
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            System.currentTimeMillis(),
            Offset(value, 0f)
        )
        val targetVelocity = velocityTracker.calculateVelocity().x / (valueRange.endInclusive - valueRange.start)
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}
