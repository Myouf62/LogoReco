package com.example.myouf.logoreco;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class AnalysisActivity extends AppCompatActivity {

    ImageView imageViewResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        imageViewResult = (ImageView) findViewById(R.id.imageViewResult);
        // En guise de test, nous utilisons pour le moment l'image base de Coca utilisée dans le TP4
        imageViewResult.setImageResource(R.drawable.coca_image_base);
    }
}
