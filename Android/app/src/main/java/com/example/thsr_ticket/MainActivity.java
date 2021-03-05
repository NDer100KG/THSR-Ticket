package com.example.thsr_ticket;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

//    int myStart = 0;
//    int myEnd = 0;
    Spinner start_station, end_station;
    EditText car_id, inputDate, ticket_num, personal_id, phone;
    HttpURLConnection_Get httpGet;
    String BASE_URL = "https://irs.thsrc.com.tw";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start_station = (Spinner)findViewById(R.id.spinner);
        end_station = (Spinner)findViewById(R.id.spinner2);
        car_id = (EditText)findViewById(R.id.carId);
        inputDate = (EditText)findViewById(R.id.inputDate);
        ticket_num = (EditText)findViewById(R.id.ticketNum);
        personal_id = (EditText)findViewById(R.id.personalId);
        phone = (EditText)findViewById(R.id.phone);

        httpGet = new HttpURLConnection_Get();

        String[] stations = {"南港", "台北", "板橋", "桃園", "新竹", "苗栗", "台中", "彰化", "雲林", "嘉義", "台南", "左營"};
        ArrayAdapter<String> _stations = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_dropdown_item,
                stations);
        start_station.setAdapter(_stations);
        start_station.setSelection(1);
        end_station.setAdapter(_stations);
        end_station.setSelection(11);

        SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date current = new Date();
        inputDate.setText(sdFormat.format(current));


//        Button submit = (Button)findViewById(R.id.submit);

    }


    public void submitClick(View view) {
        String test = httpGet.get(BASE_URL);
    }

}

class HttpURLConnection_Get extends AsyncTask<String, Void, String> {
    private final static String TAG = "HTTPURLCONNECTION test";
    @Override
    protected String doInBackground(String... urls) {
        return GET(urls[0]);
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d(TAG,"onPostExecute");
    }
    private String GET(String APIUrl) {
        String result = "";
        HttpURLConnection connection;
        try {
            URL url = new URL(APIUrl);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoInput(true);

            InputStream inputStream = connection.getInputStream();
            int status = connection.getResponseCode();
            Log.d(TAG, String.valueOf(status));
            if(inputStream != null){
                InputStreamReader reader = new InputStreamReader(inputStream,"UTF-8");
                BufferedReader in = new BufferedReader(reader);

                String line="";
                while ((line = in.readLine()) != null) {
                    result += (line+"\n");
                }
            } else{
                result = "Did not work!";
            }
            return result;
        } catch (Exception e) {
            Log.d("ATask InputStream", e.getLocalizedMessage());
            e.printStackTrace();
            return result;
        }
    }

}
