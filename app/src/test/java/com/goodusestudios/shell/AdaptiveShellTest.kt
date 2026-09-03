package com.goodusestudios.weldinggaswallet

import com.goodusestudios.weldinggaswallet.ui.NavigationMode
import com.goodusestudios.weldinggaswallet.ui.OnboardingLayoutMode
import com.goodusestudios.weldinggaswallet.ui.navigationModeForWidth
import com.goodusestudios.weldinggaswallet.ui.onboardingLayoutModeFor
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveShellTest {
    @Test fun compactWindowUsesBottomBar() =
        assertEquals(NavigationMode.BottomBar, navigationModeForWidth(599))

    @Test fun mediumWindowUsesRail() =
        assertEquals(NavigationMode.Rail, navigationModeForWidth(600))

    @Test fun expandedWindowUsesSidebar() =
        assertEquals(NavigationMode.Sidebar, navigationModeForWidth(840))

    @Test fun shortWindowUsesScrollableOnboarding() =
        assertEquals(OnboardingLayoutMode.Scrollable, onboardingLayoutModeFor(699, 1f))

    @Test fun largeTextUsesScrollableOnboarding() =
        assertEquals(OnboardingLayoutMode.Scrollable, onboardingLayoutModeFor(900, 1.31f))

    @Test fun regularWindowAnchorsOnboarding() =
        assertEquals(OnboardingLayoutMode.Anchored, onboardingLayoutModeFor(700, 1.3f))
}
