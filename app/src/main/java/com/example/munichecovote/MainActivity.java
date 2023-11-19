package com.example.munichecovote;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.MapStyleOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

//    private static final int REQUEST_CODE_SECOND_ACTIVITY = 1;
    private ActivityResultLauncher<Intent> startSecondActivityLauncher;

    private GoogleMap mMap;
    final int DEFAULT_ZOOM = 11;
    final List<Marker> markerList = new ArrayList<>();
    final int showMarkerLevel = 8;

    private LocationManager locationManager;
    private Button mAddVoteButton;
    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;
    private static final String TAG = MainActivity.class.getSimpleName();

    final int ICON_SIZE_NORMAL = 60;
    final int ICON_SIZE_PROPOSAL = 140;
    final int COLOR_GOOD = Color.parseColor("#92D8A1");
    final int COLOR_BAD = Color.parseColor("#FF9FAF");

    private Dialog LAST_DIALOG;
    private String PROPOSAL_TYPE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.maps);
        mapFragment.getMapAsync(this);

        // Add button.
        mAddVoteButton = findViewById(R.id.addVoteButton);
        LAST_DIALOG = new Dialog(MainActivity.this);
        LAST_DIALOG.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        mAddVoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LAST_DIALOG.setContentView(R.layout.add_vote);
                LAST_DIALOG.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                LAST_DIALOG.setCancelable(false);
                LAST_DIALOG.getWindow().getAttributes().windowAnimations = R.style.animation;
                LAST_DIALOG.show();
                LAST_DIALOG.setCanceledOnTouchOutside(true);

                Button cityHero = LAST_DIALOG.findViewById(R.id.buttonAddVote);
                Spinner spinnerType = LAST_DIALOG.findViewById(R.id.spinnerAddVote);
                Switch switchCurrLocation = LAST_DIALOG.findViewById(R.id.switchAddVote);
                cityHero.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PROPOSAL_TYPE = getTypeString(spinnerType);
                        if (switchCurrLocation.isChecked()) {
                            Bitmap icon = proposalTypeToBitmap((String) spinnerType.getSelectedItem());
//                            JSONObject json = fileToJSON(R.raw.well_new);
                            LatLng latLng = new LatLng(48.262535675778373, 11.6686241787109);
                            JSONObject json = proposalToJSON(latLng);

                            putGeoJSONtoMap(json, icon, ICON_SIZE_PROPOSAL, "type", true, 9997);
                            LAST_DIALOG.dismiss();

                            LayoutInflater inflater = getLayoutInflater();
                            View view = inflater.inflate(R.layout.toast,
                                    (ViewGroup)findViewById(R.id.toastLayout));

                            Toast toast = new Toast(getBaseContext());
                            toast.setView(view);
                            toast.show();

//                            Toast.makeText(MainActivity.this, "You are a city hero!", Toast.LENGTH_SHORT).show();
                        } else {
                            LAST_DIALOG.dismiss();
                            Toast.makeText(MainActivity.this, "Please choose a selection for your proposal", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(MainActivity.this, ChooseLocationActivity.class);
                            startSecondActivityLauncher.launch(intent);
                        }
                    }
                });

            }
        });

        // Request permissions.
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }else{
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);


        startSecondActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            String receivedData = data.getStringExtra("latLng");
                            // Handle the received data here
                            LatLng latLng = parseLocation(receivedData);
                            JSONObject newJson = proposalToJSON(latLng);
                            Spinner spinnerType = LAST_DIALOG.findViewById(R.id.spinnerAddVote);
                            String proposalType = getTypeString(spinnerType);
                            Bitmap icon = proposalTypeToBitmap(proposalType);
                            putGeoJSONtoMap(newJson, icon, ICON_SIZE_PROPOSAL, "type", true, 9996);

                            LayoutInflater inflater = getLayoutInflater();
                            View view = inflater.inflate(R.layout.toast,
                                    (ViewGroup)findViewById(R.id.toastLayout));

                            Toast toast = new Toast(getBaseContext());
                            toast.setView(view);
                            toast.show();
                        }
                    }
                }
        );
    }

    private LatLng parseLocation(String loc) {
        Log.d("parseLocIn", loc);

        Pattern pattern = Pattern.compile("(\\d+[.]\\d+),(\\d+[.]\\d+)");
        Matcher matcher = pattern.matcher(loc);

        if (matcher.find()) {
            String matchLat = matcher.group(1);
            String matchLng = matcher.group(2);
            Log.d("regexMatchLat", "Entire match: " + matchLat);
            Log.d("regexMatchLng", "Entire match: " + matchLng);
            return new LatLng(Double.parseDouble(matchLat), Double.parseDouble(matchLng));
        } else {
            return new LatLng(0.0, 0.0);    // TODO
        }
    }

    private JSONObject proposalToJSON(LatLng latLng) {
        Spinner spinnerType = LAST_DIALOG.findViewById(R.id.spinnerAddVote);
        String proposalType = getTypeString(spinnerType);
        EditText textDescr = LAST_DIALOG.findViewById(R.id.editDescription);
        Log.d("debugText", textDescr.getClass().toString());
        String description = textDescr.getText().toString();

        try {
            JSONObject geom = new JSONObject();
            List<Double> latLngList = new ArrayList<>();
            latLngList.add(latLng.longitude);
            latLngList.add(latLng.latitude);
            JSONArray coords = new JSONArray(latLngList);
            geom.put("type", "Point");
            geom.put("coordinates", coords);

            JSONObject props = new JSONObject();
            props.put("id", 1);
            props.put("type", proposalType);
            props.put("votes", 1);
            props.put("stars", 3);
            props.put("description", description);

            JSONObject feature = new JSONObject();
            feature.put("type", "Feature");
            feature.put("properties", props);
            feature.put("geometry", geom);

            JSONArray features = new JSONArray();
            features.put(feature);

            JSONObject json = new JSONObject();
            json.put("type", "FeatureCollection");
            json.put("name", "name");   // TODO optional
            JSONObject crs = new JSONObject();
            crs.put("type", "name");
            JSONObject crs_props = new JSONObject();
            crs_props.put("name", "urn:ogc:def:crs:OGC:1.3:CRS84");
            crs.put("properties", crs_props);
            json.put("crs", crs);
            json.put("features", features);

            return json;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getTypeString(Spinner spinner) {
        String type = (String) spinner.getSelectedItem();
        Pattern pattern = Pattern.compile("([A-Za-z ]+)");
        Matcher matcher = pattern.matcher(type);
        String proposalTypeMatched;

        if (matcher.find()) {
            Log.d("regexFind", "found");
            proposalTypeMatched = matcher.group();
            Log.d("regexMatch", "Entire match: " + proposalTypeMatched);
        } else {
            proposalTypeMatched = type;
        }

        return proposalTypeMatched.trim().toLowerCase();
    }
    private String getTypeString(String type) {
        Pattern pattern = Pattern.compile("([A-Za-z ]+)");
        Matcher matcher = pattern.matcher(type);
        String proposalTypeMatched;

        if (matcher.find()) {
            Log.d("regexFind", "found");
            proposalTypeMatched = matcher.group();
            Log.d("regexMatch", "Entire match: " + proposalTypeMatched);
        } else {
            proposalTypeMatched = type;
        }

        return proposalTypeMatched.trim().toLowerCase();
    }

    private Bitmap proposalTypeToBitmap(String type) {
        int id = -1;
        Bitmap icon;

        String proposalType = getTypeString(type);

        switch (proposalType) {
            case "water fountain":
                id = R.drawable.waterglass_blue;
                break;
            case "city green up":
                id = R.drawable.tree;
                break;
            case "public roofing":
                id = R.drawable.umbrella;
                break;
            default:
                return getDonutIcon(25, 15, Color.CYAN);
        }
        icon = BitmapFactory.decodeResource(getResources(), id);
        return icon;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        LatLng center = new LatLng(48.13789, 11.577);
        mMap = googleMap;

        mMap.setOnCameraMoveListener(() -> {
            CameraPosition cameraPosition = mMap.getCameraPosition();
            for(Marker m:markerList){
                m.setVisible(cameraPosition.zoom > showMarkerLevel);
            }
        });

        CustomOnClickListener CustomOnClickListener = new CustomOnClickListener(this);
        mMap.setOnMarkerClickListener(CustomOnClickListener);

        // show all wells
        JSONObject json = fileToJSON(R.raw.brunnen_epsg4326);
        Bitmap well_icon = BitmapFactory.decodeResource(getResources(), R.drawable.waterglass_blue);
        putGeoJSONtoMap(json, well_icon, ICON_SIZE_NORMAL,"bezeichnung", false, 9998);

        // show all well proposals
        JSONObject json_prop = fileToJSON(R.raw.brunnen_proposal_epsg4326);
        Bitmap well_prop_icon = BitmapFactory.decodeResource(getResources(), R.drawable.waterglass_blue);
        putGeoJSONtoMap(json_prop, well_prop_icon, ICON_SIZE_PROPOSAL, "type", true, 9999);

        // show all shelter proposals
        JSONObject json_prop_sh = fileToJSON(R.raw.shelter_proposals);
        Bitmap shelt_prop_icon = BitmapFactory.decodeResource(getResources(), R.drawable.umbrella);
        putGeoJSONtoMap(json_prop_sh, shelt_prop_icon, ICON_SIZE_PROPOSAL, "type", true, 9999);

        // show all shelter proposals
        JSONObject json_prop_tree = fileToJSON(R.raw.greenup_proposals);
        Bitmap tree_prop_icon = BitmapFactory.decodeResource(getResources(), R.drawable.tree);
        putGeoJSONtoMap(json_prop_tree, tree_prop_icon, ICON_SIZE_PROPOSAL, "type", true, 9999);


        // Load style of map.
        try {
            // Customise the styling of the base map using a JSON object defined in a raw resource file.
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style_json));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
        // Enable my location button.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private JSONObject fileToJSON(int file_idx) {
        String json_str = fileToJSONStr(file_idx);
        JSONObject json;
        try {
            json = new JSONObject(json_str);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return json;
    }

    private void putGeoJSONtoMap(JSONObject json, Bitmap icon_in, int targetSize, String title_prop, boolean drawProgressArc, float zIndex) {
        double lon, lat;
        String obj_name;
        LatLng obj;
        Marker marker;

        JSONArray c = null;
        try {
            c = json.getJSONArray("features");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0 ; i < c.length(); i++) {
            JSONObject feature;
            JSONObject props;
            try {
                feature = c.getJSONObject(i);
                Log.d("feature", feature.toString());
                props = feature.getJSONObject("properties");
                Log.d("props", props.toString());
                JSONObject geom = feature.getJSONObject("geometry");
                Log.d("geom", geom.toString());
                JSONArray coords = geom.getJSONArray("coordinates");
                Log.d("coords", coords.toString());

                lon = coords.getDouble(0);
                lat = coords.getDouble(1);

                try {
                    obj_name = props.getString(title_prop);
                } catch (Exception e){
                    obj_name = title_prop;
                }

                Log.d("lon", Objects.toString(lon));
                Log.d("lon", Objects.toString(lat));
                Log.d("obj_name", obj_name);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            try {
                obj = new LatLng(lat, lon);

                Bitmap icon = icon_in.copy(icon_in.getConfig(), true);
                if (drawProgressArc) {
                    int star = props.getInt("stars");
                    float angleGood = ((star - 1.0f) / 4) * 360;    // sweep angle (in degrees) measured clockwise
                    float angleBad = 360 - angleGood;
                    Log.d("angleGood", Objects.toString(angleGood));
                    Log.d("angleBad", Objects.toString(angleBad));
                    icon = drawCircularArc(icon, angleBad, COLOR_BAD, 200);
                    icon = drawCircularArc(icon, 360.0f, COLOR_GOOD, 0);
                }


                icon = resizeIcon(icon, targetSize, targetSize);
                BitmapDescriptor icon_bitmap = BitmapDescriptorFactory.fromBitmap(icon);
                marker = mMap.addMarker(new MarkerOptions()
                        .position(obj)
                        .title(obj_name)
                        .icon(icon_bitmap)
                        .zIndex(zIndex));
                marker.setTag(feature);
                markerList.add(marker);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void logJSONObject(JSONObject json) {
        Iterator<String> iter = json.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                Object value = json.get(key);
                Log.d("jsonKey", key);
                Log.d("jsonValue", value.toString());
                Log.d("jsonValueType", value.getClass().getName());
            } catch (JSONException e) {
                // Something went wrong!
            }
        }
    }

    public String fileToJSONStr(int input_idx) {
        String json = null;
        try {
            InputStream is = getResources().openRawResource(input_idx);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private Bitmap getCircularIcon(int radius, int color) {
        // Create a circular bitmap with the specified radius and color
        Bitmap bitmap = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(radius, radius, radius, paint);

        return bitmap;
    }

    private Bitmap getDonutIcon(int outerRadius, int innerRadius, int color) {
        // Create a donut-shaped bitmap with the specified outer radius, inner radius, and color
        int diameter = outerRadius * 2;
        Bitmap bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint outerPaint = new Paint();
        outerPaint.setColor(color);
        outerPaint.setStyle(Paint.Style.FILL);
        Paint innerPaint = new Paint();
        innerPaint.setColor(Color.TRANSPARENT); // Set the inner color as needed
        innerPaint.setStyle(Paint.Style.FILL);

        // Draw the outer circle
        canvas.drawCircle(outerRadius, outerRadius, outerRadius, outerPaint);

        // Set the Xfermode to clear the inner part
        innerPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // Draw the inner circle to create a hole
        canvas.drawCircle(outerRadius, outerRadius, innerRadius, innerPaint);
        return bitmap;
    }

    private Bitmap resizeIcon(Bitmap originalBitmap, int w, int h) {
        // Resize the bitmap
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();

        // Calculate the scale factor
        float scaleWidth = ((float) w) / width;
        float scaleHeight = ((float) h) / height;

        // Create a matrix for the resizing operation
        android.graphics.Matrix matrix = new android.graphics.Matrix();

        // Resize the bitmap
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, false);
    }

    private Bitmap drawCircularArc(Bitmap originalBitmap, float arcOpeningAngle, int color, int pad) {
        // Create a new bitmap with the same dimensions as the original
        int modWidth = originalBitmap.getWidth()+2*pad;
        int modHeight = originalBitmap.getHeight()+2*pad;
        Bitmap modifiedBitmap = Bitmap.createBitmap(modWidth, modHeight, Bitmap.Config.ARGB_8888);

        // Create a canvas using the new bitmap
        Canvas canvas = new Canvas(modifiedBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color); // Set the color of the arc

        int centerX = modWidth / 2;
        int centerY = modHeight / 2;

        // Set the radius of the circle (you can adjust this based on your requirements)
        int radius = Math.min(centerX, centerY);

        // Set the starting and sweep angles of the arc
        float startAngle = 0;

        // Draw the circular arc on the canvas
        canvas.drawArc(centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                startAngle, arcOpeningAngle, true, paint);

        // Draw the original bitmap onto the new bitmap
        canvas.drawBitmap(originalBitmap, pad, pad, null);
        return modifiedBitmap;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        locationManager.removeUpdates(this);
        mMap.moveCamera(cameraUpdate);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
    }
}
