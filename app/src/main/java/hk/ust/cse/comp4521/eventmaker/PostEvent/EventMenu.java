//COMP4521  Kwok Chung Hin   20111831   chkwokad@ust.hk
//COMP4521  Kwok Tsz Ting 20119118  ttkwok@ust.hk
//COMP4521  Li Lok Him  20103470    lhliab@ust.hk
package hk.ust.cse.comp4521.eventmaker.PostEvent;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import java.util.ArrayList;
import java.util.List;

import hk.ust.cse.comp4521.eventmaker.Constants;
import hk.ust.cse.comp4521.eventmaker.Event.Event;
import hk.ust.cse.comp4521.eventmaker.Event.Event_T;
import hk.ust.cse.comp4521.eventmaker.Event.Map;
import hk.ust.cse.comp4521.eventmaker.Helper.ActivityRefresh;
import hk.ust.cse.comp4521.eventmaker.Helper.ParticipantsReminder;
import hk.ust.cse.comp4521.eventmaker.R;
import hk.ust.cse.comp4521.eventmaker.Relationship.Relahelper;
import hk.ust.cse.comp4521.eventmaker.Relationship.Relationship;
import hk.ust.cse.comp4521.eventmaker.Relationship.Relationship2;
import hk.ust.cse.comp4521.eventmaker.User.UserInfo;
import hk.ust.cse.comp4521.eventmaker.User.UserModel;
import hk.ust.cse.comp4521.eventmaker.User.UserServer;

/**
 * Created by LiLokHim on 14/5/15.
 */
public class EventMenu extends Activity {
    private boolean isOwner=false;
    private String TAG = "eventMenu";
    private Event event = null;
    private String event_id;
    private int check_interval=30000;
    private String OwnerID;
    /*********UI***************/

    private ScrollView SV;
    private BroadcastReceiver mReceiver;
    private Intent ServiceParticipants;
    private Intent refresh;
    private ArrayList<String> name_array=new ArrayList<String>();
    private ArrayList<String> id_array=new ArrayList<String>();

    private Pubnub pubnub = new Pubnub("pub-c-f7c0ad94-cce2-49a3-abfb-0f414b2f8dc8", "sub-c-462fbb70-ff91-11e4-aa11-02ee2ddab7fe");
    private ListView LV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        Intent i = new Intent(Constants.signaling).putExtra("Signal", Constants.allserviceStopped);
        this.sendBroadcast(i);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
    //UI init
        LV=(ListView)findViewById(R.id.listView);
        SV=(ScrollView)findViewById(R.id.SV);
        final TextView text= (TextView) this.findViewById(R.id.chatBox);
        final EditText send= (EditText) this.findViewById(R.id.TextToSend);
        Button sendButton=(Button) this.findViewById(R.id.send);


        //get the event id and set own if he/she is owner
        Intent intent = getIntent();
        event_id = intent.getStringExtra(Constants.eventId);
        setOwnerId();

        //to allow a scroll view inside another scoll view scoll
        SV.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // Disallow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;

                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }

                // Handle ListView touch events.
                v.onTouchEvent(event);
                return true;
            }
        });

        try {
            //subscribe pubnub channel using event_id as channel key

            pubnub.subscribe(event_id, new Callback() {

                //set up call back
                public void successCallback(String channel, final Object message) {
                    //deal with the case when a new user enther the room
                    if(message.toString().contains("type:enter"))
                    {
                        String operation=message.toString().replace("type:enter+id:", "");
                        String[] idandname=operation.split("Name:");
                        System.out.println("1:"+idandname[0]);
                        System.out.println("2:" + idandname[1]);
                        if(!name_array.contains(idandname[1]))
                        {


                            name_array.add(idandname[1]);
                        id_array.add(idandname[0]);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(EventMenu.this,name_array.get(name_array.size()-1)+" added",Toast.LENGTH_SHORT);
                                String[] array = name_array.toArray(new String[name_array.size()]);

                                ListAdapter LA = new ArrayAdapter<String>(EventMenu.this, android.R.layout.simple_list_item_1, array);
                                LV.setAdapter(LA);


                            }
                        });}
                    }

                    //deal with the case when a user post a chat on chat room
                    else if(message.toString().contains("type:post"))
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                String operation=message.toString().replace("type:post","");
                                System.out.println("3:"+operation);
                                String currentText= (String) text.getText();

                                String update=currentText+"///"+operation;
                                String str = update.toString().replace("///", "\n");
                                text.setText(str);
                                SV.scrollTo(0, SV.getBottom());


                            }
                        });
                    }

                    //deal with the case that a user leave
                    else if(message.toString().contains("type:leave")){
                        String operation=message.toString().replace("type:leave+id:", "");
                        String[] idandname=operation.split("Name:");
                        System.out.println("1:"+idandname[0]);
                        System.out.println("2:" + idandname[1]);

                        if(id_array.contains(idandname[0]))
                        {
                            for(int i=0;i<id_array.size();i++)
                            {

                                if(id_array.get(i).equals(idandname[0]))
                                {
                                    id_array.remove(i);
                                    name_array.remove(i);
                                }
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    String[] array = name_array.toArray(new String[name_array.size()]);

                                    ListAdapter LA = new ArrayAdapter<String>(EventMenu.this, android.R.layout.simple_list_item_1, array);
                                    LV.setAdapter(LA);


                                }
                            });

                        }
                    }


                }
                // deal with the case that error occurs
                public void errorCallback(String channel, PubnubError error) {
                    System.out.println(error.getErrorString());
                }
            });
        } catch (PubnubException e) {
            e.printStackTrace();
        }


