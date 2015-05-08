package com.example.christian.mobilitydataapp;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by christian on 8/05/15.
 */
public class LogTabFragment extends Fragment {
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.activity_tab_fragment_log, container, false);
        TextView textview = (TextView) view.findViewById(R.id.tabtextview);
        textview.setText("Prueba log");
        return view;
    }
}
