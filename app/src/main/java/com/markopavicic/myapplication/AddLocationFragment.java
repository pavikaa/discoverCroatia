package com.markopavicic.discovercroatia;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AddLocationFragment extends Fragment {
    private GoogleMap mMap;
    private LatLng location, markerLocation;
    private EditText etName, etDescription;
    private Uri imageUri;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private String randomKey;
    private List<String> listKeys;
    private ClipData clipData;
    private Handler handler;
    private ProgressDialog d;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_location, container, false);
        checkNetworkConnection();
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        ImageButton imageButton = view.findViewById(R.id.addLocationButton);
        imageButton.setVisibility(View.INVISIBLE);
        imageButton.setOnClickListener(v -> {
            if (markerLocation != null)
                showBottomSheet();
            else
                Toast.makeText(getActivity(), "Molim prvo dodajte marker", Toast.LENGTH_SHORT).show();
        });
        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.google_map_add);
        Objects.requireNonNull(supportMapFragment).getMapAsync(googleMap -> {
            mMap = googleMap;
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            checkMyPermission();
            Toast.makeText(getActivity(), "Zadržite na karti kako biste dodali marker.", Toast.LENGTH_SHORT).show();
            mMap.setOnMyLocationClickListener(location -> {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
                markerLocation = latLng;
                imageButton.setVisibility(View.VISIBLE);
            });
            mMap.setOnMyLocationButtonClickListener(() -> false);
            location = new LatLng(45.55111, 18.69389);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 8));
            mMap.setOnMapLongClickListener(latLng -> {
                mMap.clear();
                markerLocation = latLng;
                mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
                imageButton.setVisibility(View.VISIBLE);
            });
            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(@NonNull @NotNull Marker marker) {

                }

                @Override
                public void onMarkerDrag(@NonNull @NotNull Marker marker) {
                    markerLocation = marker.getPosition();
                }

                @Override
                public void onMarkerDragEnd(@NonNull @NotNull Marker marker) {

                }
            });
        });
        return view;
    }

    private void choosePicture() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            clipData = null;
            imageUri = null;
            imageUri = data.getData();
            if (data.getClipData() != null) {
                clipData = data.getClipData();
            }
        }
    }


    private void checkMyPermission() {
        Dexter.withContext(getContext()).withPermission(Manifest.permission.ACCESS_FINE_LOCATION).withListener(new PermissionListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {

                mMap.setMyLocationEnabled(true);
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                Toast.makeText(getActivity(), "Molim dozvolite pristup lokaciji", Toast.LENGTH_SHORT).show();
                Intent i = new Intent();
                i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", requireContext().getPackageName(), "");
                i.setData(uri);
                startActivity(i);
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
    }

    private void addLocation(Location newLocation) {
        String pushId = databaseReference.push().getKey();
        List<Task<Uri>> taskArrayList = new ArrayList<>();
        listKeys = new ArrayList<>();
        int count = 1;
        if (clipData != null) {
            count = clipData.getItemCount();
        }
        for (int i = 0; i < count; i++) {
            randomKey = UUID.randomUUID().toString();
            if (clipData != null) {
                imageUri = clipData.getItemAt(i).getUri();
            }
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), imageUri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            taskArrayList.add(uploadImageTask(bitmap, randomKey));
            Tasks.whenAllSuccess(taskArrayList).addOnCompleteListener(task -> Toast.makeText(getContext(), "Lokacija uspješno dodana, te će uskoro biti pregledana", Toast.LENGTH_SHORT).show());
            Tasks.whenAllComplete(taskArrayList).addOnFailureListener(e -> Toast.makeText(getContext(), "Greška u prijenosu.", Toast.LENGTH_SHORT).show());
        }
        for (int i = 0; i < listKeys.size(); i++) {
            databaseReference.child("locations").child(pushId).child("images").child(listKeys.get(i)).setValue("");
        }

        databaseReference.child("locations")
                .child(Objects.requireNonNull(pushId))
                .child("longitude").setValue(newLocation.getLongitude());
        databaseReference.child("locations")
                .child(pushId)
                .child("latitude").setValue(newLocation.getLatitude());
        databaseReference.child("locations")
                .child(pushId)
                .child("name").setValue(newLocation.getName());
        databaseReference.child("locations")
                .child(pushId)
                .child("description").setValue(newLocation.getDescription());
        databaseReference.child("locations")
                .child(pushId)
                .child("grades").setValue(0);
        databaseReference.child("locations")
                .child(pushId)
                .child("verified").setValue(0);
        databaseReference.child("locations")
                .child(pushId)
                .child("grade").setValue(0);
        databaseReference.child("locations")
                .child(pushId)
                .child("comments").child("number").setValue(0);

    }

    private void showBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireActivity(), R.style.BottomSheetDialogTheme);
        View bottomSheetView = LayoutInflater.from(getActivity().getApplicationContext())
                .inflate(
                        R.layout.layout_bottom_sheet_add,
                        getActivity().findViewById(R.id.bottomSheetContainerAdd)
                );
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        etName = bottomSheetView.findViewById(R.id.etName);
        etDescription = bottomSheetView.findViewById(R.id.etDescription);
        Button submitButton = bottomSheetView.findViewById(R.id.btnSubmit);

        ImageButton attach = bottomSheetView.findViewById(R.id.ibAttach);
        attach.setOnClickListener(v -> choosePicture());
        submitButton.setOnClickListener(v -> {
            if (etName.getText().toString().trim().equals("")) {
                etName.setError("Molim unesite ime lokacije");
                etName.requestFocus();
            } else {
                d = new ProgressDialog(getContext());
                d.setMessage("Prijenos lokacije...");
                d.setCancelable(false);
                d.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                d.show();
                new Thread() {
                    public void run() {
                        String name, description;
                        double latitude, longitude;
                        name = etName.getText().toString().trim();
                        description = etDescription.getText().toString().trim();
                        if (description == null)
                            description = "";
                        latitude = markerLocation.latitude;
                        longitude = markerLocation.longitude;
                        Location newLocation = new Location(name, description, latitude, longitude);
                        addLocation(newLocation);
                        handler.sendEmptyMessage(0);
                    }
                }.start();
                handler = new Handler() {
                    public void handleMessage(android.os.Message msg) {
                        d.dismiss();
                    }

                };
                dialog.hide();
            }
        });
        dialog.setContentView(bottomSheetView);
        dialog.show();

    }

    private Task<Uri> uploadImageTask(final Bitmap bitmap, String randomKey) {
        StorageReference ref = storageReference.child("images/" + randomKey);
        listKeys.add(randomKey);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 100;
        int currsize;
        do {
            baos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            currsize = baos.toByteArray().length;
            quality -= 5;
        }
        while (currsize >= 1000000);

        byte[] data = baos.toByteArray();

        UploadTask uploadTask = ref.putBytes(data);
        bitmap.recycle();

        return uploadTask.continueWithTask(task -> {
            bitmap.recycle();
            return ref.getDownloadUrl();
        });
    }

    private void checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(getContext(), "Spojite se na internet i ponovo pokrenite aplikaciju.", Toast.LENGTH_LONG).show();
            getActivity().finish();
            System.exit(0);
        }
    }
}
