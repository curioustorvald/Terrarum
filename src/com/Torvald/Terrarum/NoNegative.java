package com.Torvald.Terrarum;

/**
 * Created by minjaesong on 16-03-05.
 */
public @interface NoNegative {
    String info() default "Only positive number is acceptable.";
}
