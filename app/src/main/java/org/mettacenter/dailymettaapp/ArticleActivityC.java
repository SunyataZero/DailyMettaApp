package org.mettacenter.dailymettaapp;

//Hi Emmanuel!

import android.app.DialogFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Main activity for the application shown when the user starts the app
 *
 * Please note: AppCompatActivity extends ActionbarActivity
 */
public class ArticleActivityC
        extends AppCompatActivity {

    //Adapter and pager for side swipe
    private PagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    private Cursor mCursor = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);


    }

    @Override
    protected void onStart() {
        super.onStart();


        setupStart();
    }


    /**
     *
     * Input:
     *
     * nw access: yes/no
     * tIsConnectedToInternet
     *
     * timer reached: yes/no
     * tIsUpdateIntervalReachedBl
     *
     * articles downloaded: yes/no
     * ConstsU.DB_NEVER_UPDATED
     *
     * app started AND new app version: yes/no
     * ConstsU.APP_NEVER_STARTED, tOldVer, tNewVer
     *
     *
     * Output:
     *
     * Download articles and write to db: yes/no
     *
     * Display blank screen and show message to user: yes/no
     *
     * Update ViewPager Adapter: yes/no
     *
     */
    private void setupStart() {
        SharedPreferences tSharedPreferences = getSharedPreferences(
                ConstsU.GLOBAL_SHARED_PREFERENCES, Context.MODE_PRIVATE);


        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo tActiveNetworkInfo = cm.getActiveNetworkInfo();
        boolean tIsConnectedToInternet = tActiveNetworkInfo != null
                && tActiveNetworkInfo.isConnectedOrConnecting();


        //Checking if this is the first time the app is started or if we are running a new version
        int tOldVer = PreferenceManager.getDefaultSharedPreferences(this).getInt(
                ConstsU.PREF_APP_VERSION_CODE, ConstsU.APP_NEVER_STARTED);
        int tNewVer = 0;
        try {
            tNewVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(ConstsU.TAG, e.getMessage());
            e.printStackTrace();
            finish();
        }
        if(tNewVer > tOldVer){
            //Writing the new version into the shared preferences
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putInt(ConstsU.PREF_APP_VERSION_CODE, tNewVer)
                    .commit();
        }


        long tLastUpdateInMillisLg = tSharedPreferences.getLong(
                ConstsU.PREF_LAST_UPDATE_TIME_IN_MILLIS, ConstsU.DB_NEVER_UPDATED);
        long tUpdateIntervalInMillisLg = TimeUnit.MINUTES.toMillis(ConstsU.UPDATE_INTERVAL_IN_MINUTES); //-TODO: Change to days
        boolean tIsUpdateIntervalReachedBl =
                Calendar.getInstance().getTimeInMillis() - tLastUpdateInMillisLg
                >= tUpdateIntervalInMillisLg;
        Log.d(ConstsU.TAG, "tIsUpdateIntervalReachedBl = " + tIsUpdateIntervalReachedBl);
        Log.d(ConstsU.TAG, "tLastUpdateInMillisLg = " + tLastUpdateInMillisLg);
        Log.d(ConstsU.TAG, "Calendar.getInstance().getTimeInMillis() = " + Calendar.getInstance().getTimeInMillis());
        Log.d(ConstsU.TAG, "tUpdateIntervalInMillisLg = " + tUpdateIntervalInMillisLg);


        //TODO: Do we want to do the update when a new app version is launched?




        if(tIsConnectedToInternet == false && tLastUpdateInMillisLg == ConstsU.DB_NEVER_UPDATED){
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show();

            //TODO: Show this in the main window instead
            //TODO: And show a button that can be pressed for downloading the articles


            findViewById(R.id.empty_layout).setVisibility(View.VISIBLE);
            findViewById(R.id.pager).setVisibility(View.GONE);



        }else if(tIsUpdateIntervalReachedBl == true
                || tLastUpdateInMillisLg == ConstsU.DB_NEVER_UPDATED
                || tNewVer > tOldVer){

            downloadArticlesAndFinishSetup();


        }else{
            finishSetup();
        }





        Button tManualDownloadBn = (Button)findViewById(R.id.manual_download_button);
        tManualDownloadBn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadArticlesAndFinishSetup();
            }
        });




        /*..setup continues in AppSetupCallbackClass below after (1) a callback from
        FetchArticlesTaskC (if we update the db) or (2) a direct call (if we don't update the db)
        */
    }

    private void downloadArticlesAndFinishSetup() {

        findViewById(R.id.empty_layout).setVisibility(View.GONE);
        findViewById(R.id.pager).setVisibility(View.VISIBLE);

        //Setting up the data..

        //..clearing the db
        getApplicationContext().deleteDatabase(DbHelperM.DB_NAME);

        //..fetching all the articles
        new FetchArticlesTaskC(this, new AppSetupCallbackClass())
                .execute();
    }


    public class AppSetupCallbackClass {
        public void setupCallback(){
            ArticleActivityC.this.finishSetup();
        }
    }

    private void finishSetup(){
        try{
            mCursor = getContentResolver().query(
                    ContentProviderM.ARTICLE_CONTENT_URI, null, null, null, ConstsU.SORT_ORDER);
            mPagerAdapter = new PagerAdapterC(getSupportFragmentManager(), mCursor);
        }catch(Exception e){
            Log.e(ConstsU.TAG, e.getMessage());
        }

        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        //Redrawing all fragments
        mPagerAdapter.notifyDataSetChanged();


        //Setting the position of the viewpager..

        //..in case we are starting up the app
        Calendar c = Calendar.getInstance();
        long tArticleForTodayOrPrevious = UtilitiesU.getArticlePositionFromDate(
                this, c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        if(tArticleForTodayOrPrevious == -1){
            tArticleForTodayOrPrevious = 0; //-use the latest article if no article was found for the date
        }

        //..in case we are using the date picker calendar
        int tPositionIt = (int)getIntent().getLongExtra(ConstsU.EXTRA_ARTICLE_POS_ID,
                tArticleForTodayOrPrevious);
        mViewPager.setCurrentItem(tPositionIt);
        getIntent().removeExtra(ConstsU.EXTRA_ARTICLE_POS_ID);
    }



    @Override
    protected void onDestroy(){
        super.onDestroy();
        mCursor.close();
        mCursor = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu iMenu) {
        //Inflating the menu (which will add items to the action bar)
        getMenuInflater().inflate(R.menu.menu_article, iMenu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem iMenuItem) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(iMenuItem.getItemId()){
            case R.id.action_text_search:
                onSearchRequested();
                /*
                -Android OS method which will ask the OS to show a search bar positioned over
                the action bar
                */
                return true;
            case R.id.action_choose_date:
                DialogFragment tDatePickerFragment = new DatePickerFragmentC();
                tDatePickerFragment.show(this.getFragmentManager(), "DatePicker");
                return true;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(iMenuItem);
        }
    }

    /*
    The override below does not seem to be needed, but please keep in mind that "getIntent" does
    not get the latest intent, but rather the intent that was used to start the activity

    @Override
    public void onNewIntent(Intent iIntent){
        super.onNewIntent(iIntent);
        setIntent(iIntent);
    }
    */
}
