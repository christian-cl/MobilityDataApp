package com.example.christian.mobilitydataapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by Christian Cintrano on 8/05/15.
 *
 */
public class LogTabFragment extends Fragment {
    private TextView out;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.activity_tab_fragment_log, container, false);
        out = (TextView) view.findViewById(R.id.tabtextview);
        out.setMovementMethod(new ScrollingMovementMethod());

        appendLog("Init: ");
        return view;
    }

    // Métodos para mostrar información
    public void appendLog(String text) {
        out.append(text + "\n");
        // Scrolling down
        final Layout layout = out.getLayout();
        if(layout != null){
            int scrollDelta = layout.getLineBottom(out.getLineCount() - 1)
                    - out.getScrollY() - out.getHeight();
            if(scrollDelta > 0)
                out.scrollBy(0, scrollDelta);
        }
    }
}