//
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Editable textToSend = send.getText();
                String current_text = textToSend.toString();
                String temp;
                if(UserServer.returnInfo.NamePrivacy.equals("Uncheck")){
                    temp="type:post"+UserServer.returnInfo.Name+": "+current_text;
                send.setText("");}
                else
                {
                    temp="type:post"+UserServer.returnInfo.Phone+": "+current_text;
                    send.setText("");
                }
                String current_text2=temp.replace("\n","///");

                pubnub.publish(event_id, current_text2, new Callback() {
                });

            }
        });

        /********************chat**********/


        UserModel usm=UserModel.getUserModel();
        usm.saveEventId(event_id);
        Log.i(TAG, "Going to start service");

        refresh = new Intent(EventMenu.this, ActivityRefresh.class);
        refresh.putExtra(Constants.eventId, event_id);
//        serverConnection = new ServiceConnection() {
//            @Override
//            public void onServiceConnected(ComponentName name, IBinder service) {
//
//            }
//
//            @Override
//            public void onServiceDisconnected(ComponentName name) {
//
//            }
//        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                startService(refresh);
            }
        }).start();



        ServiceParticipants = new Intent(EventMenu.this, ParticipantsReminder.class);
        ServiceParticipants.putExtra(Constants.eventId, event_id);
        new Thread(new Runnable() {
            @Override
            public void run() {
                startService(ServiceParticipants);
            }
        }).start();


        //deal with relationship
        //new user 200, exisiting user 100
        int existornew=getIntent().getIntExtra(Constants.reconnect,0);
        if(existornew==200) {
            Relahelper relahelper = new Relahelper();
            relahelper.getAllRelationship();
            Relationship2 newrela = new Relationship2();
            newrela.roomId = event_id;
            newrela.userId = UserServer.returnInfo._id;

            relahelper.createRelationship(newrela);
            Log.i(TAG,"new user entering");
        }
        else if(existornew==100){
            Log.i(TAG,"existing user coming back");
        }
        else{
            Log.i(TAG,"no intent with value from Constants.reconnect, something goes wrong");
        }
        Object lock = new Object();
        Relahelper.lock = lock;
        Relahelper.locker = true;
        Relahelper relahelper = new Relahelper();
        relahelper.relas = null;
        relahelper.getAllRelationship();
        while (relahelper.relas ==null){
            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "rela size is "+ relahelper.relas.size());
/***************init the list View***********************/
        for (Relationship ra : Relahelper.relas) {
            if (ra.roomId.equals(event_id)) {


                    if(!ra.userId.equals(UserServer.returnInfo._id))
                    {

                        for(UserInfo ui:UserServer.UserInfoArrayList)
                        if(ui._id.equals(ra.userId)){
                            if(ui.NamePrivacy.equals("Uncheck")) {
                                name_array.add(ui.Name);
                            }
                            else{
                                name_array.add(ui.Phone);
                            }
                        id_array.add(ui._id);}
                    }

            }
        }
/*****************init the List View********************/
        //userEntergvhb
        if(UserServer.returnInfo.NamePrivacy.equals("Uncheck")){
            pubnub.publish(event_id, "type:enter+id:"+UserServer.returnInfo._id+"Name:"+UserServer.returnInfo.Name, new Callback() {});
        }
        else
        {
            pubnub.publish(event_id, "type:enter+id:"+UserServer.returnInfo._id+"Name:"+UserServer.returnInfo.Phone, new Callback() {});

        }
/*****************set adapter**************************/
        LV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                for (Relationship ra : Relahelper.relas) {
                    if (ra.roomId.equals(event_id)) {

                        for (UserInfo ui : UserServer.UserInfoArrayList) {
                            if (ui._id.equals(id_array.get(position))) {
                                Intent intent = new Intent();
                                intent.setClass(EventMenu.this, UserDisplay.class);
                                intent.putExtra("User", ui);
                                EventMenu.this.startActivity(intent);
                            }
                        }


                    }
                }
            }
        });
