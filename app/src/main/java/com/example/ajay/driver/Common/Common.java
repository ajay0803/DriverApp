package com.example.ajay.driver.Common;

import android.location.Location;

import com.example.ajay.driver.Remote.FCMClient;
import com.example.ajay.driver.Remote.IFCMService;
import com.example.ajay.driver.Remote.IGoogleAPI;
import com.example.ajay.driver.Remote.RetrofitClient;


public class Common {


    public static  final String driver_tb1="Drivers";
    public static  final String user_driver_tb1="DriversInformation";
    public static  final String user_rider_tb1="RidersInformation";
    public static  final String pickup_request_tb1="PickUpRequest";
    public static  final String token_tb1="Tokens";
    private static final String baseURL ="https://maps.googleapis.com";
    private static final String fcmURL ="https://fcm.googleapis.com/";
    public static Location mLastLocation=null;


    public static IGoogleAPI getGoogleAPI()
    {
        return RetrofitClient.getClient(baseURL).create(IGoogleAPI.class);
    }
    public static IFCMService getFCMService()
    {
        return FCMClient.getClient(fcmURL).create(IFCMService.class);
    }




}

