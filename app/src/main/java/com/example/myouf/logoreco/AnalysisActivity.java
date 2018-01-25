package com.example.myouf.logoreco;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static com.example.myouf.logoreco.MainActivity.uriToCache;
import static org.bytedeco.javacpp.opencv_core.NORM_L2;
import static org.bytedeco.javacpp.opencv_features2d.drawKeypoints;
import static org.bytedeco.javacpp.opencv_features2d.drawMatches;
import static org.bytedeco.javacpp.opencv_highgui.imshow;
import static org.bytedeco.javacpp.opencv_highgui.namedWindow;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
import static org.bytedeco.javacpp.opencv_highgui.imread;

import org.bytedeco.javacpp.opencv_features2d.DMatch;
import org.bytedeco.javacpp.opencv_features2d.DMatchVectorVector;
//import org.bytedeco.javacpp.opencv_features2d.DMatchVectorVectorVector;
import static org.bytedeco.javacpp.opencv_features2d.KeyPoint;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d.BFMatcher;
//import org.bytedeco.javacpp.opencv_imgcodecs;
//import org.bytedeco.javacpp.opencv_shape;

public class AnalysisActivity extends AppCompatActivity {

    ImageView imageViewResult;
    TextView textViewAnalysis;
    Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        imageViewResult = (ImageView) findViewById(R.id.imageViewResult);
        textViewAnalysis = (TextView) findViewById(R.id.textViewAnalysis);

        Intent i = getIntent();
        selectedImageUri = i.getParcelableExtra("selectedImageUri");

        startAnalysis();
    }

    /**
     * Analysis between the class images and the chosen picture
     */
    private void startAnalysis() {
        File file = uriToCache(this,selectedImageUri,"imageToTreat");

        Mat image = imread(file.getAbsolutePath());

        Mat descriptorImage = new Mat();
        KeyPoint keyPointsImage = new KeyPoint();

        // Detecting chosen picture
        // sift.detect(image, keyPointsImage);
        // sift.compute(image, keyPointsImage, descriptorImage);
    }
}
