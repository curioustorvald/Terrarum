package com.Torvald.Terrarum.GameMap;

import java.io.Serializable;

/**
 * Created by minjaesong on 16-01-24.
 */
public class WorldTime {

    private int seconds = 0;
    private int minutes = 0;
    private int hours = 0;

    private int daysCount = 0; //NOT a calendar day

    private int days = 1;
    private int months = 1;
    private int years = 1;

    private int dayOfWeek = 0; //0: Mondag-The first day of weekday

    public static transient final int DAY_LENGTH = 79200; //must be the multiple of 3600
    private int timeDelta = 1;

    private static transient final int HOUR_SEC = 3600;
    private static transient final int MINUTE_SEC = 60;

    public final String[] DAYNAMES = { //daynames are taken from Nynorsk (å -> o)
            "Mondag"
            ,"Tysdag"
            ,"Midtedag" //From Islenska Miðvikudagur
            ,"Torsdag"
            ,"Fredag"
            ,"Laurdag"
            ,"Sundag"
            ,"Verdag" //From Norsk word 'verd'
    };
    public final String[] DAYNAMES_SHORT = {
            "Mon"
            ,"Tys"
            ,"Mid"
            ,"Tor"
            ,"Fre"
            ,"Lau"
            ,"Sun"
            ,"Ver"
    };

    public WorldTime() {
    }

    /**
     * Note: Target FPS must be 60.
     */
    public void update(){
        //time
        seconds += timeDelta;

        kickVariables();

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

    /**
     * How much time has passed today, in seconds.
     * @return
     */
    public int elapsedSeconds(){
        return (HOUR_SEC * hours + MINUTE_SEC * minutes + seconds) % DAY_LENGTH;
    }

    /**
     * How much time has passed since the beginning, in seconds.
     * @return
     */
    public long totalSeconds(){
        return (long)(DAY_LENGTH) * daysCount + HOUR_SEC * hours + MINUTE_SEC * minutes + seconds;
    }

    public boolean isLeapYear(){
        return ((years % 4 == 0) && (years % 100 != 0)) || (years % 400 == 0);
    }

    public void setTime(int t){
        days += t / DAY_LENGTH;
        hours = t / HOUR_SEC;
        minutes = (t - HOUR_SEC * hours) / MINUTE_SEC;
        seconds = t - minutes * MINUTE_SEC;
    }

    public void addTime(int t){
        setTime(elapsedSeconds() + t);
    }

    public void setTimeDelta(int d){
        timeDelta = (d == 0) ? 1 : d;
    }

    public String getDayName(){
        return DAYNAMES[dayOfWeek];
    }

    private void kickVariables() {
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
    }
}