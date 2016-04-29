package com.example.christian.neotrack.adapters;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.example.christian.neotrack.TrackActivity;

import java.util.ArrayList;

/**
 * Created by Christian Cintrano on 14/03/16.
 * Listener to recognition speech to the device
 */
public class MyRecognitionListener implements RecognitionListener {

    private final TrackActivity context;

    public MyRecognitionListener(TrackActivity context) {
        this.context = context;
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d("Speech", "onBeginningOfSpeech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d("Speech", "onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d("Speech", "onEndOfSpeech");
    }

    @Override
    public synchronized void onError(int error) {
        String text = "";
        // Translate Android SpeechRecognizer errors to Web Speech API errors.
        switch(error) {
            case SpeechRecognizer.ERROR_AUDIO:
                text = "ERROR_AUDIO";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                text = "ERROR_CLIENT";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                text = "ERROR_RECOGNIZER_BUSY";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_SERVER:
                text = "ERROR_NETWORK";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                text = "ERROR_NO_MATCH";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                text = "ERROR_SPEECH_TIMEOUT";
//                context.sr.stopListening();
//                context.sr.cancel();
//                context.sr.startListening(true);
//                MyRecognitionListener listener = new MyRecognitionListener(context);
//                context.sr.setRecognitionListener(listener);
//                context.sr.startListening(RecognizerIntent.getVoiceDetailsIntent(context.getApplicationContext()));
//                context.restartSpeech();

                context.speakerOut.speak("No texto", TextToSpeech.QUEUE_ADD, null);
                // Save input
                Location location = context.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                context.myLocationChanged(location, "STOP");
                break;
        }
        Log.d("Speech", "onError " + text);
//        recognizeSpeechDi/rectly();
        context.runningSpeech = false;
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d("Speech", "onEvent");
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d("Speech", "onPartialResults");
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d("Speech", "onReadyForSpeech");
    }


    @Override
    public void onResults(Bundle results) {
        Log.d("Speech", "onResults");
        ArrayList strlist = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (int i = 0; i < (strlist != null ? strlist.size() : 0); i++ ) {
            Log.d("Speech", "result=" + strlist.get(i));
        }

//        context.sr.startListening(RecognizerIntent.getVoiceDetailsIntent(context.getApplicationContext()));
        if (strlist != null) {
            context.speakerOut.speak("Texto introducido " + strlist.get(0), TextToSpeech.QUEUE_ADD, null);
        }
        context.speeching = false;
//        context.newSpeech = true;
        context.runningSpeech = false;
        // Save input
        Location location = context.locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        context.myLocationChanged(location,"STOP");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        Log.d("Speech", "onRmsChanged");
    }

}

