package app.beingmuslim.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.beingmuslim.android.R

/**
 * The app's language, offered in first-run setup and in settings (issue #14).
 * It takes effect as soon as it is chosen, so the user reads the rest of the
 * screen in the language they picked; the platform keeps the choice.
 */
@Composable
fun LanguageChoice(modifier: Modifier = Modifier) {
    val selected = currentLanguage()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeading(R.string.language_heading)
        Column(Modifier.selectableGroup()) {
            Language.entries.forEach { language ->
                SelectableRow(
                    label = stringResource(language.labelRes()),
                    selected = selected == language,
                    onSelect = { applyLanguage(language) },
                )
            }
        }
    }
}
