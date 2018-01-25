package com.example.myouf.logoreco;

import java.util.List;

/**
 * Created by myouf on 22/01/2018.
 */

public class Brand {

    private String brandName;
    private String url;
    private String classifier;
    private String[] images;

    public Brand(String brandname, String url, String classifier, String[] images) {
        this.brandName=brandname;
        this.url=url;
        this.classifier=classifier;
        this.images=images;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String[] getImages() {
        return images;
    }

    public void setImages(String[] images) {
        this.images = images;
    }

}
