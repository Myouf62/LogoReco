package com.example.myouf.logoreco;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
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

import static com.example.myouf.logoreco.MainActivity.NUMBER_OF_CLASSES;
import static com.example.myouf.logoreco.MainActivity.descriptorsReferencesCoca;
import static com.example.myouf.logoreco.MainActivity.descriptorsReferencesPepsi;
import static com.example.myouf.logoreco.MainActivity.descriptorsReferencesSprite;
import static com.example.myouf.logoreco.MainActivity.referencesCoca;
import static com.example.myouf.logoreco.MainActivity.referencesPepsi;
import static com.example.myouf.logoreco.MainActivity.referencesSprite;
import static com.example.myouf.logoreco.MainActivity.sift;
import static com.example.myouf.logoreco.MainActivity.uriToCache;

import static org.bytedeco.javacpp.opencv_core.NORM_L2;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.DMatch;
import org.bytedeco.javacpp.opencv_core.DMatchVector;
import org.bytedeco.javacpp.opencv_core.KeyPointVector;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_features2d.BFMatcher;
import org.bytedeco.javacpp.opencv_imgproc;

/**
 * Analysis activity launched when we want analyse a picture
 */
public class AnalysisActivity extends AppCompatActivity {

    // Useful variables
    ImageView imageViewResult;
    TextView textViewAnalysis;
    Uri selectedImageUri;
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

