package com.softprojekat.nenad.mathocr;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAImage;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

public class RecognitionActivity extends Activity {

    /*private ImageView imgPreview;
    private TextView textView;
    private TextView textViewWolfram;*/

    private TextView wolframQuery;
    private ImageView inputImg;
    private ImageView solutionImg;

    private Button btnNext;
    private Button btnPrev;

    private OpenCVEngine openCVEngine;

    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/MathOCR/";
    public static final String lang = "mat";

    private String input = "";
    private String output = "";

    private Bitmap bmpInput;
    private ArrayList<Bitmap> wolframImgResults;

    public static int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mOpenCVCallBack))
        {
            Log.e("TEST", "Cannot connect to OpenCV Manager");
        }

        setContentView(R.layout.activity_recognition);
    }

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {

                    Log.e("TEST", "Success");

                    copyTraineddate();

                    /*imgPreview = (ImageView)findViewById(R.id.imageView);
                    textView = (TextView)findViewById(R.id.textView);
                    textViewWolfram = (TextView)findViewById(R.id.textViewWolfram);*/

                    wolframQuery = (TextView)findViewById(R.id.wolframQuery);
                    inputImg = (ImageView)findViewById(R.id.inputImg);
                    solutionImg = (ImageView)findViewById(R.id.solutionImg);

                    btnNext = (Button)findViewById(R.id.nextButton);
                    btnPrev = (Button)findViewById(R.id.prevBtn);

                    btnNext.setVisibility(View.INVISIBLE);
                    btnPrev.setVisibility(View.INVISIBLE);

                    wolframImgResults = new ArrayList<Bitmap>();

                    openCVEngine = new OpenCVEngine();

                    Bitmap bmp = (Bitmap)getIntent().getParcelableExtra("image");

                    ResultPair resultPair = openCVEngine.process(bmp);

                    //inputImg.setImageBitmap(resultPair.getImageResult());
                    wolframQuery.setText("Query: " + resultPair.getTextResult());

                    input = resultPair.getTextResult();

                    new WolframTask().execute();

                    //String wolframResult = calculate(resultPair.getTextResult());

                    //textViewWolfram.setText(wolframResult);

                    /*btnCapturePicture = (Button) findViewById(R.id.btnCapturePicture);
                    imgPreview = (ImageView) findViewById(R.id.imgPreview);
                    textView = (TextView)findViewById(R.id.textView);

                    openCVEngine = new OpenCVEngine();

                    btnCapturePicture.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            // capture picture
                            captureImage();
                        }
                    });

                    copyTraineddate();*/

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    private void copyTraineddate() {

        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v("TEST", "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v("TEST", "Created directory " + path + " on sdcard");
                }
            }

        }

        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v("TEST", "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e("TEST", "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }

    }

    private void calculate(String input) {

        String retVal = "";

        String appid = "WTXJJT-QJ8LKRV24A";

        WAEngine engine = new WAEngine();
        engine.setAppID(appid);
        engine.addFormat("image");

        WAQuery query = engine.createQuery();

        query.setInput(input);

        query.setMagnification(4);

        try {

            System.out.println("Query URL:");
            System.out.println(engine.toURL(query));
            System.out.println("");

            WAQueryResult queryResult = engine.performQuery(query);

            if (queryResult.isError()) {
                System.out.println("Query error");
                System.out.println("  error code: " + queryResult.getErrorCode());
                System.out.println("  error message: " + queryResult.getErrorMessage());
            } else if (!queryResult.isSuccess()) {
                System.out.println("Query was not understood; no results available.");
            } else {
                // Got a result.
                System.out.println("Successful query. Pods follow:\n");
                for (WAPod pod : queryResult.getPods()) {
                    if (!pod.isError()) {
                        System.out.println(pod.getTitle());
                        System.out.println("------------");
                        for (WASubpod subpod : pod.getSubpods()) {
                            for (Object element : subpod.getContents()) {
                                /*if (element instanceof WAPlainText) {
                                    //System.out.println(((WAPlainText) element).getText());
                                	String result =((WAPlainText)element).getText().replace("", "=");
                                	System.out.println(result);
                                    System.out.println("");
                                    retVal += result;
                                }*/

                                if(element instanceof WAImage && (pod.getTitle().equals("Solution") || pod.getTitle().equals("Solutions") || pod.getTitle().equals("Complex solutions") || pod.getTitle().equals("Indefinite integral") || pod.getTitle().equals("Result"))) {
                                    System.out.println(pod.getTitle());
                                    Log.i("POD", pod.getTitle());
                                    WAImage image = (WAImage)element;

                                    Bitmap bmp = getBmp(image.getURL());
                                    wolframImgResults.add(bmp);

                                }

                                if(element instanceof WAImage && pod.getTitle().equals("Input")) {
                                    System.out.println(pod.getTitle());
                                    Log.i("POD", pod.getTitle());
                                    WAImage image = (WAImage)element;

                                    Bitmap bmp = getBmp(image.getURL());
                                    bmpInput = bmp;

                                }
                            }
                        }
                        System.out.println("");
                    }
                }
                // We ignored many other types of Wolfram|Alpha output, such as warnings, assumptions, etc.
                // These can be obtained by methods of WAQueryResult or objects deeper in the hierarchy.
            }


        } catch (WAException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



    }

    private class WolframTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            calculate(input);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

           // textViewWolfram.setText(output);

            inputImg.setImageBitmap(bmpInput);

            if(wolframImgResults.size() > 0)
                solutionImg.setImageBitmap(wolframImgResults.get(0));

            if(wolframImgResults.size() > 1) {
                btnNext.setVisibility(View.VISIBLE);
            }

            btnNext.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    counter++;
                    if (counter < wolframImgResults.size())
                        solutionImg.setImageBitmap(wolframImgResults.get(counter));

                    if (counter == wolframImgResults.size() - 1)
                        btnNext.setVisibility(View.INVISIBLE);

                    if(counter == 1)
                        btnPrev.setVisibility(View.VISIBLE);

                }
            });

            btnPrev.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    counter--;
                    if (counter >=0)
                        solutionImg.setImageBitmap(wolframImgResults.get(counter));

                    if (counter == 0)
                        btnPrev.setVisibility(View.INVISIBLE);

                    if(counter == wolframImgResults.size()-2)
                        btnNext.setVisibility(View.VISIBLE);

                }
            });

            //Bitmap resultImages = combine();
            //imgPreview.setImageBitmap(resultImages);
        }
    };

    private Bitmap getBmp(String url) {
        Bitmap bmp = null;
        InputStream in = null;
        try {
            in = new URL(url).openStream();
            bmp = BitmapFactory.decodeStream(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bmp;
    }

    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }

    private Bitmap combine() {

        Bitmap overlayCurrent = wolframImgResults.get(0);

        /*for(int i=1; i<wolframImgResults.size(); i++) {

            Bitmap overlayNew = overlay(overlayCurrent, wolframImgResults.get(i));
            overlayCurrent = overlayNew;
        }*/

        return overlayCurrent;

    }

    @Override
    protected void onResume() {
        super.onResume();

        counter = 0;
    }
}
