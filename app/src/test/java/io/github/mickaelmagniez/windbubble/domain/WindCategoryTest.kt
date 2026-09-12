package io.github.mickaelmagniez.windbubble.domain

import com.google.common.truth.Truth.assertThat
import io.github.mickaelmagniez.windbubble.domain.model.WindCategory
import org.junit.Test

class WindCategoryTest {

    @Test
    fun `categories are symmetric around the travel axis`() {
        val mirrored = listOf(0f to 0f, 45f to 315f, 90f to 270f, 135f to 225f, 179f to 181f)

        mirrored.forEach { (left, right) ->
            assertThat(WindCategory.fromRelativeAngle(left))
                .isEqualTo(WindCategory.fromRelativeAngle(right))
        }
    }

    @Test
    fun `boundaries fall in the expected buckets`() {
        assertThat(WindCategory.fromRelativeAngle(29f)).isEqualTo(WindCategory.HEADWIND)
        assertThat(WindCategory.fromRelativeAngle(31f)).isEqualTo(WindCategory.HEAD_CROSSWIND)
        assertThat(WindCategory.fromRelativeAngle(90f)).isEqualTo(WindCategory.CROSSWIND)
        assertThat(WindCategory.fromRelativeAngle(120f)).isEqualTo(WindCategory.TAIL_CROSSWIND)
        assertThat(WindCategory.fromRelativeAngle(170f)).isEqualTo(WindCategory.TAILWIND)
    }
}
