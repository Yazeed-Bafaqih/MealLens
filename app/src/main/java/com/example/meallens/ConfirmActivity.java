package com.example.meallens;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.meallens.data.Analyzer;
import com.example.meallens.data.DetectedItem;
import com.example.meallens.data.Ingredient;
import com.example.meallens.data.Ingredients;
import com.example.meallens.data.MealAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Confirm screen: detected dish, editable ingredients and portions, live kcal. */
public class ConfirmActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE = "image";

    private String image;
    private MealAnalysis analysis;
    private final List<DetectedItem> items = new ArrayList<>();

    private LinearLayout itemsContainer;
    private LinearLayout addPanel;
    private LinearLayout addOptions;
    private TextView liveKcal;
    private View estimateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);
        EdgeToEdge.apply(this);

        image = getIntent().getStringExtra(EXTRA_IMAGE);
        if (image == null) image = ImageUtil.DEMO;

        ((TextView) findViewById(R.id.headerTitle)).setText("Confirm your meal");
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        ImageUtil.load((ImageView) findViewById(R.id.heroImage), image);

        itemsContainer = findViewById(R.id.itemsContainer);
        addPanel = findViewById(R.id.addPanel);
        addOptions = findViewById(R.id.addOptions);
        liveKcal = findViewById(R.id.liveKcal);
        estimateButton = findViewById(R.id.estimateButton);

        findViewById(R.id.addButton).setOnClickListener(v -> showAddPanel());
        findViewById(R.id.addCancel).setOnClickListener(v -> addPanel.setVisibility(View.GONE));
        estimateButton.setOnClickListener(v -> estimate());

        // Mock recognition with a short delay, like a network call.
        new Handler(Looper.getMainLooper()).postDelayed(this::onAnalysed, Analyzer.MOCK_DELAY_MS);
    }

    private void onAnalysed() {
        if (isFinishing()) return;
        analysis = Analyzer.analyseImage(image);
        items.clear();
        items.addAll(analysis.items);

        findViewById(R.id.loadingBox).setVisibility(View.GONE);
        findViewById(R.id.resultBox).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.detectedText)).setText(analysis.title + "  " + analysis.titleAr);
        ((TextView) findViewById(R.id.confidencePill)).setText(
                String.format(Locale.US, "%d%% match", Math.round(analysis.confidence * 100)));
        renderItems();
    }

    // ------------------------------------------------------------------- items

    private void renderItems() {
        itemsContainer.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        for (int i = 0; i < items.size(); i++) {
            itemsContainer.addView(itemRow(inf, items.get(i), i));
        }
        updateTotal();
    }

    private View itemRow(LayoutInflater inf, DetectedItem item, int index) {
        View v = inf.inflate(R.layout.item_ingredient, itemsContainer, false);
        Ingredient ing = item.ingredient();
        ((TextView) v.findViewById(R.id.itemEmoji)).setText(ing.emoji);
        ((TextView) v.findViewById(R.id.itemName)).setText(ing.name);
        ((TextView) v.findViewById(R.id.itemSub)).setText(ing.nameAr);
        EditText grams = v.findViewById(R.id.gramsInput);
        grams.setText(String.valueOf(item.grams));

        v.findViewById(R.id.minusButton).setOnClickListener(x -> setGrams(item, grams, item.grams - step(item)));
        v.findViewById(R.id.plusButton).setOnClickListener(x -> setGrams(item, grams, item.grams + step(item)));
        grams.setOnFocusChangeListener((x, hasFocus) -> {
            if (!hasFocus) commitTyped(item, grams);
        });
        grams.setOnEditorActionListener((x, actionId, ev) -> {
            commitTyped(item, grams);
            grams.clearFocus();
            return false;
        });
        v.findViewById(R.id.removeButton).setOnClickListener(x -> {
            items.remove(index);
            renderItems();
        });
        return v;
    }

    private static int step(DetectedItem item) {
        return item.grams >= 100 ? 10 : 5;
    }

    private void commitTyped(DetectedItem item, EditText grams) {
        int g;
        try {
            g = Integer.parseInt(grams.getText().toString().trim());
        } catch (NumberFormatException e) {
            g = 0;
        }
        setGrams(item, grams, g);
    }

    private void setGrams(DetectedItem item, EditText grams, int value) {
        item.grams = Math.max(0, value);
        if (!grams.getText().toString().equals(String.valueOf(item.grams))) {
            grams.setText(String.valueOf(item.grams));
        }
        updateTotal();
    }

    private void updateTotal() {
        liveKcal.setText(String.format(Locale.US, "%d kcal", Math.round(Analyzer.totals(items).kcal)));
        estimateButton.setEnabled(!items.isEmpty());
    }

    // --------------------------------------------------------------------- add

    private void showAddPanel() {
        addOptions.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(this);
        boolean any = false;
        for (Ingredient ing : Ingredients.all()) {
            boolean present = false;
            for (DetectedItem it : items) if (it.ingredientId.equals(ing.id)) present = true;
            if (present) continue;
            any = true;
            View row = inf.inflate(R.layout.item_add_option, addOptions, false);
            ((TextView) row.findViewById(R.id.optionEmoji)).setText(ing.emoji);
            ((TextView) row.findViewById(R.id.optionName)).setText(ing.name);
            row.setOnClickListener(v -> {
                items.add(DetectedItem.ofDefault(ing.id));
                addPanel.setVisibility(View.GONE);
                renderItems();
            });
            addOptions.addView(row);
        }
        if (!any) {
            TextView t = new TextView(this);
            t.setText("All known ingredients are already in the list.");
            t.setTextColor(getColor(R.color.muted));
            addOptions.addView(t);
        }
        addPanel.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------------- estimate

    private void estimate() {
        if (analysis == null || items.isEmpty()) return;
        MealAnalysis confirmed = new MealAnalysis(analysis.title, analysis.titleAr, analysis.confidence, items);
        android.content.Intent i = new android.content.Intent(this, SummaryActivity.class);
        i.putExtra(SummaryActivity.EXTRA_IMAGE, image);
        i.putExtra(SummaryActivity.EXTRA_ANALYSIS, confirmed);
        startActivity(i);
    }
}
