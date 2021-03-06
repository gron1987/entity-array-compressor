package com.masyaman.datapack.annotations.serialization;

import com.masyaman.datapack.serializers.dates.DatePrecisions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.RoundingMode;

/**
 * Precision and rounding mode for Numbers or Dates. Exact meaning of value depends on actual serialization.
 * For Numbers precision usually means decimal precision which could be positive or negative. E.g. value 123.123 with
 * precision 2 will be serialized as 123.12, with precision -1 it will be serialized as 120.
 * Date serialization uses custom granularity, see {@link DatePrecisions}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Precision {
    int value();
    RoundingMode roundingMode() default RoundingMode.HALF_UP;
}
