package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.physics.CollisionChannelMode
import com.example.physics.CollisionEventResult
import com.example.physics.PacketDualityMode
import com.example.physics.ParticleCategory
import com.example.physics.ParticleSpecies
import com.example.physics.StandardModelCatalog
import com.example.rendering.ViewProjectionMode
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardPanel(
    viewModel: ColliderViewModel,
    modifier: Modifier = Modifier
) {
    val simState by viewModel.state.collectAsState()
    val eventHistory by viewModel.eventHistory.collectAsState()
    val currentEvent by viewModel.currentEvent.collectAsState()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(1) }
    val tabs = listOf("Telemetry", "Particles & Speeds", "Controls", "Event Logs")

    val isDark = simState.isDarkTheme
    val panelBg = if (isDark) Color(0xFF0B132B) else Color(0xFFF8FAFC)
    val accentCyan = Color(0xFF00E5FF)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(panelBg)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
            containerColor = panelBg,
            contentColor = accentCyan,
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    modifier = Modifier.testTag("dashboard_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = if (selectedTabIndex == index) accentCyan else Color.Gray.copy(alpha = 0.7f)
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        when (selectedTabIndex) {
            0 -> TelemetryTab(currentEvent = currentEvent, simState = simState, isDark = isDark)
            1 -> ParticlesTab(viewModel = viewModel, simState = simState, isDark = isDark)
            2 -> ControlsTab(viewModel = viewModel, simState = simState, isDark = isDark)
            3 -> EventLogsTab(viewModel = viewModel, eventHistory = eventHistory, context = context, isDark = isDark)
        }
    }
}

@Composable
fun SubPanelHeader(title: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(12.dp)
                .background(color, RoundedCornerShape(1.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}

@Composable
fun CompactSliderRow(
    title: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    color: Color,
    textColor: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = color
                )
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color.copy(alpha = 0.4f),
                inactiveTrackColor = color.copy(alpha = 0.08f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .testTag(testTag)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelemetryTab(
    currentEvent: CollisionEventResult?,
    simState: SimulationState,
    isDark: Boolean
) {
    val accentCyan = Color(0xFF00E5FF)
    val subtleBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            SubPanelHeader("LIVE COLLISION METRICS", accentCyan)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    MetricReadoutCard("ENERGY √s", "%.0f GeV".format(simState.energyGeV), "%.2f TeV".format(simState.energyGeV / 1000.0), accentCyan, isDark, Modifier.weight(1f))
                    MetricReadoutCard("B-FIELD", "%.2f T".format(simState.magneticFieldTesla), "Solenoid Bz", Color(0xFFFFD600), isDark, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    MetricReadoutCard("TRACKS", "${currentEvent?.multiplicity ?: 0}", "Total Particles", Color(0xFF00E676), isDark, Modifier.weight(1f))
                    MetricReadoutCard("MASS M_inv", "%.1f GeV/c²".format(currentEvent?.invariantMassGeV ?: 0.0), "Invariant", Color(0xFFE040FB), isDark, Modifier.weight(1f))
                }
            }
        }

        item {
            SubPanelHeader("CROSS-SECTIONS & YIELDS", accentCyan)
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    YieldSummaryRow("1. Elastic", "2 stable")
                    YieldSummaryRow("2. Annihil", "2 leptons")
                    YieldSummaryRow("3. Rad QED", "3 particles")
                    YieldSummaryRow("4. EW Pair", "4 fermions")
                    YieldSummaryRow("5. QCD Jets", "20-80+ had")
                }
            }
        }

        item {
            SubPanelHeader("RELATIVISTIC FORMULAS", Color(0xFFFFD600))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    EquationRow("Energy E:", "γ m₀ c²")
                    EquationRow("Lorentz γ:", "1 / √(1-β²)")
                    EquationRow("Radius R:", "p_T / (q B_z)")
                    EquationRow("ET Miss:", "√[(∑p_x)²+(∑p_y)²]")
                }
            }
        }
    }
}

@Composable
fun MetricReadoutCard(
    title: String,
    value: String,
    subtext: String,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.5.sp,
                    color = color.copy(alpha = 0.8f)
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (isDark) Color.White else Color.Black
                )
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    color = if (isDark) Color.Gray else Color.DarkGray
                )
            )
        }
    }
}

