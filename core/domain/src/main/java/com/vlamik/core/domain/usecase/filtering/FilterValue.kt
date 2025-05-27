package com.vlamik.core.domain.usecase.filtering

import com.vlamik.core.commons.AppText

/**
 * Sealed class representing different types of filter values.
 * Each filter value has a display representation (displayName) and the data needed for the actual filtering.
 */
sealed class FilterValue(open val displayName: AppText) {
    /**
     * Represents an exact match value (e.g., rocket name).
     * @param displayName AppText for displaying the value to the user.
     * @param value The actual String value for filtering.
     */
    data class ExactMatch(override val displayName: AppText, val value: String) :
        FilterValue(displayName)

    /**
     * Represents a numerical range (e.g., height, diameter, mass).
     * @param displayName AppText for displaying the range to the user.
     * @param start The lower bound of the range (inclusive), null if it's an "under" range.
     * @param end The upper bound of the range (inclusive), null if it's an "over" range.
     */
    data class Range(
        override val displayName: AppText,
        val start: Double? = null,
        val end: Double? = null
    ) : FilterValue(displayName)

    /**
     * Represents a range of years (e.g., first flight date).
     * @param displayName AppText for displaying the year range to the user.
     * @param startYear The lower bound of the year range (inclusive), null if it's a "before" range.
     * @param endYear The upper bound of the year range (inclusive), null if it's an "after" range.
     */
    data class YearRange(
        override val displayName: AppText,
        val startYear: Int? = null,
        val endYear: Int? = null
    ) : FilterValue(displayName)
}
