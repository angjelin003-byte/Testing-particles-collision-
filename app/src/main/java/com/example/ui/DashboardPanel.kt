package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var selectedTabIndex by remember { mutableIntStateOf(1) } // Default to Particles & Speed controls
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
        // High-Density Compact Tab Header Bar
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 2.dp,
            containerColor = panelBg,
            contentColor = accentCyan,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
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
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = if (selectedTabIndex == index) accentCyan else Color.Gray
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Tab Content Pages
        when (selectedTabIndex) {
            0 -> TelemetryTab(currentEvent = currentEvent, simState = simState, isDark = isDark)
            1 -> ParticlesTab(viewModel = viewModel, simState = simState, isDark = isDark)
            2 -> ControlsTab(viewModel = viewModel, simState = simState, isDark = isDark)
            3 -> EventLogsTab(
                viewModel = viewModel,
                eventHistory = eventHistory,
                context = context,
                isDark = isDark
            )
        }
    }
}

/**
 * Reusable ultra-compact slider row with title and value badge on the same header line
 */
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = textColor
                )
            )
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = color.copy(alpha = 0.16f)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = color
                    ),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
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
    val cardBg = if (isDark) Color(0xFF131D33) else Color.White
    val accentCyan = Color(0xFF00E5FF)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            // Live Status Header Banner (Compact)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = accentCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "LIVE TELEMETRY",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = accentCyan,
                                    fontSize = 11.5.sp
                                )
                            )
                        }

                        if (currentEvent != null) {
                            Surface(
                                color = Color(0xFF00E5FF).copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = currentEvent.primaryProcessName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00E5FF),
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Metrics Grid Cards (Compact 2x3 Grid)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MetricReadoutCard(
                        title = "ENERGY √s",
                        value = "%.0f GeV".format(simState.energyGeV),
                        subtext = "%.2f TeV".format(simState.energyGeV / 1000.0),
                        color = Color(0xFF00E5FF),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    MetricReadoutCard(
                        title = "B-FIELD",
                        value = "%.2f T".format(simState.magneticFieldTesla),
                        subtext = "Solenoid Bz",
                        color = Color(0xFFFFD600),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MetricReadoutCard(
                        title = "TRACKS / HITS",
                        value = "${currentEvent?.multiplicity ?: 0} tracks",
                        subtext = "${currentEvent?.calorimeterHits?.size ?: 0} Cal Towers",
                        color = Color(0xFF00E676),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    MetricReadoutCard(
                        title = "INVARIANT MASS",
                        value = "%.1f GeV/c²".format(currentEvent?.invariantMassGeV ?: 0.0),
                        subtext = "M = √(P_μ P^μ)",
                        color = Color(0xFFE040FB),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MetricReadoutCard(
                        title = "MISSING E_T",
                        value = "%.1f GeV".format(currentEvent?.missingETGeV ?: 0.0),
                        subtext = "Neutrino |E_T_miss|",
                        color = Color(0xFF69F0AE),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    MetricReadoutCard(
                        title = "CHARGE ΔQ",
                        value = if (currentEvent != null) "Q: %+.0f → %+.0f".format(currentEvent.initialCharge, currentEvent.finalCharge) else "Exact Q = 0",
                        subtext = "Conserved",
                        color = Color(0xFFFF9100),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            // Compact Collision Yields Table
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "CROSS-SECTIONS & OBSERVABLE YIELDS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    YieldSummaryRow("1. Elastic (2→2)", "2 fermions", "2 stable particles")
                    YieldSummaryRow("2. Annihilation (2→2)", "γ*/Z⁰ mediator", "2 leptons (μ⁺μ⁻)")
                    YieldSummaryRow("3. Radiative QED (2→3)", "2 leptons + γ", "3 particles (ℓ⁺ℓ⁻γ)")
                    YieldSummaryRow("4. Electroweak (2→4)", "W⁺W⁻ / Z⁰Z⁰", "4 fermions")
                    YieldSummaryRow("5. Hadronic Jets (QCD)", "2 quarks (q q̄)", "20 to 80+ hadrons")
                }
            }
        }

        item {
            // Relativistic Physics Formulas Card (Compact)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = null,
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RELATIVISTIC KINEMATICS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600),
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    EquationRow("Energy:", "E = γ m₀ c² = √(p²c² + m₀²c⁴)")
                    EquationRow("Lorentz Factor:", "γ = 1 / √(1 - β²)")
                    EquationRow("Cyclotron Radius:", "R = p_T / (q B_z)")
                    EquationRow("Invariant Mass:", "M = √(E² - |p|²c²) / c²")
                    EquationRow("Missing E_T:", "|E_T_miss| = √[(∑p_x)² + (∑p_y)²]")
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF131D33) else Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    color = color
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.5.sp,
                    color = if (isDark) Color.White else Color.Black
                )
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            )
        }
    }
}

