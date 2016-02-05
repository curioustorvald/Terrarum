package com.Torvald.Terrarum;

/**
 * Created by minjaesong on 16-01-24.
 */
public class WorldTime {

    public static int seconds = 0;
    public static int minutes = 0;
    public static int hours = 0;

    public static int daysCount = 0; //NOT a calendar day

    public static int days = 1;
    public static int months = 1;
    public static int years = 1;

    public static int weeks = 1;
    public static int dayOfWeek = 0; //0: Mondag-The first day of weekday

    public static final int DAY_LENGTH = 79200; //must be the multiple of 3600
    public static int timeDelta = 1;

    public static final String[] DAYNAMES = { //daynames are taken from Nynorsk (å -> o)
            "Mondag"
            ,"Tysdag"
            ,"Midtedag" //From Islenska Miðvikudagur
            ,"Torsdag"
            ,"Fredag"
            ,"Laurdag"
            ,"Sundag"
            ,"Verdag" //From Norsk word 'verd'
    };
    public static final String[] DAYNAMES_SHORT = {
            "Mon"
            ,"Tys"
            ,"Mid"
            ,"Tor"
            ,"Fre"
            ,"Lau"
            ,"Sun"
            ,"Ver"
    };

    /**
     * Note: Target FPS must be 60.
     */
    public static void update(){
        //time
        seconds += timeDelta;

        if (seconds >= 60){
            seconds = 0;
            minutes++;
        }

        if (minutes >= 60){
            minutes = 0;
            hours++;
        }

        if (hours >= DAY_LENGTH/3600){
            hours = 0;
            days++;
            daysCount++;
            dayOfWeek++;
        }

        //calendar (the world calendar)
        if (dayOfWeek == 7){
            dayOfWeek = 0;
        }
        if ((months == 12 || (months == 7 && isLeapYear())) && days == 31){
            dayOfWeek = 7;
        }

        if ((months == 12 || (months == 7 && isLeapYear())) && days == 32){
            days = 1;
            months = 1;
            years++;
        }
        else if ((months == 1 || months == 4 || months == 7 || months == 10) && days > 31){
            days = 0;
            months++;
        }
        else if (days > 30){
            days = 0;
            months++;
        }

        if (months > 12){
            months = 1;
            years++;
        }
    }

    public static int elapsedSeconds(){
        return (3600 * hours + 60 * minutes + seconds) % DAY_LENGTH;
    }

    public static long totalSeconds(){
        return (long)(DAY_LENGTH) * daysCount + 3600 * hours + 60 * minutes + seconds;
    }

    public static boolean isLeapYear(){
        boolean ret = false;

        if (years % 4 == 0){
            ret = true;

            if (years % 100 == 0){
                ret = false;

                if (years % 400 == 0){
                    ret = true;
                }
            }
        }

        return ret;
    }

    public static void setTime(int t){
        days += t / DAY_LENGTH;
        hours = t / 3600;
        minutes = (t - 3600 * hours) / 60;
        seconds = t - minutes * 60;
    }

    public static void addTime(int t){
        setTime(elapsedSeconds() + t);
    }

    public static void setTimeDelta(int d){
        timeDelta = (d == 0) ? 1 : d;
    }

    public static String getDayName(){
        return DAYNAMES[dayOfWeek];
    }
}