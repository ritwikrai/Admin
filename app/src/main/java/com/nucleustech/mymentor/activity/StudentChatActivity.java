package com.nucleustech.mymentor.activity;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nucleustech.mymentor.BuildConfig;
import com.nucleustech.mymentor.R;
import com.nucleustech.mymentor.adapter.ChatFirebaseAdapter;
import com.nucleustech.mymentor.adapter.ClickListenerChatFirebase;
import com.nucleustech.mymentor.model.ChatModel;
import com.nucleustech.mymentor.model.FileModel;
import com.nucleustech.mymentor.model.UserClass;
import com.nucleustech.mymentor.model.UserModel;
import com.nucleustech.mymentor.util.Util;
import com.nucleustech.mymentor.view.FullScreenImageActivity;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;


public class StudentChatActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener,
        ClickListenerChatFirebase {

    private Context mContext;
    private static final int IMAGE_GALLERY_REQUEST = 1;
    private static final int IMAGE_CAMERA_REQUEST = 2;
    private static final int PLACE_PICKER_REQUEST = 3;
    private static final int OTHER_FILE_REQUEST = 4;

    private boolean isFileRequest = false;

    static final String TAG = StudentChatActivity.class.getSimpleName();
    static final String CHAT_REFERENCE = "chatmodel";

    //Firebase and GoogleApiClient
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private GoogleApiClient mGoogleApiClient;
    private DatabaseReference mFirebaseDatabaseReference;
    FirebaseStorage storage = FirebaseStorage.getInstance();

    //CLass Model
    private UserModel userModel;

    //Views UI
    private RecyclerView rvListMessage;
    private LinearLayoutManager mLinearLayoutManager;
    private ImageView btSendMessage, btEmoji, buttonAttachFile;
    private EmojiconEditText edMessage;
    private View contentRoot;
    private EmojIconActions emojIcon;

    //File
    private File filePathImageCamera;

    private String studentName = "";
    private String studentEmail = "";
    private String studentFirebaseID = "";
    private String studentID = "";

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.menu_item_studentProfile) {
            Intent intent = new Intent(StudentChatActivity.this, StudentProfileActivity.class);
            intent.putExtra("name", "" + studentName);
            intent.putExtra("email", "" + studentEmail);
            intent.putExtra("studentId", "" + studentID);
            intent.putExtra("firebaseId", "" + studentFirebaseID);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_item_scheduleMeet) {

            Intent intent = new Intent(StudentChatActivity.this, ViewStudentAllSchedulesActivity.class);
            intent.putExtra("name", "" + studentName);
            intent.putExtra("studentId", "" + studentID);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_item_suggestCollege) {

            startActivity(new Intent(mContext, AllUniversitiesActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_student_chat, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        setContentView(R.layout.activity_chat_main);
        mContext = StudentChatActivity.this;

        studentName = getIntent().getStringExtra("name");

        studentEmail = getIntent().getStringExtra("email");
        studentFirebaseID = getIntent().getStringExtra("firebaseId");
        studentID = getIntent().getStringExtra("studentId");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("" + studentName);

        if (!Util.verificaConexao(this)) {
            Util.initToast(this, "You do not have an internet connection.");
            finish();
        } else {
            bindViews();
            verifyUserLogin();
            mGoogleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this, this).addApi(Auth.GOOGLE_SIGN_IN_API).build();
        }

    }

    /**
     * Verify user is logged in
     */
    private void verifyUserLogin() {
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        if (mFirebaseUser == null) {
            startActivity(new Intent(this, SigninActivity.class));
            finish();
        } else {
            userModel = new UserModel(mFirebaseUser.getDisplayName(), mFirebaseUser.getPhotoUrl().toString(), mFirebaseUser.getUid());
            readMessagensFirebase();
        }
    }

    /**
     * Link views with Java API
     */
    private void bindViews() {
        contentRoot = findViewById(R.id.contentRoot);
        edMessage = (EmojiconEditText) findViewById(R.id.editTextMessage);
        btSendMessage = (ImageView) findViewById(R.id.buttonMessage);
        buttonAttachFile = (ImageView) findViewById(R.id.buttonAttachFile);
        btSendMessage.setOnClickListener(this);
        buttonAttachFile.setOnClickListener(this);
        btEmoji = (ImageView) findViewById(R.id.buttonEmoji);
        emojIcon = new EmojIconActions(this, contentRoot, edMessage, btEmoji);
        emojIcon.ShowEmojIcon();
        rvListMessage = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Util.initToast(this, "Google Play Services error.");
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonMessage:
                sendMessageFirebase();
                break;
            case R.id.buttonAttachFile:
                showAttachOptions();
                break;
        }
    }

    private void showAttachOptions() {


        final Dialog customDialog = new Dialog(StudentChatActivity.this);

        customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.image_select_dialog, null);
        Button btn_album = (Button) view.findViewById(R.id.btn_album);
        btn_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customDialog.dismiss();
                photoGalleryIntent();
            }
        });

        Button btn_camera = (Button) view.findViewById(R.id.btn_camera);
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customDialog.dismiss();
                verifyStoragePermissions();

            }
        });
        Button btn_otherFiles = (Button) view.findViewById(R.id.btn_otherFiles);
        btn_otherFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customDialog.dismiss();
                isFileRequest = true;
                verifyStoragePermissions();

            }
        });


        Button btn_cancel = (Button) view.findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                customDialog.dismiss();
            }
        });

        customDialog.setCancelable(false);
        customDialog.setContentView(view);
        customDialog.setCanceledOnTouchOutside(false);
        // Start AlertDialog
        customDialog.show();
    }

    @Override
    public void clickImageChat(View view, int position, String nameUser, String urlPhotoUser, String urlPhotoClick) {
        Intent intent = new Intent(this, FullScreenImageActivity.class);
        intent.putExtra("nameUser", nameUser);
        intent.putExtra("urlPhotoUser", urlPhotoUser);
        intent.putExtra("urlPhotoClick", urlPhotoClick);
        startActivity(intent);
    }

    @Override
    public void clickImageMapChat(View view, int position, String latitude, String longitude) {
        String uri = String.format("geo:%s,%s?z=17&q=%s,%s", latitude, longitude, latitude, longitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
    }

    @Override
    public void clickFileChat(View view, int position, String nameUser, String urlPhotoUser, String urlPhotoClick) {
        //Toast.makeText(mContext, "File chat click.", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse("" + urlPhotoClick));
        startActivity(i);

       /*Intent intent= new Intent(mContext,ViewAttachmentActivity.class);
       intent.putExtra("nameUser",""+nameUser);
       intent.putExtra("urlPhotoClick",""+urlPhotoClick);
       startActivity(intent);*/
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        StorageReference storageRef = storage.getReferenceFromUrl(Util.URL_STORAGE_REFERENCE).child(Util.FOLDER_STORAGE_IMG);
        StorageReference storageRefFile = storage.getReferenceFromUrl(Util.URL_STORAGE_REFERENCE).child(Util.FOLDER_STORAGE_FILE);


        if (requestCode == IMAGE_GALLERY_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    sendFileFirebase(storageRef, selectedImageUri, "image");
                } else {
                    //URI IS NULL
                }
            }
        } else if (requestCode == IMAGE_CAMERA_REQUEST) {
            if (resultCode == RESULT_OK) {
                if (filePathImageCamera != null && filePathImageCamera.exists()) {
                    StorageReference imageCameraRef = storageRef.child(filePathImageCamera.getName() + "_camera");
                    sendFileFirebase(imageCameraRef, filePathImageCamera);
                } else {
                    //IS NULL
                }
            }
        } else if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
          /*      Place place = PlacePicker.getPlace(this, data);
                if (place!=null){
                    LatLng latLng = place.getLatLng();
                    MapModel mapModel = new MapModel(latLng.latitude+"",latLng.longitude+"");
                    ChatModel chatModel = new ChatModel(userModel, Calendar.getInstance().getTime().getTime()+"",mapModel);
                    mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(chatModel);
                }else{
                    //PLACE IS NULL
                }*/
            }
        } else if (requestCode == OTHER_FILE_REQUEST) {
            if (resultCode == RESULT_OK) {

                if (resultCode == RESULT_OK) {
                    Uri selectedImageUri = data.getData();
                    Log.e("Last Path", "" + selectedImageUri);
                    if (selectedImageUri != null) {

                        sendFileFirebase(storageRefFile, selectedImageUri, "file");
                    } else {
                        //URI IS NULL
                    }
                }
            }
        }

    }


    /**
     * Sends the file to the firebase
     */
    private void sendFileFirebase(StorageReference storageReference, final Uri file, String type) {
        if (storageReference != null) {

            if (type.equalsIgnoreCase("image")) {
                final String name = DateFormat.format("yyyy-MM-dd_hhmmss", new Date()).toString();
                StorageReference imageGalleryRef = storageReference.child(name + "_gallery");
                UploadTask uploadTask = imageGalleryRef.putFile(file);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure sendFileFirebase " + e.getMessage());
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.i(TAG, "onSuccess sendFileFirebase");
                        UserClass userClass = Util.fetchUserClass(mContext);
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        FileModel fileModel = new FileModel("img", downloadUrl.toString(), name, "");
                        ChatModel chatModel = new ChatModel(userModel, "", studentFirebaseID, userClass.firebaseId, userClass.firebaseInstanceId, userClass.getEmail(), Calendar.getInstance().getTime().getTime() + "", fileModel, studentEmail);
                        mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(chatModel);
                    }
                });
            } else if (type.equalsIgnoreCase("file")) {
                final String name = DateFormat.format("yyyy-MM-dd_hhmmss", new Date()).toString();
                String fileName = getFileName(file);
                //Toast.makeText(mContext, "File selected: " + fileName, Toast.LENGTH_SHORT).show();
                StorageReference fileGalleryRef = storageReference.child(name + "_mentorfile_"+fileName+"_gallery");
                UploadTask uploadTask = fileGalleryRef.putFile(file);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "onFailure sendFileFirebase " + e.getMessage());
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.i(TAG, "onSuccess sendFileFirebase");
                        UserClass userClass = Util.fetchUserClass(mContext);
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        FileModel fileModel = new FileModel("file", downloadUrl.toString(), name, "");
                        ChatModel chatModel = new ChatModel(userModel, "", studentFirebaseID, userClass.firebaseId, userClass.firebaseInstanceId, userClass.getEmail(), Calendar.getInstance().getTime().getTime() + "", fileModel, studentEmail);
                        mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(chatModel);
                    }
                });
            }
        } else {
            //IS NULL
        }

    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    /**
     * Sends the file to the firebase
     */
    private void sendFileFirebase(StorageReference storageReference, final File file) {
        if (storageReference != null) {
            Uri photoURI = FileProvider.getUriForFile(StudentChatActivity.this, BuildConfig.APPLICATION_ID + ".provider", file);
            UploadTask uploadTask = storageReference.putFile(photoURI);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "onFailure sendFileFirebase " + e.getMessage());
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.i(TAG, "onSuccess sendFileFirebase");
                    UserClass userClass = Util.fetchUserClass(mContext);
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    FileModel fileModel = new FileModel("img", downloadUrl.toString(), file.getName(), file.length() + "");
                    ChatModel chatModel = new ChatModel(userModel, "", studentFirebaseID, userClass.firebaseId, userClass.firebaseInstanceId, userClass.getEmail(), Calendar.getInstance().getTime().getTime() + "", fileModel, studentEmail);
                    mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(chatModel);
                }
            });
        } else {
            //IS NULL
        }

    }

    /**
     * Get user location
     */
    private void locationPlacesIntent() {
        /*try {
            PLACE_PICKER_REQUEST.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }*/
    }

    /**
     * Upload photo taken by camera
     */
    private void photoCameraIntent() {
        String nomeFoto = DateFormat.format("yyyy-MM-dd_hhmmss", new Date()).toString();
        filePathImageCamera = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), nomeFoto + "camera.jpg");
        Intent it = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri photoURI = FileProvider.getUriForFile(StudentChatActivity.this,
                BuildConfig.APPLICATION_ID + ".provider",
                filePathImageCamera);
        it.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        startActivityForResult(it, IMAGE_CAMERA_REQUEST);
    }

    private void otherFileIntent() {
        // Toast.makeText(mContext, "Other Files", Toast.LENGTH_SHORT).show();
        String[] mimetypes = {"application/*|text/*"};
        Intent i = new Intent();
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        i.setAction(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Choose file"), OTHER_FILE_REQUEST);
    }


    /**
     * Send photo to gallery
     */
    private void photoGalleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture_title)), IMAGE_GALLERY_REQUEST);
    }

    /**
     * Send simple text msg to chat
     */
    private void sendMessageFirebase() {
        UserClass userClass = Util.fetchUserClass(mContext);
        ChatModel model = new ChatModel(userModel, edMessage.getText().toString(), studentFirebaseID, userClass.firebaseId, userClass.firebaseInstanceId, userClass.getEmail(), Calendar.getInstance().getTime().getTime() + "", null, studentEmail);
        mFirebaseDatabaseReference.child(CHAT_REFERENCE).push().setValue(model);
        edMessage.setText(null);
    }

    ChatFirebaseAdapter firebaseAdapter;

    /**
     * Read collections chatmodel Firebase
     */
    private void readMessagensFirebase() {

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        Log.e("Data", "Json: " + mFirebaseDatabaseReference.child("" + CHAT_REFERENCE).child("chatModel"));
        mFirebaseDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("Data", "Json: " + dataSnapshot.getValue());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        firebaseAdapter = new ChatFirebaseAdapter(mFirebaseDatabaseReference.child(CHAT_REFERENCE), userModel.getName(), this, "" + studentEmail, Util.fetchUserClass(mContext).getEmail());

        firebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = firebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    rvListMessage.scrollToPosition(positionStart);
                }
            }
        });
        rvListMessage.setLayoutManager(mLinearLayoutManager);
        rvListMessage.setAdapter(firebaseAdapter);
    }

    /**
     * Checks if the app has permission to write to device storage
     * <p>
     * If the app does not has permission then the user will be prompted to grant permissions
     */
    public void verifyStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(StudentChatActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(StudentChatActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE
            );
        } else {
            // we already have permission, lets go ahead and call camera intent
            if (isFileRequest) {
                isFileRequest = false;
                otherFileIntent();
            } else {
                photoCameraIntent();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted

                    if (isFileRequest) {
                        isFileRequest = false;
                        otherFileIntent();
                    } else {
                        photoCameraIntent();
                    }
                }
                break;
        }
    }


}
