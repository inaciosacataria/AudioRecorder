/*
 * Copyright 2024 Dmytro Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimowner.audiorecorder.v2.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dimowner.audiorecorder.R
import com.dimowner.audiorecorder.v2.app.RecordsDropDownMenu

@Composable
fun TopAppBar(
    onImportClick: () -> Unit,
    onHomeMenuItemClick: (HomeDropDownMenuItemId) -> Unit,
    showMenuButton: Boolean = true
) {
    val expanded = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .height(60.dp)
            .fillMaxWidth()
            .padding(0.dp, 4.dp, 0.dp, 0.dp)
            .background(color = MaterialTheme.colorScheme.surface),
    ) {
        FilledIconButton(
            onClick = onImportClick,
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.CenterStart),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_import),
                contentDescription = stringResource(id = R.string.btn_import),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            modifier = Modifier
                .wrapContentSize()
                .align(Alignment.Center),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.app_name),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontFamily = FontFamily(
                Font(
                    DeviceFontFamilyName("sans-serif"),
                    weight = FontWeight.Medium
                )
            ),
        )

        if (showMenuButton) {
            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                RecordsDropDownMenu(
                    items = remember { getHomeDroDownMenuItems() },
                    onItemClick = { itemId ->
                        onHomeMenuItemClick(itemId)
                    },
                    expanded = expanded
                )
                FilledIconButton(
                    onClick = { expanded.value = true },
                    modifier = Modifier.padding(8.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vert),
                        contentDescription = stringResource(id = androidx.compose.ui.R.string.dropdown_menu),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopAppBarPreview() {
    TopAppBar({}, {})
}

@Composable
fun PlayPanel(
    modifier: Modifier,
    showStop: Boolean,
    showPause: Boolean,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        FilledIconButton(
            onClick = if (showPause) onPauseClick else onPlayClick,
            modifier = Modifier
                .size(42.dp)
                .align(Alignment.CenterVertically),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            val imageResourceId = if (showPause) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play
            }
            Icon(
                painter = painterResource(id = imageResourceId),
                contentDescription = stringResource(id = R.string.btn_play),
            )
        }
        if (showStop) {
            Spacer(modifier = Modifier.size(8.dp))
            FilledIconButton(
                onClick = onStopClick,
                modifier = Modifier
                    .size(42.dp)
                    .align(Alignment.CenterVertically),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = stringResource(id = R.string.button_stop),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayPanelPreview() {
    PlayPanel(
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp, 8.dp),
        showPause = false,
        showStop = true,
        onPlayClick = {},
        onStopClick = {},
        onPauseClick = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegacySlider(
    //Progress is value between 0 - 1f
    progress: Float = 0f,
    onProgressChange: (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val trackHeight = 4.dp
    val thumbSize = DpSize(20.dp, 20.dp)

    Slider(
        interactionSource = interactionSource,
        modifier = Modifier
            .requiredSizeIn(minWidth = thumbSize.width, minHeight = trackHeight)
            .padding(8.dp, 0.dp),
        value = progress,
        onValueChange = { onProgressChange(it) },
        thumb = {
            val modifier = Modifier
                    .size(thumbSize)
                    .shadow(1.dp, CircleShape, clip = false)
                    .indication(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false, radius = 20.dp)
                    )
            SliderDefaults.Thumb(interactionSource = interactionSource, modifier = modifier)
        },
        track = {
            val modifier = Modifier.height(trackHeight)
            SliderDefaults.Track(
                sliderState = it,
                modifier = modifier,
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 0.dp,
                drawStopIndicator = null
            )
        }
    )
}


@Preview(showBackground = true)
@Composable
fun LegacySliderPreview() {
    LegacySlider(
        progress = 0.5f,
        onProgressChange = {}
    )
}


@Composable
fun BottomBar(
    onSettingsClick: () -> Unit,
    onRecordsListClick: () -> Unit,
    onRecordingClick: () -> Unit,
    onStopRecordingClick: () -> Unit,
    onDeleteRecordingClick: () -> Unit,
    showStopDeleteButton: Boolean
) {
    Row(
        modifier = Modifier
            .wrapContentHeight()
            .padding(16.dp, 0.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        FilledIconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(42.dp)
                .align(Alignment.CenterVertically),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = stringResource(id = R.string.settings),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (showStopDeleteButton) {
            FilledIconButton(
                onClick = onDeleteRecordingClick,
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.CenterVertically),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_delete_forever_36),
                    contentDescription = stringResource(id = R.string.delete),
                )
            }
        }
        FilledIconButton(
            onClick = onRecordingClick,
            modifier = Modifier
                .size(84.dp)
                .align(Alignment.CenterVertically),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
//                modifier = Modifier.size(90.dp),
                painter = painterResource(id = R.drawable.ic_record),
                contentDescription = "Record", //TODO: Use string resource
            )
        }
        if (showStopDeleteButton) {
            FilledIconButton(
                onClick = onStopRecordingClick,
                modifier = Modifier
                    .size(54.dp)
                    .align(Alignment.CenterVertically),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Stop recording", //TODO: Use string resource
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        FilledIconButton(
            onClick = onRecordsListClick,
            modifier = Modifier
                .size(42.dp)
                .align(Alignment.CenterVertically),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_list),
                contentDescription = stringResource(id = R.string.records),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BottomBarPreview() {
    BottomBar({}, {}, {}, {}, {}, true)
}

@Composable
fun TimePanel(
    recordName: String,
    recordInfo: String,
    recordDuration: String,
    timeStart: String,
    timeEnd: String,
    progress: Float,
    onRenameClick: () -> Unit,
    onProgressChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier
                .wrapContentSize(),
            textAlign = TextAlign.Center,
            text = recordDuration,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 54.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            modifier = Modifier
                .wrapContentSize()
                .padding(0.dp, 0.dp, 0.dp, 4.dp),
            textAlign = TextAlign.Center,
            text = recordName,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Normal
        )
        Row {
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(4.dp, 0.dp),
                textAlign = TextAlign.Start,
                text = timeStart,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .wrapContentSize(),
                textAlign = TextAlign.Center,
                text = recordInfo,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(4.dp, 0.dp),
                textAlign = TextAlign.Start,
                text = timeEnd,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
        LegacySlider(
            progress = progress,
            onProgressChange = onProgressChange,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TimePanelPreview() {
    TimePanel(
        "Record-14",
        "1.2Mb, M4a, " +
                "44.1kHz",
        "02:23",
        "00:00",
        "05:32",
        0.3f,
        onRenameClick = {},
        onProgressChange = { prgress ->},
    )
}
