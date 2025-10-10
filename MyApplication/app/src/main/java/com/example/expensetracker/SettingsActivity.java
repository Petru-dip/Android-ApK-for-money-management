package com.example.expensetracker;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Setări");
        title.setTextSize(20f);
        title.setPadding(0, 0, 0, pad);

        Switch swAuto = new Switch(this);
        swAuto.setText("Auto-Save (salvare automată din notificări)");
        swAuto.setChecked(Settings.isAutoSaveOn(this));
        swAuto.setOnCheckedChangeListener((CompoundButton buttonView, boolean checked) ->
                Settings.setAutoSave(this, checked));

        root.addView(title);
        root.addView(swAuto);

        setContentView(root);
    }
}