        startAnalysis();
    }

    /**
     * Analysis between the class images and the chosen picture
     */
    private void startAnalysis() {
        File file = uriToCache(this,selectedImageUri,"imageToTreat");

        Mat image = imread(file.getAbsolutePath());
        opencv_imgproc.resize(image, image, new opencv_core.Size(500, 700));

        Mat descriptorImage = new Mat();
        KeyPointVector keyPointsImage = new KeyPointVector();

        // Detecting chosen picture
        sift.detect(image, keyPointsImage);
        sift.compute(image, keyPointsImage, descriptorImage);

        // Creating a matcher
        BFMatcher matcher = new BFMatcher(NORM_L2, false);
        DMatchVector matches = new DMatchVector();

        // Storage of average distances for each class
        float[] distanceMoyennesCoca = new float[NUMBER_OF_CLASSES];
        float[] distanceMoyennesPepsi = new float[NUMBER_OF_CLASSES];
        float[] distanceMoyennesSprite = new float[NUMBER_OF_CLASSES];

        // Coca-Cola class
        for (int i = 0; i < referencesCoca.length; i++) {
            matcher.match(descriptorImage, descriptorsReferencesCoca[i], matches);
            DMatchVector bestMatches = selectBest(matches,25);
            float distanceMoyenne = calculateAverageDistance(bestMatches);
            distanceMoyennesCoca[i] = distanceMoyenne;
        }

        // Pepsi class
        for (int i = 0; i < referencesPepsi.length; i++) {
            matcher.match(descriptorImage, descriptorsReferencesPepsi[i], matches);
            DMatchVector bestMatches = selectBest(matches,25);
            float distanceMoyenne = calculateAverageDistance(bestMatches);
            distanceMoyennesPepsi[i] = distanceMoyenne;
        }

        // Sprite class
        for (int i = 0; i < referencesSprite.length; i++) {
            matcher.match(descriptorImage, descriptorsReferencesSprite[i], matches);
            DMatchVector bestMatches = selectBest(matches,25);
            float distanceMoyenne = calculateAverageDistance(bestMatches);
            distanceMoyennesSprite[i] = distanceMoyenne;
        }

        // Creating a dictionary
        // Keys => drinks names
        // Values => average distances
        Map<String, Float> dictionnary = new HashMap<>();
        for (int i = 0 ; i < distanceMoyennesCoca.length ; i++) { dictionnary.put("Coca" + i, distanceMoyennesCoca[i]); }
        for (int i = 0 ; i < distanceMoyennesPepsi.length ; i++) { dictionnary.put("Pepsi" + i, distanceMoyennesPepsi[i]); }
        for (int i = 0 ; i < distanceMoyennesSprite.length ; i++) { dictionnary.put("Sprite" + i, distanceMoyennesSprite[i]); }

        // Displaying the dictionary in console
        Log.i("foo","- Contenu du premier dictionnaire contenant toutes les distances moyennes : ");
        Set<Entry<String, Float>> setHm = dictionnary.entrySet();
        Iterator<Entry<String, Float>> it = setHm.iterator();
        while(it.hasNext()) {
            Entry<String, Float> e = it.next();
            Log.i("foo","|" + e.getKey() + " : " + e.getValue());
        }

        // Creation of a new dictionary containing the 3 smallest distances found
        Map<String, Float> minDistances = new HashMap<>();
        Entry<String, Float> min;
        for (int i = 0 ; i < 3 ; i++) {
            min = null;
            for (Entry<String, Float> entry : dictionnary.entrySet()) {
                if (min == null || min.getValue() > entry.getValue()) {
                    min = entry;
                }
            }
            // Adding in the new dictionary
            minDistances.put(min.getKey(), min.getValue());
            // Removing in the old dictionary
            dictionnary.remove(min.getKey());
        }

        // Displaying the new dictionary in console
        Log.i("foo","- Contenu du dictionnaire final contenant les 3 plus petites distances : ");
        setHm = minDistances.entrySet();
        it = setHm.iterator();
        while(it.hasNext()) {
            Entry<String, Float> e = it.next();
            Log.i("foo","|" + e.getKey() + " : " + e.getValue());
        }

        // Count by class
        int nbGroupCoca = 0;
        int nbGroupPepsi = 0;
        int nbGroupSprite = 0;
        setHm = minDistances.entrySet();
        it = setHm.iterator();
        while(it.hasNext()) {
            Entry<String, Float> e = it.next();
            if (e.getKey().contains("Coca")) { nbGroupCoca++; }
            else if (e.getKey().contains("Pepsi")) { nbGroupPepsi++; }
            else if (e.getKey().contains("Sprite")) { nbGroupSprite++; }
        }

        // Final conditions
        if(nbGroupCoca >= nbGroupPepsi && nbGroupCoca >= nbGroupSprite) {
            imageViewResult.setImageResource(R.drawable.logo_coca);
            resultText = Html.fromHtml("<a href='https://www.cocacola.fr/accueil/'>Coca-Cola</a>");
            textViewAnalysis.append(resultText);
        }
        else if(nbGroupPepsi >= nbGroupCoca && nbGroupPepsi >= nbGroupSprite) {
            imageViewResult.setImageResource(R.drawable.logo_pepsi);
            resultText = Html.fromHtml("<a href='https://www.pepsi.com/en-us/'>Pepsi</a>");
            textViewAnalysis.append(resultText);
        }
        else if(nbGroupSprite >= nbGroupCoca && nbGroupSprite >= nbGroupPepsi) {
            imageViewResult.setImageResource(R.drawable.logo_sprite);
            resultText = Html.fromHtml("<a href='https://www.sprite.tm.fr/fr/home/'>Sprite</a>");
            textViewAnalysis.append(resultText);
        }
    }

    static float calculateAverageDistance(DMatchVector bestMatches) {
        float Dm = 0;
        for(int i = 0 ; i < bestMatches.size() ; i++) {
            Dm = Dm + bestMatches.get(i).distance();
        }
        Dm = Dm/bestMatches.size();
        return Dm;
    }

    /**
     * Select the best matched points between two images
     * @param matches
     * @param numberToSelect
     * @return
     */
    static DMatchVector selectBest(DMatchVector matches, int numberToSelect) {
        Log.i("foo","size: " + matches.size());
        DMatch[] sorted = toArray(matches);
        Arrays.sort(sorted, (a, b) -> {
            return a.lessThan(b) ? -1 : 1;
        });
        DMatch[] best = Arrays.copyOf(sorted, numberToSelect);
        return new DMatchVector(best);
    }

    static DMatch[] toArray(DMatchVector matches) {
        assert matches.size() <= Integer.MAX_VALUE;
        int n = (int) matches.size();
        // Convert keyPoints to Scala sequence
        DMatch[] result = new DMatch[n];
        for (int i=0 ; i<n; i++) {
            result[i] = new DMatch(matches.get(i));
        }
        return result;
    }
}