package dk.mustache.beaconbacon.activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import dk.mustache.beaconbacon.R;
import dk.mustache.beaconbacon.api.ApiManager;
import dk.mustache.beaconbacon.api.FindTheBookAsync;
import dk.mustache.beaconbacon.api.GetFloorImageAsync;
import dk.mustache.beaconbacon.api.GetIconImageAsync;
import dk.mustache.beaconbacon.api.GetMenuOverviewAsync;
import dk.mustache.beaconbacon.api.GetSpecificPlaceAsync;
import dk.mustache.beaconbacon.customviews.CustomPoiView;
import dk.mustache.beaconbacon.customviews.CustomSnackbar;
import dk.mustache.beaconbacon.customviews.MapHolderView;
import dk.mustache.beaconbacon.customviews.PoiHolderView;
import dk.mustache.beaconbacon.data.BeaconBaconManager;
import dk.mustache.beaconbacon.datamodels.BBFaustDataObject;
import dk.mustache.beaconbacon.datamodels.BBFloor;
import dk.mustache.beaconbacon.datamodels.BBPlace;
import dk.mustache.beaconbacon.datamodels.BBPoi;
import dk.mustache.beaconbacon.datamodels.BBPoiMenuItem;
import dk.mustache.beaconbacon.datamodels.BBResponseObject;
import dk.mustache.beaconbacon.enums.DisplayType;
import dk.mustache.beaconbacon.fragments.PlaceSelectionFragment;
import dk.mustache.beaconbacon.fragments.PoiSelectionFragment;
import dk.mustache.beaconbacon.interfaces.FindTheBookAsyncResponse;
import dk.mustache.beaconbacon.interfaces.FloorImageAsyncResponse;
import dk.mustache.beaconbacon.interfaces.IconImageAsyncResponse;
import dk.mustache.beaconbacon.interfaces.MenuOverviewAsyncResponse;
import dk.mustache.beaconbacon.interfaces.SpecificPlaceAsyncResponse;

import static android.graphics.Typeface.BOLD;
import static dk.mustache.beaconbacon.BBApplication.FAUST_ID;
import static dk.mustache.beaconbacon.BBApplication.PLACE_ID;

public class MapActivity extends AppCompatActivity implements View.OnClickListener, SpecificPlaceAsyncResponse, MenuOverviewAsyncResponse, FindTheBookAsyncResponse, FloorImageAsyncResponse, IconImageAsyncResponse {
    public static final String TAG = "BeaconBacon";
    public static final String PLACE_SELECTION_FRAGMENT = "place_selection_fragment";
    public static final String POI_SELECTION_FRAGMENT = "poi_selection_fragment";

    private FrameLayout rootView;
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private TextView toolbarTitle;
    private TextView toolbarSubtitle;
    private ImageView arrowLeft;
    private ImageView arrowRight;

    public FloatingActionButton fabPoi;
    public FloatingActionButton fabFindTheBook;
    public CustomSnackbar snackbar;

    private PoiSelectionFragment poiSelectionFragment;
    private PlaceSelectionFragment placeSelectionFragment;

    private FrameLayout mapView;
    private MapHolderView mapHolderView;
    private PoiHolderView poiHolderView;
    private List<BBPoi> selectedPois;

    private boolean isFindingBook;
    private boolean isFindingSpecificPlace;
    private boolean isFindingMenuForPlace;
    private boolean isFindingFloorImage;
    private boolean isFindingPoiIcons;

    private Bitmap currentFloorImage;
    private boolean bookWasFound;
    private boolean updateFindTheBook = true;
    private boolean isLocatingFindTheBookFloor;


    //region Android Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set Layout
        setContentView(R.layout.activity_map);
        rootView = findViewById(R.id.root_view);
        mapView = findViewById(R.id.map_view_container);

        //Map View
        mapHolderView = new MapHolderView(this);
//        mapHolderView.setAdjustViewBounds(true);
//        mapHolderView.setFitsSystemWindows(true);
        mapView.addView(mapHolderView);

