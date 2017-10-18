package com.example.myouf.logoreco;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button captureButton;
    Button libraryButton;
    Button analysisButton;
    ImageView imageView;

    //Declaration des constantes code de retour des requetes intent
    private static final int PHOTO_LIB_REQUEST = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        captureButton = (Button) findViewById(R.id.captureButton);
        captureButton.setOnClickListener(this);

        libraryButton = (Button) findViewById(R.id.libraryButton);
        libraryButton.setOnClickListener(this);

        analysisButton = (Button)findViewById(R.id.analysisButton);
        analysisButton.setOnClickListener(this);

        imageView = (ImageView) findViewById(R.id.imageView);

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){

            case R.id.captureButton:
                //startCaptureActivity();
                break;

            case R.id.libraryButton:
                startPhotoLibraryActivity();
                break;

            case R.id.analysisButton:
                //startAnalysisActivity();
                break;
        }

    }

    protected void startPhotoLibraryActivity(){
        Intent photoLibIntent = new Intent();
        photoLibIntent.setType("image/*");
        photoLibIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(photoLibIntent,PHOTO_LIB_REQUEST);
    }
    
}
