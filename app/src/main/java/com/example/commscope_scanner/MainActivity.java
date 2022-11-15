package com.example.commscope_scanner;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    //UI views
    private MaterialButton inputImageBtn;
    private MaterialButton QRbtn;
    private MaterialButton recognize_Text_btn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;
    private ImageView commscopelogo;



    //Tag
    private static final String Tag="Main_Tag";

    //uri of the image taken from camera or gallery
    private Uri imageUri = null;

    //to handle the result of camera or gallery intent
    private static final int  CAMERA_REQUEST_CODE=100;
    private static final int STORAGE_REQUEST_CODE=101;

    //arrays of permission required to pick image from camera or gallery
    private String[] cameraPermission;
    private String[] storagePermission;

    //progressdialog
    private ProgressDialog progressDialog;

    // Text Recognizer
    private TextRecognizer textRecognizer;

    //barcode
    private BarcodeScannerOptions barcodeScannerOptions;
    private BarcodeScanner barcodeScanner;

    //button id
    String buttonid;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //init UI views
        inputImageBtn =findViewById(R.id.inputImageBtn);
        QRbtn=findViewById(R.id.QRbtn);
        recognize_Text_btn=findViewById(R.id.recognize_Text_btn);
        imageIv=findViewById(R.id.imageIv);
        recognizedTextEt=findViewById(R.id.recognizedTextEt);
        commscopelogo=findViewById(R.id.commscopelogo);


        commscopelogo.setImageResource(R.drawable.commscopelogo);


        //init arrays of permissions required for camera or gallery
        cameraPermission =new  String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission =new  String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //init textrecognizer
        textRecognizer= TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        //init barcodeoptions and barcode
        barcodeScannerOptions=new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build();

        barcodeScanner= BarcodeScanning.getClient(barcodeScannerOptions);


        //handle text button click, show image dialog
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonid="textbuttonclicked";
                showInputImageDialog();
                recognizedTextEt.setText("");
            }
        });

        //handle QR Button click, show image dialog
        QRbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonid="QRbuttonclicked";
                showInputImageDialog();
                recognizedTextEt.setText("");
            }
        });
        // handle click, start recognizing text from image taken from camera or gallery
        recognize_Text_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //check if image is picked or not, picked if imageUri is not null
                if (imageUri==null){
                    //imageUri is null which means we haven't picked image yet can't recongnize text
                    Toast.makeText(MainActivity.this,"pick image first",Toast.LENGTH_SHORT).show();
                }else{
                    //imageUri is not null which means we have picked image, we can recognize text
                    if(buttonid=="textbuttonclicked") {
                        recognizeTextFromTextImage();
                    }else {
                        recognizeTextFromQRImage();
                    }
                }
            }
        });
    }

    private void recognizeTextFromTextImage(){
        Log.d(Tag,"Recognizing Text From Image");
        progressDialog.setMessage("preparing image........");
        progressDialog.show();

        try{
            //prepare inputimage from image uri
            InputImage inputImage=InputImage.fromFilePath(this,imageUri);
            //image prepared we are about to start text recognition process change progress message
            progressDialog.setMessage("Recognizing text....");
            //start text recognition process from image
            Task<Text> textTaskResult=textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            //process completed dismiss dialog
                            progressDialog.dismiss();
                            //get the recognized text
                            String recognizedText=text.getText();
                            Log.d(Tag,"OnSuccess:Recognized Text"+recognizedText);
                            //set the recognised text to edit text
                            recognizedTextEt.setText(recognizedText);

                        }
                    })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //failed recognizing text from image dismiss dialog show reason in toast
                    progressDialog.dismiss();
                    Log.d(Tag,"On Failure: ", e);
                    Toast.makeText(MainActivity.this,"Failed recognizing text due to"+e.getMessage(),Toast.LENGTH_SHORT).show();

                }
            });

        }catch(Exception e){
            //Exception occurred while preparing input image,dismiss dialog show reason in toast
            progressDialog.dismiss();
            Log.d(Tag,"RecognizeTextFromImage: ",e);
            Toast.makeText(this,"Failed preparing image due to"+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

     //fuunction code for recognizing data from QR Code
    private void recognizeTextFromQRImage(){
        Log.d(Tag,"Recognizing Text From Image");
        progressDialog.setMessage("preparing image........");
        progressDialog.show();

        try{
            //prepare inputimage from image uri
            InputImage inputImage=InputImage.fromFilePath(this,imageUri);
            //image prepared we are about to start text recognition process change progress message
            progressDialog.setMessage("Recognizing text....");
            //start text recognition process from image
            Task<List<Barcode>> barcodeResult=barcodeScanner.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                        @Override
                        public void onSuccess(List<Barcode> barcodes) {
                            //process completed dismiss dialog
                            progressDialog.dismiss();
                            //get the recognized text
                            extractBarcodeinfo(barcodes);

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed recognizing text from image dismiss dialog show reason in toast
                            progressDialog.dismiss();
                            Log.d(Tag,"On Failure: ", e);
                            Toast.makeText(MainActivity.this,"Failed recognizing text due to"+e.getMessage(),Toast.LENGTH_SHORT).show();

                        }
                    });

        }catch(Exception e){
            //Exception occurred while preparing input image,dismiss dialog show reason in toast
            progressDialog.dismiss();
            Log.d(Tag,"RecognizeTextFromImage: ",e);
            Toast.makeText(this,"Failed preparing image due to"+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private void extractBarcodeinfo(List<Barcode> barcodes) {
        for (Barcode barcode : barcodes){
            Rect bounds=barcode.getBoundingBox();
            Point[] corners =barcode.getCornerPoints();

            String rawValue= barcode.getRawValue();
            Log.d(Tag,"ExtractbarcodeQRinfo:rawvalue"+rawValue);

            int valuetype=barcode.getValueType();

            switch (valuetype){

                case Barcode.TYPE_WIFI:{
                    Barcode.WiFi typeWifi=barcode.getWifi();

                    String ssid=""+typeWifi.getSsid();
                    String password=""+typeWifi.getPassword();
                    String encryptiontype=""+typeWifi.getEncryptionType();

                    Log.d(Tag,"extractbarcodeQRcodeinfo:ssid"+ssid);
                    Log.d(Tag,"extractbarcodeQRcodeinfo:password"+password);
                    Log.d(Tag,"extractbarcodeQRcodeinfo:encryptiontype"+encryptiontype);

                    recognizedTextEt.setText("TYPE: TYPE_WIFI \nssid:"+ssid+"\npassword:"+password+"\nencryptionType:"+encryptiontype+"\nraw value:"+rawValue);

                }
                break;

                case Barcode.TYPE_URL:{
                    Barcode.UrlBookmark typeUrl=barcode.getUrl();
                    String title=""+typeUrl.getTitle();
                    String url=""+typeUrl.getUrl();

                    Log.d(Tag,"extractBarcodeQRInfo:Type_URL");
                    Log.d(Tag,"extractBarcodeQRINFO:Title:"+title);
                    Log.d(Tag,"extractBarcodeQRINFO:url"+url);

                    recognizedTextEt.setText("TYPE: TYPE_URL \ntitle:"+title+"\nurl:"+url+"\nraw value:"+rawValue);
                }
                break;
                case Barcode.TYPE_EMAIL:{
                    Barcode.Email typeEmail=barcode.getEmail();

                    String address=""+typeEmail.getAddress();
                    String body=""+typeEmail.getBody();
                    String subject=""+typeEmail.getSubject();

                    Log.d(Tag,"extractBarcodeQRINFO:TYPE_EMAIL:");
                    Log.d(Tag,"extractBarcodeQRINFO:Address:"+address);
                    Log.d(Tag,"extractBarcodeQRINFO:body:"+body);
                    Log.d(Tag,"extractBarcodeQRINFO:subject:"+subject);

                    recognizedTextEt.setText("TYPE: TYPE_URL \ntitle:"+address+"\nbody:"+body+"\nsubject:"+subject+"\nraw value:"+rawValue);
                }
                break;
                case Barcode.TYPE_CONTACT_INFO:{
                    Barcode.ContactInfo typeContact=barcode.getContactInfo();

                    String title=""+typeContact.getTitle();
                    String organizer=""+typeContact.getOrganization();
                    String name=""+typeContact.getName().getFirst()+" "+typeContact.getName().getLast();
                    String phone=""+typeContact.getPhones().get(0).getNumber();

                    Log.d(Tag,"extractBarcodeQRINFO:TYPE_CONTACT_INFO:");
                    Log.d(Tag,"extractBarcodeQRINFO:Title:"+title);
                    Log.d(Tag,"extractBarcodeQRINFO:organizer:"+organizer);
                    Log.d(Tag,"extractBarcodeQRINFO:name:"+name);
                    Log.d(Tag,"extractBarcodeQRINFO:phone:"+phone);

                    recognizedTextEt.setText("TYPE: TYPE_URL \ntitle:"+title+"\norganizer:"+organizer+"\nname:"+name+"\nphone:"+phone+"\nraw value:"+rawValue);
                }
                break;
                default:{
                    recognizedTextEt.setText(rawValue);
                }
            }
        }
    }


    private void showInputImageDialog() {
        PopupMenu popupMenu=new PopupMenu(this,inputImageBtn);
        //add items camera and gallery to popupmenu param 2 is menu id, param 3 is position of this menu item in menu items list, param 4 is title of the menu
        popupMenu.getMenu().add(Menu.NONE,1,1,"CAMERA");
        popupMenu.getMenu().add(Menu.NONE,2,2,"GALLERY");

        //show popupmenu
        popupMenu.show();

        //handle popupmenu item clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //get item is that is clicked from popupmenu
                int id=menuItem.getItemId();
                if (id == 1) {
                    //camera is click check if camera permission are granted or not
                    Log.d(Tag,"onMenuItemClick: Camera Clicked...");
                    if (checkCameraPermission()){
                        //camera permission granted launching the camera intent
                        pickImageCamera();
                    }else{
                        //camera permissions are not granted, request the camera permissions
                        requestCameraPermission();
                    }
                }else if(id==2){
                    //gallery is clicked, check if storage permission is granted or not
                    Log.d(Tag,"onMenuItemClick: Gallery Clicked...");
                    if(checkStoragePermission()){
                        //storage permission granted,we can launch the gallery intent
                        pickImageGallery();
                    }else{
                        //storage permission not granted, request the storage permission
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });

    }

    private void pickImageGallery(){
        Log.d(Tag,"pick image Gallery");
        Intent intent=new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    // here we will be receive the image if selected
                    if(result.getResultCode()== Activity.RESULT_OK){
                        //image picked
                        Intent data=result.getData();
                        imageUri=result.getData().getData();
                        Log.d(Tag,"OnActivityReslut: imageUri"+imageUri);
                        // set to imageview
                        imageIv.setImageURI(imageUri);
                    }else{
                        Log.d(Tag,"onActivityResult:cancelled");
                        //cancelled
                        Toast.makeText(MainActivity.this,"cancelled",Toast.LENGTH_SHORT).show();
                    }

                }
            }
    );
    private void pickImageCamera(){
        Log.d(Tag,"pickImageCamera:");
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"Sample Title");
        values.put(MediaStore.Images.Media.TITLE,"Sample Description");
        imageUri=getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
        cameraActivityResultLauncher.launch(intent);

    }
    private  ActivityResultLauncher<Intent> cameraActivityResultLauncher =registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will receive the image if taken from camera
                    if(result.getResultCode()==Activity.RESULT_OK){
                        //image is taken from camera
                        // we already have the image in imageUri using function pickImageCamera()
                        Log.d(Tag,"OnActivityResult:imageUri"+imageUri);
                        imageIv.setImageURI(imageUri);
                    }else{
                        Log.d(Tag,"onActivityResult:Cancelled");
                        Toast.makeText(MainActivity.this,"cancelled",Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermission,STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        //checking the camera and storage permissions
        boolean cameraResult=ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==(PackageManager.PERMISSION_GRANTED);
        boolean storageResult=ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==(PackageManager.PERMISSION_GRANTED);

        return cameraResult && storageResult;
    }
    private void requestCameraPermission(){
        //request for camera permission
        ActivityCompat.requestPermissions(this,cameraPermission,CAMERA_REQUEST_CODE);
    }

    //handle permission results


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length>0){
                    boolean cameraAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted=grantResults[1]==PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted&&storageAccepted){
                        pickImageCamera();
                    }else{
                        Toast.makeText(this,"camera&storage permissions are required",Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(this,"cancelled",Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                //check if some action from permission dialog performed or not allow/deny
                if(grantResults.length>0){
                    //check if storage permissions granted, contains boolean results either true or false
                    boolean storageAccepted=grantResults[0]==PackageManager.PERMISSION_GRANTED;
                    //check if storage permission is granted or not
                    if(storageAccepted){
                        //storage permission granted, we canlaunch gallery intent
                        pickImageGallery();
                    }else{
                        //storage permission denied can't launch gallery
                        Toast.makeText(this,"storage permission is required",Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }

    }

}