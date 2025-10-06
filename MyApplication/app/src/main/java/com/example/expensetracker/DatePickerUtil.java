package com.example.expensetracker;
import android.app.DatePickerDialog;
import android.content.Context;
import android.widget.EditText;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
public class DatePickerUtil {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    public static void attach(Context ctx, EditText editText) {
        editText.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dlg = new DatePickerDialog(ctx,
                    (view, y, m, d) -> {
                        Calendar sel = Calendar.getInstance();
                        sel.set(Calendar.YEAR, y);
                        sel.set(Calendar.MONTH, m);
                        sel.set(Calendar.DAY_OF_MONTH, d);
                        editText.setText(sdf.format(sel.getTime()));
                    },
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dlg.show();
        });
    }
    public static String today() {
        Calendar c = Calendar.getInstance();
        return sdf.format(c.getTime());
    }
    public static long parse(String date) {
        try { return sdf.parse(date).getTime(); } catch (Exception e) { return System.currentTimeMillis(); }
    }
}
