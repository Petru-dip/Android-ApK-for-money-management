package com.example.expensetracker;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

public abstract class BaseActivity extends AppCompatActivity {

    protected void setupToolbar(@StringRes int titleRes, boolean showBack) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle(titleRes);
                ab.setDisplayHomeAsUpEnabled(showBack);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Meniul comun: doar iconița aplicației cu inel
        getMenuInflater().inflate(R.menu.menu_common, menu);
        MenuItem appIconItem = menu.findItem(R.id.action_app_icon);
        if (appIconItem != null && appIconItem.getActionView() != null) {
            View v = appIconItem.getActionView();
            // Click pe iconiță -> "About" (sau ce vrei tu)
            v.setOnClickListener(view -> startActivity(new Intent(this, AboutActivity.class)));
        }
        return true; // per-activity poate adăuga propriul meniu apelând super + inflate
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Back din toolbar pentru ecranele secundare
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
