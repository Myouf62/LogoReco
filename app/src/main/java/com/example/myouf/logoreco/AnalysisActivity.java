package com.example.myouf.logoreco;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import static com.example.myouf.logoreco.MainActivity.classifiersFileList;
import static com.example.myouf.logoreco.MainActivity.listBrand;
import static com.example.myouf.logoreco.MainActivity.uriToCache;

import static org.bytedeco.javacpp.opencv_features2d.KeyPoint;
import static org.bytedeco.javacpp.opencv_highgui.imread;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d.FlannBasedMatcher;
import org.bytedeco.javacpp.opencv_features2d.BOWImgDescriptorExtractor;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_nonfree.SIFT;
import org.bytedeco.javacpp.opencv_ml.CvSVM;

/**
 * Analysis activity launched when we want to analyse a picture
 */
public class AnalysisActivity extends AppCompatActivity {

    // Useful variables
    ImageView imageViewResult;
    TextView textViewAnalysis;
    Uri selectedImageUri;
    ProgressDialog progressDialog;
    Spanned resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        imageViewResult = (ImageView) findViewById(R.id.imageViewResult);
        textViewAnalysis = (TextView) findViewById(R.id.textViewAnalysis);

        // Make links clickable
        textViewAnalysis.setMovementMethod(LinkMovementMethod.getInstance());
        Intent i = getIntent();

        selectedImageUri = i.getParcelableExtra("selectedImageUri");

        // Show the progress dialog
        progressDialog = ProgressDialog.show(this, "Please wait", "Analysis of your image is in progress...", true);

        // Run the treatment of analysis in another thread
        new Thread((new Runnable() {
            @Override
            public void run() {
                startAnalysis();
                // Once the analysis is finished, remove the progress dialog
                progressDialog.dismiss();
            }
        })).start();
    }

    /**
     * Analysis between the class images and the chosen picture
     */
    private void startAnalysis() {

        final Mat vocabulary;

        Loader.load(opencv_core.class);

        opencv_core.CvFileStorage storage = opencv_core.cvOpenFileStorage(this.getFilesDir() + "/vocabulary.yml", null, opencv_core.CV_STORAGE_READ);
        Pointer p = opencv_core.cvReadByName(storage, null, "vocabulary", opencv_core.cvAttrList());
        opencv_core.CvMat cvMat = new opencv_core.CvMat(p);
        vocabulary = new opencv_core.Mat(cvMat);
        opencv_core.cvReleaseFileStorage(storage);

        // Create SIFT feature point extracter
        final SIFT detector;
        // Default parameters
        detector = new SIFT(0, 3, 0.04, 10, 1.6);

        // Create a matcher with FlannBased Euclidien distance (possible also with BruteForce-Hamming)
        final FlannBasedMatcher matcher;
        matcher = new FlannBasedMatcher();

        // Create BoF (or BoW) descriptor extractor
        final BOWImgDescriptorExtractor bowide;
        bowide = new BOWImgDescriptorExtractor(detector.asDescriptorExtractor(), matcher);

        // Set the dictionary with the vocabulary we created in the first step
        bowide.setVocabulary(vocabulary);

        final CvSVM[] classifiers;
        classifiers = new CvSVM[classifiersFileList.size()];
        for (int i = 0 ; i < classifiersFileList.size() ; i++) {
            classifiers[i] = new CvSVM();
            classifiers[i].load(this.getFilesDir() + "/" + classifiersFileList.get(i).getName());
        }

        Mat response_hist = new Mat();
        KeyPoint keypoints = new KeyPoint();
        Mat inputDescriptors = new Mat();

        File file = uriToCache(this, selectedImageUri, "imageToTreat");

        Mat image = imread(file.getAbsolutePath());
        opencv_imgproc.resize(image, image, new opencv_core.Size(500, 700));
        detector.detectAndCompute(image, Mat.EMPTY, keypoints, inputDescriptors);
        bowide.compute(image, keypoints, response_hist);

        // Finding best match
        float minf = Float.MAX_VALUE;
        String bestMatch = null;

        // Loop for all classes
        for (int i = 0; i < classifiersFileList.size(); i++) {
            // classifier prediction based on reconstructed histogram
            float res = classifiers[i].predict(response_hist, true);
            if (res < minf) {
                minf = res;
                bestMatch = classifiersFileList.get(i).getName();
            }
        }

        for (Brand brand : listBrand) {
            if (brand.getClassifier().equals(bestMatch)) {
                resultText = Html.fromHtml("<a href='" + brand.getUrl() + "'>" + brand.getBrandName() + "</a>");
                AnalysisActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageViewResult.setImageURI(selectedImageUri);
                        textViewAnalysis.append(resultText);
                    }
                });
            }
        }
    }
}
