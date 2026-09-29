package dev.sadakat.qandeel.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code the app runs at startup and in its common journey, so it ships precompiled
 * (`./gradlew :app:generateBaselineProfile`). The startup part also orders the dex for startup.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        startApp()
    }

    @Test
    fun commonJourney() = rule.collect(packageName = PACKAGE) {
        startApp()
        commonJourney()
    }
}
