package com.kami_apps.deepwork.deep_work_app.presentation.components.circular_progress_indicator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kami_apps.deepwork.deep_work_app.data.util.darken
import com.kami_apps.deepwork.deep_work_app.data.util.lighten
import com.kami_apps.deepwork.deep_work_app.data.util.parseTagColor
import com.kami_apps.deepwork.deep_work_app.presentation.components.NumericTextTransition
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CustomCircularProgressIndicator(
    modifier: Modifier = Modifier,
    secondCurrentValue: String,
    minuteCurrentValue: String, // DEĞERİ DIŞARIDAN ALIYORUZ
    maxValue: Int,
    minValue: Int = 0,
    colorBackgroundGradient: String = "18402806360702976000",
    colorBackgroundGradientValue: Float = 0.2f,
    onValueChange: (newValue: Int) -> Unit = {},
    timerState: Boolean = false,// Değer değiştiğinde dışarıya bildirilecek lambda
    progressTagName: String = "",
    progressTagEmoji: String = "",
    progressStartState: Boolean = false
) {
    val glowColor = parseTagColor(colorBackgroundGradient)
    val gradientColors = listOf(
        glowColor.copy(alpha = 1f).lighten(0.3f),  // %30 daha açık ton
        glowColor.copy(alpha = 1f),                // Orijinal renk
        glowColor.copy(alpha = 1f).darken(0.3f)   // %30 daha koyu ton
    )
    var drawingRadius by remember { mutableFloatStateOf(1.0f) }

    val animatedRadiusValue by animateFloatAsState(
        targetValue = drawingRadius,
        animationSpec = tween(
            durationMillis = 500,
            easing = EaseInOutQuart
        ), label = "animatedRadiusValueAlpha"
    )

    // Calculate radius and thickness based on the available size
    Box(modifier = modifier.padding(10.dp), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(true) {
                    detectDragGestures(
                        onDragStart = { offset -> },
                        //Parmağınızı ekrana koyup sürüklemeye başladığınız ilk an çalışır. (Sürükleme Başladığında)
                        //
                        onDrag = { change: PointerInputChange, dragAmount: Offset ->

                            val currentTouchOffset = change.position
                            val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                            val vector = currentTouchOffset - canvasCenter
                            val angleRadians = atan2(vector.y, vector.x)
                            var angleDegrees = Math.toDegrees(angleRadians.toDouble()).toFloat()

                            // Açıyı 12 o'clock (90 derece) sıfır kabul edip saat yönünde ilerleyen
                            // bir 0-360 aralığına eşle
                            var sweepAngle = (angleDegrees - 90f + 360f) % 360f

                            // Hesaplanan açıya karşılık gelen yeni değeri bul
                            val newValue =
                                ((sweepAngle / 360f) * (maxValue - minValue) + minValue).roundToInt()
                                    .coerceIn(minValue, maxValue)

                            // Değeri KENDİ state'ini GÜNCELLEMEK yerine, dışarıya bildir!
                            onValueChange(newValue)
                        },
                        onDragEnd = { }
                        //onDragEnd: Kullanıcı parmağını ekrandan kaldırdığında, yani sürükleme hareketi bittiğinde tam bir kez tetiklenir.
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val circleThickness = width / 18f

            // Calculate the drawing radius based on the canvas size
            drawingRadius = (min(
                width,
                height
            ) / 2f) - (circleThickness / 2f) // Radius should fit within the bounds
            // Use the actual center of the Canvas for drawing
            val canvasCenter = center // This is correct inside DrawScope

            // Radial Gradient Effect
            // Adjust gradient radius to be relative to the canvas size or drawing radius
            val gradientRadius = maxOf(width, height) / 1.2f  // Fill the canvas
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to glowColor.copy(colorBackgroundGradientValue),                 // tam merkez
                        0.20f to glowColor.copy(colorBackgroundGradientValue),
                        1.0f to Color.Transparent                           // kenar
                    ),
                    center = canvasCenter, // Use Canvas center
                    radius = gradientRadius
                ),
                radius = gradientRadius,
                center = canvasCenter // Use Canvas center
            )
            if (progressStartState) {
                drawingRadius = (drawingRadius * 1.1).toFloat()
            } else {
                drawingRadius
            }
            ///Bu progress bar'ın kendisi
            drawCircle(
                style = Stroke(width = circleThickness),
                color = glowColor.copy(alpha = 0.3f),
                radius = animatedRadiusValue, // Use calculated radius
                center = canvasCenter // Use Canvas center
            )



            if (!timerState) {
                // Primary Progress Arc
                val sweepAngle =
                    (360f / (maxValue - minValue)) * (minuteCurrentValue.toInt() - minValue)
                val arcSize = Size(animatedRadiusValue * 2, animatedRadiusValue * 2)
                val arcTopLeft = Offset(
                    (width - arcSize.width) / 2f,
                    (height - arcSize.height) / 2f
                )
                drawArc(
                    color = Color.Red,
                    startAngle = 90f,
                    sweepAngle = sweepAngle,
                    style = Stroke(width = circleThickness, cap = StrokeCap.Round),
                    useCenter = false,
                    size = arcSize, // Use calculated size based on radius
                    topLeft = arcTopLeft // Calculated to center the arc
                )

                // Dot on the arc
                val angleInRadians =
                    Math.toRadians(90f + sweepAngle.toDouble()) // Start angle + sweep
                val dotRadius = circleThickness // Dot size relative to stroke thickness

                val dotX =
                    canvasCenter.x + animatedRadiusValue * cos(angleInRadians).toFloat() // Use calculated radius
                val dotY =
                    canvasCenter.y + animatedRadiusValue * sin(angleInRadians).toFloat() // Use calculated radius

                drawCircle(
                    brush = Brush.linearGradient(colors = gradientColors),
                    radius = dotRadius,
                    center = Offset(dotX, dotY)
                )
            }


        }
        // Numeric text is centered in the Box, which is centered in the Column
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .background(Color.Transparent)
                .animateContentSize(               // boy değişimini yavaşlat
                    animationSpec = tween(
                        durationMillis = 500,
                        easing = FastOutSlowInEasing
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            NumericTextTransition(
                secondCount = secondCurrentValue,
                minuteCount = minuteCurrentValue
            )
            Box {
                this@Column.AnimatedVisibility(
                    visible = progressStartState,
                    enter = fadeIn(tween(durationMillis = 10)),
                    exit = fadeOut(tween(durationMillis = 500, delayMillis = 250))
                ) {
                    Row(modifier = Modifier.padding(top = 5.dp)) {
                        Text(
                            text = "",
                        )
                    }
                }

                this@Column.AnimatedVisibility(
                    visible = progressStartState,
                    enter = fadeIn(tween(durationMillis = 500, delayMillis = 250)),
                    exit = fadeOut(tween(durationMillis = 10))
                ) {
                    Row(modifier = Modifier.padding(top = 5.dp)) {
                        Text(
                            text = progressTagEmoji,
                            color = Color.White,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 5.dp)
                        )
                        Text(
                            text = progressTagName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

            }
        }


    }

}


@Preview(showBackground = true, backgroundColor = 0xFF000)
@Composable
fun CustomCircularProgressIndicatorPreview() {
    // state’i preview içinde yönetiyoruz
    var progress by remember { mutableStateOf(0) }

    MaterialTheme {
        CustomCircularProgressIndicator(
            modifier = Modifier.size(200.dp),
            minuteCurrentValue = "progress", // Parent state'ini veriyoruz
            maxValue = 100,
            minValue = 0,
            colorBackgroundGradientValue = 0.2f,
            onValueChange = {// newValue ->
//                // Indicator'dan gelen değeri parent state'ine kaydediyoruz
//                progress = newValue
            },
            secondCurrentValue = "0",
            timerState = false
        )
    }
}

