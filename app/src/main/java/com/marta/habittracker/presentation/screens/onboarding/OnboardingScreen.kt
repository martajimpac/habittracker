package com.marta.habittracker.presentation.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marta.habittracker.R
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.theme.HabitColor1
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitColor2
import com.marta.habittracker.presentation.utils.ObserveAsEvents
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val iconRes: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val backgroundColor: Color,
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    navigateToLogin: () -> Unit,
) {
    ObserveAsEvents(flow = viewModel.navigateToLogin) {
        navigateToLogin()
    }

    OnboardingContent(
        onSkip = viewModel::completeOnboarding,
        onComplete = viewModel::completeOnboarding,
    )
}

@Composable
fun OnboardingContent(
    initialPage: Int = 0,   
    onSkip: () -> Unit,
    onComplete: () -> Unit,
) {
    val slides = listOf(
        OnboardingSlide(
            iconRes = R.drawable.ic_onboarding_target,
            titleRes = R.string.onboarding_slide1_title,
            descriptionRes = R.string.onboarding_slide1_description,
            backgroundColor = HabitPrimary,
        ),
        OnboardingSlide(
            iconRes = R.drawable.ic_onboarding_chart,
            titleRes = R.string.onboarding_slide2_title,
            descriptionRes = R.string.onboarding_slide2_description,
            backgroundColor = HabitPrimary.copy(alpha = 0.85f),
        ),
        OnboardingSlide(
            iconRes = R.drawable.ic_onboarding_trophy,
            titleRes = R.string.onboarding_slide3_title,
            descriptionRes = R.string.onboarding_slide3_description,
            backgroundColor = HabitPrimary.copy(alpha = 0.7f),
        ),
    )

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { slides.size }
    )

    val scope = rememberCoroutineScope()

    val currentPage = pagerState.currentPage
    val currentSlide = slides[currentPage]

    val backgroundColor by animateColorAsState(
        targetValue = currentSlide.backgroundColor,
        animationSpec = tween(durationMillis = 600),
        label = "onboardingBackground",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .safeDrawingPadding()
            .padding(
                vertical = 16.dp,
                horizontal = 24.dp,
            )
    ) {

        // Contenido que se desliza
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->

            OnboardingPage(
                slide = slides[page],
            )
        }

        // Skip fijo arriba
        HabitButton(
            text = stringResource(R.string.onboarding_skip),
            onClick = onSkip,
            variant = HabitButtonVariant.GhostPill,
            fillMaxWidth = false,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
        )

        // Controles fijos abajo
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Indicadores
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(slides.size) { index ->
                    val selected = index == currentPage

                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (selected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                Color.White.copy(
                                    alpha = if (selected) 1f else 0.4f,
                                ),
                            ),
                    )

                    if (index < slides.lastIndex) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Continue
            HabitButton(
                text = stringResource(
                    if (currentPage == slides.lastIndex) {
                        R.string.onboarding_get_started
                    } else {
                        R.string.onboarding_continue
                    }
                ),
                onClick = {
                    if (currentPage == slides.lastIndex) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(
                                currentPage + 1
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                variant = HabitButtonVariant.PrimaryLight,
            )
        }
    }
}

@Composable
private fun OnboardingPage(
    slide: OnboardingSlide,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(
                    Color.White.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(slide.iconRes),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(slide.titleRes),
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(slide.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Preview(name = "Página 1", showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingPage1Preview() {
    OnboardingContent(
        initialPage = 0,
        onSkip = {},
        onComplete = {},
    )
}

@Preview(name = "Página 2", showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingPage2Preview() {
    OnboardingContent(
        initialPage = 1,
        onSkip = {},
        onComplete = {},
    )
}

@Preview(name = "Página 3 (Final)", showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingPage3Preview() {
    OnboardingContent(
        initialPage = 2,
        onSkip = {},
        onComplete = {},
    )
}