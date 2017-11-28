package dk.mustache.beaconbacon.fragments;

import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import dk.mustache.beaconbacon.R;
import dk.mustache.beaconbacon.activities.MapActivity;
import dk.mustache.beaconbacon.adapters.PoiSelectionAdapter;
import dk.mustache.beaconbacon.api.ApiManager;
import dk.mustache.beaconbacon.data.DataManager;
import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class PoiSelectionFragment extends Fragment {
    //RecyclerView Setup
    private StickyListHeadersListView stickyListHeadersListView;
    private StickyListHeadersAdapter stickyListHeadersAdapter;

    public PoiSelectionFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poi_selection, container, false);

        Toolbar toolbar = view.findViewById(R.id.fragment_poi_toolbar);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        if(((AppCompatActivity) getActivity()).getSupportActionBar() != null)
            ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);

        setHasOptionsMenu(true);


        //RecyclerView Setup
        stickyListHeadersListView = view.findViewById(R.id.poi_list);
        stickyListHeadersAdapter = new PoiSelectionAdapter(getActivity(), DataManager.getInstance().getCurrentPlace().getPoiMenuItem());
        stickyListHeadersListView.setAdapter(stickyListHeadersAdapter);

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_close) {
            ((MapActivity) getActivity()).floatingActionButton.show();
            getActivity().getSupportFragmentManager().popBackStack();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}