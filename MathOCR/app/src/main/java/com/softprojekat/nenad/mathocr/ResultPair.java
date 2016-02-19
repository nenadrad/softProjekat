package com.softprojekat.nenad.mathocr;

import android.graphics.Bitmap;

/**
 * Created by Nenad on 2/1/2016.
 */
public class ResultPair {

    private String textResult = "";
    private Bitmap imageResult = null;

    public Bitmap getImageResult() {
        return imageResult;
    }

    public void setImageResult(Bitmap imageResult) {
        this.imageResult = imageResult;
    }

    public String getTextResult() {
        return textResult;
    }

    public void setTextResult(String textResult) {
        this.textResult = textResult;
    }


}
