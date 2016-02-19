package com.softprojekat.nenad.mathocr;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;


import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static org.opencv.core.Core.bitwise_not;

/**
 * Created by Nenad on 19.1.2016..
 */
public class OpenCVEngine {

    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/MathOCR/";

    public static final String lang = "mat";

    private boolean isFracture = false;
    private Rect fracture;


    public ResultPair process(Bitmap bitmap) {

        Mat img = new Mat();
        Utils.bitmapToMat(bitmap, img);

        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2GRAY);
        //Size size = new Size(31,31);
        //Imgproc.GaussianBlur(img, img, size, 0);
        //Imgproc.adaptiveThreshold(img, img, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 75, 10);
        //bitwise_not(img, img);
        removeNosie(img);

        Mat img2 = img.clone();

        ArrayList<Point> pointsList = new ArrayList<>();
        for(int j=0; j<img.rows(); j++)
            for(int k=0; k<img.cols(); k++) {
                double[] pixel = img.get(j,k);
                if(pixel[0] == 255) {
                    Point point = new Point(k, j);
                    pointsList.add(point);
                }
            }

        MatOfPoint2f pointsFromList = new MatOfPoint2f();
        pointsFromList.fromList(pointsList);
        MatOfPoint2f points = new MatOfPoint2f();
        pointsFromList.convertTo(points, CvType.CV_32FC2);

        RotatedRect box = Imgproc.minAreaRect(points);

        double angle = box.angle;
        if(angle < -45.0)
            angle += 90.0;

        Point[] vertices = new Point[4];
        box.points(vertices);
        for(int i=0; i<4; i++)
            Imgproc.line(img, vertices[i], vertices[(i+1)%4], new Scalar(255, 0,0), Core.LINE_AA);

        Mat rot_mat = Imgproc.getRotationMatrix2D(box.center, angle, 1);

        Mat rotated = new Mat();

        Imgproc.warpAffine(img2, rotated, rot_mat, img.size(), Imgproc.INTER_CUBIC);

        Size box_size = box.size;
        if(box.angle < -45.0)
        {
            double temp = box_size.height;
            box_size.height = box_size.width;
            box_size.width = temp;
        }

        Mat cropped = new Mat(((int) box.size.width), ((int) box.size.height), CvType.CV_32F);

        Imgproc.getRectSubPix(rotated, box_size, box.center, cropped);

        Mat cropped2 = cropped.clone();
        Mat cropped3 = cropped.clone();

        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Mat hierarchy = new Mat();
        Imgproc.findContours(cropped, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_KCOS, new Point(0,0));

        ArrayList<MatOfPoint2f> contours2f = convertToMatOfPoint2f(contours);

        // Approximate contours to polygons + get bounding rects and circles
        //ArrayList<MatOfPoint2f> contours_poly = new ArrayList<MatOfPoint2f>(contours.size());
        MatOfPoint2f[] contours_poly_array = new MatOfPoint2f[contours.size()];
        ArrayList<MatOfPoint2f> contours_poly2f = new ArrayList<MatOfPoint2f>();
        ArrayList<MatOfPoint> contours_poly = new ArrayList<MatOfPoint>();
        for(int i=0; i<contours_poly_array.length; i++)
            contours_poly_array[i] = new MatOfPoint2f();

        ArrayList<Rect> boundRect = new ArrayList<Rect>();

        //Get poly contours
        for(int i=0; i< contours.size(); i++) {
            Imgproc.approxPolyDP(contours2f.get(i), contours_poly_array[i], 3, true);
            contours_poly2f.add(contours_poly_array[i]);
            contours_poly.add(new MatOfPoint(contours_poly_array[i].toArray()));
        }

        //Get only important contours, merge contours that are within another
        ArrayList<MatOfPoint> validContours = new ArrayList<MatOfPoint>();

        for(int i=0; i<contours_poly.size();i++) {

            Rect r = Imgproc.boundingRect(contours_poly.get(i));
            if(r.area() < 20) continue;
            boolean inside = false;
            for(int j=0; j<contours_poly.size(); j++) {
                if(j==i)continue;

                Rect r2 = Imgproc.boundingRect(contours_poly.get(j));
                if(r2.area()<20 || r2.area()<r.area()) continue;
                if(r.x > r2.x && r.x + r.width < r2.x +r2.width &&
                        r.y > r2.y && r.y + r.height < r2.y + r2.height ){
                    inside = true;
                }
            }

            if(inside) continue;
            validContours.add(contours_poly.get(i));

        }

        for(int i = 0; i < validContours.size(); i++) {
            Rect rect = Imgproc.boundingRect(validContours.get(i));
            if(rect.width / rect.height > 5) {
                isFracture = true;
                fracture = rect;
            }
        }

        //Get bounding rects

        for(int i=0; i<validContours.size(); i++) {
            boundRect.add(Imgproc.boundingRect(validContours.get(i)));
        }

