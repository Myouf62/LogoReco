package com.example.myouf.logoreco;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        imageViewResult = (ImageView) findViewById(R.id.imageViewResult);
        // En guise de test, nous utilisons pour le moment l'image base de Coca utilisée dans le TP4
        imageViewResult.setImageResource(R.drawable.coca_image_base);

        testOfAdaptedTP4();
    }

    private void testOfAdaptedTP4() {
        // Image de base que l'on va comparer aux différentes images classées
        // Il est possible de remplacer "\\cocaImageBase.jpg" par "\\spriteImageBase.jpg"
        //String pathImage = "drawable://" + R.drawable.coca_image_base;
        //Uri path = Uri.parse("android.resource://res/drawable/drawable.coca_image");
        Uri path = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.coca_image_base) +
                "/" + getResources().getResourceTypeName(R.drawable.coca_image_base) +
                "/" + getResources().getResourceEntryName(R.drawable.coca_image_base));
        String pathImage = path.getPath();
        File file = galleryToCache(this,path,"imageToTreat");
        Log.i("foo", "path_image = " + pathImage);
        //Mat image = imread(getResources().getDrawable(R.drawable.coca_image_base,null).toString());

        Mat image = imread(file.getAbsolutePath());

        Mat descriptorImage = new Mat();
        KeyPointVector keyPointsImage = new KeyPointVector();

        // Lecture des images par classe
        // Classe Coca
        Uri uriCoca1 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.coca_1) +
                "/" + getResources().getResourceTypeName(R.drawable.coca_1) +
                "/" + getResources().getResourceEntryName(R.drawable.coca_1));
        File fileCoca1 = galleryToCache(this,uriCoca1,"coca_1");
        Uri uriCoca2 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.coca_2) +
                "/" + getResources().getResourceTypeName(R.drawable.coca_2) +
                "/" + getResources().getResourceEntryName(R.drawable.coca_2));
        File fileCoca2 = galleryToCache(this,uriCoca2,"coca_2");
        Uri uriCoca3 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.coca_3) +
                "/" + getResources().getResourceTypeName(R.drawable.coca_3) +
                "/" + getResources().getResourceEntryName(R.drawable.coca_3));
        File fileCoca3 = galleryToCache(this,uriCoca3,"coca_3");
        Mat modelCoca1 = imread(fileCoca1.getAbsolutePath());
        Mat modelCoca2 = imread(fileCoca2.getAbsolutePath());
        Mat modelCoca3 = imread(fileCoca3.getAbsolutePath());
        Mat[] referencesCoca = {modelCoca1,modelCoca2,modelCoca3};
        Mat[] descriptorsReferencesCoca = {new Mat(),new Mat(),new Mat()};
        KeyPointVector[] keyPointsCoca = {new KeyPointVector(),new KeyPointVector(),new KeyPointVector()};

        // Classe Pepsi
        Uri uriPepsi1 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.pepsi_1) +
                "/" + getResources().getResourceTypeName(R.drawable.pepsi_1) +
                "/" + getResources().getResourceEntryName(R.drawable.pepsi_1));
        File filePepsi1 = galleryToCache(this,uriPepsi1,"pepsi_1");
        Uri uriPepsi2 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.pepsi_2) +
                "/" + getResources().getResourceTypeName(R.drawable.pepsi_2) +
                "/" + getResources().getResourceEntryName(R.drawable.pepsi_2));
        File filePepsi2 = galleryToCache(this,uriPepsi2,"pepsi_2");
        Uri uriPepsi3 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.pepsi_3) +
                "/" + getResources().getResourceTypeName(R.drawable.pepsi_3) +
                "/" + getResources().getResourceEntryName(R.drawable.pepsi_3));
        File filePepsi3 = galleryToCache(this,uriPepsi3,"pepsi_3");
        Mat modelPepsi1 = imread(filePepsi1.getAbsolutePath());
        Mat modelPepsi2 = imread(filePepsi2.getAbsolutePath());
        Mat modelPepsi3 = imread(filePepsi3.getAbsolutePath());
        Mat[] referencesPepsi = {modelPepsi1,modelPepsi2,modelPepsi3};
        Mat[] descriptorsReferencesPepsi = {new Mat(),new Mat(),new Mat()};
        KeyPointVector[] keyPointsPepsi = {new KeyPointVector(),new KeyPointVector(),new KeyPointVector()};

        // Classe Sprite
        Uri uriSprite1 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.sprite_1) +
                "/" + getResources().getResourceTypeName(R.drawable.sprite_1) +
                "/" + getResources().getResourceEntryName(R.drawable.sprite_1));
        File fileSprite1 = galleryToCache(this,uriSprite1,"sprite_1");
        Uri uriSprite2 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.sprite_2) +
                "/" + getResources().getResourceTypeName(R.drawable.sprite_2) +
                "/" + getResources().getResourceEntryName(R.drawable.sprite_2));
        File fileSprite2 = galleryToCache(this,uriSprite2,"sprite_2");
        Uri uriSprite3 = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + getResources().getResourcePackageName(R.drawable.sprite_3) +
                "/" + getResources().getResourceTypeName(R.drawable.sprite_3) +
                "/" + getResources().getResourceEntryName(R.drawable.sprite_3));
        File fileSprite3 = galleryToCache(this,uriSprite3,"sprite_3");
        Mat modelSprite1 = imread(fileSprite1.getAbsolutePath());
        Mat modelSprite2 = imread(fileSprite2.getAbsolutePath());
        Mat modelSprite3 = imread(fileSprite3.getAbsolutePath());
        Mat[] referencesSprite = {modelSprite1,modelSprite2,modelSprite3};
        Mat[] descriptorsReferencesSprite = {new Mat(),new Mat(),new Mat()};
        KeyPointVector[] keyPointsSprite = {new KeyPointVector(),new KeyPointVector(),new KeyPointVector()};

        // Utilisation de SIFT
        int nFeatures = 0;					// Nombre de meilleures caractéristiques à retenir
        int nOctaveLayers = 3;				// Nombre de couches dans chaque octave
        double contrastThreshold = 0.03;	// Seuil de contraste utilisé pour filtrer les caractéristiques faibles en régions semi-uniformes
        int edgeThreshold = 10;				// Seuil utilisé pour filtrer les caractéristiques de pointe
        double sigma = 1.6;					// Sigma de la gaussienne appliquée à l'image d'entrée à l'octave

        Loader.load(opencv_calib3d.class);
        Loader.load(opencv_shape.class);
        SIFT sift;
        sift = SIFT.create(nFeatures, nOctaveLayers, contrastThreshold, edgeThreshold, sigma);

        // Détection de l'image de base à comparer
        sift.detect(image, keyPointsImage);
        sift.compute(image, keyPointsImage, descriptorImage);

        // Détection des autres images classées
        for (int i = 0; i < referencesCoca.length; i++) {
            sift.detect(referencesCoca[i], keyPointsCoca[i]);
            sift.compute(referencesCoca[i], keyPointsCoca[i], descriptorsReferencesCoca[i]);
        }
        for (int i = 0; i < referencesPepsi.length; i++) {
            sift.detect(referencesPepsi[i], keyPointsPepsi[i]);
            sift.compute(referencesPepsi[i], keyPointsPepsi[i], descriptorsReferencesPepsi[i]);
        }
        for (int i = 0; i < referencesSprite.length; i++) {
            sift.detect(referencesSprite[i], keyPointsSprite[i]);
            sift.compute(referencesSprite[i], keyPointsSprite[i], descriptorsReferencesSprite[i]);
        }

        // Création du matcher
        BFMatcher matcher = new BFMatcher(NORM_L2, false);
        DMatchVector matches = new DMatchVector();

        // Stockage des distances moyennes pour chaque classe
        float[] distanceMoyennesCoca = new float[3];
        float[] distanceMoyennesPepsi = new float[3];
        float[] distanceMoyennesSprite = new float[3];
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
        System.out.println("- Contenu du premier dictionnaire contenant toutes les distances moyennes : ");
        Set<Entry<String, Float>> setHm = dictionnary.entrySet();
        Iterator<Entry<String, Float>> it = setHm.iterator();
        while(it.hasNext()){
            Entry<String, Float> e = it.next();
            System.out.println("|" + e.getKey() + " : " + e.getValue());
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
        System.out.println("- Contenu du dictionnaire final contenant les 3 plus petites distances : ");
        setHm = minDistances.entrySet();
        it = setHm.iterator();
        while(it.hasNext()){
            Entry<String, Float> e = it.next();
            System.out.println("|" + e.getKey() + " : " + e.getValue());
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

        if(nbGroupCoca >= nbGroupPepsi && nbGroupCoca >= nbGroupSprite)
            System.out.println("- La clé \"Coca\" est la plus présente dans le dictionnaire.\n- L'image correspond donc à la classe Coca.");
        else if(nbGroupPepsi >= nbGroupCoca && nbGroupPepsi >= nbGroupSprite)
            System.out.println("- La clé \"Pepsi\" est la plus présente dans le dictionnaire.\n- L'image correspond donc à la classe Pepsi.");
        else if(nbGroupSprite >= nbGroupCoca && nbGroupSprite >= nbGroupPepsi)
            System.out.println("- La clé \"Sprite\" est la plus présente dans le dictionnaire.\n- L'image correspond donc à la classe Sprite.");

    }

    /**
     * Copie un fichier de la galerie vers le cache de l'application.
     *
     * @param context contexte de l'application pour récupérer le dossier de cache.
     * @param imgPath chemin sous forme d'Uri vers l'image dans la galerie.
     * @param fileName nom du fichier de destination.
     * @return fichier copié dans le cache de l'application.
     */
    public static File galleryToCache(Context context, Uri imgPath, String fileName) {
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
