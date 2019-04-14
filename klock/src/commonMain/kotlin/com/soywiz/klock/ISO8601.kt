package com.soywiz.klock

import com.soywiz.klock.internal.*
import kotlin.math.absoluteValue

// https://en.wikipedia.org/wiki/ISO_8601
object ISO8601 {
    class IsoIntervalFormat(val format: String) : DateTimeSpanFormat {
        override fun format(dd: DateTimeSpan): String {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun tryParse(str: String, doThrow: Boolean): DateTimeSpan? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class BaseIsoTimeFormat(val format: String) : TimeFormat {
        override fun format(dd: TimeSpan): String {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun tryParse(str: String, doThrow: Boolean): TimeSpan? {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class BaseIsoDateTimeFormat(val format: String, val twoDigitBaseYear: Int = 1900) : DateFormat {
        override fun format(dd: DateTimeTz): String = buildString {
            val fmtReader = MicroStrReader(format)
            while (fmtReader.hasMore) {
                when {
                    fmtReader.tryRead("YYYYYY") -> append(dd.yearInt.absoluteValue.padded(6))
                    fmtReader.tryRead("YYYY") -> append(dd.yearInt.absoluteValue.padded(4))
                    fmtReader.tryRead("YY") -> append((dd.yearInt.absoluteValue % 100).padded(2))
                    fmtReader.tryRead("MM") -> append(dd.month1.padded(2))
                    fmtReader.tryRead("DD") -> append(dd.dayOfMonth.padded(2))
                    fmtReader.tryRead("DDD") -> append(dd.dayOfWeekInt.padded(3))
                    fmtReader.tryRead("ww") -> append(dd.weekOfYear1.padded(2))
                    fmtReader.tryRead("D") -> append(dd.dayOfWeek.index1Monday)
                    fmtReader.tryRead("±") -> append(if (dd.yearInt < 0) "-" else "+")
                    else -> append(fmtReader.readChar())
                }
            }
        }

        override fun tryParse(str: String, doThrow: Boolean): DateTimeTz? {
            return tryParse(str).also {
                if (doThrow && it == null) throw DateException("Can't parse $str with $format")
            }
        }

        private fun tryParse(str: String): DateTimeTz? {
            var sign = +1
            var year = -1
            var month = -1
            var dayOfYear = -1
            var dayOfMonth = -1
            var dayOfWeek = -1
            var weekOfYear = -1
            val reader = MicroStrReader(str)
            val fmtReader = MicroStrReader(format)
            while (fmtReader.hasMore) {
                when {
                    fmtReader.tryRead("YYYYYY") -> year = reader.tryReadInt(6) ?: return null
                    fmtReader.tryRead("YYYY") -> year = reader.tryReadInt(4) ?: return null
                    //fmtReader.tryRead("YY") -> year = twoDigitBaseYear + (reader.tryReadInt(2) ?: return null) // @TODO: Kotlin compiler BUG?
                    fmtReader.tryRead("YY") -> {
                        val base = reader.tryReadInt(2) ?: return null
                        year = twoDigitBaseYear + base
                    }
                    fmtReader.tryRead("MM") -> month = reader.tryReadInt(2) ?: return null
                    fmtReader.tryRead("DD") -> dayOfMonth = reader.tryReadInt(4) ?: return null
                    fmtReader.tryRead("DDD") -> dayOfYear = reader.tryReadInt(3) ?: return null
                    fmtReader.tryRead("ww") -> weekOfYear = reader.tryReadInt(2) ?: return null
                    fmtReader.tryRead("D") -> dayOfWeek = reader.tryReadInt(1) ?: return null
                    fmtReader.tryRead("±") -> {
                        when (reader.readChar()) {
                            '+' -> sign = +1
                            '-' -> sign = -1
                            else -> return null
                        }
                    }
                    else -> if (fmtReader.readChar() != reader.readChar()) return null
                }
            }
            if (reader.hasMore) return null

            val dateTime = when {
                dayOfYear >= 0 -> DateTime(year, 1, 1) + (dayOfYear - 1).days
                weekOfYear >= 0 -> {
                    val reference = Year(year).first(DayOfWeek.Thursday) - 3.days
                    val days = ((weekOfYear - 1) * 7 + (dayOfWeek - 1))
                    reference + days.days
                }
                else -> DateTime(year, month, dayOfMonth)
            }
            return dateTime.local
        }

        fun withTwoDigitBaseYear(twoDigitBaseYear: Int = 1900) = BaseIsoDateTimeFormat(format, twoDigitBaseYear)
    }

    class IsoTimeFormat(val basicFormat: String?, val extendedFormat: String?) : TimeFormat {
        val basic = BaseIsoTimeFormat(basicFormat ?: extendedFormat ?: TODO())
        val extended = BaseIsoTimeFormat(extendedFormat ?: basicFormat ?: TODO())

        override fun format(dd: TimeSpan): String = extended.format(dd)
        override fun tryParse(str: String, doThrow: Boolean): TimeSpan? =
            basic.tryParse(str, false) ?: extended.tryParse(str, false)
            ?: (if (doThrow) throw DateException("Invalid format $str") else null)
    }

    class IsoDateTimeFormat(val basicFormat: String?, val extendedFormat: String?) : DateFormat {
        val basic = BaseIsoDateTimeFormat(basicFormat ?: extendedFormat ?: TODO())
        val extended = BaseIsoDateTimeFormat(extendedFormat ?: basicFormat ?: TODO())

        override fun format(dd: DateTimeTz): String = extended.format(dd)
        override fun tryParse(str: String, doThrow: Boolean): DateTimeTz? =
            basic.tryParse(str, false) ?: extended.tryParse(str, false)
            ?: (if (doThrow) throw DateException("Invalid format $str") else null)
    }

    // Date Calendar Variants
    val DATE_CALENDAR_COMPLETE = IsoDateTimeFormat("YYYYMMDD", "YYYY-MM-DD")
    val DATE_CALENDAR_REDUCED0 = IsoDateTimeFormat(null, "YYYY-MM")
    val DATE_CALENDAR_REDUCED1 = IsoDateTimeFormat("YYYY", null)
    val DATE_CALENDAR_REDUCED2 = IsoDateTimeFormat("YY", null)
    val DATE_CALENDAR_EXPANDED0 = IsoDateTimeFormat("±YYYYYYMMDD", "±YYYYYY-MM-DD")
    val DATE_CALENDAR_EXPANDED1 = IsoDateTimeFormat("±YYYYYYMM", "±YYYYYY-MM")
    val DATE_CALENDAR_EXPANDED2 = IsoDateTimeFormat("±YYYYYY", null)
    val DATE_CALENDAR_EXPANDED3 = IsoDateTimeFormat("±YYY", null)

    // Date Ordinal Variants
    val DATE_ORDINAL_COMPLETE = IsoDateTimeFormat("YYYYDDD", "YYYY-DDD")
    val DATE_ORDINAL_EXPANDED = IsoDateTimeFormat("±YYYYYYDDD", "±YYYYYY-DDD")

    // Date Week Variants
    val DATE_WEEK_COMPLETE = IsoDateTimeFormat("YYYYWwwD", "YYYY-Www-D")
    val DATE_WEEK_REDUCED = IsoDateTimeFormat("YYYYWww", "YYYY-Www")
    val DATE_WEEK_EXPANDED0 = IsoDateTimeFormat("±YYYYYYWwwD", "±YYYYYY-Www-D")
    val DATE_WEEK_EXPANDED1 = IsoDateTimeFormat("±YYYYYYWww", "±YYYYYY-Www")

    val DATE_ALL by lazy {
        listOf(
            DATE_CALENDAR_COMPLETE, DATE_CALENDAR_REDUCED0, DATE_CALENDAR_REDUCED1, DATE_CALENDAR_REDUCED2,
            DATE_CALENDAR_EXPANDED0, DATE_CALENDAR_EXPANDED1, DATE_CALENDAR_EXPANDED2, DATE_CALENDAR_EXPANDED3,
            DATE_ORDINAL_COMPLETE, DATE_ORDINAL_EXPANDED,
            DATE_WEEK_COMPLETE, DATE_WEEK_REDUCED, DATE_WEEK_EXPANDED0, DATE_WEEK_EXPANDED1
        )
    }

    // Time Variants
    val TIME_LOCAL_COMPLETE = IsoTimeFormat("hhmmss", "hh:mm:ss")
    val TIME_LOCAL_REDUCED0 = IsoTimeFormat("hhmm", "hh:mm")
    val TIME_LOCAL_REDUCED1 = IsoTimeFormat("hh", null)
    val TIME_LOCAL_FRACTION0 = IsoTimeFormat("hhmmss,ss", "hh:mm:ss,ss")
    val TIME_LOCAL_FRACTION1 = IsoTimeFormat("hhmm,mm", "hh:mm,mm")
    val TIME_LOCAL_FRACTION2 = IsoTimeFormat("hh,hh", null)

    // Time UTC Variants
    val TIME_UTC_COMPLETE = IsoTimeFormat("hhmmssZ", "hh:mm:ssZ")
    val TIME_UTC_REDUCED0 = IsoTimeFormat("hhmmZ", "hh:mmZ")
    val TIME_UTC_REDUCED1 = IsoTimeFormat("hhZ", null)
    val TIME_UTC_FRACTION0 = IsoTimeFormat("hhmmss,ssZ", "hh:mm:ss,ssZ")
    val TIME_UTC_FRACTION1 = IsoTimeFormat("hhmm,mmZ", "hh:mm,mmZ")
    val TIME_UTC_FRACTION2 = IsoTimeFormat("hh,hhZ", null)

    // Time Relative Variants
    val TIME_RELATIVE0 = IsoTimeFormat("±hhmm", "±hh:mm")
    val TIME_RELATIVE1 = IsoTimeFormat("±hh", null)

    val TIME_ALL by lazy {
        listOf(
            TIME_LOCAL_COMPLETE,
            TIME_LOCAL_REDUCED0,
            TIME_LOCAL_REDUCED1,
            TIME_LOCAL_FRACTION0,
            TIME_LOCAL_FRACTION1,
            TIME_LOCAL_FRACTION2,
            TIME_UTC_COMPLETE,
            TIME_UTC_REDUCED0,
            TIME_UTC_REDUCED1,
            TIME_UTC_FRACTION0,
            TIME_UTC_FRACTION1,
            TIME_UTC_FRACTION2,
            TIME_RELATIVE0,
            TIME_RELATIVE1
        )
    }

    // Date + Time Variants
    val DATETIME_COMPLETE = IsoDateTimeFormat("YYYYMMDDTHHMMSS", "YYYY-MM-DDTHH:MM:SS")

    // Interval Variants
    val INTERVAL_COMPLETE0 = IsoIntervalFormat("PnnYnnMnnDTnnHnnMnnS")
    val INTERVAL_COMPLETE1 = IsoIntervalFormat("PnnYnnW")

    val INTERVAL_REDUCED0 = IsoIntervalFormat("PnnYnnMnnDTnnHnnM")
    val INTERVAL_REDUCED1 = IsoIntervalFormat("PnnYnnMnnDTnnH")
    val INTERVAL_REDUCED2 = IsoIntervalFormat("PnnYnnMnnD")
    val INTERVAL_REDUCED3 = IsoIntervalFormat("PnnYnnM")
    val INTERVAL_REDUCED4 = IsoIntervalFormat("PnnY")

    val INTERVAL_DECIMAL0 = IsoIntervalFormat("PnnYnnMnnDTnnHnnMnn,nnS")
    val INTERVAL_DECIMAL1 = IsoIntervalFormat("PnnYnnMnnDTnnHnn,nnM")
    val INTERVAL_DECIMAL2 = IsoIntervalFormat("PnnYnnMnnDTnn,nnH")
    val INTERVAL_DECIMAL3 = IsoIntervalFormat("PnnYnnMnn,nnD")
    val INTERVAL_DECIMAL4 = IsoIntervalFormat("PnnYnn,nnM")
    val INTERVAL_DECIMAL5 = IsoIntervalFormat("PnnYnn,nnW")
    val INTERVAL_DECIMAL6 = IsoIntervalFormat("PnnY")

    val INTERVAL_ZERO_OMIT0 = IsoIntervalFormat("PnnYnnDTnnHnnMnnS")
    val INTERVAL_ZERO_OMIT1 = IsoIntervalFormat("PnnYnnDTnnHnnM")
    val INTERVAL_ZERO_OMIT2 = IsoIntervalFormat("PnnYnnDTnnH")
    val INTERVAL_ZERO_OMIT3 = IsoIntervalFormat("PnnYnnD")

    val INTERVAL_ALL by lazy {
        listOf(
            INTERVAL_COMPLETE0, INTERVAL_COMPLETE1,
            INTERVAL_REDUCED0, INTERVAL_REDUCED1, INTERVAL_REDUCED2, INTERVAL_REDUCED3, INTERVAL_REDUCED4,
            INTERVAL_DECIMAL0, INTERVAL_DECIMAL1, INTERVAL_DECIMAL2, INTERVAL_DECIMAL3, INTERVAL_DECIMAL4,
            INTERVAL_DECIMAL5, INTERVAL_DECIMAL6,
            INTERVAL_ZERO_OMIT0, INTERVAL_ZERO_OMIT1, INTERVAL_ZERO_OMIT2, INTERVAL_ZERO_OMIT3
        )
    }

    // Detects and parses all the variants
    val DATE = object : DateFormat {
        override fun format(dd: DateTimeTz): String = DATE_CALENDAR_COMPLETE.format(dd)

        override fun tryParse(str: String, doThrow: Boolean): DateTimeTz? {
            DATE_ALL.fastForEach { format ->
                val result = format.extended.tryParse(str, false)
                if (result != null) return result
            }
            DATE_ALL.fastForEach { format ->
                val result = format.basic.tryParse(str, false)
                if (result != null) return result
            }
            return if (doThrow) throw DateException("Invalid format") else null
        }
    }
    val TIME = object : TimeFormat {
        override fun format(dd: TimeSpan): String = TIME_LOCAL_FRACTION0.format(dd)

        override fun tryParse(str: String, doThrow: Boolean): TimeSpan? {
            TIME_ALL.fastForEach { format ->
                val result = format.extended.tryParse(str, false)
                if (result != null) return result
            }
            TIME_ALL.fastForEach { format ->
                val result = format.basic.tryParse(str, false)
                if (result != null) return result
            }
            return if (doThrow) throw DateException("Invalid format") else null
        }
    }
    val INTERVAL = object : DateTimeSpanFormat {
        override fun format(dd: DateTimeSpan): String = INTERVAL_DECIMAL0.format(dd)

        override fun tryParse(str: String, doThrow: Boolean): DateTimeSpan? {
            INTERVAL_ALL.fastForEach { format ->
                val result = format.tryParse(str, false)
                if (result != null) return result
            }
            return if (doThrow) throw DateException("Invalid format") else null
        }
    }
}

// ISO 8601 (first week is the one after 1 containing a thursday)
fun Year.first(dayOfWeek: DayOfWeek): DateTime {
    val start = DateTime(this.year, 1, 1)
    var n = 0
    while (true) {
        val time = (start + n.days)
        if (time.dayOfWeek == dayOfWeek) return time
        n++
    }
}

val DateTime.weekOfYear0: Int
    get() {
        val firstThursday = year.first(DayOfWeek.Thursday)
        val offset = firstThursday.dayOfMonth - 3
        return (dayOfYear - offset) / 7
    }

val DateTime.weekOfYear1: Int get() = weekOfYear0 + 1
val DateTimeTz.weekOfYear0: Int get() = local.weekOfYear0
val DateTimeTz.weekOfYear1: Int get() = local.weekOfYear1
