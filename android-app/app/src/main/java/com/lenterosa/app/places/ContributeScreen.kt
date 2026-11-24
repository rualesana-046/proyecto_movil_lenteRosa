package com.lenterosa.app.places

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenterosa.app.R
import com.lenterosa.app.ui.glass.GlassTopBar
import com.lenterosa.app.ui.glass.currentMotion
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** Pantalla Aportar (RF-04): tres aspectos obligatorios, día o noche y experiencia nocturna. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributeScreen(
    viewModel: ContributeViewModel,
    placeId: String,
    placeName: String,
    onBack: () -> Unit,
    onPublished: () -> Unit,
    onOpenAccount: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.published) {
        if (state.published) {
            viewModel.onPublishedHandled()
            onPublished()
        }
    }
    val hazeState = rememberHazeState()
    val motion = currentMotion()
    Scaffold(
        topBar = {
            GlassTopBar(
                hazeState = hazeState,
                title = { Text(stringResource(R.string.contribute_title, placeName), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        // El formulario pasa por detrás de la barra de vidrio; el relleno deja libre su espacio.
        Column(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 16.dp, bottom = padding.calculateBottomPadding() + 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(stringResource(R.string.contribute_intro), style = MaterialTheme.typography.bodyMedium)
            val missing = if (state.showMissing) state.missing else emptyList()
            RatingQuestion(Question.LIGHTING, R.string.dimension_lighting, R.string.contribute_scale_lighting,
                state.lighting, Question.LIGHTING in missing, viewModel::setRating)
            RatingQuestion(Question.WOMEN_PRESENCE, R.string.dimension_women_presence, R.string.contribute_scale_women_presence,
                state.womenPresence, Question.WOMEN_PRESENCE in missing, viewModel::setRating)
            RatingQuestion(Question.COMMERCIAL_VISIBILITY, R.string.dimension_commercial_visibility,
                R.string.contribute_scale_commercial_visibility, state.commercialVisibility,
                Question.COMMERCIAL_VISIBILITY in missing, viewModel::setRating)
            PeriodQuestion(state.visitPeriod, Question.VISIT_PERIOD in missing, viewModel::setVisitPeriod)
            RatingQuestion(Question.NIGHT_EXPERIENCE, R.string.dimension_night_experience, R.string.contribute_scale_night_experience,
                state.nightExperience, Question.NIGHT_EXPERIENCE in missing, viewModel::setRating) {
                if (state.visitPeriod == VisitPeriod.DAY) {
                    FilterChip(
                        selected = state.notAtNight,
                        onClick = viewModel::chooseNotAtNight,
                        label = { Text(stringResource(R.string.contribute_not_at_night)) },
                    )
                }
            }
            CommentField(state.comment, state.showMissing && state.commentTooLong, viewModel::setComment)
            AnimatedVisibility(visible = state.message != null, enter = motion.riseIn(), exit = motion.sinkOut()) {
                // Se conserva el último mensaje mientras sale.
                var shown by remember { mutableStateOf(state.message) }
                if (state.message != null) shown = state.message
                shown?.let { MessageCard(it, onOpenAccount) }
            }
            Button(
                onClick = { viewModel.submit(placeId) },
                enabled = !state.sending,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (state.sending) R.string.contribute_sending else R.string.contribute_submit))
            }
            Text(
                text = stringResource(R.string.contribute_anonymous),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RatingQuestion(
    question: Question,
    title: Int,
    scale: Int,
    value: Int?,
    isMissing: Boolean,
    onSelect: (Question, Int) -> Unit,
    extra: @Composable () -> Unit = {},
) {
    val titleText = stringResource(title)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titleText, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(stringResource(scale), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            (1..5).forEach { score ->
                val description = stringResource(R.string.contribute_score_description, titleText, score)
                SegmentedButton(
                    selected = value == score,
                    onClick = { onSelect(question, score) },
                    shape = SegmentedButtonDefaults.itemShape(index = score - 1, count = 5),
                    modifier = Modifier.semantics { contentDescription = description },
                    label = { Text(score.toString()) },
                )
            }
        }
        extra()
        AnimatedVisibility(visible = isMissing, enter = currentMotion().popIn(), exit = currentMotion().popOut()) { MissingText() }
    }
}

@Composable
private fun PeriodQuestion(value: VisitPeriod?, isMissing: Boolean, onSelect: (VisitPeriod) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.contribute_period_title), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(VisitPeriod.DAY to R.string.contribute_period_day, VisitPeriod.NIGHT to R.string.contribute_period_night)
                .forEachIndexed { index, (period, label) ->
                    SegmentedButton(
                        selected = value == period,
                        onClick = { onSelect(period) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        label = { Text(stringResource(label)) },
                    )
                }
        }
        AnimatedVisibility(visible = isMissing, enter = currentMotion().popIn(), exit = currentMotion().popOut()) { MissingText() }
    }
}

@Composable
private fun CommentField(comment: String, tooLong: Boolean, onChange: (String) -> Unit) {
    val length = comment.trim().length
    OutlinedTextField(
        value = comment,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.contribute_comment)) },
        supportingText = {
            Text(
                if (tooLong) {
                    stringResource(R.string.contribute_comment_too_long, ContributeUiState.MAX_COMMENT_LENGTH)
                } else {
                    stringResource(R.string.contribute_comment_hint, length, ContributeUiState.MAX_COMMENT_LENGTH)
                },
            )
        },
        isError = tooLong,
        minLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MissingText() {
    Text(stringResource(R.string.contribute_missing), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun MessageCard(message: ContributeMessage, onOpenAccount: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(messageText(message), style = MaterialTheme.typography.bodyMedium)
            if (message is ContributeMessage.NotSignedIn || message is ContributeMessage.EmailNotVerified) {
                TextButton(onClick = onOpenAccount) { Text(stringResource(R.string.detail_go_to_account)) }
            }
        }
    }
}

@Composable
private fun messageText(message: ContributeMessage): String = when (message) {
    ContributeMessage.NotSignedIn -> stringResource(R.string.detail_sign_in_to_contribute)
    ContributeMessage.EmailNotVerified -> stringResource(R.string.detail_verify_to_contribute)
    is ContributeMessage.Rejected -> message.apiMessage
        ?: stringResource(if (message.contactData) R.string.contribute_error_contact_data else R.string.contribute_error_invalid)
    is ContributeMessage.RateLimited -> listOfNotNull(
        message.apiMessage ?: stringResource(R.string.contribute_error_rate_limited),
        message.wait?.let { waitText(it) },
    ).joinToString(" ")
    is ContributeMessage.Failed -> stringResource(message.failure.message())
}

@Composable
private fun waitText(wait: WaitTime): String {
    val amount = when (wait.unit) {
        WaitUnit.DAYS -> pluralStringResource(R.plurals.wait_days, wait.amount, wait.amount)
        WaitUnit.HOURS -> pluralStringResource(R.plurals.wait_hours, wait.amount, wait.amount)
        WaitUnit.MINUTES -> pluralStringResource(R.plurals.wait_minutes, wait.amount, wait.amount)
    }
    return stringResource(R.string.contribute_wait, amount)
}
