package com.example.androidbarberbooking;
//date hour load tanımlayacaksın sonra db'de date hour tablosu aç sonra database'de process açacaksın sonra submit butonunu tanımlayacaksın ki db'ye kaydetsin en son kaldığım yer burası.
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.androidbarberbooking.databinding.ActivityRezBinding;
import com.example.androidbarberbooking.databinding.ActivityRezUserBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class RezActivityUser extends AppCompatActivity {

    private ActivityRezUserBinding binding;

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    private ArrayList<String> personnelNameArrayList;
    private ArrayList<String> personnelIdArrayList;
    private ArrayList<String> descriptionArrayList;
    private ArrayList<String> processName;
    private ArrayList<String> dateHour;
    private ArrayList<String> dateHourid;



    private static final String TAG = "ADD_PDF_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRezUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());




        //init firebaseauth
        firebaseAuth = FirebaseAuth.getInstance();
        loadPersonName();
        loadProcess();
        loadDateHour();

        //setup progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //handle click, go to previous activity
        binding.backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        //handle click, attach pdf
        binding.nameTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                personnelPickDialog();
            }
        });



        binding.clockTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hourPickDialog();
            }
        });

        //handle click, pick category
        binding.categoryTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processPickDialog();
            }
        });



        //handle click, upload pdf
        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //validate data
                validateData();
            }
        });
    }


    private String personnelName = "", nameProcess = "", hourDate = "";

    private void validateData() {

        Log.d(TAG, "validateData: validating data...");

        //get data
        personnelName = binding.nameTv.getText().toString().trim();
        nameProcess = binding.categoryTv.getText().toString().trim();
        hourDate = binding.clockTv.getText().toString().trim();

        //valdiate data
        if (TextUtils.isEmpty(personnelName)) {
            Toast.makeText(this, "Enter Personnel Name", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(nameProcess)) {
            Toast.makeText(this, "Enter Personnel Description", Toast.LENGTH_SHORT).show();
        } else if (TextUtils.isEmpty(selectedCategoryTitle)) {
            Toast.makeText(this, "Choose Workshop", Toast.LENGTH_SHORT).show();
        }
        else {
            submitRezInfo();
        }
    }





    private void submitRezInfo() {
        Log.d(TAG, "updatePdf: Starting update pdf info to fb");
        progressDialog.setMessage("Updating book info...");
        progressDialog.show();

        String hourname = binding.clockTv.getText().toString();

        System.out.println(hourname);
        long timestamp = System.currentTimeMillis();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("hour", hourDate + " REZ " + selectedCategoryTitle);
        hashMap.put("id", "" + timestamp);
        hashMap.put("personnelName", "" + personnelName);
        hashMap.put("nameProcess", "" + nameProcess);
        hashMap.put("timestamp", "" + timestamp);
        hashMap.put("uid", "" + firebaseAuth.getUid());


        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Hours/Sakal");
        ref.child("" + timestamp)
                .updateChildren(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "onSuccess: Workshop updated...");
                        progressDialog.dismiss();
                        Toast.makeText(RezActivityUser.this, "Workshop info updated...", Toast.LENGTH_SHORT).show();
                        abc();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: failed to update due to: " + e.getMessage());
                        progressDialog.dismiss();
                        Toast.makeText(RezActivityUser.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void abc() {
        progressDialog.setMessage("Adding hour...");
        progressDialog.show();

        long timestamp = System.currentTimeMillis();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", "" + timestamp);
        hashMap.put("personnelName", "" + personnelName);
        hashMap.put("nameProcess", "" + nameProcess);
        hashMap.put("hour", "" + hourDate);
        hashMap.put("timestamp", "" + timestamp);
        hashMap.put("uid", "" + firebaseAuth.getUid());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Reservation/Sakal");
        ref.child("" + timestamp)
                .setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //category add success
                        progressDialog.dismiss();
                        Toast.makeText(RezActivityUser.this, "Hour added successfully...", Toast.LENGTH_SHORT).show();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //category add failed
                        progressDialog.dismiss();
                        Toast.makeText(RezActivityUser.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void loadPersonName() {
        Log.d(TAG, "loadPdfCategories: Loading pdf categories...");
        personnelNameArrayList = new ArrayList<String>();
        personnelIdArrayList = new ArrayList<String>();
        descriptionArrayList = new ArrayList<>();

        //db reference to load categories
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Personnel/Sakal");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                personnelNameArrayList.clear();
                personnelIdArrayList.clear();
                descriptionArrayList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String personnelId = "" + ds.child("id").getValue();
                    String personnelName = "" + ds.child("title").getValue();
                    String description = "" + ds.child("description").getValue();
                    //add to respective arrayLists
                    personnelNameArrayList.add(personnelName);
                    personnelIdArrayList.add(personnelId);
                    descriptionArrayList.add(description);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    //rezuser kısmını yap db kısmını düzenlemeye çalış. en son burada kaldın

    //selected category id and category title
    private String selectedCategoryId, selectedCategoryTitle;

    private void personnelPickDialog() {
        Log.d(TAG, "categoryPickDialog: showing category pick dialog");

        //get string array of category from arraylist
        String[] categoriesArray = new String[personnelNameArrayList.size()];
        for (int i = 0; i < personnelNameArrayList.size(); i++) {
            categoriesArray[i] = personnelNameArrayList.get(i);
        }


        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Category")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //handle item click
                        //get clicked item from list
                        selectedCategoryTitle = personnelNameArrayList.get(which);
                        selectedCategoryId = personnelNameArrayList.get(which);
                        //set to category textview
                        binding.nameTv.setText(selectedCategoryTitle);

                        Log.d(TAG, "onClick: Selected Personnel: " + selectedCategoryId + selectedCategoryTitle);
                    }
                })
                .show();
    }


    private void loadProcess() {
        Log.d(TAG, "loadPdfCategories: Loading pdf categories...");
        processName = new ArrayList<String>();

        //db reference to load categories
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Process");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                processName.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String processname = "" + ds.child("name").getValue();

                    //add to respective arrayLists
                    processName.add(processname);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //selected category id and category title
    private String selectedProcessName;

    private void processPickDialog() {
        Log.d(TAG, "categoryPickDialog: showing category pick dialog");

        //get string array of category from arraylist
        String[] categoriesArray = new String[processName.size()];
        for (int i = 0; i < processName.size(); i++) {
            categoriesArray[i] = processName.get(i);
        }

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Process")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //handle item click
                        //get clicked item from list
                        selectedProcessName = processName.get(which);
                        //set to category textview
                        binding.categoryTv.setText(selectedProcessName);

                        Log.d(TAG, "onClick: Selected Personnel: " + selectedProcessName);
                    }
                })
                .show();
    }



    private void loadDateHour() {
        Log.d(TAG, "loadPdfCategories: Loading pdf categories...");
        dateHour = new ArrayList<String>();

        //db reference to load categories
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Hours/Sakal");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                dateHour.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String datehours = "" + ds.child("hour").getValue();
                    String hourid = "" + ds.child("id").getValue();

                    //add to respective arrayLists
                    dateHour.add(datehours);
                    //dateHourid.add(hourid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //selected category id and category title
    private String selectedDateHour;

    private void hourPickDialog() {
        Log.d(TAG, "categoryPickDialog: showing category pick dialog");

        //get string array of category from arraylist
        String[] categoriesArray = new String[dateHour.size()];
        for (int i = 0; i < dateHour.size(); i++) {
            categoriesArray[i] = dateHour.get(i);
        }

        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Hour")
                .setItems(categoriesArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //handle item click
                        //get clicked item from list
                        selectedDateHour = dateHour.get(which);
                        //set to category textview
                        binding.clockTv.setText(selectedDateHour);

                        Log.d(TAG, "onClick: Selected Personnel: " + selectedDateHour);
                    }
                })
                .show();
    }

}