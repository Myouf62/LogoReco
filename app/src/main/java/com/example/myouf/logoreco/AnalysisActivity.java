package com.example.myouf.logoreco;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import static org.bytedeco.javacpp.opencv_features2d.drawKeypoints;
import static org.bytedeco.javacpp.opencv_features2d.drawMatches;
import static org.bytedeco.javacpp.opencv_highgui.WINDOW_AUTOSIZE;
import static org.bytedeco.javacpp.opencv_highgui.imshow;
import static org.bytedeco.javacpp.opencv_highgui.namedWindow;
import static org.bytedeco.javacpp.opencv_highgui.waitKey;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.RealSense;
import org.bytedeco.javacpp.opencv_calib3d;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.DMatch;
import org.bytedeco.javacpp.opencv_core.DMatchVector;
import org.bytedeco.javacpp.opencv_core.KeyPointVector;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_core.Scalar;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_features2d.BFMatcher;
import org.bytedeco.javacpp.opencv_features2d.DrawMatchesFlags;
import org.bytedeco.javacpp.opencv_highgui;
import org.bytedeco.javacpp.opencv_imgcodecs;
import org.bytedeco.javacpp.opencv_shape;
import org.bytedeco.javacpp.opencv_xfeatures2d.SIFT;

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

        testOfAdaptedTP4();
    }

    private void testOfAdaptedTP4() {
        File file = uriToCache(this,selectedImageUri,"imageToTreat");

        Mat image = imread(file.getAbsolutePath());

        Mat descriptorImage = new Mat();
        KeyPointVector keyPointsImage = new KeyPointVector();

        // Détection de l'image de base à comparer
        sift.detect(image, keyPointsImage);
        sift.compute(image, keyPointsImage, descriptorImage);

        // Création du matcher
        BFMatcher matcher = new BFMatcher(NORM_L2, false);
        DMatchVector matches = new DMatchVector();

        // Stockage des distances moyennes pour chaque classe
        float[] distanceMoyennesCoca = new float[NUMBER_OF_CLASSES];
        float[] distanceMoyennesPepsi = new float[NUMBER_OF_CLASSES];
        float[] distanceMoyennesSprite = new float[NUMBER_OF_CLASSES];
        // Classe Coca
        for (int i = 0; i < referencesCoca.length; i++) {
            matcher.match(descriptorImage, descriptorsReferencesCoca[i], matches);
            DMatchVector bestMatches = selectBest(matches,25);
            float distanceMoyenne = calculateAverageDistance(bestMatches);
            distanceMoyennesCoca[i] = distanceMoyenne;
        }
        // Classe Pepsi
        for (int i = 0; i < referencesPepsi.length; i++) {
            matcher.match(descriptorImage, descriptorsReferencesPepsi[i], matches);
            DMatchVector bestMatches = selectBest(matches,25);
            float distanceMoyenne = calculateAverageDistance(bestMatches);
            distanceMoyennesPepsi[i] = distanceMoyenne;
        }
        // Classe Sprite
        for (int i = 0; i < referencesSprite.length; i++) {
            matcher.match(descriptorImage, descriptorsReferencesSprite[i], matches);
            DMatchVector bestMatches = selectBest(matches,25);
            float distanceMoyenne = calculateAverageDistance(bestMatches);
            distanceMoyennesSprite[i] = distanceMoyenne;
        }

        // Création d'un dictionnaire
        // Clés => noms des boissons
        // Valeurs => distances moyennes
        Map<String, Float> dictionnary = new HashMap<>();
        for (int i=0 ; i<distanceMoyennesCoca.length ; i++) { dictionnary.put("Coca"+i, distanceMoyennesCoca[i]); }
        for (int i=0 ; i<distanceMoyennesPepsi.length ; i++) { dictionnary.put("Pepsi"+i, distanceMoyennesPepsi[i]); }
        for (int i=0 ; i<distanceMoyennesSprite.length ; i++) { dictionnary.put("Sprite"+i, distanceMoyennesSprite[i]); }

        // Affichage du dictionnaire en console
        Log.i("foo","- Contenu du premier dictionnaire contenant toutes les distances moyennes : ");
        Set<Entry<String, Float>> setHm = dictionnary.entrySet();
        Iterator<Entry<String, Float>> it = setHm.iterator();
        while(it.hasNext()){
            Entry<String, Float> e = it.next();
            Log.i("foo","|" + e.getKey() + " : " + e.getValue());
        }

        // Création d'un nouveau dictionnaire contenant les 3 plus petites distances trouvées
        Map<String, Float> minDistances = new HashMap<>();
        Entry<String, Float> min;
        for (int i=0 ; i<3 ; i++) {
            min = null;
            for (Entry<String, Float> entry : dictionnary.entrySet()) {
                if (min == null || min.getValue() > entry.getValue()) {
                    min = entry;
                }
            }
            // Ajout dans le nouveau dictionnaire
            minDistances.put(min.getKey(), min.getValue());
            // Suppression dans l'ancien dictionnaire
            dictionnary.remove(min.getKey());
        }

        // Affichage du nouveau dictionnaire en console
        Log.i("foo","- Contenu du dictionnaire final contenant les 3 plus petites distances : ");
        setHm = minDistances.entrySet();
        it = setHm.iterator();
        while(it.hasNext()){
            Entry<String, Float> e = it.next();
            Log.i("foo","|" + e.getKey() + " : " + e.getValue());
        }

        // Décompte par classe
        int nbGroupCoca = 0;
        int nbGroupPepsi = 0;
        int nbGroupSprite = 0;
        setHm = minDistances.entrySet();
        it = setHm.iterator();
        while(it.hasNext()){
            Entry<String, Float> e = it.next();
            if (e.getKey().contains("Coca")) { nbGroupCoca++; }
            else if (e.getKey().contains("Pepsi")) { nbGroupPepsi++; }
            else if (e.getKey().contains("Sprite")) { nbGroupSprite++; }
        }

        if(nbGroupCoca >= nbGroupPepsi && nbGroupCoca >= nbGroupSprite) {
            imageViewResult.setImageResource(R.drawable.logo_coca);
            textViewAnalysis.append(" Coca-Cola");
        }
        else if(nbGroupPepsi >= nbGroupCoca && nbGroupPepsi >= nbGroupSprite) {
            imageViewResult.setImageResource(R.drawable.logo_pepsi);
            textViewAnalysis.append(" Pepsi");
        }
        else if(nbGroupSprite >= nbGroupCoca && nbGroupSprite >= nbGroupPepsi) {
            imageViewResult.setImageResource(R.drawable.logo_sprite);
            textViewAnalysis.append(" Sprite");
        }
    }

    static float calculateAverageDistance(DMatchVector bestMatches) {
        float Dm = 0;
        for(int i=0 ; i<bestMatches.size() ; i++) {
            Dm = Dm + bestMatches.get(i).distance();
        }
        Dm = Dm/bestMatches.size();
        return Dm;
    }

    /**
     * Sélectionner les meilleurs points matchés entre deux images
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

    static DMatchVector selectGoodMatches(DMatchVector matches) {
        ArrayList<DMatch> good = new ArrayList<DMatch>();
        DMatch[] sorted = toArray(matches);
        Arrays.sort(sorted, (a, b) -> {
            return a.lessThan(b) ? -1 : 1;
        });

        for(int i =0; i < sorted.length -1;++i)
        {
            float dis1 = sorted[i].distance();
            float dis2 = sorted[i+1].distance();
            if(dis1 < 0.6*dis2)
            {
                good.add(sorted[i]);
            }
        }
        DMatch[] goodArray = new DMatch[good.size()];
        good.toArray(goodArray);
        return new DMatchVector(goodArray);
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