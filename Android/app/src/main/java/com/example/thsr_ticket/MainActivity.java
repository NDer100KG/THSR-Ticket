package com.example.thsr_ticket;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {

    String TAG = "MainActivity";
    final int MaxRetryTime = 3;
    Spinner start_station, end_station;
    EditText car_id, inputDate, ticket_num, personal_id, phone, email;
    CaptchaClassifier captchaClassifier;
    Map<String, Object> headers = new HashMap<>();
    Map<String, Object> bookingInfoParams = new HashMap<>();
    Map<String, Object> submitTicketParams = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start_station = (Spinner) findViewById(R.id.spinner);
        end_station = (Spinner) findViewById(R.id.spinner2);
        car_id = (EditText) findViewById(R.id.carId);
        inputDate = (EditText) findViewById(R.id.inputDate);
        ticket_num = (EditText) findViewById(R.id.ticketNum);
        personal_id = (EditText) findViewById(R.id.personalId);
        phone = (EditText) findViewById(R.id.phone);
        email = (EditText) findViewById(R.id.email);

        String[] stations = {"南港", "台北", "板橋", "桃園", "新竹", "苗栗", "台中", "彰化", "雲林", "嘉義", "台南", "左營"};
        ArrayAdapter<String> _stations = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                stations);
        start_station.setAdapter(_stations);
        start_station.setSelection(1);
        end_station.setAdapter(_stations);
        end_station.setSelection(11);

        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date today = new Date();
        Date tomorrow = new Date(today.getTime() + (1000 * 60 * 60 * 24));
        inputDate.setText(sdFormat.format(tomorrow));

        build_default_headers();

        // Create Classifier
        try {
            captchaClassifier = new CaptchaClassifier(MainActivity.this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void submitClick(View view) {
        build_default_bookingInfoParams();
        build_default_submitTicketParams();

        boolean success = false;
        int num_try = 1;

        while(!success && num_try < MaxRetryTime) {
            num_try += 1;
            try {
                success = new BookingPageTask(this.getApplicationContext(), headers, bookingInfoParams, submitTicketParams).execute(captchaClassifier).get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(success)
            Toast.makeText(this.getApplicationContext(), "Booking Complete!", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(this.getApplicationContext(), "Booking Fail!", Toast.LENGTH_LONG).show();

    }

    private void build_default_submitTicketParams(){
        submitTicketParams.put("BookingS3FormSP:hf:0", "");
        submitTicketParams.put("diffOver", 1);
        submitTicketParams.put("idInputRadio", "radio40");
        submitTicketParams.put("idInputRadio:idNumber", personal_id.getText().toString());
        submitTicketParams.put("eaiPhoneCon:phoneInputRadio", "radio47");
        submitTicketParams.put("eaiPhoneCon:phoneInputRadio:mobilePhone", phone.getText().toString());
        submitTicketParams.put("email", email.getText().toString());
        submitTicketParams.put("agree", "on");
        submitTicketParams.put("isGoBackM", "");
        submitTicketParams.put("backHome", "");
        submitTicketParams.put("TgoError", "1");
    }

    private void build_default_headers(){
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Mobile Safari/537.36");
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/");
        headers.put("Accept-Language", "zh-TW,zh;q=0.9,en-US;q=0.8,en;q=0.7,zh-CN;q=0.6,ja;q=0.5");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Host", "irs.thsrc.com.tw");
    }

    private void build_default_bookingInfoParams(){
        bookingInfoParams.put("BookingS1Form:hf:0", "");
        bookingInfoParams.put("selectStartStation", start_station.getSelectedItemPosition() + 1);
        bookingInfoParams.put("selectDestinationStation", end_station.getSelectedItemPosition() + 1);
        bookingInfoParams.put("trainCon:trainRadioGroup", 0);
        bookingInfoParams.put("seatCon:seatRadioGroup", "radio17");
        bookingInfoParams.put("bookingMethod", 1);
        bookingInfoParams.put("toTimeInputField", inputDate.getText().toString());
        bookingInfoParams.put("toTimeTable", "");
        bookingInfoParams.put("toTrainIDInputField", car_id.getText().toString());
        bookingInfoParams.put("backTimeInputField", inputDate.getText().toString());
        bookingInfoParams.put("backTimeTable", "");
        bookingInfoParams.put("backTrainIDInputField", "");
        bookingInfoParams.put("ticketPanel:rows:0:ticketAmount", ticket_num.getText().toString() + "F");
        bookingInfoParams.put("ticketPanel:rows:1:ticketAmount", "0H");
        bookingInfoParams.put("ticketPanel:rows:2:ticketAmount", "0W");
        bookingInfoParams.put("ticketPanel:rows:3:ticketAmount", "0E");
        bookingInfoParams.put("ticketPanel:rows:4:ticketAmount", "0P");
    }
}

