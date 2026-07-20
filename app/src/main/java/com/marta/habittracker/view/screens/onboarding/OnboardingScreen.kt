package com.marta.habittracker.view.screens.onboarding

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marta.habittracker.R
import com.marta.habittracker.ui.theme.HabitAmber
import com.marta.habittracker.ui.theme.HabitAmberLight
import com.marta.habittracker.ui.theme.HabitPrimary
import com.marta.habittracker.ui.theme.HabitPrimaryLight
import com.marta.habittracker.ui.theme.HabitTeal
import com.marta.habittracker.ui.theme.HabitTealLight
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val emoji: String,
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
            emoji = "🎯",
            titleRes = R.string.onboarding_slide1_title,
            descriptionRes = R.string.onboarding_slide1_description,
            gradient = listOf(HabitPrimary, HabitPrimaryLight),
        ),
        OnboardingSlide(
            emoji = "📊",
            titleRes = R.string.onboarding_slide2_title,
            descriptionRes = R.string.onboarding_slide2_description,
            gradient = listOf(HabitTeal, HabitTealLight),
        ),
        OnboardingSlide(
            emoji = "🏆",
            titleRes = R.string.onboarding_slide3_title,
            descriptionRes = R.string.onboarding_slide3_description,
            gradient = listOf(HabitAmber, HabitAmberLight),
        ),
    )

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
            onSkip = viewModel::completeOnboarding,
            onContinue = {
                if (page == slides.lastIndex) {
                    viewModel.completeOnboarding()
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
            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = slide.emoji, fontSize = 72.sp)
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

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = HabitPrimary,
            ),
        ) {
            Text(
                text = stringResource(
                    if (isLast) R.string.onboarding_get_started else R.string.onboarding_continue
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
