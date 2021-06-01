package com.markopavicic.discovercroatia;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class LocationActivity extends AppCompatActivity {
    private final List<String> commentsList = new ArrayList<>();
    private final List<String> datesList = new ArrayList<>();
    private final List<String> imagesList = new ArrayList<>();
    private RecyclerAdapter adapter;
    private Double lat, lng;
    private TextView laName, laDescription, laRatings;
    private RatingBar laRating;
    private String name;
    private Uri imageUri;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private FirebaseDatabase database;
    private DatabaseReference databaseReference;
    private String randomKey;
    private LinearLayout layout;
    private String id;
    private List<String> listKeys;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        checkNetworkConnection();
        lat = getIntent().getDoubleExtra("lat", 0);
        lng = getIntent().getDoubleExtra("lng", 0);
        laName = findViewById(R.id.laName);
        laDescription = findViewById(R.id.laDescription);
        laRating = findViewById(R.id.laRating);
        laRatings = findViewById(R.id.laRatings);
        ImageButton ibReport = findViewById(R.id.ibReport);
        ImageButton ibUpload = findViewById(R.id.ibUpload);
        RecyclerView recycler = findViewById(R.id.rvComments);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecyclerAdapter();
        recycler.setAdapter(adapter);
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        setupRecyclerData();
        laDescription.setMovementMethod(new ScrollingMovementMethod());

        ibUpload.setOnClickListener(v -> choosePicture());
        ibReport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_EMAIL,
                    new String[]{"mpavicic99@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Prijava lokacije");
            intent.putExtra(Intent.EXTRA_TEXT, "Prijava lokacije: " + name + " " + lat.toString() + " , " + lng.toString());
            intent.setType("message/rfc822");
            startActivity(Intent.createChooser(intent, "Odaberi Email klijent :"));
            Toast.makeText(v.getContext(), "Odaberi Email klijent", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupRecyclerData() {
        databaseReference.child("locations").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {

                for (DataSnapshot locationSnapshot : snapshot.getChildren()) {
                    if (Objects.requireNonNull(locationSnapshot.child("latitude").getValue(Double.class)).equals(lat) && Objects.requireNonNull(locationSnapshot.child("longitude").getValue(Double.class)).equals(lng)) {
                        id = locationSnapshot.getKey();
                        name = locationSnapshot.child("name").getValue(String.class);
                        laName.setText(name);
                        String descriptionDB = locationSnapshot.child("description").getValue(String.class);
                        if (descriptionDB.isEmpty()) {
                            laDescription.setText(R.string.msgNoDescription);
                        } else {
                            laDescription.setText(locationSnapshot.child("description").getValue(String.class));
                        }
                        float rating = locationSnapshot.child("grade").getValue(Float.class) / locationSnapshot.child("grades").getValue(Float.class);
                        Integer ratings = locationSnapshot.child("grades").getValue(Integer.class);
                        if (rating <= 0 || rating > 5 || Float.isNaN(rating)) {
                            laRating.setRating(rating);
                            laRatings.setText(R.string.msgNoRatings);
                        } else {
                            laRating.setRating(rating);
                            laRatings.setText(String.format("%.2f", rating) + "/5 " + getString(R.string.msgRatings1) + " " + Objects.requireNonNull(ratings).toString() + " " + getString(R.string.msgRatings2));
                        }
                        commentsList.clear();
                        datesList.clear();
                        for (DataSnapshot commentSnapshot : locationSnapshot.child("comments").getChildren()) {
                            commentsList.add(commentSnapshot.child("text").getValue(String.class));
                            datesList.add(commentSnapshot.child("date").getValue(String.class));
                        }
                        adapter.addData(commentsList, datesList);
                        imagesList.clear();
                        for (DataSnapshot imageSnapshot : locationSnapshot.child("images").getChildren()) {
                            String key = imageSnapshot.getKey();
                            imagesList.add(key);
                        }
                        if (!imagesList.isEmpty()) {

                            layout = findViewById(R.id.lvGallery);
                            layout.getLayoutParams().height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 220, getResources().getDisplayMetrics());
                            layout.removeAllViews();
                            for (int i = 0; i < imagesList.size(); i++) {
                                ImageView imageView = new ImageView(getApplicationContext());
                                imageView.setId(i);
                                imageView.setPadding(2, 2, 2, 2);
                                imageView.setAdjustViewBounds(true);
                                imageView.setScaleType(ImageView.ScaleType.FIT_XY);

                                storage.getReference().child("images/" + imagesList.get(i)).getDownloadUrl().addOnSuccessListener(uri -> {
                                    Glide.with(getApplicationContext())
                                            .load(uri)
                                            .into(imageView);
                                    layout.addView(imageView);
                                });

                                imageView.setOnClickListener(v -> {
                                    Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
                                    ByteArrayOutputStream bs = new ByteArrayOutputStream();
                                    int quality = 100;
                                    int currsize;
                                    do {
                                        bs.reset();
                                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bs);
                                        currsize = bs.toByteArray().length;
                                        quality -= 5;
                                    }
                                    while (currsize >= 517716);

                                    Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
                                    intent.putExtra("byteArray", bs.toByteArray());
                                    startActivity(intent);

                                });
                            }

                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
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
            List<Task<Uri>> taskArrayList = new ArrayList<>();
            listKeys = new ArrayList<>();
            int count = 1;
            if (data.getClipData() != null) {
                count = data.getClipData().getItemCount();
            }

            for (int i = 0; i < count; i++) {
                randomKey = UUID.randomUUID().toString();
                if (data.getClipData() != null) {
                    imageUri = data.getClipData().getItemAt(i).getUri();
                } else {
                    imageUri = data.getData();
                }
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                taskArrayList.add(uploadImageTask(bitmap, randomKey));
                Tasks.whenAllSuccess(taskArrayList).addOnCompleteListener(task -> {
                    Toast.makeText(getApplicationContext(), "Uspješan prijenos.", Toast.LENGTH_SHORT).show();
                    uploadLinks();
                    setupRecyclerData();
                });
                Tasks.whenAllComplete(taskArrayList).addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Greška u prijenosu.", Toast.LENGTH_SHORT).show());
            }

        }
    }


    private void uploadLinks() {
        for (int i = 0; i < listKeys.size(); i++) {
            databaseReference.child("locations").child(id).child("images").child(listKeys.get(i)).setValue("");
        }
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
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(getApplicationContext(), "Spojite se na internet i ponovo pokrenite aplikaciju.", Toast.LENGTH_LONG).show();
            finish();
            System.exit(0);
        }
    }
}