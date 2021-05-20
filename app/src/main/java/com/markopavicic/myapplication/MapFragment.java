package com.markopavicic.myapplication;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.clustering.ClusterManager;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MapFragment extends Fragment {
    private GoogleMap mMap;
    private DatabaseReference reference;
    private LatLng location;
    private ClusterManager<MarkerClusterItem> clusterManager;
    private int DELAY_TIME;
    private BottomSheetDialog dialog;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        reference = FirebaseDatabase.getInstance().getReference();
        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.google_map);

        Objects.requireNonNull(supportMapFragment).getMapAsync(googleMap -> {
            mMap = googleMap;
            setUpClusterer();
            checkNetworkConnection();

        });
        return view;
    }

    private void loadLocations() {
        reference.child("locations").addListenerForSingleValueEvent(new ValueEventListener() {
            Boolean check = false;
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                    Integer verified = locationSnapshot.child("verified").getValue(Integer.class);
                    if (verified != null) {
                        if (verified.equals(1)) {
                            check = true;
                            location = new LatLng(locationSnapshot.child("latitude").getValue(Double.class), locationSnapshot.child("longitude").getValue(Double.class));
                            //mMap.addMarker(new MarkerOptions().position(location).title(locationSnapshot.child("name").getValue(String.class)));
                            MarkerClusterItem markerClusterItem = new MarkerClusterItem(location);
                            clusterManager.addItem(markerClusterItem);
                        }
                    }
                }
                if (check)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 8));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setUpClusterer() {

        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        clusterManager = new ClusterManager<>(getContext(), mMap);
        clusterManager.setOnClusterClickListener(cluster -> {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cluster.getPosition(), 12), 1300, null);
            return true;
        });

        clusterManager.setOnClusterItemClickListener(item -> {
            if (mMap.getCameraPosition().zoom == 15)
                DELAY_TIME = 1;
            else
                DELAY_TIME = 1600;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(item.getPosition(), 15), DELAY_TIME, null);
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                dialog = new BottomSheetDialog(getActivity(), R.style.BottomSheetDialogTheme);
                View bottomSheetView = LayoutInflater.from(getActivity().getApplicationContext())
                        .inflate(
                                R.layout.layout_bottom_sheet,
                                getActivity().findViewById(R.id.bottomSheetContainer)
                        );
                TextView name, description, rating, comments;
                name = bottomSheetView.findViewById(R.id.name);
                description = bottomSheetView.findViewById(R.id.description);
                description.setMovementMethod(new ScrollingMovementMethod());
                rating = bottomSheetView.findViewById(R.id.rating);
                comments = bottomSheetView.findViewById(R.id.comments);
                FirebaseDatabase.getInstance().getReference().child("locations").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                        for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                            String nameDB, descriptionDB;
                            Float gradeDB;
                            Integer commentsDB;
                            if (locationSnapshot.child("latitude").getValue(Double.class).equals(item.getPosition().latitude) && locationSnapshot.child("longitude").getValue(Double.class).equals(item.getPosition().longitude)) {
                                nameDB = locationSnapshot.child("name").getValue(String.class);
                                descriptionDB = locationSnapshot.child("description").getValue(String.class);
                                gradeDB = locationSnapshot.child("grade").getValue(Float.class);
                                if (!gradeDB.equals(0))
                                    gradeDB = gradeDB / locationSnapshot.child("grades").getValue(Float.class);
                                commentsDB = locationSnapshot.child("comments").child("number").getValue(Integer.class);
                                name.setText(nameDB);
                                if (descriptionDB.isEmpty())
                                    description.setText(R.string.msgNoDescription);
                                else
                                    description.setText(descriptionDB);
                                if (gradeDB.equals(0) || gradeDB.isNaN())
                                    rating.setText(R.string.msgNoRatings);
                                else
                                    rating.setText(getString(R.string.msgRating11) + " " + String.format("%.2f", gradeDB) + "/5");
                                if (commentsDB > 0)

                                    comments.setText(getString(R.string.msgNumComments) + " " + commentsDB);
                                else
                                    comments.setText(R.string.msgNoComments);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull @NotNull DatabaseError error) {

                    }
                });
                LinearLayout linearLayoutRate = bottomSheetView.findViewById(R.id.rate);
                LinearLayout linearLayoutOpen = bottomSheetView.findViewById(R.id.open);
                LinearLayout linearLayoutComment = bottomSheetView.findViewById(R.id.comment);
                LinearLayout linearLayoutOpenInGoogleMaps = bottomSheetView.findViewById(R.id.openingoogle);
                linearLayoutOpenInGoogleMaps.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("geo:" + item.getPosition().latitude + "," + item.getPosition().longitude));
                    Intent chooser = Intent.createChooser(intent, "Pokreni karte");
                    startActivity(chooser);
                });

                linearLayoutOpen.setOnClickListener(v -> {
                    Intent i = new Intent(getActivity(), LocationActivity.class);
                    i.putExtra("lat", item.getPosition().latitude);
                    i.putExtra("lng", item.getPosition().longitude);
                    startActivity(i);
                });

                linearLayoutComment.setOnClickListener(v -> {
                    Dialog d = new Dialog(getActivity());
                    d.setContentView(R.layout.custom_dialog_comment);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        d.getWindow().setBackgroundDrawable(getActivity().getDrawable(R.drawable.custom_dialog));
                    d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    d.setCancelable(false);
                    d.getWindow().getAttributes().windowAnimations = R.style.animation;
                    d.show();
                    EditText etComment = d.findViewById(R.id.etComment);
                    Button submitComment = d.findViewById(R.id.btnSubmitComment);
                    ImageButton ibCloseComment = d.findViewById(R.id.ibCloseComment);
                    ibCloseComment.setOnClickListener(v14 -> d.dismiss());
                    submitComment.setOnClickListener(v13 -> {
                        if (etComment.getText().toString().equals(""))
                            Toast.makeText(getActivity(), "Molim unesite komentar.", Toast.LENGTH_SHORT).show();
                        else {
                            FirebaseDatabase.getInstance().getReference().child("locations").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                                    for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                                        if (locationSnapshot.child("latitude").getValue(Double.class).equals(item.getPosition().latitude) && locationSnapshot.child("longitude").getValue(Double.class).equals(item.getPosition().longitude)) {
                                            String pushId = reference.push().getKey();
                                            locationSnapshot.child("comments").child(pushId).child("text").getRef().setValue(etComment.getText().toString().trim());
                                            locationSnapshot.child("comments").child(pushId).child("date").getRef().setValue(sdf.format(new Date()));
                                            Integer comments1 = locationSnapshot.child("comments").child("number").getValue(Integer.class);
                                            locationSnapshot.child("comments").child("number").getRef().setValue(comments1 + 1);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull @NotNull DatabaseError error) {

                                }
                            });

                            d.dismiss();
                        }
                    });
                });


                linearLayoutRate.setOnClickListener(v -> {
                    Dialog d = new Dialog(getActivity());
                    d.setContentView(R.layout.custom_dialog_rate);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        d.getWindow().setBackgroundDrawable(getActivity().getDrawable(R.drawable.custom_dialog));
                    d.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    d.setCancelable(false);
                    d.getWindow().getAttributes().windowAnimations = R.style.animation;
                    d.show();
                    Button submitRating = d.findViewById(R.id.btnSubmitRating);
                    RatingBar ratingBar = d.findViewById(R.id.ratingBar);
                    ImageButton ibCloseRating = d.findViewById(R.id.ibCloseRating);
                    ibCloseRating.setOnClickListener(v12 -> d.dismiss());
                    submitRating.setOnClickListener(v1 -> {
                        if (ratingBar.getRating() == 0)
                            Toast.makeText(getActivity(), "Molim unesite ocjenu.", Toast.LENGTH_SHORT).show();
                        else {
                            FirebaseDatabase.getInstance().getReference().child("locations").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                                    for (DataSnapshot locationSnapshot : snapshot.getChildren()) {

                                        if (locationSnapshot.child("latitude").getValue(Double.class).equals(item.getPosition().latitude) && locationSnapshot.child("longitude").getValue(Double.class).equals(item.getPosition().longitude)) {
                                            Float rating1 = locationSnapshot.child("grade").getValue(Float.class);
                                            Integer ratings = locationSnapshot.child("grades").getValue(Integer.class);
                                            locationSnapshot.child("grades").getRef().setValue(ratings + 1);
                                            locationSnapshot.child("grade").getRef().setValue(rating1 + ratingBar.getRating());
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull @NotNull DatabaseError error) {

                                }
                            });

                            d.dismiss();
                        }
                    });
                });
                dialog.setContentView(bottomSheetView);
                dialog.show();
            }, DELAY_TIME);

            return true;
        });
        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);

        // Add cluster items (markers) to the cluster manager.
        loadLocations();
    }
    private void checkNetworkConnection()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if(networkInfo==null || !networkInfo.isConnected())
        {
            Toast.makeText(getContext(), "Spojite se na internet i ponovo pokrenite aplikaciju.", Toast.LENGTH_LONG).show();
            getActivity().finish();
            System.exit(0);
        }
    }
}