        poiHolderView = new PoiHolderView(this);
//        poiHolderView.setAdjustViewBounds(true);
//        poiHolderView.setFitsSystemWindows(true);
        mapView.addView(poiHolderView);

        mapHolderView.poiHolderView = poiHolderView;

        progressBar = findViewById(R.id.map_progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        setupToolbar();

        //FABs
        fabPoi = findViewById(R.id.map_poi_fab);
        fabPoi.setOnClickListener(this);
        fabFindTheBook = findViewById(R.id.map_ftb_fab);
        fabFindTheBook.setOnClickListener(this);


        //How was the MapActivity opened? (), (place_id) or (place_id, faust_id)
        String place_id = getIntent().getStringExtra(PLACE_ID);
        String faust_id = getIntent().getStringExtra(FAUST_ID);

        if(place_id != null) {
            progressBar.setVisibility(View.VISIBLE);

            //Loop all places to find this one and set toolbar titles
            for (int i = 0; i < BeaconBaconManager.getInstance().getAllPlaces().getData().size(); i++) {
                if(Objects.equals(String.valueOf(BeaconBaconManager.getInstance().getAllPlaces().getData().get(i).getId()), place_id)) {
                    //TODO We can't set floor name because we don't know it until we've found a Specific Place
//                    toolbarTitle.setText(BeaconBaconManager.getInstance().getAllPlaces().getData().get(i).getFloors().get(0).getName());
                    toolbarSubtitle.setText(BeaconBaconManager.getInstance().getAllPlaces().getData().get(i).getName());
                    break;
                }
            }

            if(faust_id != null) {
                //We have place_id and a faust_id
                    if(BeaconBaconManager.getInstance().getRequestObject() != null) {
                    findABook(place_id);
                } else {
                    Log.e("MapActivity", "Faust id provided, but no Request Object was set. Make a new BBRequestObject and set it to the BeaconBaconManager.");
                }

                findSpecificPlace(place_id, true);
            } else {
                //We have a place id
                findSpecificPlace(place_id, false);
            }

        } else {
            //Open place selection
            openPlaceSelectionFragment();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BeaconBaconManager.getInstance().setRequestObject(null);
        BeaconBaconManager.getInstance().setCurrentPlace(null);
        BeaconBaconManager.getInstance().setCurrentFloorIndex(-1);
        BeaconBaconManager.getInstance().setCurrentFloorId(-1);
    }

    @SuppressLint("Range")
    private void showFindTheBookSnackbar() {
        snackbar = CustomSnackbar.make(rootView, CustomSnackbar.LENGTH_INDEFINITE);

        final Snackbar.SnackbarLayout snackbarView = (Snackbar.SnackbarLayout) snackbar.getView();
        if(BeaconBaconManager.getInstance().getConfigurationObject() != null && BeaconBaconManager.getInstance().getConfigurationObject().getTintColor() != -1)
            snackbarView.setBackgroundColor(getResources().getColor(BeaconBaconManager.getInstance().getConfigurationObject().getTintColor()));

        //Modify LayoutParams for Top-snackbar
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)snackbarView.getLayoutParams();
        params.gravity = Gravity.TOP;

        //Calculate Toolbar height
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
            params.topMargin = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());