@Composable
fun YieldSummaryRow(channel: String, finalObs: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(channel, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp, color = Color(0xFF00E5FF).copy(alpha = 0.8f)))
        Text(finalObs, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, color = Color.Gray))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParticlesTab(
    viewModel: ColliderViewModel,
    simState: SimulationState,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtleBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            SubPanelHeader("DETONATION CONTROL", Color(0xFFFF0844))
            Button(
                onClick = { viewModel.fireCollision() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("fire_collision_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0844)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Bolt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TRIGGER COLLISION EVENT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White, fontSize = 10.sp))
            }
        }

        item {
            SubPanelHeader("RELATIVISTIC BEAM SPEEDS", Color(0xFF00E5FF))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LINKED BEAMS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = textColor.copy(alpha = 0.6f)))
                        Switch(
                            checked = simState.syncBeamSpeeds,
                            onCheckedChange = { viewModel.setSyncBeamSpeeds(it) },
                            modifier = Modifier.size(32.dp).testTag("sync_speeds_switch")
                        )
                    }

                    CompactSliderRow(
                        title = "v_A / c",
                        valueText = "%.3f".format(simState.speedA),
                        value = simState.speedA,
                        onValueChange = { viewModel.setSpeedA(it) },
                        valueRange = 0.05f..0.999f,
                        color = Color(0xFF00E5FF),
                        textColor = textColor,
                        testTag = "beam_a_speed_slider"
                    )

                    CompactSliderRow(
                        title = "v_B / c",
                        valueText = "%.3f".format(simState.speedB),
                        value = simState.speedB,
                        onValueChange = { viewModel.setSpeedB(it) },
                        valueRange = 0.05f..0.999f,
                        color = Color(0xFFFF9100),
                        textColor = textColor,
                        testTag = "beam_b_speed_slider"
                    )
                }
            }
        }

        item {
            SubPanelHeader("INTERACTION CHANNEL", Color(0xFF80DEEA))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CollisionChannelMode.entries.forEach { mode ->
                            val isSelected = mode == simState.channelMode
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)) else androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { viewModel.setChannelMode(mode) }
                            ) {
                                Text(mode.shortName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 9.sp, color = if (isSelected) Color(0xFF00E5FF) else textColor.copy(alpha = 0.7f)), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            SubPanelHeader("BEAM PARTICLE SPECIES", Color(0xFF00E5FF))
            CompactParticleSelectorCard("BEAM A (+Z)", simState.particleA, { viewModel.setParticleA(it) }, Color.Transparent, isDark)
            Spacer(modifier = Modifier.height(4.dp))
            CompactParticleSelectorCard("BEAM B (-Z)", simState.particleB, { viewModel.setParticleB(it) }, Color.Transparent, isDark)
        }

        item {
            SubPanelHeader("SPECIES PALETTE", Color(0xFF69F0AE))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    val categories = listOf(
                        "Leptons" to Color(0xFF00E676),
                        "Mesons" to Color(0xFF2979FF),
                        "Baryons" to Color(0xFFFF3D00),
                        "Gauge" to Color(0xFFFFD600),
                        "Higgs" to Color(0xFFE040FB),
                        "Ions" to Color(0xFFFF9100)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEach { (catName, catColor) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(catColor))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(catName, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = catColor))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactParticleSelectorCard(
    label: String,
    selectedSpecies: ParticleSpecies,
    onSelect: (ParticleSpecies) -> Unit,
    cardBg: Color,
    isDark: Boolean
) {
    val subtleBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 9.sp))
                Text(selectedSpecies.symbol, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = selectedSpecies.color, fontSize = 9.sp))
            }
            Spacer(modifier = Modifier.height(3.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StandardModelCatalog.ALL_SPECIES.forEach { species ->
                    val isSelected = species.id == selectedSpecies.id
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onSelect(species) }.testTag("select_particle_${species.id}"),
                        color = if (isSelected) species.color.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, species.color) else androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
                    ) {
                        Text(species.symbol, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = if (isSelected) species.color else Color.Gray, fontSize = 11.sp), modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlsTab(
    viewModel: ColliderViewModel,
    simState: SimulationState,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtleBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            SubPanelHeader("VIEWPORT & ENVIRONMENT", Color(0xFF00E5FF))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ViewProjectionMode.entries.forEach { mode ->
                            val isSelected = mode == simState.projectionMode
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)) else androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { viewModel.setProjectionMode(mode) }
                            ) {
                                Text(mode.displayName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 9.sp, color = if (isSelected) Color(0xFF00E5FF) else textColor.copy(alpha = 0.7f)), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    CompactSliderRow("BG HUE", "%.0f°".format(simState.bgHue), simState.bgHue, { viewModel.setBgHue(it) }, 0f..360f, Color(0xFF00E5FF), textColor, "bg_hue_slider")
                    CompactSliderRow("BG BRIGHT", "%.2f".format(simState.bgBrightness), simState.bgBrightness, { viewModel.setBgBrightness(it) }, 0.02f..1.0f, Color(0xFFFFD600), textColor, "bg_brightness_slider")
                }
            }
        }

        item {
            SubPanelHeader("QUANTUM PACKET DYNAMICS", Color(0xFFE040FB))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        PacketDualityMode.entries.forEach { mode ->
                            val isSelected = mode == simState.packetDualityMode
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFFE040FB).copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE040FB)) else androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { viewModel.setPacketDualityMode(mode) }
                            ) {
                                Text(mode.displayName, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 9.sp, color = if (isSelected) Color(0xFFE040FB) else textColor.copy(alpha = 0.7f)), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CompactSliderRow("WAVE DISPERSION σ(t)", "%.1f×".format(simState.wavePacketDispersionRate), simState.wavePacketDispersionRate, { viewModel.setWavePacketDispersionRate(it) }, 0.2f..3.0f, Color(0xFF00E5FF), textColor, "wave_dispersion_slider")
                    CompactSliderRow("BUNCH CONE", "%.1f×".format(simState.particlePacketConeScale), simState.particlePacketConeScale, { viewModel.setParticlePacketConeScale(it) }, 0.4f..3.0f, Color(0xFFFF9100), textColor, "particle_cone_slider")
                }
            }
        }

        item {
            SubPanelHeader("PARTICLE TRAIL STYLING", Color(0xFF69F0AE))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    CompactSliderRow("TRAIL LENGTH", "${simState.trailLength} pts", simState.trailLength.toFloat(), { viewModel.setTrailLength(it.toInt()) }, 5f..40f, Color(0xFFE040FB), textColor, "trail_length_slider")
                    CompactSliderRow("TRAIL WIDTH", "%.1f×".format(simState.trailWidth), simState.trailWidth, { viewModel.setTrailWidth(it) }, 0.5f..4.0f, Color(0xFF00E5FF), textColor, "trail_width_slider")
                }
            }
        }

        item {
            SubPanelHeader("LABORATORY FIELDS", Color(0xFFFFD600))
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder)
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    CompactSliderRow("SOLENOID B_z", "%.2f T".format(simState.magneticFieldTesla), simState.magneticFieldTesla, { viewModel.setMagneticFieldTesla(it) }, 0f..8f, Color(0xFFFFD600), textColor, "magnetic_field_slider")
                    CompactSliderRow("TIME FLOW", "%.4f c".format(simState.timeScale), simState.timeScale, { viewModel.setTimeScale(it) }, 0.001f..0.035f, Color(0xFF69F0AE), textColor, "timescale_slider")
                    CompactSliderRow("IMPACT b", "%.2f fm".format(simState.impactParameterFm), simState.impactParameterFm.toFloat(), { viewModel.setImpactParameterFm(it.toDouble()) }, 0.0f..3.0f, Color(0xFF00E5FF), textColor, "impact_parameter_slider")
                }
            }
        }
    }
}

