package com.example.expensetracker;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(new android.widget.TextView(this) {{
            setText("BrimTech Finance\nVersiune 1.0");
            setPadding(32,32,32,32);
            setTextSize(18f);
        }});
    }
}
