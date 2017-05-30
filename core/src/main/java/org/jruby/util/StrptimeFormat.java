package org.jruby.util;

/**
 * This class is ported from RubyDateFormatter.Format in JRuby 9.1.5.0.
 * @see https://github.com/jruby/jruby/blob/036ce39f0476d4bd718e23e64caff36bb50b8dbc/core/src/main/java/org/jruby/util/RubyDateFormatter.java
 */
enum StrptimeFormat {
    FORMAT_STRING, // raw string, no formatting
    FORMAT_SPECIAL, // composition of other formats

    FORMAT_WEEK_LONG, // %A
    FORMAT_WEEK_SHORT, // %a
    FORMAT_MONTH_LONG, // %B
    FORMAT_MONTH_SHORT, // %b, %h
    FORMAT_CENTURY, // %C
    FORMAT_DAY, // %d
    FORMAT_DAY_S, // %e
    FORMAT_WEEKYEAR, // %G
    FORMAT_WEEKYEAR_SHORT, // %g
    FORMAT_HOUR, // %H
    FORMAT_HOUR_M, // %I
    FORMAT_DAY_YEAR, // %j
    FORMAT_HOUR_BLANK, // %k
    FORMAT_MILLISEC, // %L
    FORMAT_HOUR_S, // %l
    FORMAT_MINUTES, // %M
    FORMAT_MONTH, // %m
    FORMAT_NANOSEC, // %N
    FORMAT_MERIDIAN_LOWER_CASE, // %P
    FORMAT_MERIDIAN, // %p
    FORMAT_MICROSEC_EPOCH, // %Q Only for Date/DateTime from here
    FORMAT_SECONDS, // %S
    FORMAT_EPOCH, // %s
    FORMAT_WEEK_YEAR_S, // %U
    FORMAT_DAY_WEEK2, // %u
    FORMAT_WEEK_WEEKYEAR, // %V
    FORMAT_WEEK_YEAR_M, // %W
    FORMAT_DAY_WEEK, // %w
    FORMAT_YEAR_LONG, // %Y
    FORMAT_YEAR_SHORT, // %y

    FORMAT_COLON_ZONE_OFF, // %z, %:z, %::z, %:::z must be given number of colons as data

    FORMAT_ZONE_ID; // %Z Change between Time and Date
}
