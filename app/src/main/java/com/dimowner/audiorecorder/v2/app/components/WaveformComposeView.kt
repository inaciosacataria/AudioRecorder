package com.dimowner.audiorecorder.v2.app.components

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dimowner.audiorecorder.R
import com.dimowner.audiorecorder.util.AndroidUtils
import com.dimowner.audiorecorder.util.TimeUtils
import com.dimowner.audiorecorder.v2.app.calculateGridStep
import com.dimowner.audiorecorder.v2.app.calculateScale
import timber.log.Timber

private val GIRD_SUBLINE_HEIGHT: Float = AndroidUtils.dpToPx(12)
private val PADD: Float = AndroidUtils.dpToPx(6)

@Composable
fun WaveformComposeView(
    modifier: Modifier,
    state: WaveformState,
    onSeekStart: () -> Unit,
    onSeekEnd: (mills: Long) -> Unit,
    onSeekProgress: (mills: Long) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val viewState = remember {
        mutableStateOf(WaveformViewState(drawLinesArray = floatArrayOf()))
    }

    val paintState = remember {
        mutableStateOf(
            PaintState(
                waveformPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 1.3f
                    isAntiAlias = true
                    color = ContextCompat.getColor(context, R.color.dark_white)
                },
                linePaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = AndroidUtils.dpToPx(1.5f)
                    isAntiAlias = true
                    color = ContextCompat.getColor(context, R.color.dark_white)
                },
                gridPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    color = ContextCompat.getColor(context, R.color.md_grey_100_75)
                    strokeWidth = AndroidUtils.dpToPx(1) / 2
                },
                scrubberPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = AndroidUtils.dpToPx(2f)
                    isAntiAlias = false
                    color = ContextCompat.getColor(context, R.color.md_yellow_A700)
                },
                textPaint = Paint().apply {
                    style = Paint.Style.STROKE
                    strokeWidth = AndroidUtils.dpToPx(1f)
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                    color = ContextCompat.getColor(context, R.color.dark_white)
                    typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                    textSize = viewState.value.textHeight
                },
            )
        )
    }

    Canvas(modifier = modifier
        .onSizeChanged {
            val durationPx = it.width * state.widthScale
            val millsPerPx = state.durationMills / durationPx
            val pxPerMill = durationPx / state.durationMills
            val pxPerSample = durationPx / state.durationSample
            val samplePerPx = state.durationSample / durationPx
            val textHeight = with(density) { 14.sp.toPx() }
            val waveformShiftPx = updateShift(
                viewState.value, it,
                -(state.playProgressMills * pxPerMill).toInt()+it.width/2
            )

            viewState.value = viewState.value.copy(
                waveformShiftPx = waveformShiftPx,
                durationPx = durationPx,
                millsPerPx = millsPerPx,
                pxPerMill = pxPerMill,
                pxPerSample = pxPerSample,
                samplePerPx = samplePerPx,
                drawLinesArray = FloatArray(it.width * 4),
                textHeight = textHeight
            )
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    onSeekStart()
                },
                onDrag = { change, dragAmount ->
                    val shift = updateShift(
                        viewState.value, size,
                        (viewState.value.waveformShiftPx + dragAmount.x).toInt()
                    )
                    val half = size.width / 2
                    viewState.value = viewState.value.copy(
                        waveformShiftPx = shift
                    )
                    onSeekProgress(((-shift + half) * viewState.value.millsPerPx).toLong())
                    Timber.v("onDrag shift: $shift change: $change amount: $dragAmount")
                },
                onDragEnd = {
                    val shift = viewState.value.waveformShiftPx.toInt()
                    val half = size.width / 2
                    onSeekEnd(((-shift + half) * viewState.value.millsPerPx).toLong())
                },
            )
        }
    ) {
        drawIntoCanvas { canvas ->
            drawGrid(canvas, size, viewState.value, state, paintState.value)
            drawWaveform(canvas, size, viewState.value, state, paintState.value)
            drawStartAndEnd(canvas, size, viewState.value, state, paintState.value)
            //Draw scrubber
            canvas.nativeCanvas.drawLine(
                size.width / 2f,
                0f,
                size.width / 2f,
                size.height,
                paintState.value.scrubberPaint
            )

        }
    }
}

