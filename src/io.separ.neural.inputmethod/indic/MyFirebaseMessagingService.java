package io.separ.neural.inputmethod.indic;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by sepehr on 11/27/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    public MyFirebaseMessagingService(){
        super();
        Log.d("AGAH", "MyFirebaseMessagingService");
    }
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("AGAH", "onMessageReceived");
        String type = remoteMessage.getData().get("type");
        if(type != null) {
            Bitmap image = null;
            String message = null;
            Intent intent = null;
            if (type.equals("bazaar_promotion")) {
                message = remoteMessage.getData().get("message");
                String imageUri = remoteMessage.getData().get("image");
                String packageName = remoteMessage.getData().get("packageName");
                image = getBitmapfromUrl(imageUri);
                intent = sendBazaarNotification(packageName);
            }else if(type.equals("link_promotion")){
                message = remoteMessage.getData().get("message");
                String imageUri = remoteMessage.getData().get("image");
                String url = remoteMessage.getData().get("url");
                image = getBitmapfromUrl(imageUri);
                intent = sendLinkNotification(url);
            }
            if(message != null && image != null && intent != null) {
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                        PendingIntent.FLAG_ONE_SHOT);
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                        .setLargeIcon(image)/*Notification icon image*/
                        .setSmallIcon(R.drawable.ic_launcher_keyboard)
                        .setContentTitle(message)
                        .setStyle(new NotificationCompat.BigPictureStyle()
                                .bigPicture(image))/*Notification with Image*/
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
                return;
            }
        }
    }

    private Intent sendLinkNotification(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        return browserIntent;
    }

    private Intent sendBazaarNotification(String packageName) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("bazaar://details?id=" + packageName));
        intent.setPackage("com.farsitel.bazaar");
        return intent;
    }

    public Bitmap getBitmapfromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
