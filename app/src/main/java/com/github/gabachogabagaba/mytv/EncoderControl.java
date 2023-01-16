package com.github.gabachogabagaba.mytv;
import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

public class EncoderControl {
    private String address;
    private OkHttpClient client = new OkHttpClient();
    final String TAG = "EncoderControl";

    EncoderControl(String address) {
        this.address = address;
    }

    void getTemperature() {
        MediaType JSON = MediaType.get("application/json");
        RequestBody requestBody = RequestBody.create(JSON, "{ \"jsonrpc\": \"2.0\", \"method\": \"enc.getSysState\", \"params\": [], \"id\": 1 }");
        Request request = new Request.Builder()
                .url(address + "/RPC/")
                .post(requestBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String stringResponse = response.body().string();

//                    Log.d("EncoderControl", "JSON response " + stringResponse);
            JSONObject jsonobject = new JSONObject(stringResponse);
            Log.d("EncoderControl", "Temperature: " + jsonobject.getJSONObject("result").getInt("temperature"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getInputState() {
        boolean inputAvailable = false;
        MediaType JSON = MediaType.get("application/json");
        RequestBody requestBody = RequestBody.create(JSON, "{ \"jsonrpc\": \"2.0\", \"method\": \"enc.getInputState\", \"params\": [], \"id\": 1 }");
        Request request = new Request.Builder()
                .url(address + "/RPC/")
                .post(requestBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String stringResponse = response.body().string();

//                    Log.d(TAG, "JSON response " + stringResponse);
            JSONObject jsonObject = new JSONObject(stringResponse);
            inputAvailable = jsonObject.getJSONArray("result").getJSONObject(0).getBoolean("avalible");
            Log.d(TAG, "Input available: " + inputAvailable);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return inputAvailable;
    }

    void setEncoderState(boolean enabled) {

        Request request = new Request.Builder()
                .url(address + "/config/config.json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            String stringResponse = response.body().string();
//                    Log.d("EncoderControl", "JSON response " + stringResponse);
            JSONArray jsonArray = new JSONArray(stringResponse);
            boolean encState = jsonArray.getJSONObject(0).getBoolean("enable");
            Log.d(TAG, "Encoder state: " + encState);
            Log.d(TAG, "Encoder state update: " + enabled);
            if(encState != enabled) {
                // Toggle the encoder state
                jsonArray.getJSONObject(0).put("enable", enabled);
                String paramsString = jsonArray.toString();
                paramsString.replace("[", "\\[");
                paramsString.replace("]", "\\]");
                paramsString.replace("\"", "\\\"");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("jsonrpc", "2.0");
                jsonObject.put("method", "enc.update");
                jsonObject.put("id", 1);
                jsonObject.put("params", new JSONArray().put(paramsString));
                String postString = jsonObject.toString();
                Log.d(TAG, "JSON post" + postString);
                MediaType JSON = MediaType.get("application/json");
                RequestBody requestBody = RequestBody.create(JSON, postString);
                Request new_request = new Request.Builder()
                        .url(address + "/RPC/")
                        .post(requestBody)
                        .build();

                try (Response response2 = client.newCall(new_request).execute()) {
                    String stringResponse2 = response2.body().string();
                    Log.d(TAG, "JSON response " + stringResponse2);


                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }

        } catch (Exception e) {
        e.printStackTrace();
        }
    }
}