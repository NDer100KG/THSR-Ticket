package com.example.thsr_ticket;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.RawRes;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;

import org.checkerframework.checker.nullness.qual.Raw;
import org.tensorflow.lite.Interpreter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BookingPageTask extends AsyncTask<CaptchaClassifier, Void, Boolean> {
    static final String BASE_URL = "https://irs.thsrc.com.tw";
    static final String BOOKING_PAGE_URL = "https://irs.thsrc.com.tw/IMINT/";
    static String SUBMIT_FORM_URL = "https://irs.thsrc.com.tw/IMINT/;jsessionid={$JESSIONID}?wicket:interface=:{$PAGECOUNT}:BookingS1Form::IFormSubmitListener";
    static String CONFIRM_TICKET_URL = "https://irs.thsrc.com.tw/IMINT/?wicket:interface=:{$PAGECOUNT}:BookingS3Form::IFormSubmitListener";
    static String TAG = "BookingPageTask";

    Context context;
    Map<String, Object> headers;
    Map<String, Object> bookingInfoParams;
    Map<String, Object> submitTicketParams;

    int page_count = 0;
    Session session;
    String ans = "";
    String image_url = "";
    Bitmap captcha = null;
    String ERROR = "";

    public BookingPageTask(Context context, Map<String, Object> headers, Map<String, Object> bookingInfoParams, Map<String, Object> submitTicketParams) {
        this.context = context;
        this.headers = headers;
        this.bookingInfoParams = bookingInfoParams;
        this.submitTicketParams = submitTicketParams;
    }

    @Override
    protected Boolean doInBackground(CaptchaClassifier... captchaClassifiers) {
        if (!getImageURL())
            return false;

        try {
            getImage(image_url);
            ans = captchaClassifiers[0].classifyFrame(captcha);
        } catch (Exception e) {
            return false;
        }

        if (!submitBookingForm()) {
            return false;
        }

        return submit_ticket();
    }

    private boolean submit_ticket(){
        CONFIRM_TICKET_URL = CONFIRM_TICKET_URL.replace("{$PAGECOUNT}", Integer.toString(page_count));

        RawResponse resp = session.post(CONFIRM_TICKET_URL).headers(headers).params(submitTicketParams).send();
        String[] resp_str = resp.readToText().split("\n");

        boolean check_fail = false;
        for (int i = 0; i < resp_str.length; i++) {
            if (resp_str[i].contains("<!----- error message starts ----->")) {
                check_fail = true;
            }
            if (resp_str[i].contains("<!----- error message ends ----->")) {
                break;
            }
            if(check_fail){
                if (resp_str[i].contains("<span class=\"feedbackPanelERROR\">")){
                    ERROR = resp_str[i];
                    Log.i(TAG, resp_str[i]);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean submitBookingForm() {
        String _JSESSIONID = session.currentCookies().get(2).value();
        SUBMIT_FORM_URL = SUBMIT_FORM_URL.replace("{$JESSIONID}", _JSESSIONID);
        SUBMIT_FORM_URL = SUBMIT_FORM_URL.replace("{$PAGECOUNT}", Integer.toString(page_count));
        page_count += 1;

        bookingInfoParams.put("homeCaptcha:securityCode", ans);

        RawResponse resp = session.post(SUBMIT_FORM_URL).headers(headers).params(bookingInfoParams).send();
        String[] resp_str = resp.readToText().split("\n");

        boolean check_fail = false;
        for (String s : resp_str) {
            if (s.contains("<!----- error message starts ----->")) {
                check_fail = true;
                continue;
            }
            if (s.contains("<!----- error message ends ----->")) {
                break;
            }
            if (check_fail) {
                if (s.contains("檢測碼輸入錯誤") || s.contains("請選擇") || s.contains("請輸入")) {
                    ERROR = s;
                    Log.i(TAG, s);
                    return false;
                }
            }
        }

        return true;
    }

    private void getImage(String image_url) {
        RawResponse resp = session.get(image_url).headers(headers).send();
        byte[] image = resp.readToBytes();

        captcha = BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    private boolean getImageURL() {
        boolean get = false;
        RawResponse resp = session.get(BOOKING_PAGE_URL).headers(headers).send();
        String[] resp_str = resp.readToText().split("\n");

        for (int i = resp_str.length - 1; i >= 0; i--) {
            if (!resp_str[i].contains("<img id=\"BookingS1Form_homeCaptcha_passCode"))
                continue;
            else {
                image_url = resp_str[i];
                get = true;
                break;
            }
        }

        if (get) {
            image_url = image_url.substring(image_url.indexOf("src=") + 5, image_url.length() - 3);
            image_url = BASE_URL + image_url;
        }
        return get;
    }

    @Override
    protected void onPreExecute() {
        // Runs on UI thread- Any code you wants
        // to execute before web service call. Put it here.
        // Eg show progress dialog
        session = Requests.session();
    }

    @Override
    protected void onPostExecute(Boolean b){
        if(!ERROR.equals("")){
            Toast.makeText(context, ERROR, Toast.LENGTH_SHORT).show();
        }
    }
}
