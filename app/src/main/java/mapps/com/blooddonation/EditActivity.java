package mapps.com.blooddonation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EditActivity extends AppCompatActivity implements OnMapReadyCallback {

    //=====UI variables=====
    LinearLayout registerGroup;
    MapView mapView;
    ScrollView scrollView;
    EditText emailEdit, passwordEdit, confirmPassword, name, address, mobile_number;
    Spinner blood_type;
    TextView nameLabel, emailLabel, passwordLabel, addressLabel, mobileNumberLabel, mapLabel;
    ProgressBar progressBar, toolProgressBar;


    GoogleMap map;
    LatLng location;
    String email, password;
    boolean register;
    RequestQueue queue;
    List<Pair<String, String>> keyLabels = new ArrayList<>();
    TextView labelViews[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        Toolbar toolbar = findViewById(R.id.form_toolbar);
        setSupportActionBar(toolbar);

        name = findViewById(R.id.form_name);
        address = findViewById(R.id.form_address);
        mobile_number = findViewById(R.id.form_mobile_number);
        blood_type = findViewById(R.id.form_blood_type);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        scrollView = findViewById(R.id.scrollView);
        toolProgressBar = findViewById(R.id.toolbar_progress_bar);
        progressBar = findViewById(R.id.edit_progress);

        nameLabel = findViewById(R.id.name_label);
        addressLabel = findViewById(R.id.address_label);
        emailLabel = findViewById(R.id.email_label);
        passwordLabel = findViewById(R.id.password_label);
        mobileNumberLabel = findViewById(R.id.mobile_number_label);
        mapLabel = findViewById(R.id.map_label);

        queue = Volley.newRequestQueue(this);

        Bundle data = getIntent().getExtras();

        register = data == null;

        if (register) {
            registerGroup = findViewById(R.id.reg_group);
            registerGroup.setVisibility(View.VISIBLE);
            emailEdit = findViewById(R.id.email);
            passwordEdit = findViewById(R.id.password);
            confirmPassword = findViewById(R.id.cpassword);

            progressBar.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);
            mapView.getMapAsync(EditActivity.this);
        } else {
            email = data.getString("email");
            password = data.getString("password");

            getUserData();
        }

        keyLabels.add(new Pair<>("name", "Name"));
        keyLabels.add(new Pair<>("email", "Email"));
        keyLabels.add(new Pair<>("password2", "Password"));
        keyLabels.add(new Pair<>("mobile_number", "Mobile Number"));
        keyLabels.add(new Pair<>("address", "Address"));
        keyLabels.add(new Pair<>("map", "Select location by long clicking on the map"));
        labelViews = new TextView[]{nameLabel, emailLabel, passwordLabel, mobileNumberLabel, addressLabel, mapLabel};
    }

    void getUserData() {
        try {
            JSONObject data = new JSONObject();
            data.put("email", email);
            data.put("password", password);

            JSONArray arData = new JSONArray();
            arData.put(data);

            JsonArrayRequest request = new JsonArrayRequest(Request.Method.POST, Helper.apiURL + "get_data", arData, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    scrollView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    try {
                        JSONObject dataResponse = response.getJSONObject(0);
                        if (dataResponse.getString("status").equals("success")) {
                            name.setText(dataResponse.getString("name"));
                            address.setText(dataResponse.getString("address"));
                            mobile_number.setText(dataResponse.getString("mobile_number"));

                            blood_type.setSelection(dataResponse.getInt("blood_type"));
                            location = new LatLng(dataResponse.getDouble("latitude"), dataResponse.getDouble("longitude"));

                            mapView.getMapAsync(EditActivity.this);
                        } else {
                            Toast.makeText(getBaseContext(), "Invalid request, Restart the app and try again", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
                        scrollView.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.GONE);
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    scrollView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getBaseContext(), "Error, check internet connection", Toast.LENGTH_LONG).show();
                }
            });

            queue.add(request);
        } catch (JSONException e) {
            Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
            scrollView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }

    public void submit(View view) {
        Helper.clearErrors(keyLabels, labelViews);

        if (location == null) {
            mapLabel.setText(Html.fromHtml("Select location by long clicking on the map<br>" +
                    "<font color='red'>Location is required</font>"), TextView.BufferType.SPANNABLE);
            progressBar.setVisibility(View.GONE);
            return;
        }

        String url = Helper.apiURL + "register_edit_user";

        if (register) {
            email = emailEdit.getText().toString().trim();
            password = passwordEdit.getText().toString();
            String confirmPasswordVal = confirmPassword.getText().toString();
            if (!password.equals(confirmPasswordVal)) {
                Toast.makeText(getBaseContext(), "Password and Confirm password does not match", Toast.LENGTH_LONG).show();
                return;
            }
        }

        toolProgressBar.setVisibility(View.VISIBLE);
        JSONObject data = new JSONObject();
        try {
            data.put("register", register);
            data.put("email", email);
            data.put("password", password);
            data.put("name", name.getText().toString());
            data.put("address", address.getText().toString());
            data.put("mobile_number", mobile_number.getText().toString());
            data.put("blood_type", blood_type.getSelectedItemPosition());
            data.put("latitude", location.latitude);
            data.put("longitude", location.longitude);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    toolProgressBar.setVisibility(View.GONE);
                    try {
                        if (response.getBoolean("status")) {
                            if (register) {
                                Toast.makeText(getBaseContext(), "Registration success, Activation email sent.", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getBaseContext(), "Changes saved.", Toast.LENGTH_LONG).show();
                            }
                            finish();
                        } else {
                            Helper.handleErrors(response, keyLabels, labelViews);
                        }
                    } catch (JSONException e) {
                        Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    toolProgressBar.setVisibility(View.GONE);
                    Toast.makeText(getBaseContext(), "Error, check internet connection", Toast.LENGTH_LONG).show();
                }
            });

            queue.add(request);
        } catch (JSONException e) {
            Toast.makeText(getBaseContext(), "Unexpected error occurred, code: 1", Toast.LENGTH_LONG).show();
            toolProgressBar.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);

        map.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                scrollView.requestDisallowInterceptTouchEvent(true);
            }
        });

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                scrollView.requestDisallowInterceptTouchEvent(false);
            }
        });

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                location = latLng;
                map.clear();
                map.addMarker(new MarkerOptions().position(latLng));
            }
        });

        if (!register) {
            map.addMarker(new MarkerOptions().position(location));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 12f));
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            return;
        }

        if (register) {
            FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location loc) {
                            if (loc != null) {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 12f));
                            } else {
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(20.5937, 78.9629), 3.5f));
                            }
                        }
                    });
        }

        map.setMyLocationEnabled(true);
    }

    @Override
    protected void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