        //Display
        Scalar color = new Scalar(255,0,0);
        for(int i=0; i<validContours.size(); i++) {
            if(boundRect.get(i).area() < 100) continue;
            Imgproc.drawContours(cropped2, validContours, i, color, 1, 8, new Mat(), 0, new Point());
            Imgproc.rectangle(cropped2, boundRect.get(i).tl(), boundRect.get(i).br(), color, 2, 8, 0);
        }

        Bitmap newBp = Bitmap.createBitmap(((int) box_size.width), ((int) box_size.height), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(cropped2, newBp);
        Utils.matToBitmap(img, bitmap);



        String retVal = "";

        retVal = extractContours(cropped3, validContours);

        ResultPair resPair = new ResultPair();
        resPair.setImageResult(newBp);
        resPair.setTextResult(retVal);

        return resPair;

    }

    class PointComparator implements Comparator<MatOfPoint> {

        @Override
        public int compare(MatOfPoint arg0, MatOfPoint arg1) {
            // TODO Auto-generated method stub
            int x1 = Imgproc.boundingRect(arg0).x;
            int x2 = Imgproc.boundingRect(arg1).x;
            if(x1 == x2)
                return 0;
            else if(x2 < x1)
                return 1;
            else
                return -1;

        }

    }


    public String extractContours(Mat image, ArrayList<MatOfPoint> contours_poly)  {

        //Sort contours by x value going from left to right
        //contours_poly.sort(new PointComparator());

        String recognizedText = "";

        Collections.sort(contours_poly, new PointComparator());

        if(!isFracture) {

            //Loop through all contours to extract
            for (int i = 0; i < contours_poly.size(); i++) {

                Rect r = Imgproc.boundingRect(contours_poly.get(i));

                Mat mask = Mat.zeros(image.size(), CvType.CV_8UC1);
                //Draw mask onto image
                Imgproc.drawContours(mask, contours_poly, i, new Scalar(255), Core.FILLED);

                //Check for equal sign (2 dashes on top of each other) and merge
                if (i + 1 < contours_poly.size()) {
                    Rect r2 = Imgproc.boundingRect(contours_poly.get(i + 1));
                    if (Math.abs(r2.x - r.x) < 30) {
                        //Draw mask onto image
                        Imgproc.drawContours(mask, contours_poly, i + 1, new Scalar(255), Core.FILLED);
                        i++;
                        int minX = Math.min(r.x, r2.x);
                        int minY = Math.min(r.y, r2.y);
                        int maxX = Math.max(r.x + r.width, r2.x + r2.width);
                        int maxY = Math.max(r.y + r.height, r2.y + r2.height);
                        r = new Rect(minX, minY, maxX - minX, maxY - minY);

                        if ((double) r2.width / r2.height > 2) {
                            System.out.println("=");
                            recognizedText += "=";
                            continue;
                        } else {
                            System.out.println(i);
                            continue;
                        }
                    }
                }

                if ((double) r.width / r.height > 2.0) {
                    System.out.println("-");
                    recognizedText += "-";
                    continue;
                }

                if (r.y + r.height < image.size().height * 1.0 / 2.0) {
                    System.out.println("^");
                    recognizedText += "^";
                }

                //Copy
                Mat extractPic = new Mat();
                //Extract the character using the mask
                image.copyTo(extractPic, mask);
                Mat resizedPic = extractPic.submat(r);

                Mat imageToShow = resizedPic.clone();
                Bitmap bitmap = Bitmap.createBitmap(imageToShow.width(), imageToShow.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(imageToShow, bitmap);

                //String name = "region"+i+".jpg";

                String result = "nista";
                result = recognize(bitmap);
                recognizedText += result;

                //writeToFile(imageToShow, name);
            }
        }
        else {

            String before = "";
            String after = "";
            String up = "";
            String down = "";

            for(int i = 0; i < contours_poly.size(); i++) {
                Rect rect = Imgproc.boundingRect(contours_poly.get(i));

                Mat maskImage = new Mat(image.size(), CvType.CV_8UC1);
                Imgproc.drawContours(maskImage, contours_poly, i, new Scalar(255), Core.FILLED);

                /** if fracture **/
                if(rect.x + rect.width < fracture.x) {

                    if(i+1 < contours_poly.size()) {
                        Rect rect2 = Imgproc.boundingRect(contours_poly.get(i+1));
                        if(Math.abs(rect2.x - rect.x) < 30) {
                            Imgproc.drawContours(maskImage, contours_poly, i+1, new Scalar(255), Core.FILLED);
                            i++;
                            int minX = Math.min(rect.x, rect2.x);
                            int minY = Math.min(rect.y, rect2.y);
                            int maxX = Math.max(rect.x+rect.width, rect2.x+rect2.width);
                            int maxY = Math.max(rect.y+rect.height, rect2.y+rect2.height);
                            rect = new Rect(minX, minY, maxX - minX, maxY - minY);

                            if((double)rect2.width/rect2.height > 2) {
                                before += "=";
                                continue;
                            }
                        }
                    }

                    if((double)rect.width/rect.height > 2.0) {
                        before += "-";
                        continue;
                    }

                    if(rect.y + rect.height < image.size().height / 2)
                        before += "^";

                    Mat extractedImage = new Mat();
                    image.copyTo(extractedImage, maskImage);
                    Mat resizedImage = extractedImage.submat(rect);
                    Mat imageToShow = resizedImage.clone();
                    Bitmap bitmap = Bitmap.createBitmap(imageToShow.width(), imageToShow.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(imageToShow, bitmap);
                    before += recognize(bitmap);
                }
                else if(rect.x > fracture.x + fracture.width) {

                    if(i+1 < contours_poly.size()) {
                        Rect rect2 = Imgproc.boundingRect(contours_poly.get(i+1));
                        if(Math.abs(rect2.x - rect.x) < 30) {
                            Imgproc.drawContours(maskImage, contours_poly, i+1, new Scalar(255), Core.FILLED);
                            i++;
                            int minX = Math.min(rect.x, rect2.x);
                            int minY = Math.min(rect.y, rect2.y);
                            int maxX = Math.max(rect.x+rect.width, rect2.x+rect2.width);
                            int maxY = Math.max(rect.y+rect.height, rect2.y+rect2.height);
                            rect = new Rect(minX, minY, maxX - minX, maxY - minY);

                            if((double)rect2.width/rect2.height > 2) {
                                after += "=";
                                continue;
                            }
                        }
                    }

                    if((double)rect.width/rect.height > 2.0) {
                        after += "-";
                        continue;
                    }

                    if(rect.y + rect.height < image.size().height / 2)
                        after += "^";

                    Mat extractedImage = new Mat();
                    image.copyTo(extractedImage, maskImage);
                    Mat resizedImage = extractedImage.submat(rect);
                    Mat imageToShow = resizedImage.clone();
                    Bitmap bitmap = Bitmap.createBitmap(imageToShow.width(), imageToShow.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(imageToShow, bitmap);
                    after += recognize(bitmap);
                }
                else if(rect.x + rect.width > fracture.x && rect.x + rect.width < fracture.x + fracture.width) {
                    if (rect.y + rect.height < fracture.y + fracture.height) {

                        if ((double) rect.width / rect.height > 2.0) {
                            up += "-";
                            continue;
                        }

                        if (rect.y + rect.height < fracture.y / 2)
                            up += "^";

                        Mat extractedImage = new Mat();
                        image.copyTo(extractedImage, maskImage);
                        Mat resizedImage = extractedImage.submat(rect);
                        Mat imageToShow = resizedImage.clone();
                        Bitmap bitmap = Bitmap.createBitmap(imageToShow.width(), imageToShow.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(imageToShow, bitmap);
                        up += recognize(bitmap);
                    } else if (rect.y > fracture.y) {

                        if ((double) rect.width / rect.height > 2.0) {
                            down += "-";
                            continue;
                        }

                        if (rect.y + rect.height < image.size().height / 4)
                            down += "^";

                        Mat extractedImage = new Mat();
                        image.copyTo(extractedImage, maskImage);
                        Mat resizedImage = extractedImage.submat(rect);
                        Mat imageToShow = resizedImage.clone();
                        Bitmap bitmap = Bitmap.createBitmap(imageToShow.width(), imageToShow.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(imageToShow, bitmap);
                        down += recognize(bitmap);
                    }
                }

        }

            recognizedText += before + "(" + up + ")/(" + down + ")" + after;

        }

        //writeToFile(image, "output2.jpg");

        return recognizedText;

    }


    public static ArrayList<MatOfPoint2f> convertToMatOfPoint2f(ArrayList<MatOfPoint> contours) {
        ArrayList<MatOfPoint2f> contours2f = new ArrayList<MatOfPoint2f>();
        for(MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f(contour.toArray());
            contours2f.add(contour2f);
        }
        return contours2f;
    }

    private String recognize(Bitmap bitmap) {

        TessBaseAPI tessBaseApi = new TessBaseAPI();
        tessBaseApi.setDebug(true);
        tessBaseApi.init(DATA_PATH, lang);
        tessBaseApi.setImage(bitmap);

        String result = "nista";
        result = tessBaseApi.getUTF8Text();

        tessBaseApi.end();

        Log.v("OCR", "OCRED TEXT: " + result);

        return result;

    }

    private void removeNosie(Mat img) {

        Size size = new Size(3, 3);
        Imgproc.GaussianBlur(img, img, size, 0);
        Imgproc.adaptiveThreshold(img, img, 255, Imgproc.RETR_EXTERNAL, Imgproc.THRESH_BINARY, 75, 10);
        Core.bitwise_not(img, img);
        //Imgproc.dilate(img, img, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5)));

    }






}
