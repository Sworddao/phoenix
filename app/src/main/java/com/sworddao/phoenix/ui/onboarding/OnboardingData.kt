package com.sworddao.phoenix.ui.onboarding

import androidx.annotation.StringRes
import com.sworddao.phoenix.R
import com.sworddao.phoenix.ui.components.BaoExpression

data class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    @StringRes val descriptionRes: Int,
    val baoExpression: BaoExpression,
    val isLastPage: Boolean = false
)

val onboardingPages = listOf(
    OnboardingPage(
        titleRes = R.string.onboarding_page1_title,
        subtitleRes = R.string.onboarding_page1_subtitle,
        descriptionRes = R.string.onboarding_page1_description,
        baoExpression = BaoExpression.WAVE
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_page2_title,
        subtitleRes = R.string.onboarding_page2_subtitle,
        descriptionRes = R.string.onboarding_page2_description,
        baoExpression = BaoExpression.HAPPY
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_page3_title,
        subtitleRes = R.string.onboarding_page3_subtitle,
        descriptionRes = R.string.onboarding_page3_description,
        baoExpression = BaoExpression.EXCITED
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_page4_title,
        subtitleRes = R.string.onboarding_page4_subtitle,
        descriptionRes = R.string.onboarding_page4_description,
        baoExpression = BaoExpression.HAPPY,
        isLastPage = true
    )
)
