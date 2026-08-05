package com.sworddao.phoenix.ui.onboarding

import com.sworddao.phoenix.ui.components.BaoExpression

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val description: String,
    val baoExpression: BaoExpression,
    val isLastPage: Boolean = false
)

val onboardingPages = listOf(
    OnboardingPage(
        title = "Welcome to Phoenix",
        subtitle = "Your Mandarin Adventure Begins",
        description = "You've just arrived in China with almost no Mandarin knowledge. But don't worry — you won't be alone.",
        baoExpression = BaoExpression.WAVE
    ),
    OnboardingPage(
        title = "Meet Bao",
        subtitle = "Your Learning Companion",
        description = "This friendly red panda will guide you through your journey. Bao learns alongside you and celebrates every step of progress.",
        baoExpression = BaoExpression.HAPPY
    ),
    OnboardingPage(
        title = "Learn by Living",
        subtitle = "No Lessons. Just Adventure.",
        description = "Talk to people, complete quests, and explore real-world situations. Learning happens naturally through conversation.",
        baoExpression = BaoExpression.EXCITED
    ),
    OnboardingPage(
        title = "Ready to Begin?",
        subtitle = "Your Journey Starts Now",
        description = "Start in Qingyuan Village and work your way to Phoenix Summit. Every conversation opens new opportunities.",
        baoExpression = BaoExpression.HAPPY,
        isLastPage = true
    )
)
