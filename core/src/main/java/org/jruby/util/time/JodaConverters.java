package org.jruby.util.time;

/**
 * The JodaConverters class contains static methods for org.joda.time.* and java.time.* conversion.
 *
 * Borrowed from 'timeywimey' library at https://github.com/meetup/timeywimey (MIT license)
 *
 * @author Sak Lee
 * @version 0.1.0
 * @since 2016-05-16
 */
public class JodaConverters {
	private static int milliToNanoConst = 1000000;

	/* ************************ *
	 * Classes without timezone *
	 * ************************ */

	/**
	 * Converts Joda-Time LocalDate to Java 8 equivalent.
	 *
	 * @param localDate Joda-Time LocalDate
	 * @return Java 8 LocalDate
	 */
	public static java.time.LocalDate jodaToJavaLocalDate( org.joda.time.LocalDate localDate ) {
		return java.time.LocalDate.of( localDate.getYear(), localDate.getMonthOfYear(), localDate.getDayOfMonth() );
	}

	/**
	 * Converts Java 8 LocalDate to Joda-Time equivalent.
	 *
	 * @param localDate Java 8 LocalDate
	 * @return Joda-Time LocalDate
	 */
	public static org.joda.time.LocalDate javaToJodaLocalDate( java.time.LocalDate localDate ) {
		return new org.joda.time.LocalDate( localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth() );
	}

	/**
	 * Converts Joda-Time LocalTime to Java 8 equivalent.
	 *
	 * @param localTime Joda-Time LocalTime
	 * @return Java 8 LocalTime
	 */
	public static java.time.LocalTime jodaToJavaLocalTime( org.joda.time.LocalTime localTime ) {
		return java.time.LocalTime.of(
				localTime.getHourOfDay(),
				localTime.getMinuteOfHour(),
				localTime.getSecondOfMinute(),
				localTime.getMillisOfSecond() * milliToNanoConst );
	}

	/**
	 * Converts Java 8 LocalTime to Joda-Time equivalent.
	 * <p>
	 * This is a potentially lossy operation. Any time info below millis unit are deleted.
	 *
	 * @param localTime Java 8 LocalTime
	 * @return Joda-Time LocalTime
	 */
	public static org.joda.time.LocalTime javaToJodaLocalTime( java.time.LocalTime localTime ) {
		return new org.joda.time.LocalTime(
				localTime.getHour(),
				localTime.getMinute(),
				localTime.getSecond(),
				localTime.getNano() / milliToNanoConst );
	}

	/**
	 * Converts Joda-Time LocalDateTime to Java 8 equivalent.
	 *
	 * @param localDateTime Joda-Time LocalDateTime
	 * @return Java 8 LocalDateTime
	 */
	public static java.time.LocalDateTime jodaToJavaLocalDateTime( org.joda.time.LocalDateTime localDateTime ) {
		return java.time.LocalDateTime.of(
				localDateTime.getYear(),
				localDateTime.getMonthOfYear(),
				localDateTime.getDayOfMonth(),
				localDateTime.getHourOfDay(),
				localDateTime.getMinuteOfHour(),
				localDateTime.getSecondOfMinute(),
				localDateTime.getMillisOfSecond() * milliToNanoConst );
	}

	/**
	 * Converts Java 8 LocalDateTime to Joda-Time equivalent.
	 * <p>
	 * This is a potentially lossy operation. Any time info below millis unit are lost.
	 *
	 * @param localDateTime Java 8 LocalDateTime
	 * @return Joda-Time LocalDateTime
	 */
	public static org.joda.time.LocalDateTime javaToJodaLocalDateTime( java.time.LocalDateTime localDateTime ) {
		return new org.joda.time.LocalDateTime(
				localDateTime.getYear(),
				localDateTime.getMonthValue(),
				localDateTime.getDayOfMonth(),
				localDateTime.getHour(),
				localDateTime.getMinute(),
				localDateTime.getSecond(),
				localDateTime.getNano() / milliToNanoConst );
	}

	/**
	 * Converts Joda-Time MonthDay to Java 8 equivalent.
	 *
	 * @param monthDay Joda-Time MonthDay
	 * @return Java 8 MonthDay
	 */
	public static java.time.MonthDay jodaToJavaMonthDay( org.joda.time.MonthDay monthDay ) {
		return java.time.MonthDay.of( monthDay.getMonthOfYear(), monthDay.getDayOfMonth() );
	}

	/**
	 * Converts Java 8 MonthDay to Joda-Time equivalent.
	 *
	 * @param monthDay Java 8 MonthDay
	 * @return Joda-Time MonthDay
	 */
	public static org.joda.time.MonthDay javaToJodaMonthDay( java.time.MonthDay monthDay ) {
		return new org.joda.time.MonthDay( monthDay.getMonthValue(), monthDay.getDayOfMonth() );
	}

