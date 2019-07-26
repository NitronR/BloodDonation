package mapps.com.blooddonation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //=====UI variables=====
    // View variables
    private ListView dataListView;
    private TextView noDataView, sortedBy;
    private ProgressBar progressBar;
    private LinearLayout filterBar, filters;
    private NavigationView navigationView;
    private RelativeLayout relativeLayout_global, relativeLayout_drawer;

    // View data variables
    private static String filter[] = {"", "", "-1", ""};
    private ObjectAnimator objAnim;

    //=====Data Variables=====
    Location location;
    List<Data> dataItems;
    boolean gettingData = false;

    RequestQueue queue;

    String email = null;
    String password = null;

    private int LOGIN_REQUEST = 0, CHANGE_PASS_REQUEST = 1;
    String bloodTypes[] = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};

    final String VERSION = "0.1.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        dataListView = findViewById(R.id.data_list);
        noDataView = findViewById(R.id.no_data);
        progressBar = findViewById(R.id.progressBar2);
        filterBar = findViewById(R.id.filter_bar);
        filters = findViewById(R.id.filters);
        sortedBy = findViewById(R.id.sorted_by);
        sortedBy.bringToFront();

        queue = Volley.newRequestQueue(this);

        // Feature discovery when launched for the first time
        SharedPreferences sp = getSharedPreferences("sp", MODE_PRIVATE);
        if (!sp.contains("first_done")) {
            SharedPreferences.Editor editor = sp.edit();
            editor.putBoolean("first_done", true);
            editor.apply();

            Handler handler = new Handler();

            relativeLayout_global = findViewById(R.id.relativeLayout_global);
            relativeLayout_global.post(new Runnable() {
                @Override
                public void run() {
                    enterReveal(relativeLayout_global);
                }
            });

            relativeLayout_drawer = findViewById(R.id.relativeLayout_drawer);
            relativeLayout_drawer.post(new Runnable() {
                @Override
                public void run() {
                    enterReveal(relativeLayout_drawer);
                }
            });


            RelativeLayout relativeLayout_drawerAnimated = findViewById(R.id.relativeLayout_drawerAnimated);
            pulseAnimation(relativeLayout_drawerAnimated);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    objAnim.end();
                    exitReveal(relativeLayout_drawer);
                    exitReveal(relativeLayout_global);
                }
            }, 3000);
        }

        // Check for location permission
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If don't have location permission, request permission and get data sorted by name
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            preGetData(false);
        } else {
            // If have permission, get location then get data sorted by nearest
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location loc) {
                            if (loc != null) {
                                location = loc;
                                filter[3] = "1";
                            }
                            preGetData(false);
                        }
                    });
        }
    }

    void checkFilters() {
        // Updates filter bar according to filters array
        filters.removeAllViews();

        if (filter[2].equals("-1")) {
            filter[2] = "";
        }

        String[] labels = {"Name: ", "Address: ", "Type: "};

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(15, 15, 15, 15);
        for (int i = 0; i < 3; i++) {
            if (!filter[i].trim().equals("")) {
                @SuppressLint("InflateParams") TextView filterView = (TextView) getLayoutInflater().inflate(R.layout.filter_view, null);
                filterView.setLayoutParams(layoutParams);
                filterView.setText(labels[i] + (i == 2 ? bloodTypes[Integer.parseInt(filter[i])] : filter[i]));
                filters.addView(filterView);
            }
        }

        if (filter[2].equals("")) {
            filter[2] = "-1";
        }

        if (filters.getChildCount() > 0)
            filterBar.setVisibility(View.VISIBLE);
        else
            filterBar.setVisibility(View.GONE);
    }

    void preGetData(boolean search) {
        // prepare for getting data
        if (!search && gettingData)
            return;

        if (!Helper.isConnected(this)) {
            noDataView.setText("Not connected to the internet.");
            noDataView.setVisibility(View.VISIBLE);
            return;
        }
        gettingData = true;
        checkFilters();
        sortedBy.setVisibility(View.VISIBLE);
        sortedBy.setText("Sorted By: " + (filter[3].equals("") ? ("Name") : ("Nearest")));
        dataListView.setVisibility(View.GONE);
        noDataView.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        getData();
    }

    public void getData() {
        // gets data according to filters
        final String url = Helper.apiURL + "get_data";
        final JSONArray arData = new JSONArray();
        final JSONObject data = new JSONObject();
        try {
            JSONArray criteria = new JSONArray();
            for (int i = 0; i < 4; i++)
                criteria.put(filter[i]);
            data.put("criteria", criteria);

            if (filter[3].equals("1") && location != null) {
                data.put("latitude", location.getLatitude());
                data.put("longitude", location.getLongitude());
            }

            arData.put(data);

            JsonArrayRequest dataRequest = new JsonArrayRequest(Request.Method.POST, url, arData, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    postGetData();
                    try {
                        JSONObject version = response.getJSONObject(0);

                        if (version.getString("version").equals(VERSION)) {
                            response.remove(0);
                            setData(response);
                        } else {
                            UpdateDialog updateDialog = new UpdateDialog();
                            updateDialog.show(getFragmentManager(), "updateDialog");
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }

            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    postGetData();
                    Toast.makeText(getBaseContext(), "Error, check internet connection", Toast.LENGTH_LONG).show();
                }
            });
            queue.add(dataRequest);
        } catch (JSONException e) {
            Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
            postGetData();
            e.printStackTrace();
        }
    }

    void postGetData() {
        // executed after getData
        gettingData = false;
        progressBar.setVisibility(View.GONE);
    }

    void setData(JSONArray items) throws JSONException {
        // apply data
        if (items.length() == 0) {
            noDataView.setVisibility(View.VISIBLE);
            return;
        } else
            noDataView.setVisibility(View.GONE);

        dataItems = new ArrayList<>();

        JSONObject item;
        for (int i = 0; i < items.length(); i++) {
            item = items.getJSONObject(i);
            dataItems.add(new Data(item.getLong("id"),
                    bloodTypes[item.getInt("blood_type")],
                    item.getString("name"),
                    item.getString("address"),
                    item.getString("email"),
                    item.getString("mobile_number"),
                    Float.parseFloat(item.getString("latitude")),
                    Float.parseFloat(item.getString("longitude"))));
        }

        dataListView.setVisibility(View.VISIBLE);
        dataListView.setAdapter(new DataAdapter(getBaseContext(), dataItems));
    }

    public void clearFilters(View view) {
        filter[0] = "";
        filter[1] = "";
        filter[2] = "-1";
        preGetData(true);
    }

    class DataAdapter extends BaseAdapter {
        private Context context;
        private LayoutInflater inflater;
        private List<Data> items;

        DataAdapter(Context context, List<Data> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return items.get(i).getId();
        }

        @SuppressLint({"SetTextI18n", "InflateParams"})
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (inflater == null)
                inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            if (view == null)
                view = inflater != null ? inflater.inflate(R.layout.list_item, null) : null;

            final Data d = items.get(i);

            if (view != null) {
                TextView item_title = view.findViewById(R.id.item_title),
                        content = view.findViewById(R.id.content);

                final Button navigate = view.findViewById(R.id.navigate);

                navigate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Uri navUri = Uri.parse("google.navigation:q=" + String.valueOf(d.getLatitude()) + "," + String.valueOf(d.getLongitude()));
                        Intent intent = new Intent(Intent.ACTION_VIEW, navUri);
                        intent.setPackage("com.google.android.apps.maps");
                        startActivity(intent);
                    }
                });

                item_title.setText("Blood type: " + d.getBlood_type());
                content.setText(Html.fromHtml("<b>Name:</b> " + d.getName() + "<br>" +
                        "<b>Mobile Number:</b> " + d.getMobile_number() + "<br>" +
                        "<b>Address:</b> " + d.getAddress() + "<br>" +
                        "<b>Email:</b> " + d.getEmail()), TextView.BufferType.SPANNABLE);
            }
            return view;
        }
    }

    @Override
    protected void onResume() {
        updateNavDrawer();
        preGetData(false);
        super.onResume();
    }

    void updateNavDrawer() {
        // update navigation drawer based on whether user is logged in or not
        boolean loggedIn = email != null;
        navigationView.getMenu().findItem(R.id.nav_login).setVisible(!loggedIn);
        navigationView.getMenu().findItem(R.id.nav_register).setVisible(!loggedIn);
        navigationView.getMenu().findItem(R.id.nav_edit_profile).setVisible(loggedIn);
        navigationView.getMenu().findItem(R.id.nav_change_password).setVisible(loggedIn);
        navigationView.getMenu().findItem(R.id.nav_logout).setVisible(loggedIn);

        if (loggedIn) {
            TextView navEmail = findViewById(R.id.nav_email);
            navEmail.setText(email);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_filter) {
            FilterDialog filterDialog = new FilterDialog();
            filterDialog.show(getFragmentManager(), "filter");
            return true;
        } else if (id == R.id.action_refresh) {
            preGetData(false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class FilterDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            @SuppressLint("InflateParams") final View dialogView = inflater.inflate(R.layout.filter_dialog, null);
            final TextView filterName = dialogView.findViewById(R.id.filter_name),
                    filterAddress = dialogView.findViewById(R.id.filter_address);
            final Spinner filterBloodType = dialogView.findViewById(R.id.filter_blood_type),
                    filterSortBy = dialogView.findViewById(R.id.sort_by);

            filterName.setText(filter[0]);
            filterAddress.setText(filter[1]);
            filterBloodType.setSelection(Integer.parseInt(filter[2]) + 1);

            builder.setTitle("Filter by")
                    .setView(dialogView)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            filter[0] = filterName.getText().toString();
                            filter[1] = filterAddress.getText().toString();
                            filter[2] = String.valueOf(filterBloodType.getSelectedItemPosition() - 1);

                            if (filterSortBy.getSelectedItemPosition() == 0) {
                                if (ActivityCompat.checkSelfPermission(getActivity().getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity().getBaseContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    Toast.makeText(getActivity().getBaseContext(), "Enable location permission for this app in the settings to sort by location.", Toast.LENGTH_LONG).show();
                                    filter[3] = "";
                                } else {
                                    filter[3] = "1";
                                }
                            } else {
                                filter[3] = "";
                            }
                            ((MainActivity) getActivity()).preGetData(true);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FilterDialog.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }
    }

    public static class UpdateDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle("Update Available")
                    .setMessage("Please get the latest version of the app to continue.")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            getActivity().finish();
                        }
                    });
            return builder.create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            getActivity().finish();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_login) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivityForResult(intent, LOGIN_REQUEST);
        } else if (id == R.id.nav_register) {
            Intent intent = new Intent(MainActivity.this, EditActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_edit_profile) {
            Intent intent = new Intent(MainActivity.this, EditActivity.class);
            intent.putExtra("email", email);
            intent.putExtra("password", password);
            startActivity(intent);
        } else if (id == R.id.nav_change_password) {
            Intent intent = new Intent(getBaseContext(), LoginActivity.class);
            intent.putExtra("change_password", true);
            startActivityForResult(intent, CHANGE_PASS_REQUEST);
        } else if (id == R.id.nav_logout) {
            email = null;
            password = null;
            updateNavDrawer();
            Toast.makeText(getBaseContext(), "Logout successful.", Toast.LENGTH_LONG).show();
            preGetData(true);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(getBaseContext(), AboutActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == LOGIN_REQUEST) {
                Bundle d = data.getExtras();
                if (d != null) {
                    email = d.getString("email");
                    password = d.getString("password");
                    updateNavDrawer();
                }
            } else if (requestCode == CHANGE_PASS_REQUEST) {
                Bundle d = data.getExtras();
                if (d != null) {
                    password = d.getString("password");
                }
            }
        }
    }

    private void enterReveal(View view) {

        // get the center for the clipping circle
        int cx = view.getMeasuredWidth() / 2;
        int cy = view.getMeasuredHeight() / 2;

        // get the final radius for the clipping circle
        int finalRadius = Math.max(view.getWidth(), view.getHeight()) / 2;

        // create the animator for this view (the start radius is zero)
        Animator anim =
                io.codetail.animation.ViewAnimationUtils.createCircularReveal(view, cx, cy, 0, finalRadius);

        // make the view visible and start the animation
        view.setVisibility(View.VISIBLE);
        anim.start();
    }

    private void exitReveal(final View view) {

        // get the center for the clipping circle
        int cx = view.getMeasuredWidth() / 2;
        int cy = view.getMeasuredHeight() / 2;

        // get the initial radius for the clipping circle
        int initialRadius = view.getWidth() / 2;

        // create the animation (the final radius is zero)
        Animator anim =
                io.codetail.animation.ViewAnimationUtils.createCircularReveal(view, cx, cy, initialRadius, 0);

        // make the view invisible when the animation is done
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                view.setVisibility(View.GONE);
            }
        });

        // start the animation
        anim.start();
    }

    private void pulseAnimation(View view) {
        objAnim = ObjectAnimator.ofPropertyValuesHolder(view, PropertyValuesHolder.ofFloat("scaleX", 1.8f),
                PropertyValuesHolder.ofFloat("scaleY", 1.8f),
                PropertyValuesHolder.ofFloat("alpha", 0f));
        objAnim.setDuration(1000);
        objAnim.setRepeatCount(ObjectAnimator.INFINITE);
        objAnim.setRepeatMode(ObjectAnimator.RESTART);
        objAnim.start();
    }
}

class Data implements Serializable {
    private long id;
    private String blood_type;
    private String name;
    private String address;
    private String email;
    private String mobile_number;
    private float latitude, longitude;

    Data(long id, String blood_type, String name, String address, String email, String mobile_number, float latitude, float longitude) {
        this.id = id;
        this.blood_type = blood_type;
        this.name = name;
        this.address = address;
        this.email = email;
        this.mobile_number = mobile_number;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public long getId() {
        return id;
    }

    String getBlood_type() {
        return blood_type;
    }

    String getName() {
        return name;
    }

    String getAddress() {
        return address;
    }

    String getEmail() {
        return email;
    }

    String getMobile_number() {
        return mobile_number;
    }

    float getLatitude() {
        return latitude;
    }

    float getLongitude() {
        return longitude;
    }
}
