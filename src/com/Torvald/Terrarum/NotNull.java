package com.Torvald.Terrarum;

/**
 * Created by minjaesong on 16-03-05.
 */
public @interface NotNull {
    String info() default "Cannot be null.";
}
