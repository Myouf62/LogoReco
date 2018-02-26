package com.example.myouf.logoreco;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import static com.example.myouf.logoreco.MainActivity.classifiersFileList;
import static com.example.myouf.logoreco.MainActivity.uriToCache;
import static org.bytedeco.javacpp.opencv_highgui.imread;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.MatVector;
import static org.bytedeco.javacpp.opencv_features2d.KeyPoint;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d.FlannBasedMatcher;
import org.bytedeco.javacpp.opencv_features2d.BOWImgDescriptorExtractor;
import org.bytedeco.javacpp.opencv_nonfree.SIFT;

import org.bytedeco.javacpp.opencv_ml.CvSVM;

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

        final Mat vocabulary;

        Loader.load(opencv_core.class);
        opencv_core.CvFileStorage storage = opencv_core.cvOpenFileStorage(this.getFilesDir() + "vocabulary.yml", null, opencv_core.CV_STORAGE_READ);
        Pointer p = opencv_core.cvReadByName(storage, null, "vocabulary", opencv_core.cvAttrList());
        opencv_core.CvMat cvMat = new opencv_core.CvMat(p);
        vocabulary = new opencv_core.Mat(cvMat);
        opencv_core.cvReleaseFileStorage(storage);

        //create SIFT feature point extracter
        final SIFT detector;
        // default parameters ""opencv2/features2d/features2d.hpp""
        detector = new SIFT(0, 3, 0.04, 10, 1.6);

        //create a matcher with FlannBased Euclidien distance (possible also with BruteForce-Hamming)
        final FlannBasedMatcher matcher;
        matcher = new FlannBasedMatcher();

        //create BoF (or BoW) descriptor extractor
        final BOWImgDescriptorExtractor bowide;
        bowide = new BOWImgDescriptorExtractor(detector.asDescriptorExtractor(), matcher);

        //Set the dictionary with the vocabulary we created in the first step
        bowide.setVocabulary(vocabulary);

        final CvSVM[] classifiers;
        classifiers = new CvSVM[classifiersFileList.size()];
        for (int i = 0 ; i < classifiersFileList.size() ; i++) {
            classifiers[i] = new CvSVM();
            classifiers[i].load(this.getFilesDir() + classifiersFileList.get(i).getName());
        }

        Mat response_hist = new Mat();
        KeyPoint keypoints = new KeyPoint();
        Mat inputDescriptors = new Mat();

        MatVector imagesVec;

        File file = uriToCache(this,selectedImageUri,"imageToTreat");

        Mat image = imread(file.getAbsolutePath());
        detector.detectAndCompute(image, Mat.EMPTY, keypoints, inputDescriptors);
        bowide.compute(image, keypoints, response_hist);

        // Finding best match
        float minf = Float.MAX_VALUE;
        String bestMatch = null;

        long timePrediction = System.currentTimeMillis();

        // Loop for all classes
        for (int i = 0; i < classifiersFileList.size(); i++) {
            // classifier prediction based on reconstructed histogram
            float res = classifiers[i].predict(response_hist, true);
            if (res < minf) {
                minf = res;
                bestMatch = classifiersFileList.get(i).getName();
            }
        }

        timePrediction = System.currentTimeMillis() - timePrediction;

    }
}
