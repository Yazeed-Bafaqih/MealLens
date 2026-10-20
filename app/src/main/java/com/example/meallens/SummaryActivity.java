package com.example.meallens;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.meallens.data.Analyzer;
import com.example.meallens.data.DetectedItem;
import com.example.meallens.data.Flag;
import com.example.meallens.data.MealAnalysis;
import com.example.meallens.data.MealStore;
import com.example.meallens.data.Nutrition;
import com.example.meallens.data.SavedMeal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Summary: calories, health score, macros, health flags and per-ingredient breakdown. */
public class SummaryActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE = "image";
    public static final String EXTRA_ANALYSIS = "analysis";
    public static final String EXTRA_SAVED_ID = "savedId";

    private String image;
    private MealAnalysis analysis;
    private boolean readOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);
        EdgeToEdge.apply(this);

        String savedId = getIntent().getStringExtra(EXTRA_SAVED_ID);
        if (savedId != null) {
            SavedMeal saved = MealStore.find(savedId);
            if (saved == null) {
                finish();
                return;
            }
            image = saved.imageUri;
            analysis = saved.analysis;
            readOnly = true;
        } else {
            image = getIntent().getStringExtra(EXTRA_IMAGE);
            analysis = (MealAnalysis) getIntent().getSerializableExtra(EXTRA_ANALYSIS);
            if (analysis == null) {
                finish();
                return;
            }
        }

        ((TextView) findViewById(R.id.headerTitle)).setText("Meal summary");
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        ImageUtil.load((ImageView) findViewById(R.id.heroImage), image);

        Button save = findViewById(R.id.saveButton);
        if (readOnly) {
            save.setText("Done");
            save.setTextColor(getColor(R.color.primary));
            save.setBackgroundTintList(getColorStateList(R.color.primarySoft));
            save.setOnClickListener(v -> finish());
        } else {
            save.setOnClickListener(v -> saveMeal());
        }

        render();
    }

    private void render() {
        List<DetectedItem> items = analysis.items;
        Nutrition t = Analyzer.totals(items);
        int score = Analyzer.healthScore(items);
        double macroKcal = t.protein * 4 + t.carbs * 4 + t.fat * 9;
        if (macroKcal <= 0) macroKcal = 1;

        ((TextView) findViewById(R.id.kcalText)).setText(String.valueOf(Math.round(t.kcal)));
        ((TextView) findViewById(R.id.estimatedText)).setText(String.format(Locale.US,
                "Estimated for %s · %d g total · %d%% match",
                analysis.title, analysis.totalGrams(), Math.round(analysis.confidence * 100)));

        TextView captionView = findViewById(R.id.captionText);
        if (analysis.caption == null || analysis.caption.trim().isEmpty()) {
            captionView.setVisibility(View.GONE);
        } else {
            captionView.setVisibility(View.VISIBLE);
            captionView.setText("“" + analysis.caption.trim() + "”");
        }

        TextView summaryView = findViewById(R.id.summaryText);
        if (analysis.summary == null || analysis.summary.trim().isEmpty()) {
            summaryView.setVisibility(View.GONE);
        } else {
            summaryView.setVisibility(View.VISIBLE);
            summaryView.setText(analysis.summary.trim());
        }

        // Score ring
        int tone = score >= 70 ? R.color.ok : score >= 45 ? R.color.warn : R.color.danger;
        TextView scoreText = findViewById(R.id.scoreText);
        scoreText.setText(String.valueOf(score));
        scoreText.setTextColor(getColor(tone));
        GradientDrawable ring = (GradientDrawable) findViewById(R.id.scoreRing).getBackground().mutate();
        ring.setStroke(dp(5), getColor(tone));
        ((TextView) findViewById(R.id.scoreSub)).setText(score >= 70 ? "Balanced meal."
                : score >= 45 ? "Enjoy occasionally. Watch the portion."
                : "Treat meal. Best kept small and infrequent.");

        // Macro bar + rows
        double pP = t.protein * 4 / macroKcal, pC = t.carbs * 4 / macroKcal, pF = t.fat * 9 / macroKcal;
        setWeight(findViewById(R.id.barProtein), pP);
        setWeight(findViewById(R.id.barCarbs), pC);
        setWeight(findViewById(R.id.barFat), pF);
        macroRow(R.id.rowProtein, "🏋️", "Protein", t.protein, pP, R.color.primary);
        macroRow(R.id.rowCarbs, "🌾", "Carbs", t.carbs, pC, R.color.warn);
        macroRow(R.id.rowFat, "💧", "Fat", t.fat, pF, R.color.info);
        minor(R.id.minorSugar, "Sugar", Math.round(t.sugar) + " g");
        minor(R.id.minorFiber, "Fiber", Math.round(t.fiber) + " g");
        minor(R.id.minorSodium, "Sodium", Math.round(t.sodium) + " mg");

        // Flags
        LinearLayout flags = findViewById(R.id.flagsContainer);
        flags.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        List<Flag> allFlags = new ArrayList<>(Analyzer.buildFlags(items));
        Set<String> seenTitles = new HashSet<>();
        for (Flag f : allFlags) seenTitles.add(f.title.toLowerCase(Locale.US));
        if (analysis.aiFlags != null) {
            for (Flag f : analysis.aiFlags) {
                if (seenTitles.add(f.title.toLowerCase(Locale.US))) allFlags.add(f);
            }
        }
        for (Flag f : allFlags) {
            View v = inf.inflate(R.layout.item_flag, flags, false);
            int bg, fg;
            String icon;
            switch (f.level) {
                case DANGER: bg = R.drawable.bg_flag_danger; fg = R.color.danger; icon = "⛔"; break;
                case WARN: bg = R.drawable.bg_flag_warn; fg = R.color.warn; icon = "⚠️"; break;
                case OK: bg = R.drawable.bg_flag_ok; fg = R.color.ok; icon = "✅"; break;
                default: bg = R.drawable.bg_flag_info; fg = R.color.info; icon = "ℹ️"; break;
            }
            v.setBackgroundResource(bg);
            ((TextView) v.findViewById(R.id.flagIcon)).setText(icon);
            TextView title = v.findViewById(R.id.flagTitle);
            title.setText(f.title);
            title.setTextColor(getColor(fg));
            ((TextView) v.findViewById(R.id.flagDetail)).setText(f.detail);
            flags.addView(v);
        }

        // Breakdown sorted by kcal
        List<DetectedItem> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> Double.compare(b.nutrition().kcal, a.nutrition().kcal));
        LinearLayout breakdown = findViewById(R.id.breakdownContainer);
        breakdown.removeAllViews();
        for (int i = 0; i < sorted.size(); i++) {
            DetectedItem it = sorted.get(i);
            Nutrition n = it.nutrition();
            View v = inf.inflate(R.layout.item_breakdown, breakdown, false);
            ((TextView) v.findViewById(R.id.rowEmoji)).setText(it.ingredient().emoji);
            ((TextView) v.findViewById(R.id.rowName)).setText(it.ingredient().name);
            ((TextView) v.findViewById(R.id.rowSub)).setText(String.format(Locale.US,
                    "%d g · P %d · C %d · F %d", it.grams, Math.round(n.protein), Math.round(n.carbs), Math.round(n.fat)));
            ((TextView) v.findViewById(R.id.rowKcal)).setText(String.format(Locale.US, "%d kcal", Math.round(n.kcal)));
            v.findViewById(R.id.rowDivider).setVisibility(i < sorted.size() - 1 ? View.VISIBLE : View.GONE);
            breakdown.addView(v);
        }
    }

    private void macroRow(int rowId, String icon, String label, double grams, double pct, int color) {
        View row = findViewById(rowId);
        GradientDrawable sw = (GradientDrawable) row.findViewById(R.id.macroSwatch).getBackground().mutate();
        sw.setColor(getColor(color));
        ((TextView) row.findViewById(R.id.macroIcon)).setText(icon);
        ((TextView) row.findViewById(R.id.macroLabel)).setText(label);
        ((TextView) row.findViewById(R.id.macroPct)).setText(String.format(Locale.US, "%d%%", Math.round(pct * 100)));
        ((TextView) row.findViewById(R.id.macroValue)).setText(String.format(Locale.US, "%d g", Math.round(grams)));
    }

    private void minor(int id, String label, String value) {
        View v = findViewById(id);
        ((TextView) v.findViewById(R.id.minorLabel)).setText(label);
        ((TextView) v.findViewById(R.id.minorValue)).setText(value);
    }

    private static void setWeight(View v, double weight) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) v.getLayoutParams();
        lp.weight = (float) Math.max(weight, 0.0001);
        v.setLayoutParams(lp);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void saveMeal() {
        String time = "Today, " + new SimpleDateFormat("h:mm a", Locale.US).format(new Date());
        MealStore.add(new SavedMeal(String.valueOf(System.currentTimeMillis()), image, analysis,
                Analyzer.totals(analysis.items).kcal, time));
        Toast.makeText(this, "Meal saved", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, HomeActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
