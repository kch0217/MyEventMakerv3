package hk.ust.cse.comp4521.eventmaker.User;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import java.util.HashMap;
import java.util.Map;

import hk.ust.cse.comp4521.eventmaker.Event.Event;
import hk.ust.cse.comp4521.eventmaker.Event.Event_T;
import hk.ust.cse.comp4521.eventmaker.PostEvent.EventMenu;
import hk.ust.cse.comp4521.eventmaker.Relationship.Relahelper;
import hk.ust.cse.comp4521.eventmaker.Relationship.Relationship;

/**
 * Created by Ken on 7/4/2015.
 */
public class UserModel {

    private static String TAG = "UserModel";
    private Context context;
    private static UserModel usermodel = new UserModel();
    private SharedPreferences prefs;

    public SharedPreferences getSharedPreferences(){
        return prefs;
    }

    private UserModel(){

    }

    public static UserModel getUserModel(){
        return usermodel;
    }



    public void setContext(Context myContext)
    {
        this.context = myContext;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Map<String, Object> getAllInfo(){ // retrieve user data from the device

        Map<String, Object> allInfo = new HashMap<>();
        allInfo.put("Name", prefs.getString("Name", ""));
        allInfo.put("Age", prefs.getInt("Age", -1));
        allInfo.put("Gender", prefs.getString("Gender", ""));
        allInfo.put("Interest", prefs.getString("Interest", ""));
        allInfo.put("Interest2", prefs.getString("Interest2", ""));
        allInfo.put("Phone", prefs.getString("Phone", ""));
        allInfo.put("NamePrivacy", prefs.getString("NamePrivacy",""));
        allInfo.put("AgePrivacy", prefs.getString("AgePrivacy",""));
        allInfo.put("GenderPrivacy", prefs.getString("GenderPrivacy",""));


        return allInfo;
    }

    public String getPhoneNumber(){ // get phone number from the device
        TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Log.i(null, "TEL Number is " + tel.getLine1Number());
        return tel.getLine1Number();
    }

    public String getPhoneNumberFromSP(){ // get phone number from shared preference
        return prefs.getString("Phone", "0000");
    }

    public void saveAllInfo(Map<String, Object> data, boolean modify){ //save user data to device

        String _id = null;
        if (modify){ //if it is to modify, update the records on the server
            UserServer myserver = new UserServer();
            UserInfo userInfo = myserver.UserInfoArrayList.get(myserver.calcID(prefs.getString("Phone", null)));

            if (userInfo != null) {
                _id = userInfo._id;

            }
            else
                modify = false;
        }

        SharedPreferences.Editor prefed = prefs.edit(); //update the shared preference
        prefed.putString("Name", (String)data.get("Name"));
        prefed.putInt("Age", (Integer) data.get("Age"));
        prefed.putString("Gender", (String) data.get("Gender"));
        prefed.putString("Interest", (String) data.get("Interest"));
        prefed.putString("Interest2", (String) data.get("Interest2"));
        prefed.putString("Phone", (String) data.get("Phone"));
        prefed.putString("NamePrivacy", (String) data.get("NamePrivacy"));
        prefed.putString("AgePrivacy", (String) data.get("AgePrivacy"));
        prefed.putString("GenderPrivacy", (String) data.get("GenderPrivacy"));
        prefed.commit();

        UserInfo2 newInfo = new UserInfo2();
        newInfo.Name = (String) data.get("Name");
        newInfo.Age = (Integer) data.get("Age");
        newInfo.Gender =  (String) data.get("Gender");
        newInfo.Interest = (String) data.get("Interest");
        newInfo.Interest2 =  (String) data.get("Interest2");
        newInfo.Phone = (String) data.get("Phone");
        newInfo.NamePrivacy = (String) data.get("NamePrivacy");
        newInfo.AgePrivacy = (String) data.get("AgePrivacy");
        newInfo.GenderPrivacy = (String) data.get("GenderPrivacy");
        UserServer myserver = new UserServer();
        if (modify){ //if modify, update
            Log.i(TAG,"Modifying");
            newInfo._id = _id;
            myserver.updateUser(newInfo);
        }
        else { //if it is a new record, insert it
            Log.i(TAG, "Adding");
            myserver.addAUser(newInfo);
        }


    }

    public String getInterest(){
        String interest = prefs.getString("Interest", "");
        Log.i(null, "Getting Interest: "+interest);
        return interest;
    }

// save the setting of enabling the notification of passive searching
//    public void saveSetting(boolean allowed){
//        SharedPreferences.Editor prefed = prefs.edit();
//        prefed.putBoolean("allowPassiveSearching", allowed);
//        prefed.commit();
//    }

    // load the setting of enabling the notification of passive searching
//    public Map<String, Object> getSetting(){
//        Map<String, Object> data= new HashMap<>();
//        data.put("allowPassiveSearching", prefs.getBoolean("allowPassiveSearching", false));
//        return data;
//    }


    public void wipeAlldata(){ //clear all data inside the device and the server (only the owner)
        UserServer myServer = new UserServer();
        myServer.deleteUser(prefs.getString("Phone", null));
        SharedPreferences.Editor prefed =prefs.edit();
        prefed.clear();
        //delete the corresponding event and relationship
        Event_T evhelper=new Event_T();
        Relahelper relhelper=new Relahelper();
        boolean flag=false;
        boolean admin=false;
        String roomtemp;
        Log.i("UserModel", "Downloading events and relationship");
        Object lock = new Object();
        Event_T eventT = new Event_T();
        Event_T.lock = lock;
        Event_T.locker = true;
        Event_T.test = null;
        eventT.getAllEvent();
        while (Event_T.test ==null){
            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i("UserModel", "Finish downloading events");
        Relahelper helper = new Relahelper();
        Relahelper.locker = true;
        Relahelper.lock= lock;
        Relahelper.relas = null;
        helper.getAllRelationship();
        while(Relahelper.relas ==null){
            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i("UserModel", "Passing through wait");
        for(Event evt: Event_T.test){
            if(evt._ownerid.equals(UserServer.returnInfo._id)){
                evhelper.deleteEvent(evt._id);
                flag=true;
                admin=true;
                for(Relationship rel: Relahelper.relas){
                    if(rel.roomId.equals(evt._id)){
                        relhelper.deleteRelationship(rel._id);

                    }
                }
                break;
            }

        }
        if(!admin) {
            for (Relationship rel : Relahelper.relas) {
                if (rel.userId.equals(UserServer.returnInfo._id)) {
                    roomtemp=rel.roomId;
                    relhelper.deleteRelationship(rel._id);
                    if (flag == false) {
                        //use pubnub to issue the signals that someone has left
                        Pubnub pubnub = new Pubnub("pub-c-f7c0ad94-cce2-49a3-abfb-0f414b2f8dc8", "sub-c-462fbb70-ff91-11e4-aa11-02ee2ddab7fe");
                        try {
                            pubnub.subscribe(roomtemp, new Callback(){
                                public void successCallback(String channel, final Object message) {

                                }

                                public void errorCallback(String channel, PubnubError error) {
                                    System.out.println(error.getErrorString());
                                }
                            });
                        } catch (PubnubException e) {
                            e.printStackTrace();
                        }
                        pubnub.publish(roomtemp, "type:leave+id:"+UserServer.returnInfo._id+"Name:"+UserServer.returnInfo.Name, new Callback() {});
                        pubnub.unsubscribe(roomtemp);

                    }

                }
            }
        }
        prefed.commit();
    }

    public void saveEventId(String id){
        //save event id for reconnection
        SharedPreferences.Editor pref=prefs.edit();
        pref.putString("Event", id);
        pref.commit();
    }

    public void deleteEventId(){
        //delete event id
        SharedPreferences.Editor pedit=prefs.edit();
        pedit.remove("Event");
        pedit.commit();
    }
}
