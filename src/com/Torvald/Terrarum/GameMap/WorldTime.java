package com.Torvald.Terrarum.GameMap;

/**
 * Created by minjaesong on 16-01-24.
 */
public class WorldTime {

    public int seconds = 0;
    public int minutes = 0;
    public int hours = 0;

    public int daysCount = 0; //NOT a calendar day

    public int days = 1;
    public int months = 1;
    public int years = 1;

    public int weeks = 1;
    public int dayOfWeek = 0; //0: Mondag-The first day of weekday

    public static final int DAY_LENGTH = 79200; //must be the multiple of 3600
    public int timeDelta = 1;

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

    public int elapsedSeconds(){
        return (3600 * hours + 60 * minutes + seconds) % DAY_LENGTH;
    }

    public long totalSeconds(){
        return (long)(DAY_LENGTH) * daysCount + 3600 * hours + 60 * minutes + seconds;
    }

    public boolean isLeapYear(){
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

    public void setTime(int t){
        days += t / DAY_LENGTH;
        hours = t / 3600;
        minutes = (t - 3600 * hours) / 60;
        seconds = t - minutes * 60;
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
}