private fun drawStartAndEnd(
    canvas: Canvas,
    size: Size,
    viewState: WaveformViewState,
    state: WaveformState,
    paintState: PaintState
) {
    //Draw waveform start indication
    canvas.nativeCanvas.drawLine(
        viewState.waveformShiftPx,
        viewState.textIndent,
        viewState.waveformShiftPx,
        size.height - viewState.textIndent,
        paintState.linePaint
    )
    //Draw waveform end indication
    canvas.nativeCanvas.drawLine(
        viewState.waveformShiftPx + state.waveformData.size * viewState.pxPerSample,
        viewState.textIndent,
        viewState.waveformShiftPx + state.waveformData.size * viewState.pxPerSample,
        size.height - viewState.textIndent,
        paintState.linePaint
    )
}

private fun drawGrid(
    canvas: Canvas,
    size: Size,
    viewState: WaveformViewState,
    state: WaveformState,
    paintState: PaintState
) {
    val subStepPx = (state.gridStepMills / 2) * viewState.pxPerMill
    val halfWidthMills = (size.width / 2) * viewState.millsPerPx
    val gridEndMills = state.durationMills + halfWidthMills.toInt() + state.gridStepMills
    val halfScreenStepCount = (halfWidthMills/state.gridStepMills).toInt()

    for (indexMills in -halfScreenStepCount*state.gridStepMills until gridEndMills step state.gridStepMills) {
        val sampleIndexPx = indexMills * viewState.pxPerMill
        val xPos = (viewState.waveformShiftPx + sampleIndexPx)
        if (xPos >= -state.gridStepMills && xPos <= size.width + state.gridStepMills) {
            //Draw grid lines
            //Draw main grid line
            canvas.nativeCanvas.drawLine(
                xPos,
                viewState.textIndent,
                xPos,
                size.height - viewState.textIndent,
                paintState.gridPaint
            )
            val xSubPos = xPos + subStepPx
            //Draw grid top sub-line
            canvas.nativeCanvas.drawLine(
                xSubPos,
                viewState.textIndent,
                xSubPos,
                GIRD_SUBLINE_HEIGHT + viewState.textIndent,
                paintState.gridPaint
            )
            //Draw grid bottom sub-line
            canvas.nativeCanvas.drawLine(
                xSubPos,
                size.height - GIRD_SUBLINE_HEIGHT - viewState.textIndent,
                xSubPos,
                size.height - viewState.textIndent,
                paintState.gridPaint
            )

            if (state.showTimeline) {
                //Draw timeline texts
                if (indexMills >= 0) {
                    val text = TimeUtils.formatTimeIntervalHourMin(indexMills)
                    //Bottom timeline text
                    canvas.nativeCanvas.drawText(text, xPos, size.height - PADD, paintState.textPaint)
                    //Top timeline text
                    canvas.nativeCanvas.drawText(text, xPos, viewState.textHeight, paintState.textPaint)
                }
            }
        }
    }
}

