package com.throttling.ratelimiter.util;

import com.throttling.ratelimiter.constant.Message;
import com.throttling.ratelimiter.enums.TimeUnit;

public class TimeUnitConversionUtil {

    private static final long MILLIS_IN_SECOND = 1000;
    private static final long MILLIS_IN_MINUTE = 60 * MILLIS_IN_SECOND;
    private static final long MILLIS_IN_HOUR = 60 * MILLIS_IN_MINUTE;
    private static final long MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR;
    private static final long MILLIS_IN_WEEK = 7 * MILLIS_IN_DAY;
    private static final long MILLIS_IN_MONTH = 30 * MILLIS_IN_DAY;


    public static double convert(long milliseconds, TimeUnit timeUnit) {
        if (timeUnit == null) {
            throw new IllegalArgumentException(Message.INVALID_TIME_UNIT);
        }

        switch (timeUnit) {
            case SEC:
                return milliseconds / (double) MILLIS_IN_SECOND;

            case MIN:
                return milliseconds / (double) MILLIS_IN_MINUTE;

            case HOUR:
                return milliseconds / (double) MILLIS_IN_HOUR;
                
            case DAY:
            	return milliseconds / (double) MILLIS_IN_DAY;

            case WEEK:
                return milliseconds / (double) MILLIS_IN_WEEK;

            case MONTH:
                return milliseconds / (double) MILLIS_IN_MONTH;

            default:
                throw new IllegalArgumentException(Message.INVALID_TIME_UNIT);
        }
    }
}
