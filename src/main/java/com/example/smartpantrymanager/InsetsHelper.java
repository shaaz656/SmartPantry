package com.example.smartpantrymanager;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
public class InsetsHelper {
    public static void apply(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}