/****************************************************/
        Thread get_event_thread=new get_my_event();
        get_event_thread.start();

        IntentFilter intentFilter = new IntentFilter(Constants.signaling);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getIntExtra("Signal", -1) ==Constants.ConnectionError){
                    AlertDialog.Builder builder = new AlertDialog.Builder(EventMenu.this);

                    //  Chain together various setter methods to set the dialog characteristics
                    builder.setMessage("Connection Problem!")
                            .setTitle("Error")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }

                            });

                    // Get the AlertDialog from create()
                    builder.create().show();
                }
                else if (intent.getIntExtra("Signal", -1) ==Constants.EventDeleted){
                    AlertDialog.Builder builder = new AlertDialog.Builder(EventMenu.this);

                    //  Chain together various setter methods to set the dialog characteristics
                    builder.setMessage("Owner has deleted the event!")
                            .setTitle("Error")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }

                            });

                    // Get the AlertDialog from create()
                    builder.create().show();

                }

            }
        };


        this.registerReceiver(mReceiver, intentFilter);
    }





    //Get all the users from the event
    public List<UserInfo> get_users_in_event()
    {
        List<UserInfo> allUsers= UserServer.UserInfoArrayList;
        List<UserInfo> ret_list=new ArrayList<UserInfo>();

        //find the owner info
        for(UserInfo user:allUsers) {
            if (event._ownerid.equals(user._id))
            {
                ret_list.add(user);
                break;
            }
        }
        //find the others


        return ret_list;

    }

    //check the event from test and update the event periodically, also call update_info every time
    public class get_my_event extends Thread {
        public void run() {
            List<Event> allEvents= Event_T.test;

            for(Event events:allEvents)
            {
                if(events._id.equals(event_id)){
                    event=events;
//                    update_info();
                    break;
                }

            }
            try {
                Thread.sleep(check_interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    public class update_rela implements Runnable{

        @Override
        public void run() {

        }
    }



    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {

        super.onPause();

    }

    @Override
    protected void onStop() {

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        this.unregisterReceiver(this.mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_eventmenu, menu);
        return true;
    }
// to set the Owner ID
    public void setOwnerId()
    {
        for(Event event:Event_T.test)
        {
            if(event._id.equals(event_id))
            {
                OwnerID=event._ownerid;
                if(OwnerID.equals(UserServer.returnInfo._id))
                {
                    isOwner=true;
                }
            }
        }
    }

    // to direct to user display in the list view
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_abandon) {

            pubnub.publish(event_id, "type:leave+id:"+UserServer.returnInfo._id+"Name:"+UserServer.returnInfo.Name, new Callback() {});
            name_array.clear();
            id_array.clear();

            boolean admin=false;
            for(Event evt:Event_T.test){
                if(evt._id.equals(event_id)){
                    if(evt._ownerid.equals(UserServer.returnInfo._id)){
                        admin=true;

                    }
                }
            }
            if(admin){
                Event_T helper=new Event_T();
                helper.deleteEvent(event_id);
                Relahelper relhelper=new Relahelper();
                for(Relationship rel: Relahelper.relas){
                    if(rel.roomId.equals(event_id)){
                        Log.i(TAG,"deleting"+rel._id);
                        relhelper.deleteRelationship(rel._id);
                        Log.i(TAG,"delete rel"+rel._id);
                    }
                }
                UserModel.getUserModel().deleteEventId();
                finish();
                Log.i(TAG,"admin leaving");
            }
            else {
                if(!admin) {
                    Relahelper relhelper=new Relahelper();
                    boolean find=false;
                    while(!find){
                        for(Relationship rel:Relahelper.relas){
                            if(rel.roomId.equals(event_id)){
                                if(rel.userId.equals(UserServer.returnInfo._id)){
                                    find=true;
                                    relhelper.deleteRelationship(rel._id);
                                    Log.i(TAG,"find the relationship and delete"+rel._id);
                                    break;
                                }
                            }
                        }
                    }
                    UserModel.getUserModel().deleteEventId();
                    finish();
                    Log.i(TAG, "non admin leaving");
                }
                else{
                    Log.i(TAG,"well");
                }
            }

        }
        if (id == R.id.action_locationEvent){
            Intent intent = new Intent(getApplicationContext(), Map.class);
            intent.putExtra("lat", event.latitude);
            intent.putExtra("lon", event.longitude);
            intent.putExtra(Constants.eventCode,200);
            startActivity(intent);

        }

        if (id == R.id.action_settings){
            Intent tosetting=new Intent(EventMenu.this,eventSetting.class);
            tosetting.putExtra(Constants.eventSetting, event_id);
            for(Event evt: Event_T.test){
                if(evt._id.equals(event_id)){
                    if(evt._ownerid.equals(UserServer.returnInfo._id)){
                        tosetting.putExtra(Constants.eventSettingType,100);
                    }
                    else{
                        tosetting.putExtra(Constants.eventSettingType,200);
                    }
                }
            }
            startActivity(tosetting);
        }


        return super.onOptionsItemSelected(item);
    }
}