package com.example.expensetracker;

import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setupToolbar(R.string.app_name, true);

        TextView tv = findViewById(R.id.aboutText);
        tv.setText("BrimTech Expense Tracker\n\nVersiunea 1.0\n© 2025 BrimTech Solutions");
    }
}
