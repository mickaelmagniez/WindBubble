package io.github.mickaelmagniez.windbubble.domain

import com.google.common.truth.Truth.assertThat
import io.github.mickaelmagniez.windbubble.domain.model.SpeedUnit
import org.junit.Test

class SpeedUnitTest {

    @Test
    fun `ten meters per second converts to every supported unit`() {
        assertThat(SpeedUnit.KILOMETERS_PER_HOUR.displayValue(10f)).isEqualTo(36)
        assertThat(SpeedUnit.METERS_PER_SECOND.displayValue(10f)).isEqualTo(10)
        assertThat(SpeedUnit.MILES_PER_HOUR.displayValue(10f)).isEqualTo(22)
        assertThat(SpeedUnit.KNOTS.displayValue(10f)).isEqualTo(19)
    }

    @Test
    fun `format appends the unit label`() {
        assertThat(SpeedUnit.KILOMETERS_PER_HOUR.format(5f)).isEqualTo("18 km/h")
    }
}
