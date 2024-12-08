package com.dimowner.audiorecorder.v2.app.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dimowner.audiorecorder.v2.app.calculateGridStep
import com.dimowner.audiorecorder.v2.app.calculateScale
import com.dimowner.audiorecorder.v2.app.home.HomeScreenState
import com.dimowner.audiorecorder.v2.app.home.LegacySlider
import com.dimowner.audiorecorder.v2.app.home.PlayPanel

@Composable
internal fun RecordPlaybackPanel(
    modifier: Modifier,
    uiState: HomeScreenState,
    onProgressChange: (Float) -> Unit,
    onSeekStart: () -> Unit,
    onSeekProgress: (Long) -> Unit,
    onSeekEnd: (Long) -> Unit,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .wrapContentSize().padding(8.dp),
            textAlign = TextAlign.Center,
            text = uiState.time,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        WaveformComposeView(
            modifier = Modifier.fillMaxWidth().height(70.dp),
            state = uiState.waveformState,
            showTimeline = false,
            onSeekStart = {
                onSeekStart()
            },
            onSeekProgress = { mills ->
                onSeekProgress(mills)
            },
            onSeekEnd = { mills ->
                onSeekEnd(mills)
            }
        )
        Row(
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp, 0.dp),
                textAlign = TextAlign.Start,
                text = uiState.startTime,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
            )
            Text(
                modifier = Modifier
                    .wrapContentHeight().weight(1f)
                    .padding(8.dp, 6.dp, 8.dp, 0.dp),
                textAlign = TextAlign.Center,
                text = uiState.recordName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(8.dp, 0.dp),
                textAlign = TextAlign.Start,
                text = uiState.endTime,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
        LegacySlider(
            progress = uiState.progress,
            onProgressChange = onProgressChange
        )
        PlayPanel(
            modifier = Modifier.wrapContentHeight().fillMaxWidth(),
            showPause = uiState.showPause,
            showStop = uiState.showStop,
            onPlayClick = { onPlayClick() },
            onStopClick = { onStopClick() },
            onPauseClick = { onPauseClick() }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Preview
@Composable
fun PlaybackPanelPreview() {
    val waveformData = intArrayOf(36,44,48,77,105,100,67,76,67,63,39,67,71,74,51,43,71,65,66,55,41,77,88,87,55,77,84,77,83,75,68,88,77,58,44,36,38,48,54,48,80,111,99,115,103,101,102,106,111,96,99,96,74,92,107,110,93,115,104,117,125,107,95,125,119,101,90,101,114,99,81,103,101,91,79,120,110,132,138,122,146,128,125,124,130,132,131,117,122,128,127,119,122,128,133,129,137,127,121,128,132,132,127,130,135,132,124,122,129,123,133,130,131,126,123,131,132,138,134,137,135,140,128,127,130,141,135,125,129,131,125,136,132,133,126,134,132,129,133,135,131,140,127,135,134,131,107,64,55,128,121,113,125,136,140,128,123,132,138,123,118,122,126,126,130,130,128,126,131,136,137,137,136,124,133,135,132,134,121,121,117,116,121,129,126,125,125,110,67,112,101,124,103,99,119,115,99,105,101,116,107,107,106,95,98,115,121,105,106,117,123,95,97,125,100,89,101,103,110,121,90,101,110,96,122,123,104,64,129,136,151,127,150,132,139,132,144,140,150,133,134,129,141,130,140,140,144,136,129,134,152,135,136,147,133,145,131,151,131,133,138,138,130,140,133,128,99,71,113,124,122,98,96,107,121,120,104,110,127,111,97,92,114,117,107,102,105,112,130,113,115,110,120,111,100,96,118,125,124,111,92,123,122,105,98,93,113,88,75,64,64,55,46,39,52,54,54,67,45,64,54,66,79,81,90,70,54,79,120,77,88,52,104,106,73,127,86,101,88,85,80,98,130,116,101,90,104,128,139,127,131,135,132,138,131,140,147,131,133,139,137,139,127,138,135,135,129,143,131,130,140,132,132,136,143,119,79,77,82,74,58,62,62,64,123,101,77,73,62,57,42,45,46,55,53,47,55,54,62,59,65,65,91,118,107,97,121,106,86,103,88,94,112,94,119,106,97,105,114,105,108,112,136,110,110,107,101,89,99,105,93,97,92,118,131,106,100,129,116,108,131,121,129,93,107,90,97,124,129,108,131,126,112,111,117,124,94,100,119,104,114,63,52,48,49,43,46,49,45,54,54,48,50,53,58,69,51,55,62,49,33,132,118,100,103,102,106,105,114,107,129,112,111,113,97,115,119,126,91,127,129,147,137,140,139,137,138,133,136,141,142,135,137,145,135,148,145,124,133,88,140,136,128,134,130,140,136,139,132,136,140,140,137,149,138,140,118,101,80,54,85,96,104,89,67,93,55,52,78,83,123,68,73,65,109,92,92,73,90,145,133,140,136,145,152,143,127,154,133,150,140,136,141,147,130,145,132,148,135,135,135,141,140,147,128,144,136,142,134,144,140,152,148,150,151,152,126,105,98,81,75,68,68,68,83,52,57,57,56,70,70,62,62,62,67,61,62,59,78,74)
    val durationMills = 437232L
    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        RecordPlaybackPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            uiState = HomeScreenState(
                waveformState = WaveformState(
                    widthScale = calculateScale(durationMills, defaultWidthScale = 1.5f),
                    durationMills = durationMills,
                    playProgressMills = 60000L,
                    waveformData = waveformData,
                    durationSample = waveformData.size,
                    gridStepMills = calculateGridStep(durationMills),
                ),
                startTime = "00:00",
                endTime = "3:42",
                time = "1:51",
                recordName = "Test Record Name",
                recordInfo = "1.5 MB, mp4, 192 kbps, 48 kHz",
                isContextMenuAvailable = true,
                isStopRecordingButtonAvailable = true,
            ),
            onProgressChange = {},
            onSeekStart = {},
            onSeekProgress = {},
            onSeekEnd = {},
            onPlayClick = {},
            onStopClick = {},
            onPauseClick = {}
        )
    }
}
