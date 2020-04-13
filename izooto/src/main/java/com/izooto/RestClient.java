package com.izooto;


import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestClient {

    private static final String BASE_URL = "https://aevents.izooto.com/";
    private static final int TIMEOUT = 120000;
    public static final int GET_TIMEOUT = 60000;
    public static final String EVENT_URL="https://et.izooto.com/evt";
    public static final String PROPERTIES_URL="https://prp.izooto.com/prp";
    public static final String IMPRESSION_URL="https://impr.izooto.com/imp";
    public static  final String NOTIFICATIONCLICK="https://clk.izooto.com/clk";

    private static int getThreadTimeout(int timeout) {
        return timeout + 5000;
    }


    static void get(final String url, final ResponseHandler responseHandler) {
        new Thread(new Runnable() {
            public void run() {
                makeApiCall(url, null, null, responseHandler, GET_TIMEOUT);
            }
        }).start();
    }
    static void postRequest(final String url,final ResponseHandler responseHandler)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
               makeApiCall(url,"POST",null,responseHandler,GET_TIMEOUT);
            }
        }).start();
    }

    public static void makeApiCall(final String url, final String method, final JSONObject jsonBody, final ResponseHandler responseHandler, final int timeout) {
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.submit(new Runnable() {
            @Override
            public void run() {
                startHTTPConnection(url, method, jsonBody, responseHandler, timeout);
            }
        });
    }

    private static void startHTTPConnection(String url, String method, JSONObject jsonBody, ResponseHandler responseHandler, int timeout) {
        HttpURLConnection con = null;
        int httpResponse = -1;
        String json = null;

        try {
            if (url.contains("https:") || url.contains("http:") || url.contains("impr.izooto.com")) {
                con = (HttpURLConnection) new URL(url).openConnection();

            } else {
                con = (HttpURLConnection) new URL(BASE_URL + url).openConnection();
                Log.e("URL",BASE_URL+url);
            }

            con.setUseCaches(false);
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout);

            if (jsonBody != null)
                con.setDoInput(true);

            if (method != null) {
                if(method.equalsIgnoreCase("POST")) {
                    con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                }
                else {
                    con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                }
                con.setRequestMethod(method);
                con.setDoOutput(true);
            }

            if (jsonBody != null) {
                String strJsonBody = jsonBody.toString();
                byte[] sendBytes = strJsonBody.getBytes("UTF-8");
                con.setFixedLengthStreamingMode(sendBytes.length);
                OutputStream outputStream = con.getOutputStream();
                outputStream.write(sendBytes);
            }

            httpResponse = con.getResponseCode();
            InputStream inputStream;
            Scanner scanner;
            if (httpResponse == HttpURLConnection.HTTP_OK) {
                if (url.equals(AppConstant.CDN+iZooto.mIzooToAppId+".js"))
                    Lg.d(AppConstant.APP_NAME_TAG, AppConstant.SUCCESS);
                else
                    Lg.d(AppConstant.APP_NAME_TAG, AppConstant.SUCCESS);

                inputStream = con.getInputStream();
                scanner = new Scanner(inputStream, "UTF-8");
                json = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                scanner.close();
                if (responseHandler != null) {
                    callResponseHandlerOnSuccess(responseHandler, json);
                }
                else
                    Lg.w(AppConstant.APP_NAME_TAG, AppConstant.ATTACHREQUEST);

            } else {
                if (url.equals(AppConstant.CDN+iZooto.mIzooToAppId+".js"))
                    Lg.d(AppConstant.APP_NAME_TAG, AppConstant.SUCCESS);
                else
                    Lg.d(AppConstant.APP_NAME_TAG, AppConstant.FAILURE);
                inputStream = con.getErrorStream();
                if (inputStream == null)
                    inputStream = con.getInputStream();

                if (inputStream != null) {
                    scanner = new Scanner(inputStream, "UTF-8");
                    json = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                    scanner.close();
                }
                if (responseHandler != null) {
                    callResponseHandlerOnFailure(responseHandler, httpResponse, json, null);

                }
                else
                    Lg.w(AppConstant.APP_NAME_TAG, AppConstant.ATTACHREQUEST);

            }
        } catch (Throwable t) {
            if (t instanceof java.net.ConnectException || t instanceof java.net.UnknownHostException)
                Lg.i(AppConstant.APP_NAME_TAG,  AppConstant.EXCEPTIONERROR+ t.getClass().getName());
            else
                Lg.i(AppConstant.APP_NAME_TAG,  AppConstant.EXCEPTIONERROR+ t);

            if (responseHandler != null)
                callResponseHandlerOnFailure(responseHandler, httpResponse, null, t);
            else
                Lg.w(AppConstant.APP_NAME_TAG, AppConstant.ATTACHREQUEST);
        } finally {
            if (con != null)
                con.disconnect();
        }
    }


    private static void callResponseHandlerOnSuccess(final ResponseHandler handler, final String response) {
        new AppExecutors().networkIO().execute(new Runnable() {
            @Override
            public void run() {
                handler.onSuccess(response);
            }
        });

    }

    private static void callResponseHandlerOnFailure(final ResponseHandler handler, final int statusCode, final String response, final Throwable throwable) {
        new AppExecutors().networkIO().execute(new Runnable() {
            @Override
            public void run() {
                handler.onFailure(statusCode, response, throwable);
            }
        });
    }

    static class ResponseHandler {
        void onSuccess(String response) {
            Lg.d(AppConstant.APP_NAME_TAG,  AppConstant.APISUCESS);
        }

        void onFailure(int statusCode, String response, Throwable throwable) {
            Lg.e(AppConstant.APP_NAME_TAG, AppConstant.APIFAILURE  + response);
        }
    }
}