	/**
	 * Converts Joda-Time YearMonth to Java 8 equivalent.
	 *
	 * @param yearMonth Joda-Time YearMonth
	 * @return Java 8 YearMonth
	 */
	public static java.time.YearMonth jodaToJavaYearMonth( org.joda.time.YearMonth yearMonth ) {
		return java.time.YearMonth.of( yearMonth.getYear(), yearMonth.getMonthOfYear() );
	}

	/**
	 * Converts Java 8 YearMonth to Joda-Time equivalent.
	 *
	 * @param yearMonth Java 8 YearMonth
	 * @return Joda-Time YearMonth
	 */
	public static org.joda.time.YearMonth javaToJodaYearMonth( java.time.YearMonth yearMonth ) {
		return new org.joda.time.YearMonth( yearMonth.getYear(), yearMonth.getMonthValue() );
	}


	/* ****************************** *
	 * Class with timezone or instant *
	 * ****************************** */

	/**
	 * Converts Joda-Time DateTime to Java 8 equivalent.
	 *
	 * @param dateTime Joda-Time DateTime
	 * @return Java 8 ZonedDateTime
	 */
	public static java.time.ZonedDateTime jodaToJavaDateTime( org.joda.time.DateTime dateTime ) {
		return jodaToJavaInstant( dateTime.toInstant() ).atZone( jodaToJavaTimeZone( dateTime.getZone() ) );
	}

	/**
	 * Converts Java 8 ZonedDateTime to Joda-Time equivalent.
	 *
	 * @param dateTime Java 8 ZonedDateTime
	 * @return Joda-Time DateTime
	 */
	public static org.joda.time.DateTime javaToJodaDateTime( java.time.ZonedDateTime dateTime ) {
		return new org.joda.time.DateTime( javaToJodaInstant( dateTime.toInstant() ), javaToJodaTimeZone( dateTime.getZone() ) );
	}

	/**
	 * Converts Joda-Time Instant to Java 8 equivalent.
	 *
	 * @param instant Joda-Time Instant
	 * @return Java 8 Instant
	 */
	public static java.time.Instant jodaToJavaInstant( org.joda.time.Instant instant ) {
		return java.time.Instant.ofEpochMilli( instant.getMillis() );
	}

	/**
	 * Converts Java 8 Instant to Joda-Time equivalent.
	 *
	 * @param instant Java 8 Instant
	 * @return Joda-Time Instant
	 */
	public static org.joda.time.Instant javaToJodaInstant( java.time.Instant instant ) {
		return new org.joda.time.Instant( instant.toEpochMilli() );
	}

	/**
	 * Converts Joda-Time DateTimeZone to Java 8 equivalent.
	 *
	 * @param timeZone Joda-Time DateTimeZone
	 * @return Java 8 ZoneId
	 */
	public static java.time.ZoneId jodaToJavaTimeZone( org.joda.time.DateTimeZone timeZone ) {
		return java.time.ZoneId.of( timeZone.getID() );
	}

	/**
	 * Converts Java 8 ZoneId to Joda-Time equivalent.
	 *
	 * @param timeZone Java 8 ZoneId
	 * @return Joda-Time DateTimeZone
	 */
	public static org.joda.time.DateTimeZone javaToJodaTimeZone( java.time.ZoneId timeZone ) {
		return org.joda.time.DateTimeZone.forID( timeZone.getId() );
	}


	/* ************** *
	 * Amount of Time *
	 * ************** */

	/**
	 * Converts Joda-Time Duration to Java 8 equivalent.
	 *
	 * @param duration Joda-Time Duration
	 * @return Java 8 Duration
	 */
	public static java.time.Duration jodaToJavaDuration( org.joda.time.Duration duration ) {
		return java.time.Duration.ofMillis( duration.getMillis() );
	}

	/**
	 * Converts Java 8 Duration to Joda-Time equivalent.
	 *
	 * @param duration Java 8 Duration
	 * @return Joda-Time Duration
	 */
	public static org.joda.time.Duration javaToJodaDuration( java.time.Duration duration ) {
		return org.joda.time.Duration.millis( duration.toMillis() );
	}

	/**
	 * Converts Joda-Time Period to Java 8 equivalent.
	 * <p>
	 * This is a potentially lossy operation. Any time info below day unit are lost.
	 *
	 * @param period Joda-Time Period
	 * @return Java 8 Period
	 */
	public static java.time.Period jodaToJavaPeriod( org.joda.time.Period period ) {
		return java.time.Period.of( period.getYears(), period.getMonths(), period.getDays() );
	}

	/**
	 * Converts Java 8 Period to Joda-Time equivalent.
	 *
	 * @param period Java 8 Period
	 * @return Joda-Time Period
	 */
	public static org.joda.time.Period javaToJodaPeriod( java.time.Period period ) {
		return new org.joda.time.Period( period.getYears(), period.getMonths(), 0, period.getDays(), 0, 0, 0, 0 );
	}
}
