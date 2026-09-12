package io.github.mickaelmagniez.windbubble.domain.model

/** Where the direction the dial is locked on comes from. */
enum class HeadingSource {
    /** Course over ground from the GPS: the direction you are actually travelling. */
    MOVEMENT,

    /**
     * The device compass, used while you are too slow for the GPS to give a course. It follows
     * where the *phone* points, so it is only meaningful when the phone faces your direction.
     */
    COMPASS,

    /** The last trustworthy course, kept while neither of the above is available. */
    LAST_KNOWN,
}
