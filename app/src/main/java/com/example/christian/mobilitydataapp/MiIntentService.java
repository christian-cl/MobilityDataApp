/**
 * Christian Cintrano on 27/04/15.
 */

package com.example.christian.mobilitydataapp;

import android.app.IntentService;
import android.content.Intent;

public class MiIntentService extends IntentService {

    public static final String ACTION_PROGRESO =
            "net.sgoliver.intent.action.PROGRESO";
    public static final String ACTION_FIN =
            "net.sgoliver.intent.action.FIN";
    private MobilitySQLite db = new MobilitySQLite(this);

    public MiIntentService() {
        super("MiIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        int iter = intent.getIntExtra("iteraciones", 0);

        for(int i=1; i<=iter; i++) {
            tareaLarga();


            db.savePoints(11.5,-11.002514,"CALLE FALSA 123");

            //Comunicamos el progreso
            Intent bcIntent = new Intent();
            bcIntent.setAction(ACTION_PROGRESO);
            bcIntent.putExtra("progreso", i*10);
            sendBroadcast(bcIntent);
        }

        Intent bcIntent = new Intent();
        bcIntent.setAction(ACTION_FIN);
        sendBroadcast(bcIntent);
    }

    private void tareaLarga()
    {
        try {
            Thread.sleep(1000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}