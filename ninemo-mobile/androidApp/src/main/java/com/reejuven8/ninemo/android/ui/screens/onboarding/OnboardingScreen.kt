package com.reejuven8.ninemo.android.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.reejuven8.ninemo.android.ui.components.NineMoButton
import com.reejuven8.ninemo.shared.model.DateBasis
import com.reejuven8.ninemo.shared.viewmodel.OnboardingViewModel
import com.reejuven8.ninemo.shared.viewmodel.UiState
import org.koin.compose.viewmodel.koinViewModel

private val RiskFlagOptions = listOf("PCOS", "Hypothyroid", "Type 2 diabetes", "Hypertension", "Previous C-section", "Gestational diabetes history")

/** P4 — 3-step onboarding wizard. Never computes EDD/BMI on-device; server owns that math. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val vm: OnboardingViewModel = koinViewModel()
    val step by vm.step.collectAsStateWithLifecycle()
    val dateBasis by vm.dateBasis.collectAsStateWithLifecycle()
    val riskFlags by vm.riskFlags.collectAsStateWithLifecycle()
    val submitState by vm.submitState.collectAsStateWithLifecycle()
    val backendPending by vm.backendPending.collectAsStateWithLifecycle()

    if (submitState is UiState.Success || (submitState is UiState.Error && backendPending)) {
        OnboardingResultStep(
            profile = (submitState as? UiState.Success)?.data,
            backendPending = backendPending,
            onContinue = onDone,
        )
        return
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .background(
                            if (i < step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }
        Text(
            "Step $step of 3",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 22.dp),
        )

        when (step) {
            1 -> DatesStep(dateBasis, vm::setLmpDate, vm::setUltrasoundDate, vm::setIvfTransferDate)
            2 -> BodyMetricsStep(vm::setAgeYears, vm::setHeightCm, vm::setPrePregnancyWeightKg, vm::setBloodGroup)
            3 -> RiskFlagsStep(riskFlags, vm::toggleRiskFlag)
        }

        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (step) {
                    1 -> "Next: body metrics"
                    2 -> "Next: risk flags"
                    else -> "Last step"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            NineMoButton(
                text = if (step < 3) "Continue" else "Submit for assessment",
                onClick = { if (step < 3) vm.next() else vm.submit() },
                loading = submitState is UiState.Loading,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DatesStep(
    dateBasis: DateBasis,
    onLmp: (String) -> Unit,
    onUltrasound: (String, Int?) -> Unit,
    onIvf: (String) -> Unit,
) {
    Text(
        "Let's find your due date",
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(top = 6.dp),
    )
    Text(
        "Pick whichever date you know — your due date is calculated by our clinical engine, not on this phone.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )

    var lmp by remember { mutableStateOf(dateBasis.lmpDate ?: "") }
    var ultrasound by remember { mutableStateOf(dateBasis.ultrasoundDate ?: "") }
    var ivf by remember { mutableStateOf(dateBasis.ivfTransferDate ?: "") }
    val selected = when {
        dateBasis.lmpDate != null -> "lmp"
        dateBasis.ultrasoundDate != null -> "ultrasound"
        dateBasis.ivfTransferDate != null -> "ivf"
        else -> null
    }

    Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DateOptionCard("Last menstrual period", selected == "lmp") {
            OutlinedTextField(
                value = lmp,
                onValueChange = { lmp = it; onLmp(it) },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
        }
        DateOptionCard("Ultrasound (dating scan)", selected == "ultrasound") {
            OutlinedTextField(
                value = ultrasound,
                onValueChange = { ultrasound = it; onUltrasound(it, null) },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
        }
        DateOptionCard("IVF transfer date", selected == "ivf") {
            OutlinedTextField(
                value = ivf,
                onValueChange = { ivf = it; onIvf(it) },
                placeholder = { Text("YYYY-MM-DD") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
        }
    }
}

@Composable
private fun DateOptionCard(title: String, selected: Boolean, field: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(18.dp),
            )
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(18.dp),
            )
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Box(Modifier.padding(top = 10.dp)) { field() }
    }
}

@Composable
private fun BodyMetricsStep(
    onAge: (Int) -> Unit,
    onHeight: (Double) -> Unit,
    onWeight: (Double) -> Unit,
    onBloodGroup: (String) -> Unit,
) {
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }

    Text("Body metrics", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
    Text(
        "Your BMI is computed server-side from these values.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
    Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = age,
            onValueChange = { age = it; it.toIntOrNull()?.let(onAge) },
            label = { Text("Age (years)") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        OutlinedTextField(
            value = height,
            onValueChange = { height = it; it.toDoubleOrNull()?.let(onHeight) },
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        OutlinedTextField(
            value = weight,
            onValueChange = { weight = it; it.toDoubleOrNull()?.let(onWeight) },
            label = { Text("Pre-pregnancy weight (kg)") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        OutlinedTextField(
            value = bloodGroup,
            onValueChange = { bloodGroup = it; onBloodGroup(it) },
            label = { Text("Blood group") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
    }
}

@Composable
private fun RiskFlagsStep(selected: Set<String>, onToggle: (String) -> Unit) {
    Text("Risk flags", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 6.dp))
    Text(
        "These adjust triage thresholds server-side. Select any that apply — none is fine too.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp),
    )
    Column(Modifier.padding(top = 20.dp)) {
        RiskFlagOptions.forEach { flag ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = flag in selected, onClick = { onToggle(flag) })
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = flag in selected, onCheckedChange = { onToggle(flag) })
                Text(flag, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun OnboardingResultStep(
    profile: com.reejuven8.ninemo.shared.model.PregnancyProfileResponse?,
    backendPending: Boolean,
    onContinue: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (profile != null) {
            Text("You're all set", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Your due date: ${profile.eddDate}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Week ${profile.gestationalWeek} · Trimester ${profile.trimester}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            Text("We couldn't save that", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Your profile wasn't created — check the dates and measurements you entered and try again. " +
                    "Every part of the app needs this to be set up.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        NineMoButton(text = "Continue", onClick = onContinue, modifier = Modifier.padding(top = 28.dp))
    }
}
