package com.example.expensetracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(new android.widget.TextView(this) {{
            setText("Setări – în curând");
            setPadding(32,32,32,32);
            setTextSize(18f);
        }});
    }
}
