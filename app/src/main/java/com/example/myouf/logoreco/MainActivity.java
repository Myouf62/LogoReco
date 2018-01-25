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
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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


import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_nonfree;
import org.bytedeco.javacpp.opencv_nonfree.SIFT;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_ml;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static org.bytedeco.javacpp.opencv_highgui.imread;
import static org.bytedeco.javacpp.opencv_features2d.KeyPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.data;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Serializable {

    final String TAG=MainActivity.class.getName();
    String pathToPhoto;
    Bitmap photoBitmap;
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

    /* Partie connexion au serveur */
    //Déclaration de la queue
    RequestQueue queueJSON;
    RequestQueue queueYML;
    RequestQueue queueClassifier;

    //URL d'accès au serveur (pour les request sur la queue)
    String serverUrl = "http://www-rech.telecom-lille.fr/nonfreesift/";
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
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePicture, CAMERA_REQUEST);
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

    protected void startPhotoLibraryActivity(){
        Intent photoLibIntent = new Intent();
        photoLibIntent.setType("image/*");
        photoLibIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(photoLibIntent,PHOTO_LIB_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        if (requestCode==PHOTO_LIB_REQUEST && resultCode==RESULT_OK){
            selectedImageUri = intent.getData();
            setImageViewContentFromLibrary(selectedImageUri);
        }

        if (requestCode==CAMERA_REQUEST && resultCode==RESULT_OK){
            selectedImageUri = intent.getData();
            setImageViewContentFromCamera(selectedImageUri);
        }
    }

    /**
     * This function set the imageView with the picture taken directly with the camera
     * @param selectedImageUri
     */
    public void setImageViewContentFromCamera(Uri selectedImageUri){
        if (checkPermissionREAD_EXTERNAL_STORAGE(this)) {
            try {
                String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
                Cursor cur = getContentResolver().query(selectedImageUri, orientationColumn, null, null, null);
                int orientation = -1;
                if (cur != null && cur.moveToFirst()) {
                    orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]));
                }
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                Bitmap sourceBitmap = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(selectedImageUri), null, null);
                ExifInterface exif = new ExifInterface(selectedImageUri.getPath());
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Bitmap adjustedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
                imageViewBase.setImageBitmap(adjustedBitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This function set the imageView with the picture selected in the gallery
     * @param selectedImageUri
     */
    public void setImageViewContentFromLibrary(Uri selectedImageUri){
        try {
            int rotation=0;
            Bitmap sourceBitmap = BitmapFactory.decodeStream(this.getContentResolver().openInputStream(selectedImageUri), null, null);
            Matrix matrix = new Matrix();
            if (sourceBitmap.getHeight() < sourceBitmap.getWidth()){
                rotation=90;
            }
            matrix.preRotate(rotation);
            Bitmap adjustedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix, true);
            imageViewBase.setImageBitmap(adjustedBitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
