package io.github.mickaelmagniez.windbubble.domain

import com.google.common.truth.Truth.assertThat
import io.github.mickaelmagniez.windbubble.core.util.angleDelta
import io.github.mickaelmagniez.windbubble.core.util.lerpAngle
import io.github.mickaelmagniez.windbubble.core.util.normalizeDegrees
import io.github.mickaelmagniez.windbubble.core.util.toCardinalPoint
import org.junit.Test

class AnglesTest {

    @Test
    fun `normalisation keeps angles in the zero to three sixty range`() {
        assertThat((-90f).normalizeDegrees()).isWithin(TOLERANCE).of(270f)
        assertThat(450f.normalizeDegrees()).isWithin(TOLERANCE).of(90f)
    }

    @Test
    fun `delta takes the shortest path across north`() {
        assertThat(angleDelta(from = 350f, to = 10f)).isWithin(TOLERANCE).of(20f)
        assertThat(angleDelta(from = 10f, to = 350f)).isWithin(TOLERANCE).of(-20f)
    }

    @Test
    fun `interpolation crosses north instead of going the long way round`() {
        assertThat(lerpAngle(from = 350f, to = 10f, fraction = 0.5f)).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun `cardinal points match the sixteen point compass`() {
        assertThat(0f.toCardinalPoint()).isEqualTo("N")
        assertThat(90f.toCardinalPoint()).isEqualTo("E")
        assertThat(247.5f.toCardinalPoint()).isEqualTo("WSW")
        assertThat(359f.toCardinalPoint()).isEqualTo("N")
    }

    private companion object {
        const val TOLERANCE = 0.001f
    }
}
