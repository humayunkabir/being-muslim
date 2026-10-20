package app.beingmuslim.android.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.beingmuslim.android.R
import app.beingmuslim.android.citycatalog.City
import app.beingmuslim.android.qibla.CompassAccuracy
import app.beingmuslim.android.qibla.CompassReading
import app.beingmuslim.android.qibla.CompassSensor
import app.beingmuslim.android.qibla.QiblaBearing
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.delay

/**
 * The qibla for the user's city, from the city's coordinates alone, plus a live
 * compass where the device has one (issues #9 and #10).
 *
 * The numeric bearing from true north is always on screen, so a missing or
 * unusable compass never removes the direction. The live dial appears only when
 * the platform reports a usable accuracy: a reading it does not trust is
 * replaced with a calibration prompt rather than shown as a direction to follow
 * (`docs/PRODUCT_REQUIREMENTS.md` §6).
 */
@Composable
fun QiblaScreen(
    state: AppUiState.Qibla,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val city = state.city
    val bearing = city?.let { QiblaBearing.from(it.latitude, it.longitude) }

    // The system back gesture leaves for the times, as Back does.
    BackHandler(onBack = onBack)

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
                text = stringResource(R.string.qibla_title),
                style = MaterialTheme.typography.headlineSmall,
            )

            if (city == null || bearing == null) {
                // The app opens the qibla only for a chosen city; this state is
                // still explained rather than left blank (issue #9).
                Text(
                    text = stringResource(R.string.qibla_no_city),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                QiblaContent(city = city, bearing = bearing)
            }

            TextButton(onClick = onBack) {
                Text(stringResource(R.string.qibla_back))
            }
        }
    }
}

@Composable
private fun QiblaContent(city: City, bearing: Double) {
    val context = LocalContext.current
    val sensor = remember(context) { CompassSensor(context) }
    val sensorAvailable = remember(sensor) { sensor.isAvailable() }
    val readings: Flow<CompassReading> = remember(sensor, city, sensorAvailable) {
        if (sensorAvailable) sensor.readings(city.latitude, city.longitude) else emptyFlow()
    }
    val reading by readings.collectAsStateWithLifecycle(initialValue = null)

    // A sensor that never delivers a reading must not leave the screen waiting
    // forever; after a short wait the compass is reported as unavailable.
    var compassTimedOut by remember(sensorAvailable) { mutableStateOf(false) }
    LaunchedEffect(sensorAvailable) {
        if (!sensorAvailable) return@LaunchedEffect
        delay(COMPASS_WAIT_TIMEOUT_MS)
        compassTimedOut = true
    }

    // A reading the platform does not trust is treated as no reading at all: the
    // dial then shows the bearing from true north instead of a direction that
    // may be wrong.
    val liveHeading = when (reading?.accuracy) {
        CompassAccuracy.HIGH, CompassAccuracy.MEDIUM -> reading?.trueHeadingDegrees
        else -> null
    }

    Text(
        text = stringResource(R.string.qibla_bearing, bearing.roundToInt()),
        style = MaterialTheme.typography.headlineMedium,
    )
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        QiblaDial(
            qiblaBearing = bearing,
            heading = liveHeading,
            modifier = Modifier.size(240.dp),
        )
    }

    when {
        !sensorAvailable -> StateMessage(stringResource(R.string.qibla_compass_unavailable))
        reading == null && compassTimedOut ->
            StateMessage(stringResource(R.string.qibla_compass_unavailable))
        reading == null -> StateMessage(stringResource(R.string.qibla_compass_waiting))
        liveHeading == null -> StateMessage(stringResource(R.string.qibla_calibration_required))
        else -> {
            val turn = QiblaBearing.turnDegrees(liveHeading, bearing)
            Text(
                text = when {
                    abs(turn) <= FACING_TOLERANCE_DEGREES ->
                        stringResource(R.string.qibla_facing)
                    turn > 0 ->
                        stringResource(R.string.qibla_turn_right, abs(turn).roundToInt())
                    else ->
                        stringResource(R.string.qibla_turn_left, abs(turn).roundToInt())
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (reading?.accuracy == CompassAccuracy.MEDIUM) {
                StateMessage(stringResource(R.string.qibla_reading_approximate))
            }
        }
    }

    Text(
        text = stringResource(R.string.qibla_explanation, city.displayName(currentLanguage())),
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = stringResource(R.string.qibla_compass_limitation),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun StateMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
    )
}

/**
 * A compass dial. The dial turns with the device's [heading] so that north — and
 * the qibla marker at [qiblaBearing] — sit at their true-north positions; the
 * fixed pointer at the top is the direction the top of the phone faces. With no
 * [heading], the dial is north-up and the bearing alone is shown.
 */
@Composable
private fun QiblaDial(
    qiblaBearing: Double,
    heading: Double?,
    modifier: Modifier = Modifier,
) {
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val marker = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f - 2.dp.toPx()

        drawCircle(
            color = outline,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )

        rotate(degrees = -(heading ?: 0.0).toFloat()) {
            repeat(TICK_COUNT) { index ->
                rotate(degrees = index * TICK_DEGREES) {
                    val length = if (index % 3 == 0) 20.dp.toPx() else 10.dp.toPx()
                    drawLine(
                        color = outline,
                        start = Offset(center.x, center.y - radius),
                        end = Offset(center.x, center.y - radius + length),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            // North: the long tick, drawn over the plain one so it is distinct.
            drawLine(
                color = onSurface,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y - radius + 26.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
            )
            rotate(degrees = qiblaBearing.toFloat()) {
                val tip = Offset(center.x, center.y - radius * 0.78f)
                drawLine(
                    color = marker,
                    start = center,
                    end = tip,
                    strokeWidth = 4.dp.toPx(),
                )
                drawCircle(color = marker, radius = 8.dp.toPx(), center = tip)
            }
        }

        if (heading != null) {
            val pointer = Path().apply {
                moveTo(center.x, center.y - radius - 6.dp.toPx())
                lineTo(center.x - 9.dp.toPx(), center.y - radius + 10.dp.toPx())
                lineTo(center.x + 9.dp.toPx(), center.y - radius + 10.dp.toPx())
                close()
            }
            drawPath(path = pointer, color = onSurface)
        }
    }
}

/** Within this many degrees of the qibla, the user is told they are facing it. */
private const val FACING_TOLERANCE_DEGREES = 3.0

/** How long to wait for the first compass reading before calling it unavailable. */
private const val COMPASS_WAIT_TIMEOUT_MS = 5_000L

private const val TICK_COUNT = 12
private const val TICK_DEGREES = 30f