private fun drawWaveform(
    canvas: Canvas,
    size: Size,
    viewState: WaveformViewState,
    state: WaveformState,
    paintState: PaintState
) {
    if (state.waveformData.isNotEmpty()) {
        for (i in viewState.drawLinesArray.indices) {
            viewState.drawLinesArray[i] = 0f
        }
        val half = size.height / 2
        var step = 0
        for (index in 0 until viewState.durationPx.toInt()) {
            var sampleIndex = (index * viewState.samplePerPx).toInt()
            if (sampleIndex >= state.waveformData.size) {
                sampleIndex = state.waveformData.size - 1
            }
            val xPos = viewState.waveformShiftPx + index
            if (xPos >= 0 && xPos <= size.width && step + 3 < viewState.drawLinesArray.size) {
                viewState.drawLinesArray[step] = xPos
                viewState.drawLinesArray[step + 1] = (half + state.waveformData[sampleIndex] + 1)
                viewState.drawLinesArray[step + 2] = xPos
                viewState.drawLinesArray[step + 3] = (half - state.waveformData[sampleIndex] - 1)
                step += 4
            }
        }
        canvas.nativeCanvas.drawLines(viewState.drawLinesArray, 0,
            viewState.drawLinesArray.size, paintState.waveformPaint)
    }
}

private fun updateShift(
    viewState: WaveformViewState,
    size: IntSize,
    px: Int
): Float {
    var shift = px.toFloat()
    val half = size.width/2
    if (shift <= -viewState.durationPx+half) {
        shift = -viewState.durationPx+half
    }
    if (shift > half) {
        shift = half.toFloat()
    }
    return shift
}

@Preview(showBackground = true)
@Composable
fun WaveformComposeViewPreview() {
    val durationMills = 486974L
    val waveformData = intArrayOf(176,175,177,174,178,173,180,174,179,175,179,175,177,175,176,175,178,173,179,174,179,175,179,175,177,176,175,175,177,174,180,173,179,174,179,175,177,176,181,179,180,179,178,178,181,177,180,177,180,175,180,178,180,178,178,174,181,178,180,177,180,179,177,179,181,179,181,178,180,177,180,177,179,176,176,178,180,177,180,178,180,177,180,178,178,177,180,177,177,178,181,178,180,178,178,145,130,141,138,133,127,127,142,137,136,142,127,134,138,134,115,132,135,128,140,132,132,139,142,136,134,127,139,133,126,156,122,134,143,156,152,142,135,145,139,122,127,130,126,119,131,135,129,138,143,125,136,123,137,135,142,140,145,159,143,158,158,146,156,153,146,160,157,148,146,140,139,149,146,139,141,119,92,79,53,178,176,180,178,181,177,178,178,180,177,179,178,180,177,179,176,179,177,180,178,174,177,179,177,179,176,176,177,178,179,177,179,178,179,176,180,178,176,178,180,176,180,178,179,177,180,178,178,177,179,178,181,177,179,176,179,178,180,177,181,177,180,178,180,177,179,177,164,135,142,143,137,145,154,147,150,177,178,176,179,177,180,177,180,176,180,178,179,177,179,176,178,177,177,176,180,177,181,179,179,177,179,177,180,178,178,175,176,168,168,171,163,160,152,152,135,111,85,92,82,76,82,85,85,87,87,85,85,83,84,83,101,90,55,74,82,86,92,80,97,94,89,95,90,91,107,89,90,82,97,83,103,100,105,107,115,103,91,103,96,100,106,97,101,93,98,107,96,105,94,96,141,125,116,108,106,88,99,105,93,94,89,94,90,88,88,91,92,79,89,85,83,83,84,88,88,87,89,86,87,93,94,110,111,98,99,104,93,110,148,121,113,100,106,113,111,115,117,117,111,114,109,127,125,123,144,144,122,136,138,134,129,143,142,149,153,153,160,151,162,165,160,159,159,174,172,175,174,180,177,180,178,176,178,179,177,179,177,181,176,180,178,180,176,178,175,178,175,180,177,179,177,177,178,181,175,179,177,179,177,179,157,160,151,178,176,178,177,178,175,178,175,174,176,177,176,177,179,179,176,179,175,178,177,177,176,179,174,179,176,179,175,178,176,179,175,177,176,176,172,176,178,177,175,180,175,178,177,177,177,178,175,178,176,177,175,179,177,176,177,177,178,177,177,177,176,177,178,177,175,177,170,170,154,163,145,152,150,140,128,177,176,179,176,179,176,181,177,179,178,180,178,180,178,180,177,180,176,180,178,179,176,181,177,177,177,181,178,181,176,181,175,179,176,181,178,180,176,179,175,177,172,176,170,169,166,168,155,157,150,150,153,150,138,135,135,132,126,128,125,116,119,114,110,104,106,102,96,102,94,94,95,92,88,86,80,51,45,34,25,16,8,5,2,1)
    WaveformComposeView(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        state = WaveformState(
            widthScale = calculateScale(durationMills),
            durationMills = durationMills,
            waveformData = waveformData,
            durationSample = waveformData.size,
            gridStepMills = calculateGridStep(durationMills)
        ),
        onSeekStart = {},
        onSeekProgress = { mills ->
        },
        onSeekEnd = { mills ->
        }
    )
}

