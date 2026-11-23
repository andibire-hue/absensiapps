package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AttendanceStatus
import com.example.data.repository.ClassRecapSummary
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusAlpaBg
import com.example.ui.theme.StatusAlpaBorder
import com.example.ui.theme.StatusAlpaFg
import com.example.ui.theme.StatusHadirBg
import com.example.ui.theme.StatusHadirBorder
import com.example.ui.theme.StatusHadirFg
import com.example.ui.theme.StatusIzinBg
import com.example.ui.theme.StatusIzinBorder
import com.example.ui.theme.StatusIzinFg
import com.example.ui.theme.StatusSakitBg
import com.example.ui.theme.StatusSakitBorder
import com.example.ui.theme.StatusSakitFg
import com.example.ui.theme.StatusUnmarkedBg
import com.example.ui.theme.StatusUnmarkedBorder
import com.example.ui.theme.StatusUnmarkedFg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.util.DateUtils
import java.util.Locale

fun statusColors(status: AttendanceStatus): Triple<Color, Color, Color> = when (status) {
    AttendanceStatus.H -> Triple(StatusHadirBg, StatusHadirFg, StatusHadirBorder)
    AttendanceStatus.S -> Triple(StatusSakitBg, StatusSakitFg, StatusSakitBorder)
    AttendanceStatus.I -> Triple(StatusIzinBg, StatusIzinFg, StatusIzinBorder)
    AttendanceStatus.A -> Triple(StatusAlpaBg, StatusAlpaFg, StatusAlpaBorder)
    AttendanceStatus.UNMARKED -> Triple(StatusUnmarkedBg, StatusUnmarkedFg, StatusUnmarkedBorder)
}

@Composable
fun StatusBadge(
    status: AttendanceStatus,
    modifier: Modifier = Modifier
) {
    val (bg, fg, border) = statusColors(status)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SearchInputBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextMuted) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cari",
                tint = TextMuted
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Hapus pencarian",
                        tint = TextMuted
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceCard,
            unfocusedContainerColor = SurfaceCard,
            focusedBorderColor = NavyPrimary,
            unfocusedBorderColor = OutlineSubtle
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndonesianDatePickerModal(
    initialIsoDate: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = DateUtils.parseIsoToMillis(initialIsoDate)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        onDateSelected(DateUtils.formatMillisToIso(millis))
                    }
                    onDismiss()
                }
            ) {
                Text("Pilih")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun AttendanceDonutRing(
    percentage: Double,
    modifier: Modifier = Modifier
) {
    val animatedSweep by animateFloatAsState(
        targetValue = ((percentage.toFloat().coerceIn(0f, 100f)) / 100f) * 360f,
        animationSpec = tween(durationMillis = 600),
        label = "donut_sweep"
    )
    Box(
        modifier = modifier.size(88.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize().padding(6.dp)) {
            val stroke = 10.dp.toPx()
            drawArc(
                color = Color(0xFFE2E8F0),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = EmeraldAccent,
                startAngle = -90f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.US, "%.0f%%", percentage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary
            )
            Text(
                text = "Hadir",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@Composable
fun ClassAttendanceBarChart(
    summaries: List<ClassRecapSummary>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grafik Kehadiran Per Kelas",
                    style = MaterialTheme.typography.titleSmall,
                    color = NavyPrimary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChartLegendDot("H", StatusHadirFg)
                    ChartLegendDot("S", StatusSakitFg)
                    ChartLegendDot("I", StatusIzinFg)
                    ChartLegendDot("A", StatusAlpaFg)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            val maxTotal = summaries.maxOfOrNull {
                it.totalHadir + it.totalSakit + it.totalIzin + it.totalAlpa
            }?.coerceAtLeast(1) ?: 1

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                if (summaries.isEmpty()) return@Canvas
                val count = summaries.size
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.46f).coerceAtMost(34.dp.toPx())
                val chartHeight = size.height - 8.dp.toPx()

                // Baseline
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.5.dp.toPx()
                )

                summaries.forEachIndexed { idx, s ->
                    val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val total = s.totalHadir + s.totalSakit + s.totalIzin + s.totalAlpa
                    if (total == 0) {
                        // Draw faint placeholder pill
                        drawRoundRect(
                            color = Color(0xFFF1F5F9),
                            topLeft = Offset(left, chartHeight - 8.dp.toPx()),
                            size = Size(barWidth, 8.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    } else {
                        var currentBottom = chartHeight
                        val segments = listOf(
                            s.totalHadir to StatusHadirFg,
                            s.totalSakit to StatusSakitFg,
                            s.totalIzin to StatusIzinFg,
                            s.totalAlpa to StatusAlpaFg
                        )
                        segments.forEach { (value, segColor) ->
                            if (value > 0) {
                                val segHeight = (value.toFloat() / maxTotal.toFloat()) * (chartHeight - 10.dp.toPx())
                                val top = (currentBottom - segHeight).coerceAtLeast(0f)
                                drawRoundRect(
                                    color = segColor,
                                    topLeft = Offset(left, top),
                                    size = Size(barWidth, currentBottom - top),
                                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                )
                                currentBottom = top
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                summaries.forEach { s ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Kls ${s.schoolClass.id}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NavyPrimary
                        )
                        Text(
                            text = String.format(Locale.US, "%.0f%%", s.percentage),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}