@Composable
fun EventLogsTab(
    viewModel: ColliderViewModel,
    eventHistory: List<CollisionEventResult>,
    context: Context,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtleBorder = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val formatted = viewModel.formatAllLogsForExport()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("CollisionLogs", formatted))
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                modifier = Modifier.height(32.dp).weight(1f)
            ) {
                Text("COPY ALL LOGS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 9.sp))
            }

            OutlinedButton(
                onClick = { viewModel.clearLogs() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp).weight(0.5f)
            ) {
                Text("CLEAR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFFF1744), fontSize = 9.sp))
            }
        }

        if (eventHistory.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No collision data recorded.", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 9.sp))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(eventHistory) { ev ->
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("EVENT #${ev.eventId}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF), fontSize = 9.sp))
                                Text("${ev.multiplicity} trk", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, color = Color.Gray, fontSize = 8.5.sp))
                            }
                            Text(ev.primaryProcessName, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp, color = textColor.copy(alpha = 0.8f)))
                            Text("√s = %.1f GeV | M = %.1f".format(ev.centerOfMassEnergyGeV, ev.invariantMassGeV), style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 8.sp, color = textColor.copy(alpha = 0.6f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EquationRow(label: String, formula: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 9.sp,
                color = Color.Gray
            )
        )
        Text(
            text = formula,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
