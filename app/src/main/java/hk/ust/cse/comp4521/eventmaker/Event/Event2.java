package hk.ust.cse.comp4521.eventmaker.Event;

import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Created by User on 5/4/2015.
 */
public class Event2 extends Event {
    transient String _id;
    transient String _ownerid;

    public Event2(){
        longitude=0.0;
        latitude=0.0;
        numOfPart=0;
        interest="";
        _id="";
        _ownerid="";
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
        currentTimestamp = new java.sql.Timestamp(now.getTime());
        starting="";
        ending="";
    }

    public Event2(double lat,double lon,int num,String interest, Timestamp cur, String start,String end){
        _id="";
        _ownerid="";
        longitude=lon;
        latitude=lat;
        numOfPart=num;
        this.interest=interest;
        currentTimestamp=cur;
        starting=start;
        ending=end;
    }

    public Event2(Event2 evt) {
        super(evt);
    }
}