@Composable
fun YieldSummaryRow(channel: String, intermediate: String, finalObs: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = channel,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.5.sp,
                color = Color(0xFF00E5FF)
            )
        )
        Text(
            text = finalObs,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = Color.LightGray
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParticlesTab(
    viewModel: ColliderViewModel,
    simState: SimulationState,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF131D33) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            // Compact Fire Collision CTA Button
            Button(
                onClick = { viewModel.fireCollision() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("fire_collision_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0844)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FIRE RELATIVISTIC COLLISION",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 11.5.sp
                    )
                )
            }
        }

        item {
            // ==========================================
            // SPEED CONTROL CARD TO 2 MAIN PARTICLES
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "BEAM PARTICLE SPEED CONTROLS",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp
                                )
                            )
                        }

                        // Symmetric Beam Speeds Link Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.setSyncBeamSpeeds(!simState.syncBeamSpeeds) }
                                .background(if (simState.syncBeamSpeeds) Color(0xFF00E5FF).copy(alpha = 0.20f) else Color.Transparent)
                                .border(0.8.dp, if (simState.syncBeamSpeeds) Color(0xFF00E5FF) else Color.Gray, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (simState.syncBeamSpeeds) "LINKED (A=B)" else "ASYMMETRIC",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.5.sp,
                                    color = if (simState.syncBeamSpeeds) Color(0xFF00E5FF) else Color.Gray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Relativistic Beam A Calculations
                    val gammaA = 1.0f / sqrt((1.0f - simState.speedA * simState.speedA).coerceAtLeast(0.0001f))
                    val pMagA = gammaA * simState.particleA.restMassGeV.toFloat() * simState.speedA

                    // SLIDING BAR 1: Beam Particle A Speed
                    CompactSliderRow(
                        title = "PARTICLE A (${simState.particleA.symbol}) SPEED [v_A / c]",
                        valueText = "%.3f c (γ=%.2f, |p|=%.2f GeV)".format(simState.speedA, gammaA, pMagA),
                        value = simState.speedA,
                        onValueChange = { viewModel.setSpeedA(it) },
                        valueRange = 0.05f..0.999f,
                        color = Color(0xFF00E5FF),
                        textColor = textColor,
                        testTag = "beam_a_speed_slider"
                    )

                    // Relativistic Beam B Calculations
                    val gammaB = 1.0f / sqrt((1.0f - simState.speedB * simState.speedB).coerceAtLeast(0.0001f))
                    val pMagB = gammaB * simState.particleB.restMassGeV.toFloat() * simState.speedB

                    // SLIDING BAR 2: Beam Particle B Speed
                    CompactSliderRow(
                        title = "PARTICLE B (${simState.particleB.symbol}) SPEED [v_B / c]",
                        valueText = "%.3f c (γ=%.2f, |p|=%.2f GeV)".format(simState.speedB, gammaB, pMagB),
                        value = simState.speedB,
                        onValueChange = { viewModel.setSpeedB(it) },
                        valueRange = 0.05f..0.999f,
                        color = Color(0xFFFF9100),
                        textColor = textColor,
                        testTag = "beam_b_speed_slider"
                    )

                    // Quick Speed Presets Row
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "0.50c" to 0.50f,
                            "0.85c" to 0.85f,
                            "0.95c" to 0.95f,
                            "0.999c" to 0.999f
                        ).forEach { (label, spd) ->
                            val isSel = abs(simState.speedA - spd) < 0.015f
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) Color(0xFF00E5FF).copy(alpha = 0.25f) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel.setSpeedA(spd)
                                        viewModel.setSpeedB(spd)
                                    }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 9.sp,
                                        color = if (isSel) Color(0xFF00E5FF) else textColor
                                    ),
                                    modifier = Modifier.padding(vertical = 3.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Interaction Channel Card (Compact)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "INTERACTION CHANNEL MODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CollisionChannelMode.entries.forEach { mode ->
                            val isSelected = mode == simState.channelMode
                            val chipBg = if (isSelected) Color(0xFF00E5FF) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            val chipText = if (isSelected) Color.Black else (if (isDark) Color.White else Color.Black)

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setChannelMode(mode) }
                                    .testTag("channel_mode_${mode.name}"),
                                color = chipBg
                            ) {
                                Text(
                                    text = mode.shortName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = chipText,
                                        fontSize = 9.5.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = simState.channelMode.description,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            color = if (isDark) Color(0xFF80DEEA) else Color(0xFF006064)
                        )
                    )
                }
            }
        }

        item {
            CompactParticleSelectorCard(
                label = "BEAM A (Moving +Z)",
                selectedSpecies = simState.particleA,
                onSelect = { viewModel.setParticleA(it) },
                cardBg = cardBg,
                isDark = isDark
            )
        }

        item {
            CompactParticleSelectorCard(
                label = "BEAM B (Moving -Z)",
                selectedSpecies = simState.particleB,
                onSelect = { viewModel.setParticleB(it) },
                cardBg = cardBg,
                isDark = isDark
            )
        }

        item {
            // Species Color Classification (Compact)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "SPECIES CLASSIFICATION PALETTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    val categories = listOf(
                        Triple("Leptons (e⁻, μ⁻, τ⁻, ν)", "Point-like fermions", Color(0xFF00E676)),
                        Triple("Mesons (π⁺, π⁻, K⁺, K⁰)", "Quark-antiquark bound", Color(0xFF2979FF)),
                        Triple("Baryons (p, p̄, n)", "3-quark hadrons", Color(0xFFFF3D00)),
                        Triple("Gauge Bosons (γ, W⁺, Z⁰)", "Vector force mediators", Color(0xFFFFD600)),
                        Triple("Higgs Boson (H⁰)", "Scalar excitation", Color(0xFFE040FB)),
                        Triple("Heavy Ions (⁴He, ²⁰⁸Pb)", "Composite nuclei", Color(0xFFFF9100))
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { (catName, _, catColor) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = catColor.copy(alpha = 0.16f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, catColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(catColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = catColor,
                                            fontSize = 8.5.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Particle Selector Card with tight chip flow
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactParticleSelectorCard(
    label: String,
    selectedSpecies: ParticleSpecies,
    onSelect: (ParticleSpecies) -> Unit,
    cardBg: Color,
    isDark: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E5FF),
                        fontSize = 10.sp
                    )
                )
                Text(
                    text = "${selectedSpecies.name} (${selectedSpecies.symbol})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = selectedSpecies.color,
                        fontSize = 10.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StandardModelCatalog.ALL_SPECIES.forEach { species ->
                    val isSelected = species.id == selectedSpecies.id
                    val chipBg = if (isSelected) species.color else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                    val chipText = if (isSelected) Color.Black else (if (isDark) Color.White else Color.Black)

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(species) }
                            .testTag("select_particle_${species.id}"),
                        color = chipBg
                    ) {
                        Text(
                            text = species.symbol,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = chipText,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Mass: %.4f GeV | q = %+.1f e | Spin: %s".format(
                    selectedSpecies.restMassGeV, selectedSpecies.charge, selectedSpecies.spin
                ),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            )
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
    val cardBg = if (isDark) Color(0xFF131D33) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            // Environment Background & Projection Editor (Compact)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "ENVIRONMENT & PROJECTION VIEWPORT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Projection Mode Selector Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ViewProjectionMode.entries.forEach { mode ->
                            val isSelected = mode == simState.projectionMode
                            val chipBg = if (isSelected) Color(0xFF00E5FF) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            val chipText = if (isSelected) Color.Black else (if (isDark) Color.White else Color.Black)

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setProjectionMode(mode) }
                                    .testTag("projection_mode_${mode.name}"),
                                color = chipBg
                            ) {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = chipText,
                                        fontSize = 9.5.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Background Presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        BackgroundPresets.PRESETS.forEachIndexed { index, preset ->
                            val isSelected = index == simState.bgPresetIndex
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setBgPreset(index) }
                                    .testTag("bg_preset_$index"),
                                color = preset.color,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF)) else null
                            ) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 8.5.sp,
                                        color = if (preset.isDark) Color.White else Color.Black
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    CompactSliderRow(
                        title = "BG HUE",
                        valueText = "%.0f°".format(simState.bgHue),
                        value = simState.bgHue,
                        onValueChange = { viewModel.setBgHue(it) },
                        valueRange = 0f..360f,
                        color = Color(0xFF00E5FF),
                        textColor = textColor,
                        testTag = "bg_hue_slider"
                    )

                    CompactSliderRow(
                        title = "BG BRIGHTNESS",
                        valueText = "%.2f".format(simState.bgBrightness),
                        value = simState.bgBrightness,
                        onValueChange = { viewModel.setBgBrightness(it) },
                        valueRange = 0.02f..1.0f,
                        color = Color(0xFFFFD600),
                        textColor = textColor,
                        testTag = "bg_brightness_slider"
                    )
                }
            }
        }

        item {
            // Wave & Particle Packet Ejection Card (Compact)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "WAVE & PARTICLE PACKET EJECTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Duality Modes
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PacketDualityMode.entries.forEach { mode ->
                            val isSelected = mode == simState.packetDualityMode
                            val chipBg = if (isSelected) Color(0xFF00E5FF) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            val chipText = if (isSelected) Color.Black else (if (isDark) Color.White else Color.Black)

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setPacketDualityMode(mode) }
                                    .testTag("packet_duality_mode_${mode.name}"),
                                color = chipBg
                            ) {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = chipText,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Toggles in 1 compact row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ψ Wave Ejection",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    fontSize = 9.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = simState.enableWavePacketEjection,
                                onCheckedChange = { viewModel.setEnableWavePacketEjection(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF)),
                                modifier = Modifier.testTag("enable_wave_packets_switch")
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Particle Bunch",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    fontSize = 9.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = simState.enableParticlePacketEjection,
                                onCheckedChange = { viewModel.setEnableParticlePacketEjection(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFFF9100)),
                                modifier = Modifier.testTag("enable_particle_packets_switch")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    CompactSliderRow(
                        title = "WAVE DISPERSION RATE σ(t)",
                        valueText = "%.2f×".format(simState.wavePacketDispersionRate),
                        value = simState.wavePacketDispersionRate,
                        onValueChange = { viewModel.setWavePacketDispersionRate(it) },
                        valueRange = 0.2f..3.0f,
                        color = Color(0xFF00E5FF),
                        textColor = textColor,
                        testTag = "wave_dispersion_slider"
                    )

                    CompactSliderRow(
                        title = "BUNCH CONE OPENING",
                        valueText = "%.2f×".format(simState.particlePacketConeScale),
                        value = simState.particlePacketConeScale,
                        onValueChange = { viewModel.setParticlePacketConeScale(it) },
                        valueRange = 0.4f..3.0f,
                        color = Color(0xFFFF9100),
                        textColor = textColor,
                        testTag = "particle_cone_slider"
                    )
                }
            }
        }

        item {
            // Particle Trail Editor (Compact)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "PARTICLE TRAIL DYNAMICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE040FB),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    CompactSliderRow(
                        title = "TRAIL LENGTH",
                        valueText = "${simState.trailLength} pts",
                        value = simState.trailLength.toFloat(),
                        onValueChange = { viewModel.setTrailLength(it.toInt()) },
                        valueRange = 5f..40f,
                        color = Color(0xFFE040FB),
                        textColor = textColor,
                        testTag = "trail_length_slider"
                    )

                    CompactSliderRow(
                        title = "TRAIL THICKNESS",
                        valueText = "%.1f×".format(simState.trailWidth),
                        value = simState.trailWidth,
                        onValueChange = { viewModel.setTrailWidth(it) },
                        valueRange = 0.5f..4.0f,
                        color = Color(0xFF00E5FF),
                        textColor = textColor,
                        testTag = "trail_width_slider"
                    )

                    // Color Mode Chips
                    Spacer(modifier = Modifier.height(2.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TrailColorMode.entries.forEach { mode ->
                            val isSelected = mode == simState.trailColorMode
                            val chipBg = if (isSelected) Color(0xFFE040FB) else (if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            val chipText = if (isSelected) Color.Black else (if (isDark) Color.White else Color.Black)

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.setTrailColorMode(mode) }
                                    .testTag("trail_mode_${mode.name}"),
                                color = chipBg
                            ) {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = chipText,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            // Environment & Fields (Compact)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "FIELDS & DETECTOR GEOMETRY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    CompactSliderRow(
                        title = "SOLENOID B-FIELD",
                        valueText = "%.2f T".format(simState.magneticFieldTesla),
                        value = simState.magneticFieldTesla,
                        onValueChange = { viewModel.setMagneticFieldTesla(it) },
                        valueRange = 0f..8f,
                        color = Color(0xFFFFD600),
                        textColor = textColor,
                        testTag = "magnetic_field_slider"
                    )

                    CompactSliderRow(
                        title = "TIME SLOW-MO",
                        valueText = "%.4f c".format(simState.timeScale),
                        value = simState.timeScale,
                        onValueChange = { viewModel.setTimeScale(it) },
                        valueRange = 0.001f..0.035f,
                        color = Color(0xFF69F0AE),
                        textColor = textColor,
                        testTag = "timescale_slider"
                    )

                    CompactSliderRow(
                        title = "IMPACT PARAMETER b",
                        valueText = "%.2f fm".format(simState.impactParameterFm),
                        value = simState.impactParameterFm.toFloat(),
                        onValueChange = { viewModel.setImpactParameterFm(it.toDouble()) },
                        valueRange = 0.0f..3.0f,
                        color = Color(0xFF00E5FF),
                        textColor = textColor,
                        testTag = "impact_parameter_slider"
                    )
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
    val cardBg = if (isDark) Color(0xFF131D33) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val formatted = viewModel.formatAllLogsForExport()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("CollisionLogs", formatted))
                    Toast.makeText(context, "Exported ${eventHistory.size} event logs to clipboard!", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("export_logs_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "COPY LOGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 10.sp
                    )
                )
            }

            OutlinedButton(
                onClick = { viewModel.clearLogs() },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("clear_logs_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF1744),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "CLEAR",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF1744),
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (eventHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No collision events recorded.\nFire a head-on collision to record telemetry.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(eventHistory) { ev ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "EVENT #${ev.eventId} [${ev.primaryProcessName}]",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E5FF),
                                        fontSize = 10.sp
                                    )
                                )
                                Text(
                                    text = "${ev.multiplicity} trk • ${ev.chargedMultiplicity} ch",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        color = Color.Gray
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "√s = %.1f GeV | M_inv = %.2f GeV/c² | ∑E_T = %.1f GeV".format(
                                    ev.centerOfMassEnergyGeV, ev.invariantMassGeV, ev.totalTransverseEnergyGeV
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    color = textColor
                                )
                            )
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
                color = Color.White
            )
        )
    }
}
