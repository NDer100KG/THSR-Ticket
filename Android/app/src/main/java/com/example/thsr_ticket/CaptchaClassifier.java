/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.thsr_ticket;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.StringTokenizer;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

/**
 * Classifies images with Tensorflow Lite.
 */
public class CaptchaClassifier {

    private static final String TAG = "TfLite";

    /**
     * Dimensions of inputs.
     */
    private static final int DIM_BATCH_SIZE = 1;

    private static final int DIM_PIXEL_WIDTH = 128;
    private static final int DIM_PIXEL_HEIGHT = 128;
    private static final int DIM_PIXEL_SIZE = 1;

    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 255.0f;

    private static final int numClasses = 19;

    /* Preallocated buffers for storing image data in. */
    private int[] intValues = new int[DIM_PIXEL_HEIGHT * DIM_PIXEL_WIDTH];

    protected Interpreter tflite;

    protected ByteBuffer imgData = null;
    private float[][][] outputArray = null;

    Map<Integer, String> dic19 = new HashMap<>();
    Map<Integer, Object> outputMap = new HashMap<>();

    CaptchaClassifier(Activity activity) throws IOException {
        // TODO(b/169965231): Add support for delegates.
        tflite = new Interpreter(loadModelFile(activity));
        show_model_info();
        imgData =
                ByteBuffer.allocateDirect(
                        DIM_BATCH_SIZE
                        * DIM_PIXEL_WIDTH
                        * DIM_PIXEL_HEIGHT
                        * DIM_PIXEL_SIZE
                        * 4);
        imgData.order(ByteOrder.nativeOrder());
        outputArray = new float[4][1][numClasses];
        outputMap.put(0, outputArray[0]);
        outputMap.put(1, outputArray[1]);
        outputMap.put(2, outputArray[2]);
        outputMap.put(3, outputArray[3]);
        InitDic19();
        Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
    }


    /**
     * Classifies a frame from the preview stream.
     */
    String classifyFrame(Bitmap bitmap) {

        if (tflite == null) {
            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
        }

        bitmap = Bitmap.createScaledBitmap(bitmap, DIM_PIXEL_WIDTH, DIM_PIXEL_HEIGHT, true);

        convertBitmapToByteBuffer(bitmap);

        long startTime = SystemClock.uptimeMillis();
        Object[] inputArray = {imgData};
        tflite.runForMultipleInputsOutputs(inputArray, outputMap);
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to run model inference: " + Long.toString(endTime - startTime));

        String ans = decode();

        // Print the results.
        long duration = endTime - startTime;

        Log.i(TAG, ans);
        Log.i(TAG, "duration: " + Long.toString(duration));
        return ans;
    }

    private String decode(){
        String ans = "";
        for(int i = 0; i < 4; i++){
            ans = ans + dic19.get(argmax(outputArray[i][0]));
        }
        return ans;
    }

    private int argmax(float[] array){
        int maxIdx = 0;
        float max = -1;
        for(int i = 0; i < array.length; i++){
            if(array[i] > 0.5f){
                maxIdx = i;
                break;
            }
            else{
                if(array[i] > max){
                    max = array[i];
                    maxIdx = i;
                }
            }
        }
        return maxIdx;
    }

    public void setNumThreads(int num_threads) {
        if (tflite != null) tflite.setNumThreads(num_threads);
    }

    public void close() {
        tflite.close();
        tflite = null;
    }

    /**
     * Writes Image data into a {@code ByteBuffer}.
     */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < DIM_PIXEL_WIDTH; ++i) {
            for (int j = 0; j < DIM_PIXEL_HEIGHT; ++j) {
                final int val = intValues[pixel++];
                addPixelValue(val);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d(TAG, "Timecost to put values into ByteBuffe`r: " + Long.toString(endTime - startTime));
    }

    protected String getModelPath() {
        return "model.tflite";
    }

    protected void addPixelValue(int pixelValue) {
        imgData.putFloat((((pixelValue) & 0xFF) / IMAGE_STD) - IMAGE_MEAN);
    }

    private void show_model_info(){
        int n_in = tflite.getInputTensorCount();
        int n_out = tflite.getOutputTensorCount();
        String s_show = "";
        // Show input info
        for(int i = 0; i < n_in; i++){
            Tensor T = tflite.getInputTensor(i);
            s_show += T.name() + " : [";
            for(int _s = 0; _s < T.shape().length; _s ++){
                s_show = s_show + T.shape()[_s];
                if(_s != T.shape().length - 1){
                    s_show += " ,";
                }
            }
            s_show = s_show + "]  " + T.dataType().toString() + "\n";
        }
        // Show output info
        for(int i = 0; i < n_out; i++){
            Tensor T = tflite.getOutputTensor(i);
            s_show += T.name() + " : [";
            for(int _s = 0; _s < T.shape().length; _s ++){
                s_show = s_show + T.shape()[_s];
                if(_s != T.shape().length - 1){
                    s_show += " ,";
                }
            }
            s_show = s_show + "]  " + T.dataType().toString() + "\n";
        }
        Log.i(TAG, s_show);
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(getModelPath());
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void InitDic19(){
        dic19.put(0, "2");
        dic19.put(1, "3");
        dic19.put(2, "4");
        dic19.put(3, "5");
        dic19.put(4, "7");
        dic19.put(5, "9");
        dic19.put(6, "a");
        dic19.put(7, "c");
        dic19.put(8, "f");
        dic19.put(9, "h");
        dic19.put(10, "k");
        dic19.put(11, "m");
        dic19.put(12, "n");
        dic19.put(13, "p");
        dic19.put(14, "q");
        dic19.put(15, "r");
        dic19.put(16, "t");
        dic19.put(17, "y");
        dic19.put(18, "z");
    }
}