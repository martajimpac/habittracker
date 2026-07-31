package com.marta.habittracker.presentation.screens.onboarding

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.marta.habittracker.R
import com.marta.habittracker.presentation.theme.HabitAmber
import com.marta.habittracker.presentation.theme.HabitAmberLight
import com.marta.habittracker.presentation.theme.HabitPrimary
import com.marta.habittracker.presentation.theme.HabitPrimaryLight
import com.marta.habittracker.presentation.components.HabitButton
import com.marta.habittracker.presentation.components.HabitButtonVariant
import com.marta.habittracker.presentation.theme.HabitTeal
import com.marta.habittracker.presentation.theme.HabitTealLight
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val iconRes: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val gradient: List<Color>,
)

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    navigateToLogin: () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.navigateToLogin.collect {
            navigateToLogin()
        }
    }

    val slides = listOf(
        OnboardingSlide(
            iconRes = R.drawable.ic_onboarding_target,
            titleRes = R.string.onboarding_slide1_title,
            descriptionRes = R.string.onboarding_slide1_description,
            gradient = listOf(HabitPrimary, HabitPrimaryLight),
        ),
        OnboardingSlide(
            iconRes = R.drawable.ic_onboarding_chart,
            titleRes = R.string.onboarding_slide2_title,
            descriptionRes = R.string.onboarding_slide2_description,
            gradient = listOf(HabitTeal, HabitTealLight),
        ),
        OnboardingSlide(
            iconRes = R.drawable.ic_onboarding_trophy,
            titleRes = R.string.onboarding_slide3_title,
            descriptionRes = R.string.onboarding_slide3_description,
            gradient = listOf(HabitAmber, HabitAmberLight),
        ),
    )

    OnboardingContent(
        slides = slides,
        onSkip = viewModel::completeOnboarding,
        onComplete = viewModel::completeOnboarding,
    )
}

@Composable
fun OnboardingContent(
    slides: List<OnboardingSlide>,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        val slide = slides[page]
        OnboardingPage(
            slide = slide,
            pageIndex = page,
            pageCount = slides.size,
            isLast = page == slides.lastIndex,
            onSkip = onSkip,
            onContinue = {
                if (page == slides.lastIndex) {
                    onComplete()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(page + 1)
                    }
                }
            },
        )
    }
}

@Composable
private fun OnboardingPage(
    slide: OnboardingSlide,
    pageIndex: Int,
    pageCount: Int,
    isLast: Boolean,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(slide.gradient))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            HabitButton(
                text = stringResource(R.string.onboarding_skip),
                onClick = onSkip,
                variant = HabitButtonVariant.GhostPill,
                fillMaxWidth = false,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color.White.copy(alpha = 0.2f)),
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) { index ->
                val selected = index == pageIndex
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (selected) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (selected) 1f else 0.4f)),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        HabitButton(
            text = stringResource(
                if (isLast) R.string.onboarding_get_started else R.string.onboarding_continue
            ),
            onClick = onContinue,
            modifier = Modifier.padding(bottom = 40.dp),
            variant = HabitButtonVariant.PrimaryLight,
        )
    }
}
