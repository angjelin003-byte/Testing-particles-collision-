package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physics.CollisionEventResult
import com.example.physics.ParticleSpecies
import com.example.physics.StandardModelCatalog

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

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Telemetry", "Particles", "Controls", "Event Logs")

    val isDark = simState.isDarkTheme
    val panelBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val accentCyan = Color(0xFF00E5FF)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(panelBg)
            .padding(8.dp)
    ) {
        // Tab Header Bar
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 4.dp,
            containerColor = panelBg,
            contentColor = accentCyan,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    modifier = Modifier.testTag("dashboard_tab_$index"),
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelemetryTab(
    currentEvent: CollisionEventResult?,
    simState: SimulationState,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val accentCyan = Color(0xFF00E5FF)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Live Status Header Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = accentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE COLLISION TELEMETRY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = accentCyan,
                                fontSize = 13.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentEvent != null) {
                        Surface(
                            color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PROCESS: ${currentEvent.primaryProcessName}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF80DEEA) else Color(0xFF00838F),
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Status: Beam Ready. Tap 'FIRE COLLISION' in Particles tab.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }

        item {
            // Metrics Grid Cards
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricReadoutCard(
                        title = "BEAM ENERGY √s",
                        value = "%.1f GeV".format(simState.energyGeV),
                        subtext = "(%.3f TeV)".format(simState.energyGeV / 1000.0),
                        color = Color(0xFF00E5FF),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    MetricReadoutCard(
                        title = "B-FIELD SOLENOID",
                        value = "%.2f T".format(simState.magneticFieldTesla),
                        subtext = "Solenoidal Bz",
                        color = Color(0xFFFFD600),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricReadoutCard(
                        title = "MULTIPLICITY",
                        value = "${currentEvent?.multiplicity ?: 0} tracks",
                        subtext = "${currentEvent?.calorimeterHits?.size ?: 0} Cal Hits",
                        color = Color(0xFF00E676),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    MetricReadoutCard(
                        title = "INVARIANT MASS",
                        value = "%.1f GeV/c²".format(currentEvent?.invariantMassGeV ?: 0.0),
                        subtext = "M = √(E² - |p|²c²)",
                        color = Color(0xFFE040FB),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricReadoutCard(
                        title = "MISSING E_T",
                        value = "%.1f GeV".format(currentEvent?.missingETGeV ?: 0.0),
                        subtext = "|E_T_miss| (Neutrinos)",
                        color = Color(0xFFB9F6CA),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                    MetricReadoutCard(
                        title = "CHARGE CONSERVATION",
                        value = if (currentEvent != null) "Q_in=%+.0f → Q_out=%+.0f".format(currentEvent.initialCharge, currentEvent.finalCharge) else "Exact Q = 0",
                        subtext = "ΔQ = 0 Validated",
                        color = Color(0xFFFF9100),
                        isDark = isDark,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            // Relativistic Mathematics & Physics Formulas Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Functions,
                            contentDescription = null,
                            tint = Color(0xFFFFD600),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RELATIVISTIC PHYSICS EQUATIONS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD600),
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    EquationRow("Relativistic Energy:", "E = √(p²c² + m₀²c⁴) = γ m₀ c²")
                    EquationRow("Cyclotron Radius:", "R = p_T / (q B_z)")
                    EquationRow("Pseudo-Rapidity:", "η = -ln[tan(θ/2)]")
                    EquationRow("Minkowski Invariant Mass:", "M = √(P_μ P^μ) / c²")
                    EquationRow("Missing Transverse Energy:", "|E_T_miss| = √[(∑p_x)² + (∑p_y)²]")
                }
            }
        }

        item {
            // Generated Particle Species Breakdown Chips
            if (currentEvent != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "PARTICLE SPECIES SPECTRUM",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.LightGray else Color.DarkGray,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val speciesCounts = currentEvent.generatedParticles.groupBy { it.species.symbol }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            speciesCounts.forEach { (symbol, list) ->
                                val sp = list.first().species
                                Surface(
                                    color = sp.color.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, sp.color)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = symbol,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = sp.color
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "×${list.size}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.sp,
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
        }
    }
}

@Composable
fun EquationRow(label: String, formula: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 10.sp,
                color = Color.Gray
            )
        )
        Text(
            text = formula,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color(0xFF80DEEA)
            )
        )
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
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 13.sp
                )
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParticlesTab(
    viewModel: ColliderViewModel,
    simState: SimulationState,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Big Fire Collision CTA Button
            Button(
                onClick = { viewModel.fireCollision() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("fire_collision_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF0844)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FIRE HEAD-ON COLLISION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                )
            }
        }

        item {
            ParticleSelectorCard(
                label = "BEAM PARTICLE A (Moving +Z)",
                selectedSpecies = simState.particleA,
                onSelect = { viewModel.setParticleA(it) },
                cardBg = cardBg,
                isDark = isDark
            )
        }

        item {
            ParticleSelectorCard(
                label = "BEAM PARTICLE B (Moving -Z)",
                selectedSpecies = simState.particleB,
                onSelect = { viewModel.setParticleB(it) },
                cardBg = cardBg,
                isDark = isDark
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParticleSelectorCard(
    label: String,
    selectedSpecies: ParticleSpecies,
    onSelect: (ParticleSpecies) -> Unit,
    cardBg: Color,
    isDark: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StandardModelCatalog.ALL_SPECIES.forEach { species ->
                    val isSelected = species.id == selectedSpecies.id
                    val chipBg = if (isSelected) species.color else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    val chipText = if (isSelected) Color.Black else (if (isDark) Color.White else Color.Black)

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(species) }
                            .testTag("select_particle_${species.id}"),
                        color = chipBg
                    ) {
                        Text(
                            text = species.symbol,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = chipText,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "${selectedSpecies.name} (${selectedSpecies.symbol}) - ${selectedSpecies.category.displayName}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = selectedSpecies.color,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = "Mass: %.4f GeV | Charge: %+.1f e | Spin: %s | B: %d, L: %d".format(
                            selectedSpecies.restMassGeV,
                            selectedSpecies.charge,
                            selectedSpecies.spin,
                            selectedSpecies.baryonNumber,
                            selectedSpecies.leptonNumber
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ControlsTab(
    viewModel: ColliderViewModel,
    simState: SimulationState,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "ENVIRONMENT & FIELD PARAMETERS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Center-of-Mass Energy √s: %.0f GeV (%.2f TeV)".format(
                            simState.energyGeV,
                            simState.energyGeV / 1000.0
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = textColor,
                            fontSize = 11.sp
                        )
                    )
                    Slider(
                        value = simState.energyGeV.toFloat(),
                        onValueChange = { viewModel.setEnergyGeV(it.toDouble()) },
                        valueRange = 100f..14000f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E5FF),
                            activeTrackColor = Color(0xFF00E5FF)
                        ),
                        modifier = Modifier.testTag("energy_slider")
                    )

                    Text(
                        text = "Solenoidal Magnetic Field B: %.2f Tesla".format(simState.magneticFieldTesla),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = textColor,
                            fontSize = 11.sp
                        )
                    )
                    Slider(
                        value = simState.magneticFieldTesla,
                        onValueChange = { viewModel.setMagneticFieldTesla(it) },
                        valueRange = 0f..8f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD600),
                            activeTrackColor = Color(0xFFFFD600)
                        ),
                        modifier = Modifier.testTag("magnetic_field_slider")
                    )

                    Text(
                        text = "Slow-Mo Time Scale: %.4f c".format(simState.timeScale),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = textColor,
                            fontSize = 11.sp
                        )
                    )
                    Slider(
                        value = simState.timeScale,
                        onValueChange = { viewModel.setTimeScale(it) },
                        valueRange = 0.0005f..0.02f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00E676),
                            activeTrackColor = Color(0xFF00E676)
                        ),
                        modifier = Modifier.testTag("time_scale_slider")
                    )

                    Text(
                        text = "Trajectory Glow Intensity: %.1f×".format(simState.glowIntensity),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = textColor,
                            fontSize = 11.sp
                        )
                    )
                    Slider(
                        value = simState.glowIntensity,
                        onValueChange = { viewModel.setGlowIntensity(it) },
                        valueRange = 0.2f..3.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFE040FB),
                            activeTrackColor = Color(0xFFE040FB)
                        ),
                        modifier = Modifier.testTag("glow_slider")
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "DETECTOR LAYER VISIBILITY",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LayerToggleRow("Silicon Inner Tracker", simState.showTracker, { viewModel.toggleTrackerLayer() }, Color(0xFF7C4DFF))
                    LayerToggleRow("Electromagnetic Calorimeter (ECAL)", simState.showEcal, { viewModel.toggleEcalLayer() }, Color(0xFF00E676))
                    LayerToggleRow("Hadronic Calorimeter (HCAL)", simState.showHcal, { viewModel.toggleHcalLayer() }, Color(0xFFFF9100))
                    LayerToggleRow("Outer Muon Drift Tubes", simState.showMuon, { viewModel.toggleMuonLayer() }, Color(0xFFFF1744))
                }
            }
        }
    }
}

@Composable
fun LayerToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = color)
        )
    }
}

@Composable
fun EventLogsTab(
    viewModel: ColliderViewModel,
    eventHistory: List<CollisionEventResult>,
    context: Context,
    isDark: Boolean
) {
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val formatted = viewModel.formatAllLogsForExport()
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Particle Collider Logs", formatted)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Event logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.testTag("copy_logs_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "COPY LOGS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 11.sp
                    )
                )
            }

            OutlinedButton(
                onClick = { viewModel.clearLogs() },
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "CLEAR", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (eventHistory.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recorded collision event logs.\nTap 'FIRE COLLISION' to generate logs.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(eventHistory) { ev ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "EVENT #${ev.eventId} | ${ev.primaryProcessName}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF),
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ev.decayTreeFormatted,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    color = if (isDark) Color(0xFFB0BEC5) else Color(0xFF37474F)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
