package com.example.myouf.logoreco;

/**
 * Class representing a brand
 */
public class Brand {

    // Useful variables
    private String brandName;
    private String url;
    private String classifier;
    private String[] images;

    /**
     * Constructor
     * @param brandname Name of the brand
     * @param url Url of the website
     * @param classifier Name of the classifier
     * @param images Table of strings naming the images
     */
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