data class PaintState(
    val waveformPaint: Paint = Paint(),
    val linePaint: Paint = Paint(),
    val gridPaint: Paint = Paint(),
    val scrubberPaint: Paint = Paint(),
    val textPaint: Paint = TextPaint(),
)

data class WaveformViewState(
    val waveformShiftPx: Float = 0F,
    val textHeight: Float = AndroidUtils.dpToPx(14),
    val textIndent: Float = textHeight + PADD,

    val drawLinesArray: FloatArray,
    val durationPx: Float = 0F,
    val millsPerPx: Float = 0F,
    val pxPerMill: Float = 0F,
    val pxPerSample: Float = 0F,
    val samplePerPx: Float = 0F,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WaveformViewState) return false

        if (waveformShiftPx != other.waveformShiftPx) return false
        if (textHeight != other.textHeight) return false
        if (textIndent != other.textIndent) return false
        if (!drawLinesArray.contentEquals(other.drawLinesArray)) return false
        if (durationPx != other.durationPx) return false
        if (millsPerPx != other.millsPerPx) return false
        if (pxPerMill != other.pxPerMill) return false
        if (pxPerSample != other.pxPerSample) return false
        if (samplePerPx != other.samplePerPx) return false

        return true
    }

    override fun hashCode(): Int {
        var result = waveformShiftPx.hashCode()
        result = 31 * result + textHeight.hashCode()
        result = 31 * result + textIndent.hashCode()
        result = 31 * result + drawLinesArray.contentHashCode()
        result = 31 * result + durationPx.hashCode()
        result = 31 * result + millsPerPx.hashCode()
        result = 31 * result + pxPerMill.hashCode()
        result = 31 * result + pxPerSample.hashCode()
        result = 31 * result + samplePerPx.hashCode()
        return result
    }
}

data class WaveformState(
    val durationMills: Long = 0L,
    val playProgressMills: Long = 0L,
    val waveformData: IntArray = intArrayOf(),
    val showTimeline: Boolean = true,

    /** 1 means that waveform will take whole view width. 2 means that waveform will take double view width to draw.  */
    val widthScale: Float = 1.5f,
    val durationSample: Int = 0,
    val gridStepMills: Long = 4000,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WaveformState) return false

        if (durationMills != other.durationMills) return false
        if (playProgressMills != other.playProgressMills) return false
        if (!waveformData.contentEquals(other.waveformData)) return false
        if (showTimeline != other.showTimeline) return false
        if (widthScale != other.widthScale) return false
        if (durationSample != other.durationSample) return false
        if (gridStepMills != other.gridStepMills) return false

        return true
    }

    override fun hashCode(): Int {
        var result = durationMills.hashCode()
        result = 31 * result + playProgressMills.hashCode()
        result = 31 * result + waveformData.contentHashCode()
        result = 31 * result + showTimeline.hashCode()
        result = 31 * result + widthScale.hashCode()
        result = 31 * result + durationSample
        result = 31 * result + gridStepMills.hashCode()
        return result
    }
}
