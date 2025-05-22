package com.vlamik.core.domain.usecase.filtering

/**
 * Object containing logic to check if a numerical value matches a range filter.
 */
object RangeFilter {
    /**
     * Checks if a given numerical value falls within a range filter.
     * @param value The numerical value to check.
     * @param range The FilterValue.Range object.
     * @return True if the value falls within the range, otherwise False.
     */
    fun matches(
        value: Double,
        range: FilterValue.Range
    ): Boolean {
        // Use the values from the FilterValue.Range object
        return when {
            // If FilterValue defines both start and end (represents an "from - to" range)
            range.start != null && range.end != null ->
                value >= range.start && value <= range.end
            // If FilterValue defines only start (represents an "over" range)
            range.start != null ->
                value >= range.start
            // If FilterValue defines only end (represents an "under" range)
            range.end != null ->
                value <= range.end
            // Other case (should not happen with correct FilterValue)
            else -> false
        }
    }
}
