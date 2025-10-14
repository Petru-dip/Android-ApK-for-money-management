package com.example.expensetracker;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.transition.MaterialFadeThrough;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable edge-to-edge drawing
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        updateSystemBarsAppearance();

        // Apply Material FadeThrough for activity transitions
        MaterialFadeThrough fade = new MaterialFadeThrough();
        fade.setSecondaryAnimatorProvider(null);
        fade.setDuration(200);
        getWindow().setEnterTransition(fade);
        getWindow().setReturnTransition(fade);
        getWindow().setExitTransition(new MaterialFadeThrough());
        getWindow().setReenterTransition(new MaterialFadeThrough());
    }

    protected void setupToolbar(@StringRes int titleRes, boolean showBack) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            ActionBar ab = getSupportActionBar();
            if (ab != null) {
                ab.setTitle(titleRes);
                ab.setDisplayHomeAsUpEnabled(showBack);
            }

            // Apply status bar top inset as padding to toolbar
            ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
                Insets status = insets.getInsets(WindowInsetsCompat.Type.statusBars());
                v.setPadding(v.getPaddingLeft(), status.top, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        // Also pad the activity content for navigation bar
        View content = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (content != null) {
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
                Insets nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), nav.bottom);
                return insets;
            });
        }
    }

    private void updateSystemBarsAppearance() {
        View decor = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decor);
        if (controller == null) return;
        boolean isLightTheme = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                != Configuration.UI_MODE_NIGHT_YES;
        controller.setAppearanceLightStatusBars(isLightTheme);
        controller.setAppearanceLightNavigationBars(isLightTheme);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // încarcă ambele meniuri
        getMenuInflater().inflate(R.menu.drawer_menu, menu);   // meniul tău original
        getMenuInflater().inflate(R.menu.menu_common, menu); // iconița aplicației

        // click pe iconița încercuită
        MenuItem appIconItem = menu.findItem(R.id.action_app_icon);
        if (appIconItem != null && appIconItem.getActionView() != null) {
            View v = appIconItem.getActionView();
            v.setOnClickListener(view -> startActivity(new Intent(this, AboutActivity.class)));
        }
        return true;
    }

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