        snackbarView.setLayoutParams(params);
        snackbar.setAction("Afslut", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BeaconBaconManager.getInstance().setRequestObject(null);
                mapHolderView.poiHolderView.findTheBookObject = null;
                updatePlace(BeaconBaconManager.getInstance().getCurrentPlace());
                hideFindTheBookElements();
            }
        });
        snackbar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_close) {
            if(getSupportFragmentManager().getBackStackEntryCount() != 0) {
                //Let the fragment consume the event
                return false;
            } else {
                //Dismiss activity
                finish();
                overridePendingTransition(0, R.anim.slide_out_bottom);
                return true;
            }
        }

        return false;
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.bb_toolbar_title_layout) {
            hideGuiElements();
            placeSelectionFragment = new PlaceSelectionFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                    .replace(R.id.fragment_container, placeSelectionFragment, PLACE_SELECTION_FRAGMENT)
                    .addToBackStack(null)
                    .commit();

        } else if (i == R.id.map_poi_fab) {
            hideGuiElements();
            poiSelectionFragment = new PoiSelectionFragment();
            poiSelectionFragment.selectedPois = selectedPois;
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_bottom, R.anim.slide_in_bottom, R.anim.slide_out_bottom)
                    .replace(R.id.fragment_container, poiSelectionFragment, POI_SELECTION_FRAGMENT)
                    .addToBackStack(null)
                    .commit();

        } else if (i == R.id.map_ftb_fab) {
            mapHolderView.scrollToBook();
            for (int j = 0; j < BeaconBaconManager.getInstance().getCurrentPlace().getFloors().size(); j++) {
                if (BeaconBaconManager.getInstance().getResponseObject().getData().get(0).getFloor().getId() == BeaconBaconManager.getInstance().getCurrentPlace().getFloors().get(j).getId())
                    updateCurrentFloor(j, BeaconBaconManager.getInstance().getResponseObject().getData().get(0).getFloor().getId());
            }

        } else if (i == R.id.bb_toolbar_arrow_left) {
            updateArrows(-1);

        } else if (i == R.id.bb_toolbar_arrow_right) {
            updateArrows(1);

        }
    }
    //endregion



    //region Setup
    private void setupToolbar() {
        toolbar = findViewById(R.id.bb_toolbar_regular);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        LinearLayout toolbarTitleLayout = findViewById(R.id.bb_toolbar_title_layout);
        toolbarTitleLayout.setOnClickListener(this);

        toolbarTitle = findViewById(R.id.bb_toolbar_title);
        toolbarSubtitle = findViewById(R.id.bb_toolbar_subtitle);
        if(BeaconBaconManager.getInstance().getConfigurationObject() != null && BeaconBaconManager.getInstance().getConfigurationObject().getTypeface() != null) {
            toolbarTitle.setTypeface(BeaconBaconManager.getInstance().getConfigurationObject().getTypeface(), BOLD);
            toolbarSubtitle.setTypeface(BeaconBaconManager.getInstance().getConfigurationObject().getTypeface());
        }

        arrowLeft = findViewById(R.id.bb_toolbar_arrow_left);
        arrowRight = findViewById(R.id.bb_toolbar_arrow_right);
        arrowLeft.setOnClickListener(this);
        arrowRight.setOnClickListener(this);
    }

    private void updateToolbar() {
        toolbarSubtitle.setText(BeaconBaconManager.getInstance().getCurrentPlace().getName());
        if(BeaconBaconManager.getInstance().getCurrentPlace().getFloors() != null && BeaconBaconManager.getInstance().getCurrentPlace().getFloors().size() > 0)
            toolbarTitle.setText(BeaconBaconManager.getInstance().getCurrentPlace().getFloors().get(0).getName());

        updateArrows(0);
    }
    //endregion



    //region Place and Find The Book initialization
    private void openPlaceSelectionFragment() {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //TODO Should remove this handler, AllPlaces must have been fetched before opening the library
                if(BeaconBaconManager.getInstance().getAllPlaces() != null && BeaconBaconManager.getInstance().getAllPlaces().getData() != null) {
                    //We haven't gotten a place, let's prompt the user to select one
                    hideGuiElements();
                    progressBar.setVisibility(View.GONE);

                    placeSelectionFragment = new PlaceSelectionFragment();
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.hold_anim, R.anim.slide_out_bottom, R.anim.hold_anim, R.anim.slide_out_bottom)
                            .replace(R.id.fragment_container, placeSelectionFragment, PLACE_SELECTION_FRAGMENT)
                            .addToBackStack(null)
                            .commit();
                } else {
                    handler.postDelayed(this, 100);
                }

            }
        };
        handler.postDelayed(runnable,100);
    }

    private void hideGuiElements() {
        fabPoi.hide();
        fabFindTheBook.hide();
        if(snackbar != null) {
            snackbar.getView()
                    .animate()
                    .alpha(0)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            snackbar.getView().setVisibility(View.GONE);
                        }
                    })
                    .setDuration(300)
                    .start();
        }
    }

    public void findABook(String place_id) {
        isFindingBook = true;

        //A Request object was set, lets find the book
        FindTheBookAsync findTheBookAsync = new FindTheBookAsync();
        findTheBookAsync.delegate = this;
        ApiManager.getInstance().findTheBookAsync(findTheBookAsync, place_id);
    }

    private void findSpecificPlace(String place_id, boolean awaitFindBook) {
        isFindingSpecificPlace = true;
        isFindingBook = awaitFindBook;

        //Initiate Fetch Specific Place
        GetSpecificPlaceAsync getSpecificPlaceAsync = new GetSpecificPlaceAsync();
        getSpecificPlaceAsync.delegate = this;
        ApiManager.getInstance().fetchSpecificPlaceAsync(getSpecificPlaceAsync, place_id);
    }
    //endregion



    //region Update Content
    private void updateMapView(boolean updateFloor, boolean updatePois, List<CustomPoiView> pois) {
        mapHolderView
                .animate()
                .alpha(0)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        final Handler handler = new Handler();
                        final Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                if(!isFindingFloorImage && !isFindingPoiIcons) {
                                    progressBar.setVisibility(View.GONE);
                                    fabPoi.setVisibility(View.VISIBLE);
                                    if(BeaconBaconManager.getInstance().getRequestObject() != null && bookWasFound)
                                        fabFindTheBook.setVisibility(View.VISIBLE);

                                    mapHolderView.invalidate();
                                    mapHolderView.clearAnimation();
                                    mapHolderView
                                            .animate()
                                            .alpha(1)
                                            .setDuration(300)
                                            .start();
                                } else {
                                    handler.postDelayed(this, 100);
                                }
                            }
                        };
                        handler.postDelayed(runnable,100);
                    }
                })
                .setDuration(300)
                .start();

        if(updateFloor) {
            if(mapHolderView.poiHolderView != null)
                mapHolderView.poiHolderView.floorWasSwitched();

            mapHolderView.setImageBitmap(null);
        }
        if(updatePois)
            mapHolderView.setMapPois(null);

        if(!isFindingFloorImage)
            mapHolderView.setImageBitmap(currentFloorImage);
        if(!isFindingPoiIcons)
            mapHolderView.setMapPois(pois);
        if(!isFindingBook && BeaconBaconManager.getInstance().getRequestObject() != null && !isFindingFloorImage) {
            final Handler handler = new Handler();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if(updateFindTheBook) {
                        updateFindTheBook = false;
                        isLocatingFindTheBookFloor = true;

                        for (int i = 0; i < BeaconBaconManager.getInstance().getCurrentPlace().getFloors().size(); i++) {
                            if (BeaconBaconManager.getInstance().getResponseObject().getData().get(0).getFloor().getId() == BeaconBaconManager.getInstance().getCurrentPlace().getFloors().get(i).getId())
                                updateCurrentFloor(i, BeaconBaconManager.getInstance().getResponseObject().getData().get(0).getFloor().getId());
                        }
                    }
                }
            };
            handler.postDelayed(runnable,100);
        }
        if(!isLocatingFindTheBookFloor) {
            if(BeaconBaconManager.getInstance().getRequestObject() != null)
                mapHolderView.setFindTheBook(BeaconBaconManager.getInstance().getRequestObject().getImage(), BeaconBaconManager.getInstance().getResponseObject());
            updateToolbarTitle(BeaconBaconManager.getInstance().getCurrentPlace());
        }
    }

    private void updateMenuOverview(BBPlace place) {
        isFindingMenuForPlace = true;

        //Get the menu overview right away
        GetMenuOverviewAsync getMenuOverviewAsync = new GetMenuOverviewAsync();
        getMenuOverviewAsync.delegate = this;
        ApiManager.createInstance(this).fetchMenuOverviewAsync(getMenuOverviewAsync, String.valueOf(place.getId()));
    }

    private void updatePlace(BBPlace place) {
        //Loop all places to find this one
        for (int i = 0; i < BeaconBaconManager.getInstance().getAllPlaces().getData().size(); i++) {
            if (BeaconBaconManager.getInstance().getAllPlaces().getData().get(i).getId() == place.getId()) {

                //Sort the Place's floors by Order
                Collections.sort(place.getFloors(), new Comparator<BBFloor>() {
                    @Override
                    public int compare(BBFloor floor1, BBFloor floor2) {
                        return floor1.getOrder() - floor2.getOrder();
                    }
                });

                //Replace existing place with the fetched place
                BeaconBaconManager.getInstance().getAllPlaces().getData().set(i, place);

                //Set current Place and floor
                updateCurrentPlace(place);
                updateCurrentFloor(0, BeaconBaconManager.getInstance().getCurrentPlace().getFloors().get(0).getId());

                updateToolbar();

                if (place.getFloors() != null && place.getFloors().size() > 0 && !Objects.equals(place.getFloors().get(0).getImage(), "")) {
                    isFindingFloorImage = true;

                    GetFloorImageAsync getFloorImageAsync = new GetFloorImageAsync();
                    getFloorImageAsync.delegate = this;
                    ApiManager.getInstance().getFloorImage(getFloorImageAsync);
                } else {
                    mapHolderView.setMapPois(null);
                    mapHolderView.setFindTheBook(null, null);
                    mapHolderView.setImageBitmap(null);
                    progressBar.setVisibility(View.GONE);
                    showAlert(getString(R.string.alert_title_general), getString(R.string.alert_message_library));
                }
            }
        }
    }

    private void updateArrows(int direction) {
        if(BeaconBaconManager.getInstance().getCurrentPlace().getFloors() != null && BeaconBaconManager.getInstance().getCurrentPlace().getFloors().size() != 0) {

            int floorListSize = BeaconBaconManager.getInstance().getCurrentPlace().getFloors().size();
            int currentFloor = BeaconBaconManager.getInstance().getCurrentFloorIndex();

            //Change the floor
            if (direction == 1 && currentFloor != floorListSize - 1) {
                //Update text
                toolbarSubtitle.setText(BeaconBaconManager.getInstance().getCurrentPlace().getName());
                toolbarTitle.setText(BeaconBaconManager.getInstance().getCurrentPlace().getFloors().get(currentFloor + direction).getName());

                updateCurrentFloor(currentFloor + direction, BeaconBaconManager.getInstance().getCurrentPlace().getFloors().get(currentFloor + direction).getId());
            } else if (direction == -1 && currentFloor != 0) {
                //Update text
                toolbarSubtitle.setText(BeaconBaconManager.getInstance().getCurrentPlace().getName());
                toolbarTitle.setText(BeaconBaconManager.getInstance().getCurrentPlace().getFloors().get(currentFloor + direction).getName());

                updateCurrentFloor(currentFloor + direction, BeaconBaconManager.getInstance().getCurrentPlace().getFloors().get(currentFloor + direction).getId());
            }

            //Update arrows
            if (currentFloor + direction == 0) {
                arrowLeft.setClickable(false);
                arrowLeft.setImageDrawable(getResources().getDrawable(R.drawable.ic_chevron_left_light));
            } else {
                arrowLeft.setClickable(true);
                arrowLeft.setImageDrawable(getResources().getDrawable(R.drawable.ic_chevron_left));
            }

            if (currentFloor + direction == floorListSize - 1) {
                arrowRight.setClickable(false);
                arrowRight.setImageDrawable(getResources().getDrawable(R.drawable.ic_chevron_right_light));
            } else {
                arrowRight.setClickable(true);
                arrowRight.setImageDrawable(getResources().getDrawable(R.drawable.ic_chevron_right));
            }
        } else {
            toolbarTitle.setText("-");
            arrowLeft.setImageDrawable(getResources().getDrawable(R.drawable.ic_chevron_left_light));
            arrowRight.setImageDrawable(getResources().getDrawable(R.drawable.ic_chevron_right_light));
        }
    }

    private void updateCurrentFloor(int newCurrentFloorIndex, int newCurrentFloorId) {
        progressBar.setVisibility(View.VISIBLE);

        BeaconBaconManager.getInstance().setCurrentFloorIndex(newCurrentFloorIndex);
        BeaconBaconManager.getInstance().setCurrentFloorId(newCurrentFloorId);
        BBPlace currentPlace = BeaconBaconManager.getInstance().getCurrentPlace();

        if (currentPlace.getFloors() != null &&
                currentPlace.getFloors().size() > 0 &&
                !Objects.equals(currentPlace.getFloors().get(newCurrentFloorIndex).getImage(), "")) {

            GetFloorImageAsync getFloorImageAsync = new GetFloorImageAsync();
            getFloorImageAsync.delegate = this;
            ApiManager.getInstance().getFloorImage(getFloorImageAsync);

            isFindingPoiIcons = true;

            GetIconImageAsync getIconImageAsync = new GetIconImageAsync();
            getIconImageAsync.delegate = this;
            ApiManager.getInstance().getIconImage(this, getIconImageAsync, selectedPois);
        } else {
            mapHolderView.setMapPois(null);
            mapHolderView.setFindTheBook(null, null);
            mapHolderView.setImageBitmap(null);
        }
    }

    private void updateCurrentPlace(BBPlace newCurrentPlace) {
        progressBar.setVisibility(View.VISIBLE);
        BeaconBaconManager.getInstance().setCurrentPlace(newCurrentPlace);
    }
    //endregion



    //region Updates from Fragments
    public void setSelectedPois(List<BBPoi> selectedPois) {
        this.selectedPois = selectedPois;

        isFindingPoiIcons = true;

        GetIconImageAsync getIconImageAsync = new GetIconImageAsync();
        getIconImageAsync.delegate = this;
        ApiManager.getInstance().getIconImage(this, getIconImageAsync, selectedPois);
    }

    public void setNewCurrentPlace(BBPlace newCurrentPlace) {
        updateToolbarTitle(newCurrentPlace);

        progressBar.setVisibility(View.VISIBLE);

        getSupportFragmentManager().popBackStack();

        //Initiate Fetch Specific Place
        GetSpecificPlaceAsync getSpecificPlaceAsync = new GetSpecificPlaceAsync();
        getSpecificPlaceAsync.delegate = this;
        ApiManager.getInstance().fetchSpecificPlaceAsync(getSpecificPlaceAsync, String.valueOf(newCurrentPlace.getId()));
    }

    private void updateToolbarTitle(BBPlace newCurrentPlace) {
        toolbarSubtitle.setText(newCurrentPlace.getName());
        if(newCurrentPlace.getFloors() != null && newCurrentPlace.getFloors().size() != 0) {
            toolbarTitle.setText(newCurrentPlace.getFloors().get(BeaconBaconManager.getInstance().getCurrentFloorIndex()).getName());
        } else {
            toolbarTitle.setText("-");
        }

        updateArrows(0);
    }
    //endregion



    //region Async Tasks Finished
    //---Images
    @Override
    public void floorImageAsyncFinished(Bitmap bitmap) {
        isFindingFloorImage = false;

        currentFloorImage = bitmap;

        if(!isFindingBook)
            isLocatingFindTheBookFloor = false;

        updateMapView(true, false, null);
    }

    @Override
    public void iconImageAsyncFinished(List<CustomPoiView> customPoiViews) {
        isFindingPoiIcons = false;
        updateMapView(false, true, customPoiViews);
    }
    //---

    @Override
    public void specificPlaceAsyncFinished(final JsonObject output) {
        isFindingSpecificPlace = false;

        if(output != null) {
            final Handler handler = new Handler();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if(!isFindingBook) {
                        //Get the Place from JsonObject output
                        JsonElement mJson = new JsonParser().parse(output.toString());
                        BBPlace place = new Gson().fromJson(mJson, BBPlace.class);

                        updatePlace(place);
                        updateMenuOverview(place);

                        if(BeaconBaconManager.getInstance().getRequestObject() != null)
                            checkIfBookWasFound();
                    } else {
                        handler.postDelayed(this, 100);
                    }
                }
            };
            handler.postDelayed(runnable, 100);
        } else {
            //No such place found, open place selection
            openPlaceSelectionFragment();
        }
    }

    private void checkIfBookWasFound() {
        if (!bookWasFound) {
            showAlert(getString(R.string.alert_title_find_book), String.format(getString(R.string.alert_message_find_book), BeaconBaconManager.getInstance().getRequestObject().getTitle(), BeaconBaconManager.getInstance().getCurrentPlace().getName()));
            hideFindTheBookElements();
        }
    }

    private void hideFindTheBookElements() {
        fabFindTheBook.setVisibility(View.GONE);
        if(snackbar != null) {
            snackbar.getView().setVisibility(View.GONE);
            snackbar.dismiss();
        }
        mapHolderView.setFindTheBook(null, null);
    }

    @Override
    public void menuOverviewAsyncFinished(Bundle output) {
        int placeId = Integer.valueOf(output.getString("place_id"));

        Gson gson = new Gson();
        String jsonOutput = output.getString("json");
        Type listType = new TypeToken<List<BBPoiMenuItem>>(){}.getType();
        List<BBPoiMenuItem> menuItems = gson.fromJson(jsonOutput, listType);

        //Sort the Place's floors by Order
        if(menuItems != null) {
            Collections.sort(menuItems, new Comparator<BBPoiMenuItem>() {
                @Override
                public int compare(BBPoiMenuItem order1, BBPoiMenuItem order2) {
                    return order1.getOrder() - order2.getOrder();
                }
            });
        }

        //Find the place and update the POI Menu items
        for(int i = 0; i< BeaconBaconManager.getInstance().getAllPlaces().getData().size(); i++) {
            if(placeId == BeaconBaconManager.getInstance().getAllPlaces().getData().get(i).getId()) {
                BeaconBaconManager.getInstance().getAllPlaces().getData().get(i).setPoiMenuItem(menuItems);
            }
        }
    }

    @Override
    public void findTheBookAsyncFinished(JsonObject output) {
        if(output != null) {
            JsonElement mJson = new JsonParser().parse(output.toString());
            try {
                BBResponseObject responseObject = new Gson().fromJson(mJson, BBResponseObject.class);

                if(responseObject != null && responseObject.getStatus().equals("Found")) {
                    bookWasFound = true;


                    int maxDistLocations = 200;
                    //TODO Temp data
//                    List<BBFaustDataObject> list = new ArrayList<>();
//
//                    BBFaustDataObject tempDataObject = new BBFaustDataObject();
//
//                    BBFaustDataLocation tempDataLocation = new BBFaustDataLocation();
//                    tempDataLocation.setId(1620);
//                    tempDataLocation.setArea("");
//                    tempDataLocation.setType("point");
//                    tempDataLocation.setPosX(1132);
//                    tempDataLocation.setPosY(1258);
//
//                    BBFaustDataFloor tempDataFloor = new BBFaustDataFloor();
//                    tempDataFloor.setId(3);
//                    tempDataFloor.setMap_pixel_to_centimeter_ratio("0.4");
//
//                    tempDataObject.setFloor(tempDataFloor);
//                    tempDataObject.setLocation(tempDataLocation);
//
//
//                    BBFaustDataObject tempDataObject2 = new BBFaustDataObject();
//
//                    BBFaustDataLocation tempDataLocation2 = new BBFaustDataLocation();
//                    tempDataLocation2.setId(1620);
//                    tempDataLocation2.setArea("");
//                    tempDataLocation2.setType("point");
//                    tempDataLocation2.setPosX(1132);
//                    tempDataLocation2.setPosY(1258);
//
//                    BBFaustDataFloor tempDataFloor2 = new BBFaustDataFloor();
//                    tempDataFloor2.setId(3);
//                    tempDataFloor2.setMap_pixel_to_centimeter_ratio("0.4");
//
//                    tempDataObject2.setFloor(tempDataFloor2);
//                    tempDataObject2.setLocation(tempDataLocation2);
//
//
//                    list.add(tempDataObject);
//                    list.add(tempDataObject2);
//
//                    BBResponseObject bbResponseObject = responseObject;
//                    bbResponseObject.setData(list);
//                    //TODO end temp data
//
//                    int maxDistLocations = 200;
//
//                    if (bbResponseObject.getData() == null || bbResponseObject.getData().size() == 0) {
//                        bbResponseObject.setDisplayType(DisplayType.NONE);
//                    } else if (bbResponseObject.getData().size() == 1) {
//                        bbResponseObject.setDisplayType(DisplayType.SINGLE);
//                    } else {
//                        BBFaustDataObject dataObject1 = bbResponseObject.getData().get(0);
//                        BBFaustDataObject dataObject2 = bbResponseObject.getData().get(1);
//
//                        if (dataObject1.getFloor().getId() != dataObject2.getFloor().getId()) {
//                            bbResponseObject.setDisplayType(DisplayType.SINGLE);
//                            BeaconBaconManager.getInstance().setResponseObject(bbResponseObject);
//                            return;
//                        } else {
//                            Point point1 = new Point(dataObject1.getLocation().getPosX(), dataObject1.getLocation().getPosY());
//                            Point point2 = new Point(dataObject2.getLocation().getPosX(), dataObject2.getLocation().getPosY());
//
//                            double distance = Math.sqrt(Math.pow(point2.x - point1.x, 2) + Math.pow(point2.y - point1.y, 2)) * Double.valueOf(bbResponseObject.getData().get(0).getFloor().getMap_pixel_to_centimeter_ratio());
//
//                            if (distance > 400) {
//                                bbResponseObject.setDisplayType(DisplayType.SINGLE);
//                                BeaconBaconManager.getInstance().setResponseObject(bbResponseObject);
//                                return;
//                            } else {
//                                bbResponseObject.setDisplayType(DisplayType.CLUSTER);
//                                bbResponseObject.setRadius((int) Math.max(maxDistLocations, distance) + 100);
//                            }
//                        }
//                    }


                    if(responseObject.getData() == null || responseObject.getData().size() == 0) {
                        responseObject.setDisplayType(DisplayType.NONE);
                    } else if(responseObject.getData().size() == 1) {
                        responseObject.setDisplayType(DisplayType.SINGLE);
                    } else {
                        BBFaustDataObject dataObject1 = responseObject.getData().get(0);
                        BBFaustDataObject dataObject2 = responseObject.getData().get(1);

                        if (dataObject1.getFloor().getId() != dataObject2.getFloor().getId()) {
                            responseObject.setDisplayType(DisplayType.SINGLE);
                            BeaconBaconManager.getInstance().setResponseObject(responseObject);
                            return;
                        } else {
                            Point point1 = new Point(dataObject1.getLocation().getPosX(), dataObject1.getLocation().getPosY());
                            Point point2 = new Point(dataObject2.getLocation().getPosX(), dataObject2.getLocation().getPosY());

                            double distance = Math.sqrt(Math.pow(point2.x - point1.x, 2) + Math.pow(point2.y - point1.y, 2)) * Double.valueOf(responseObject.getData().get(0).getFloor().getMap_pixel_to_centimeter_ratio());

                            if (distance > 400) {
                                responseObject.setDisplayType(DisplayType.SINGLE);
                                BeaconBaconManager.getInstance().setResponseObject(responseObject);
                                return;
                            } else {
                                responseObject.setDisplayType(DisplayType.CLUSTER);
                                responseObject.setRadius((int) Math.max(maxDistLocations, distance) + 100);
                            }
                        }
                    }

                    BeaconBaconManager.getInstance().setResponseObject(responseObject);

                    showFindTheBookSnackbar();
                } else {
                    bookWasFound = false;
                    hideFindTheBookElements();
                }
            } catch (Exception e) {
                bookWasFound = false;
            }
        } else {
            bookWasFound = false;
        }
        isFindingBook = false;
    }
    //endregion



    //region Misc
    private void showAlert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.general_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //If we want to do something when user dismisses dialog
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    //endregion
}
