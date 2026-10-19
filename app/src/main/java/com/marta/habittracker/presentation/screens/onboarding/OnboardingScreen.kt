package com.marta.habittracker.presentation.screens.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marta.habittracker.R
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.theme.InstaDevTheme
import com.marta.habittracker.presentation.theme.isHabitDarkTheme
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import com.marta.habittracker.presentation.utils.SyncStatusBarIcons
import kotlin.math.cos
import kotlin.math.sin

private val BloomCoral = Color(0xFFFF7775)
private val BloomYellow = Color(0xFFFFB938)

private data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val button: Int,
)

private val onboardingPages = listOf(
    OnboardingPage(
        title = R.string.onboarding_slide1_title,
        description = R.string.onboarding_slide1_description,
        button = R.string.onboarding_get_started,
    ),
    OnboardingPage(
        title = R.string.onboarding_slide2_title,
        description = R.string.onboarding_slide2_description,
        button = R.string.onboarding_continue,
    ),
    OnboardingPage(
        title = R.string.onboarding_slide3_title,
        description = R.string.onboarding_slide3_description,
        button = R.string.onboarding_lets_bloom,
    ),
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    navigateToLogin: () -> Unit,
) {
    ObserveAsEvents(flow = viewModel.navigateToLogin) {
        navigateToLogin()
    }

    OnboardingContent(onComplete = viewModel::completeOnboarding)
}

@Composable
fun OnboardingContent(
    onComplete: () -> Unit,
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val colors = MaterialTheme.colorScheme

    SyncStatusBarIcons(lightBackground = !isHabitDarkTheme())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surface)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.onboarding_wordmark),
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.primary,
                letterSpacing = (-2).sp,
            )

            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                transitionSpec = {
                    val forward = targetState > initialState
                    val enter = slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = 300,
                            easing = FastOutSlowInEasing,
                        ),
                        initialOffsetX = { width ->
                            if (forward) width / 3 else -width / 3
                        },
                    ) + fadeIn(animationSpec = tween(300))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = 180,
                            easing = FastOutSlowInEasing,
                        ),
                        targetOffsetX = { width ->
                            if (forward) -width / 4 else width / 4
                        },
                    ) + fadeOut(animationSpec = tween(180))
                    enter togetherWith exit
                },
                label = "onboarding_transition",
            ) { pageIndex ->
                val page = onboardingPages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    BloomFlowerIllustration(
                        page = pageIndex,
                        modifier = Modifier.size(width = 240.dp, height = 250.dp),
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Text(
                        text = stringResource(page.title),
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(page.description),
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                onboardingPages.indices.forEach { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == currentPage) 22.dp else 7.dp,
                                height = 7.dp,
                            )
                            .background(
                                color = if (index == currentPage) {
                                    colors.primary
                                } else {
                                    colors.primary.copy(alpha = 0.18f)
                                },
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HabitButton(
                text = stringResource(onboardingPages[currentPage].button),
                onClick = {
                    if (currentPage < onboardingPages.lastIndex) {
                        currentPage++
                    } else {
                        onComplete()
                    }
                },
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BloomFlowerIllustration(
    page: Int,
    modifier: Modifier = Modifier,
) {
    var grown by remember(page) { mutableStateOf(false) }
    val stem = MaterialTheme.colorScheme.primary

    LaunchedEffect(page) {
        grown = false
        grown = true
    }

    val flowerScale by animateFloatAsState(
        targetValue = if (grown) 1f else 0.65f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "flower_growth",
    )
    val flowerAlpha by animateFloatAsState(
        targetValue = if (grown) 1f else 0f,
        animationSpec = tween(350),
        label = "flower_alpha",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(flowerScale)
                .alpha(flowerAlpha),
        ) {
            val w = size.width
            val h = size.height
            val stemPath = Path().apply {
                moveTo(w * 0.50f, h * 0.91f)
                cubicTo(
                    w * 0.48f, h * 0.70f,
                    w * 0.54f, h * 0.45f,
                    w * 0.50f, h * 0.23f,
                )
            }
            drawPath(
                path = stemPath,
                color = stem,
                style = Stroke(width = w * 0.035f, cap = StrokeCap.Round),
            )
            drawOval(
                color = stem,
                topLeft = Offset(w * 0.24f, h * 0.54f),
                size = Size(w * 0.25f, h * 0.10f),
            )
            drawOval(
                color = stem,
                topLeft = Offset(w * 0.51f, h * 0.43f),
                size = Size(w * 0.24f, h * 0.10f),
            )
            drawBloom(
                center = Offset(w * 0.50f, h * 0.20f),
                petalRadius = w * 0.09f,
                petalColor = if (page == 1) BloomYellow else BloomCoral,
            )
            if (page == 2) {
                drawBloom(
                    center = Offset(w * 0.72f, h * 0.34f),
                    petalRadius = w * 0.065f,
                    petalColor = BloomCoral,
                )
            }
        }
    }
}

private fun DrawScope.drawBloom(
    center: Offset,
    petalRadius: Float,
    petalColor: Color,
) {
    repeat(5) { index ->
        val angle = Math.toRadians((index * 72.0) - 90.0)
        drawCircle(
            color = petalColor,
            radius = petalRadius,
            center = Offset(
                center.x + cos(angle).toFloat() * petalRadius * 0.72f,
                center.y + sin(angle).toFloat() * petalRadius * 0.72f,
            ),
        )
    }
    drawCircle(
        color = BloomYellow,
        radius = petalRadius * 0.48f,
        center = center,
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingContentPreview() {
    InstaDevTheme {
        OnboardingContent(onComplete = {})
    }
}
