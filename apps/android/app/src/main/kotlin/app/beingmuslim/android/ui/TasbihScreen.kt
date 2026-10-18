package app.beingmuslim.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R
import app.beingmuslim.android.tasbih.TasbihCounter

/** A target beyond this many digits cannot be held in an [Int], so it is not accepted. */
private const val MAX_TARGET_DIGITS = 9

/**
 * The tasbih counter: the label the user types, the target they set, and the
 * count. The app ships no text of its own and attributes none — the label is the
 * user's own words (`docs/PRODUCT_REQUIREMENTS.md` §2.1).
 *
 * Counting is one large control with haptic feedback, so it can be used without
 * looking at the screen. The haptic comes from the platform, so it follows the
 * system's haptics setting. Every change is reported to the caller, which
 * persists it.
 */
@Composable
fun TasbihScreen(
    counter: TasbihCounter,
    onLabelChange: (String) -> Unit,
    onTargetChange: (Int?) -> Unit,
    onCount: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The system back gesture leaves with the count kept, as Back does.
    BackHandler(onBack = onBack)

    // The fields hold text, so a half-typed target ("1" on the way to "100") is
    // not rewritten under the cursor; the parsed value goes to the counter.
    var labelText by rememberSaveable { mutableStateOf(counter.label) }
    var targetText by rememberSaveable { mutableStateOf(counter.target?.toString().orEmpty()) }

    val haptics = LocalHapticFeedback.current
    val target = counter.target
    // A count can sit past its target when the user lowers the target under it.
    val progress = if (target == null || counter.count >= target) {
        stringResource(R.string.tasbih_count_only, counter.count)
    } else {
        stringResource(R.string.tasbih_progress, counter.count, target)
    }
    val countDescription = stringResource(R.string.tasbih_count_action, progress)
    val hint = when {
        counter.label.isBlank() || target == null -> R.string.tasbih_not_ready
        counter.count >= target -> R.string.tasbih_reached
        else -> null
    }

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.tasbih_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.tasbih_intro),
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedTextField(
                value = labelText,
                onValueChange = {
                    labelText = it
                    onLabelChange(it)
                },
                label = { Text(stringResource(R.string.tasbih_label_field)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = targetText,
                onValueChange = { typed ->
                    // Digits only, and no more than an Int can hold: a target is
                    // a count, and a value that cannot be read would silently
                    // read as no target at all.
                    targetText = typed.filter(Char::isDigit).take(MAX_TARGET_DIGITS)
                    onTargetChange(targetText.toIntOrNull())
                },
                label = { Text(stringResource(R.string.tasbih_target_field)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    onCount()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                enabled = counter.canCount,
                modifier = Modifier
                    .fillMaxWidth()
                    // Grows with the text size rather than clipping it.
                    .defaultMinSize(minHeight = 120.dp)
                    .semantics {
                        contentDescription = countDescription
                        // Otherwise the new count is never read out, and the
                        // feature is silent to a screen reader.
                        liveRegion = LiveRegionMode.Polite
                    },
            ) {
                Text(text = progress, style = MaterialTheme.typography.headlineMedium)
            }
            hint?.let {
                Text(
                    text = stringResource(it),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    enabled = counter.count > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.tasbih_reset))
                }
                TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.tasbih_back))
                }
            }
        }
    }
}
