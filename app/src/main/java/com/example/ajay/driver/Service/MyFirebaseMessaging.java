package com.example.ajay.driver.Service;

import android.content.Intent;

import com.example.ajay.driver.CustomerCall;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

public class MyFirebaseMessaging extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        //Because we will send the firebase message with contain lat and lng from Rider app
        //so we need to convert message to latlng
        LatLng customer_location=new Gson().fromJson(remoteMessage.getNotification().getBody(),LatLng.class);

        Intent intent=new Intent(getBaseContext(), CustomerCall.class);
        intent.putExtra("lat",customer_location.latitude);
        intent.putExtra("lng",customer_location.longitude);
        intent.putExtra("customer",remoteMessage.getNotification().getTitle());

        startActivity(intent);

    }
}
