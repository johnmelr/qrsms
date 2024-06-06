package com.github.johnmelr.qrsms.utils

import android.icu.text.DateFormat
import android.icu.util.Calendar

/**
 * Utils class containing different utility function that is reusable for different
 * segment of the application.
 */
object DateUtils {
    /**
     *  Utility function that formats a given Date in millisecond
     *  to either a short format of the Weekday "E" (if the message is dated within the last week)
     *  or by the Month and Day "MMMd"
     *
     *  Params:
     *  dateInMillisecond: Long - Accepts a Long value representing the current date in
     *  milliseconds from the epoch time
     *
     *  Return:
     *  Returns the formatted date as String
     **/
    fun parseDateFromMilliToDate(dateInMillisecond: Long): String {
        val calendar: Calendar = Calendar.getInstance()

        // Set calendar to 00:00 of the current day
        // then subtract a week amount of time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val lastWeekDate:Long = calendar.timeInMillis - 604800000 // Constant value for a week
        // Only added to the Calendar class
        // for api 26 and above

        calendar.timeInMillis = dateInMillisecond

        var dateFormatter: DateFormat = DateFormat.getInstanceForSkeleton(
            DateFormat.ABBR_MONTH_DAY
        )

        // Show Weekday instead of date for messages within the past week
        if (calendar.timeInMillis > lastWeekDate) {
            dateFormatter = DateFormat.getInstanceForSkeleton(
                DateFormat.ABBR_WEEKDAY
            )
        }

        val parsedDate = dateFormatter.format(calendar.time)
        return parsedDate.toString()
    }
}