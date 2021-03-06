//COMP4521  Kwok Chung Hin   20111831   chkwokad@ust.hk
//COMP4521  Kwok Tsz Ting 20119118  ttkwok@ust.hk
//COMP4521  Li Lok Him  20103470    lhliab@ust.hk
package hk.ust.cse.comp4521.eventmaker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Locale;

import hk.ust.cse.comp4521.eventmaker.Event.Event;
import hk.ust.cse.comp4521.eventmaker.Event.Event_T;
import hk.ust.cse.comp4521.eventmaker.Event.Map;
import hk.ust.cse.comp4521.eventmaker.Event.Matching;
import hk.ust.cse.comp4521.eventmaker.Helper.ServerConnection;
import hk.ust.cse.comp4521.eventmaker.PassiveSearch.SearchHelper;
import hk.ust.cse.comp4521.eventmaker.PostEvent.EventMenu;
import hk.ust.cse.comp4521.eventmaker.User.UserInfo;
import hk.ust.cse.comp4521.eventmaker.User.UserModel;
import hk.ust.cse.comp4521.eventmaker.User.UserServer;


public class SearchFrag extends ActionBarActivity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;



    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    Intent mainloc;
    private Object lock;
    private ProgressDialog pd2;
    private static Boolean networkIO;
    public static Handler handle = new Handler(){
        @Override
        public void handleMessage(Message inputMessage) { //handler to receive the connection state from server connection
            if (inputMessage.what == Constants.ConnectionError) {
                networkIO = false;
            }
            else {
                networkIO = true;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_frag);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
        pd2= ProgressDialog.show(SearchFrag.this, "Loading", "Downloading important info from the Internet.", true);
        Intent i = new Intent(Constants.signaling).putExtra("Signal", Constants.allserviceStopped); //before allowing user to choose their interest, all background services related to this application have to be stopped
        this.sendBroadcast(i); //send a broadcast message to others
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(serviceConnection); //unbind location detection service
    }

    @Override
    protected void onResume() {

        super.onResume();
        Intent i = new Intent(Constants.signaling).putExtra("Signal", Constants.allserviceStopped);
        this.sendBroadcast(i); //send a broadcast message to others to indicate all serivces have to be stopped
        UserServer userServer = new UserServer();


        ServerConnection serverConn = new ServerConnection(SearchFrag.this, handle); //update all the user data from service and check the connection
        serverConn.run();


        if (userServer.connectionState ==true) { //if the connection is successful
            lock = new Object();
            userServer.lock = lock;
            UserInfo user = userServer.getAUser(UserModel.getUserModel().getPhoneNumberFromSP());

            if (UserServer.returnInfo == null) { //wait till the user info is obtained
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

            Event_T event_t = new Event_T();
            event_t.getAllEvent();
            event_t.locker= true;
            event_t.lock = lock;


            while (Event_T.test == null) { //wait till events info is obtained
                synchronized (lock){
                    Log.i("SearchFrag", "Loading Events");
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            }
            SharedPreferences pre = UserModel.getUserModel().getSharedPreferences();
            boolean find=false;
            //for reconnection
            //check if the event id exists in sharedpreference

            if (pre.contains("Event")  ) {
                final String eventId = pre.getString("Event", "");
                Log.i("SEARCH","finding event"+eventId);
                //find the event
                for(Event evt:Event_T.test){
                    if(evt._id.equals(eventId)){
                        find=true;
                    }
                }
                if(find) {
                    //if the event is found, the user would be directed back to the event.
                    new AlertDialog.Builder(SearchFrag.this)
                            .setTitle("redirection")
                            .setMessage("brining you back to the event")
                            .setNeutralButton("ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent back = new Intent(SearchFrag.this, EventMenu.class);
                                    back.putExtra(Constants.eventId, eventId);
                                    back.putExtra(Constants.reconnect,100);
                                    back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    Log.i("SEARCH", "sending back to event menu");
                                    startActivity(back);
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }//event already disappeared
                else{
                    //delete the eventid from sharedpreference
                    UserModel.getUserModel().deleteEventId();
                }
            }


        }
        pd2.dismiss();

        mainloc = new Intent(getApplicationContext(), SearchHelper.class); //start location detection service by binding to it
        mainloc.putExtra("Mode", "Voluntary");
        MainSearchFragment.getloc = mainloc;
        bindService(mainloc, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_searchfrag, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Log.i(null, "Setting Button is clicked");
            Intent intent = new Intent(getApplicationContext(), Setting.class);
            startActivity(intent);
            return true;
        }
        if (id ==R.id.action_about){
            Intent intent = new Intent(this, About.class);
            startActivity(intent);
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a MainSearchFragment (defined as a static inner class below).

            if (position ==0)
                return MainSearchFragment.newInstance(position + 1);
            else
                return PassiveSearchFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);

            }
            return null;
        }
    }


    public static class MainSearchFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "main search";
        private static String [] activity;



        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static MainSearchFragment newInstance(int sectionNumber) {
            MainSearchFragment fragment = new MainSearchFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);

            return fragment;
        }

        public MainSearchFragment() {
        }

        private static Intent getloc;
        private ProgressDialog pd;



        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_search, container, false);

            //set listview
            activity = getResources().getStringArray(R.array.interest_array2);

            final ListView list = (ListView) rootView.findViewById(R.id.searchselectionList);
            list.setAdapter(new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_single_choice, activity));
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {


                    if (SearchHelper.mCurrentLocation == null) { //to check if there is any location detection
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()); //create dialogue to alert users about location service

                        //  Chain together various setter methods to set the dialog characteristics
                        builder.setMessage("No location detected!!")
                                .setTitle("Error")
                                .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }

                                });

                        // Get the AlertDialog from create()
                        builder.create().show();
                        return;
                    }
                    pd = ProgressDialog.show(getActivity(), "Network Access", "Connecting to the server", true);
                    ServerConnection serverConn = new ServerConnection(getActivity(), handle); //check network connectivity
                    serverConn.run(); //test network connection
                    pd.dismiss();

                    while (UserServer.connectionState == null) {

                    }
                    if (!UserServer.connectionState) {
                        return ;
                    }

                    Object lock = new Object();
                    Event_T.lock = lock;
                    Event_T.locker = true;
                    Event_T eventS = new Event_T();
                    eventS.test = null;
                    eventS.getAllEvent(); //get all events from server
                    while (eventS.test ==null){
                        synchronized (lock){
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }

                    SharedPreferences pre = UserModel.getUserModel().getSharedPreferences();
                    boolean find=false;
                    //check if event id exists in sharedpreference
                    if (pre.contains("Event")) {
                        final String eventId = pre.getString("Event", "");
                        for(Event evt:Event_T.test){
                            if(evt._id.equals(eventId)){
                                find=true;
                            }
                        }
                        if(find) {
                            //if the event is present, the user would be directed back to the event
                            new AlertDialog.Builder(getActivity())
                                    .setTitle("redirection")
                                    .setMessage("brining you back to the event")
                                    .setNeutralButton("ok", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent back = new Intent(getActivity(), EventMenu.class);
                                            back.putExtra(Constants.eventId, eventId);
                                            back.putExtra(Constants.reconnect, 100);
                                            Log.i("SEARCH", "sending back to event menu");
                                            getActivity().stopService(getloc);
                                            back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                            startActivity(back);
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        }
                        else{
                            //otherwise, the id would be deleted
                            UserModel.getUserModel().deleteEventId();
                        }
                    }
                    else {

                        //get the latitude and longitude of the current location
                        double lat = SearchHelper.mCurrentLocation.getLatitude();
                        double lon = SearchHelper.mCurrentLocation.getLongitude();

                        Log.i(ARG_SECTION_NUMBER, "Successfully get the location");
                        Log.i("SearchFrag", (String) list.getAdapter().getItem(i) + " " + lat + " " + lon);

                        //find if there is an existing event of a specific interest nearby
                        String id = Matching.checking((String) list.getAdapter().getItem(i), lat, lon);
                        //stop the location detection service
                        getActivity().stopService(getloc);


                        ///if no events are nearby, create a new event and go to the map class to choose the location for it
                        if (id == null) {
                            Intent intent2 = new Intent(getActivity(), Map.class);
                            intent2.putExtra("Interest", (String) list.getAdapter().getItem(i));
                            intent2.putExtra("lat", lat);
                            intent2.putExtra("lon", lon);
                            intent2.putExtra(Constants.eventCode, 100);
                            Log.i(ARG_SECTION_NUMBER, "Create new event");
                            intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent2);

                        } else { //if an event exists, navigate to it
                            Intent intent2 = new Intent(getActivity(), EventMenu.class);
                            intent2.putExtra(Constants.eventId, id);
                            intent2.putExtra(Constants.reconnect, 200);
                            Log.i(ARG_SECTION_NUMBER, "Go to the existing event");
                            intent2.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); //clear back stack
                            startActivity(intent2);
                        }
                    }
                }
            });

        return rootView;
        }
    }

    public static class PassiveSearchFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "passive search";
        private static String [] activity;
        private ListAdapter adapter;
        private ArrayList<String> tempPassive;
        private Button enablepassive;
        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PassiveSearchFragment newInstance(int sectionNumber) {
            PassiveSearchFragment fragment = new PassiveSearchFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PassiveSearchFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_passivesearch, container, false);

            //set listview
            activity = getResources().getStringArray(R.array.interest_array2);

            final ListView list = (ListView) rootView.findViewById(R.id.passivesearchselectionList);
            adapter = new ArrayAdapter<String>(this.getActivity(), android.R.layout.simple_list_item_single_choice, activity);

            list.setAdapter(adapter);
            tempPassive = new ArrayList<>();

            //add or remove the interests in a list when user selects or deselects them
            list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String selected = (String) list.getItemAtPosition(i);
                    Log.i(ARG_SECTION_NUMBER, selected + " selected!");
                    if (tempPassive.contains(selected)) {
                        tempPassive.remove(selected);
                    }
                    else
                        tempPassive.add(selected);
                }

            });
            final Intent intent = new Intent(getActivity(), SearchHelper.class);
            enablepassive = (Button) rootView.findViewById(R.id.passiveSearchEnabler);
            enablepassive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //enable passive search
                    String result = "Selected interests:";
                    for (int i= 0 ; i< tempPassive.size(); i++){
                        result = result +" "+ tempPassive.get(i);
                    }
                    Log.i(ARG_SECTION_NUMBER, result);

                    //start passive search service
                    intent.putExtra("Mode", "Passive");
                    intent.putStringArrayListExtra("Interest", tempPassive);
                    getActivity().startService(intent);

                    return;

                }
            });

            Button disablepassive = (Button) rootView.findViewById(R.id.passiveSearchDisabler);
            disablepassive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //disable passive search and send message to intended recipient that notification should be cancelled
                    Intent i = new Intent(Constants.closeNot).putExtra("Signal", Constants.closeNotification);
                    getActivity().sendBroadcast(i);
                    getActivity().stopService(intent);

                    return;
                }
            });

            return rootView;
        }



    }

}
