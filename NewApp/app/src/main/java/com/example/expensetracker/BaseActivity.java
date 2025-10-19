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
        MaterialToolbar toolbar = findViewById(R.id.toolbar_include);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle(titleRes);
                ab.setDisplayHomeAsUpEnabled(showBack);
            }
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // încarcă ambele meniuri
////        getMenuInflater().inflate(R.menu.drawer_menu, menu);   // meniul tău original
////        getMenuInflater().inflate(R.menu.menu_common, menu); // iconița aplicației
//
//        // click pe iconița încercuită
////        MenuItem appIconItem = menu.findItem(R.id.action_app_icon);
////        if (appIconItem != null && appIconItem.getActionView() != null) {
////            View v = appIconItem.getActionView();
////            v.setOnClickListener(view -> startActivity(new Intent(this, AboutActivity.class)));
////        }
//        return true;
////    }
////
//
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // butonul back din bară (stânga)
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
