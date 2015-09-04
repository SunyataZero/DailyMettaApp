package org.mettacenter.dailymettaapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 *
 */
public class SearchResultsActivityC
        extends AppCompatActivity {

    @Override
    public void onCreate(Bundle iSavedInstanceState){
        super.onCreate(iSavedInstanceState);
        setContentView(R.layout.activity_search_results);
        SearchResultsFragmentC tSearchResultsFragment = new SearchResultsFragmentC();
        getFragmentManager().beginTransaction().replace(R.id.fragment_container, tSearchResultsFragment).commit();
    }

}
