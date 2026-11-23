package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MeetingStatus
import com.example.data.local.MeetingWithCounts
import com.example.data.local.SchoolClassEntity
import com.example.data.local.SettingsEntity
import com.example.ui.theme.BackgroundLightGray
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.NavyContainer
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyPrimaryDark
import com.example.ui.theme.NavyPrimaryLight
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.StatusAlpaBg
import com.example.ui.theme.StatusAlpaFg
import com.example.ui.theme.StatusHadirBg
import com.example.ui.theme.StatusHadirFg
import com.example.ui.theme.StatusIzinBg
import com.example.ui.theme.StatusIzinFg
import com.example.ui.theme.StatusSakitBg
import com.example.ui.theme.StatusSakitFg
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DashboardSummary
import com.example.util.DateUtils

@Composable
fun DashboardScreen(
    settings: SettingsEntity,
    classes: List<SchoolClassEntity>,
    summary: DashboardSummary,
    onStartAttendance: (Int) -> Unit,
    onOpenMeeting: (Int, Long) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayLabel = DateUtils.formatIndonesianFull(DateUtils.todayIso())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundLightGray),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(NavyPrimaryDark, NavyPrimary, NavyPrimaryLight)
                        ),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_school_sport_logo),
                                    contentDescription = "Logo ABSENSI PJOK UPTD SDN LOKOJULI",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().padding(3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ABSENSI PJOK",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = settings.schoolName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier
                                .testTag("dashboard_settings_button")
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Pengaturan dan Backup",
                                tint = Color.White
                            )
                        }
                    }

                    // Teacher & Academic Metadata Strip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Guru: ${settings.teacherName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = todayLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldAccent
                                ) {
                                    Text(
                                        text = "${settings.semester} • ${settings.academicYear}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Statistics Section
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ringkasan Statistik",
                    style = MaterialTheme.typography.titleMedium,
                    color = NavyPrimary
                )
                // Total Siswa Wide Banner
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, OutlineSubtle),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NavyContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    tint = NavyPrimary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Total Siswa Terdaftar",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Kelas 1 sampai Kelas 6",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                        Text(
                            text = "${summary.totalStudents}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 4 Status Stat Cards in 2x2 Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMiniCard(
                        title = "Hadir",
                        count = summary.totalHadir,
                        icon = Icons.Default.CheckCircle,
                        bgColor = StatusHadirBg,
                        fgColor = StatusHadirFg,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        title = "Sakit",
                        count = summary.totalSakit,
                        icon = Icons.Default.LocalHospital,
                        bgColor = StatusSakitBg,
                        fgColor = StatusSakitFg,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMiniCard(
                        title = "Izin",
                        count = summary.totalIzin,
                        icon = Icons.Default.MailOutline,
                        bgColor = StatusIzinBg,
                        fgColor = StatusIzinFg,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        title = "Alpa",
                        count = summary.totalAlpa,
                        icon = Icons.Default.WarningAmber,
                        bgColor = StatusAlpaBg,
                        fgColor = StatusAlpaFg,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Six Class Cards (Kelas 1 - Kelas 6)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pilih Kelas untuk Absensi PJOK",
                    style = MaterialTheme.typography.titleMedium,
                    color = NavyPrimary
                )
                val displayClasses = if (classes.isNotEmpty()) classes else (1..6).map {
                    SchoolClassEntity(it, "Kelas $it")
                }
                displayClasses.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { sc ->
                            val count = summary.studentCountByClass[sc.id] ?: 0
                            ClassActionCard(
                                className = sc.className,
                                studentCount = count,
                                onStartClick = { onStartAttendance(sc.id) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("class_card_${sc.id}")
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Recent Attendance Activity
        item {
            Text(
                text = "Aktivitas Absensi Terbaru",
                style = MaterialTheme.typography.titleMedium,
                color = NavyPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (summary.recentMeetings.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, OutlineSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Belum ada aktivitas absensi",
                            style = MaterialTheme.typography.titleSmall,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Tambahkan data siswa di menu Siswa, lalu tekan MULAI ABSENSI pada kartu kelas di atas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(summary.recentMeetings, key = { it.meeting.id }) { item ->
                RecentActivityCard(
                    item = item,
                    onClick = { onOpenMeeting(item.meeting.classId, item.meeting.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    title: String,
    count: Int,
    icon: ImageVector,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = fgColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleLarge,
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ClassActionCard(
    className: String,
    studentCount: Int,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = className,
                    style = MaterialTheme.typography.titleMedium,
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldContainer
                ) {
                    Text(
                        text = "$studentCount siswa",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnEmeraldContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_attendance_${className.lowercase().replace(" ", "_")}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NavyPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = EmeraldAccent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "MULAI ABSENSI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RecentActivityCard(
    item: MeetingWithCounts,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFinal = item.meeting.status == MeetingStatus.FINALIZED.code
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = BorderStroke(1.dp, OutlineSubtle),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NavyContainer
                    ) {
                        Text(
                            text = "${item.className} • P${item.meeting.meetingNumber}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NavyPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateUtils.formatIndonesianShort(item.meeting.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isFinal) StatusHadirBg else StatusSakitBg
                ) {
                    Text(
                        text = if (isFinal) "Final" else "Draft",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFinal) StatusHadirFg else StatusSakitFg,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = item.meeting.topic.ifBlank { "Pembelajaran PJOK" },
                style = MaterialTheme.typography.titleSmall,
                color = NavyPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniStatusCount("H", item.hadirCount, StatusHadirFg)
                MiniStatusCount("S", item.sakitCount, StatusSakitFg)
                MiniStatusCount("I", item.izinCount, StatusIzinFg)
                MiniStatusCount("A", item.alpaCount, StatusAlpaFg)
                if (item.unmarkedCount > 0) {
                    MiniStatusCount("Belum", item.unmarkedCount, TextMuted)
                }
            }
        }
    }
}

@Composable
private fun MiniStatusCount(label: String, count: Int, color: Color) {
    Text(
        text = "$label: $count",
        style = MaterialTheme.typography.labelMedium,
        color = color,
        fontWeight = FontWeight.Bold
    )
}
