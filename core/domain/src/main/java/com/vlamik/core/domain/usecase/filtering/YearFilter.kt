package com.vlamik.core.domain.usecase.filtering

/**
 * Object containing logic to check if a year matches a year range filter.
 */
object YearFilter {
    /**
     * Checks if the year from a given date string falls within a year range filter.
     * @param date The date string.
     * @param yearRange The FilterValue.YearRange object.
     * @return True if the year falls within the range, otherwise False.
     */
    fun matches(
        date: String,
        yearRange: FilterValue.YearRange
    ): Boolean {
        val year = FilterUtils.extractYear(date) ?: return false

        // Use the values from the FilterValue.YearRange object
        return when {
            // If FilterValue defines both startYear and endYear (represents an "from - to" range)
            yearRange.startYear != null && yearRange.endYear != null ->
                year >= yearRange.startYear && year <= yearRange.endYear
            // If FilterValue defines only startYear (represents an "after year" range)
            yearRange.startYear != null ->
                year >= yearRange.startYear
            // If FilterValue defines only endYear (represents a "before year" range)
            yearRange.endYear != null ->
                year <= yearRange.endYear
            // Other case (should not happen with correct FilterValue)
            else -> false
        }
    }
}
