package com.example.myouf.logoreco;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.bytedeco.javacpp.opencv_nonfree.SIFT;
import org.bytedeco.javacpp.opencv_core.Mat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Serializable {

    final String TAG=MainActivity.class.getName();
    Button captureButton;
    Button libraryButton;
    Button analysisButton;
    ImageView imageViewBase;
    Uri selectedImageUri;

    //Declaration des constantes code de retour des requetes intent
    private static final int PHOTO_LIB_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    public static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;

    //Constante representant le nombre de classes utilisees
    public static final int NUMBER_OF_CLASSES = 3;

    public static Mat[] referencesCoca;
    public static Mat[] descriptorsReferencesCoca;
    public static Mat[] referencesPepsi;
    public static Mat[] descriptorsReferencesPepsi;
    public static Mat[] referencesSprite;
    public static Mat[] descriptorsReferencesSprite;

    public static SIFT sift;

    private String mCurrentPhotoPath;

    /* Partie connexion au serveur */
    //Déclaration de la queue
    RequestQueue queueJSON;
    RequestQueue queueYML;
    RequestQueue queueClassifier;

    //URL d'accès au serveur (pour les request sur la queue)
    public static String serverUrl = "http://www-rech.telecom-lille.fr/freeorb/";
    public static List<Brand> listBrand;
    public static File fileYML;
    public static ArrayList<File> classifiersFileList;

    public static String getFileContents(final File file)throws IOException {
        final InputStream inputStream = new FileInputStream(file);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        final StringBuilder stringBuilder = new StringBuilder();
        boolean done = false;
        while (!done) {
            final String line = reader.readLine();
            done = (line == null);

            if (line != null) {
                stringBuilder.append(line);
            }
        }
        reader.close();
        inputStream.close();
        return stringBuilder.toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queueJSON = Volley.newRequestQueue(this);
        queueYML = Volley.newRequestQueue(this);
        queueClassifier = Volley.newRequestQueue(this);

        captureButton = (Button) findViewById(R.id.captureButton);
        captureButton.setOnClickListener(this);

        libraryButton = (Button) findViewById(R.id.libraryButton);
        libraryButton.setOnClickListener(this);

        analysisButton = (Button)findViewById(R.id.analysisButton);
        analysisButton.setOnClickListener(this);

        imageViewBase = (ImageView) findViewById(R.id.imageViewBase);

        //Request Json
        getJson();

        //Request YML
        getYML();
    }

    /**
     * Fonction qui génère une liste de classifier à transmettre à l'analysisActivity
     *
     * @param:brand objet brand comportant toutes les informations nécessaires au traitement
     */
    private void getClassifier(Brand brand) {
        File fileClassifier = new File(this.getFilesDir(), brand.getClassifier());
        StringRequest stringRequestClassifier = new StringRequest(Request.Method.GET, serverUrl + "classifiers/" + brand.getClassifier(),
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        FileOutputStream outputStream;
                        try {
                            outputStream = openFileOutput(brand.getClassifier(), Context.MODE_PRIVATE);
                            outputStream.write(response.getBytes());
                            outputStream.close();
                            classifiersFileList.add(fileClassifier);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "@@ ");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "Erreur récupération Classifier");
                    }
                }
        );

        queueClassifier.add(stringRequestClassifier);
    }

    /**
     * Recupération de l'index Json
     */
    private void getJson(){
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, serverUrl + "index.json",null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if (response != null) {
                            listBrand = new ArrayList<Brand>();
                            try {
                                JSONArray jsonArray = response.getJSONArray("brands");
                                if (jsonArray != null) {
                                    for (int i = 0 ; i < jsonArray.length() ; i++) {
                                        JSONObject obj = jsonArray.getJSONObject(i);

                                        JSONArray images = obj.getJSONArray("images");
                                        String[] imgNames = new String[images.length()];

                                        for (int j = 0 ; j < images.length() ; j++) {
                                            imgNames[j] = images.get(j).toString();
                                        }

                                        Brand br = new Brand(obj.getString("brandname"), obj.getString("url"), obj.getString("classifier"), imgNames);
                                        listBrand.add(br);
                                    }
                                }
                                classifiersFileList = new ArrayList<File>();
                                for (Brand brands : listBrand){
                                    getClassifier(brands);
                                }
                            } catch (Exception ex) {

                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "erreur jsonrequest");
                    }
                });

        queueJSON.add(jsonRequest);
    }

    /*
     * Recupération de fichier YML
     */
    public void getYML(){
        fileYML = new File(this.getFilesDir(), "vocabulary.yml");
        StringRequest stringRequestYML = new StringRequest(Request.Method.GET, serverUrl + "vocabulary.yml",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        FileOutputStream outputStream;
                        try {
                            outputStream = openFileOutput("vocabulary.yml", Context.MODE_PRIVATE);
                            outputStream.write(response.getBytes());
                            outputStream.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "@@ ");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // mTextView.setText("That didn't work!");
                        Log.d(TAG, "@@ marche pas");
                    }
                }
        );

        queueYML.add(stringRequestYML);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.captureButton:
                startCaptureActivity();
                break;

            case R.id.libraryButton:
                startPhotoLibraryActivity();
                break;

            case R.id.analysisButton:
                startAnalysisActivity();
                break;
        }

    }

    private Uri getUriFromDrawable(String drawableName) {
        int id = getResources().getIdentifier(drawableName, "drawable", getPackageName());
        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(id) +
                "/" + getResources().getResourceTypeName(id) +
                "/" + getResources().getResourceEntryName(id));
        return uri;
    }

    /**
     * Copie un fichier de la galerie vers le cache de l'application.
     *
     * @param context contexte de l'application pour récupérer le dossier de cache.
     * @param imgPath chemin sous forme d'Uri vers l'image dans la galerie.
     * @param fileName nom du fichier de destination.
     * @return fichier copié dans le cache de l'application.
     */
    public static File uriToCache(Context context, Uri imgPath, String fileName) {
        InputStream is;
        FileOutputStream fos;
        int size;
        byte[] buffer;
        String filePath = context.getCacheDir() + "/" + fileName;
        File file = new File(filePath);

        try {
            is = context.getContentResolver().openInputStream(imgPath);
            if (is == null) {
                return null;
            }

            size = is.available();
            buffer = new byte[size];

            if (is.read(buffer) <= 0) {
                return null;
            }

            is.close();

            fos = new FileOutputStream(filePath);
            fos.write(buffer);
            fos.close();

            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    protected void startAnalysisActivity(){
        Intent intent = new Intent(this, AnalysisActivity.class);
        if (selectedImageUri != null) {
            intent.putExtra("selectedImageUri", selectedImageUri);
            startActivity(intent);
        }
        else {
            Toast toast = Toast.makeText(this, "You have to choose a picture", Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    protected void startCaptureActivity() {
        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePicture.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePicture, CAMERA_REQUEST);
            }
        }
    }

    protected void startPhotoLibraryActivity(){
        Intent photoLibIntent = new Intent();
        photoLibIntent.setType("image/*");
        photoLibIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(photoLibIntent,PHOTO_LIB_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        if (requestCode==PHOTO_LIB_REQUEST && resultCode==RESULT_OK){
            selectedImageUri = intent.getData();
            setImageView(selectedImageUri);
        }

        if (requestCode==CAMERA_REQUEST && resultCode==RESULT_OK){
            selectedImageUri = Uri.fromFile(new File(mCurrentPhotoPath));
            setImageView(selectedImageUri);
            galleryAddPic();
        }
    }

    /**
     * Private method to get a unique file name for a new photo
     * @return A unique file name for a new photo using a date-time stamp
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName, // prefix
                ".jpg", // suffix
                storageDir // directory
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Add the photo to the Media Provider's database, making it available in the Android Gallery application and to other apps
     */
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    /**
     * This function set the imageView with the picture
     * @param selectedImageUri
     */
    public void setImageView(Uri selectedImageUri){
        if (checkPermissionREAD_EXTERNAL_STORAGE(this)) {
            Bitmap sourceBitmap = null;
            try {
                sourceBitmap = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(selectedImageUri), null, null);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            imageViewBase.setImageBitmap(sourceBitmap);
        }
    }


    /**
     * This function set the imageView with the selected picture
     * @param selectedImageUri
     */
    public void setImageViewContent(Uri selectedImageUri){
        if (checkPermissionREAD_EXTERNAL_STORAGE(this)) {
            try {
                Bitmap srcBmp = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(selectedImageUri), null, null);
                imageViewBase.setImageBitmap(srcBmp);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * This function check if we have the rights to read the external storage
     * @param context
     * @return Boolean
     */
    public boolean checkPermissionREAD_EXTERNAL_STORAGE(
            final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (Activity) context,
                        Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showDialog("External storage", context,
                            Manifest.permission.READ_EXTERNAL_STORAGE);

                } else {
                    ActivityCompat
                            .requestPermissions(
                                    (Activity) context,
                                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }

        } else {
            return true;
        }
    }


    /**
     * This function ask the permission to the user to continue
     * @param msg
     * @param context
     * @param permission
     */
    public void showDialog(final String msg, final Context context,
                           final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true);
        alertBuilder.setTitle("Permission necessary");
        alertBuilder.setMessage(msg + " permission is necessary");
        alertBuilder.setPositiveButton(android.R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions((Activity) context,
                                new String[] { permission },
                                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                    }
                });
        AlertDialog alert = alertBuilder.create();
        alert.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do your stuff
                } else {

                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions,
                        grantResults);
        }
    }

}
