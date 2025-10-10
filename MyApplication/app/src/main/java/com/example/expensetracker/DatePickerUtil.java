package com.example.expensetracker;

import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DatePickerUtil {

    // Folosim UN SINGUR format peste tot ca să fie compatibil cu parse()/UI
    private static final String PATTERN = "yyyy-MM-dd";
    private static final SimpleDateFormat SDF =
            new SimpleDateFormat(PATTERN, Locale.getDefault());

    private DatePickerUtil() {}

    public static String today() {
        synchronized (SDF) {
            return SDF.format(new Date());
        }
    }

    public static long parse(String s) {
        if (s == null || s.trim().isEmpty()) return System.currentTimeMillis();
        try {
            synchronized (SDF) {
                return SDF.parse(s.trim()).getTime();
            }
        } catch (ParseException e) {
            // fallback: acum
            return System.currentTimeMillis();
        }
    }

    // ✅ NECESAR pentru EditExpenseActivity
    public static String format(long millis) {
        synchronized (SDF) {
            return SDF.format(new Date(millis));
        }
    }

    public static void attach(Context ctx, EditText target) {
        target.setFocusable(false);
        target.setOnClickListener(v -> {
            final Calendar cal = Calendar.getInstance();
            // dacă are deja o dată, pornește dialogul de la acea dată
            long init = parse(target.getText() != null ? target.getText().toString() : null);
            cal.setTimeInMillis(init);

            DatePickerDialog dlg = new DatePickerDialog(
                    ctx,
                    (view, year, month, dayOfMonth) -> {
                        Calendar c = Calendar.getInstance();
                        c.set(Calendar.YEAR, year);
                        c.set(Calendar.MONTH, month);
                        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        target.setText(format(c.getTimeInMillis()));
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            );
            dlg.show();
        });
    }
}
