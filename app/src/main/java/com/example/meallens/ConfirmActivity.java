package com.example.meallens;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.meallens.data.Analyzer;
import com.example.meallens.data.DetectedItem;
import com.example.meallens.data.Ingredient;
import com.example.meallens.data.Ingredients;
import com.example.meallens.data.MealAnalysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Confirm screen: AI-detected dish, editable ingredients and portions, live kcal. */
public class ConfirmActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE = "image";
    public static final String EXTRA_CAPTION = "caption";

    private String image;
    private String caption = "";
    private MealAnalysis analysis;
    private final List<DetectedItem> items = new ArrayList<>();
    private final List<Ingredient> recognised = new ArrayList<>();
    private boolean restoredFromState;

    private LinearLayout itemsContainer;
    private LinearLayout addPanel;
    private LinearLayout addOptions;
    private TextView liveKcal;
    private View estimateButton;
    private View loadingBox;
    private View resultBox;
    private View errorBox;
    private TextView errorMessage;
    private RecognitionViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);
        EdgeToEdge.apply(this);

        image = getIntent().getStringExtra(EXTRA_IMAGE);
        if (image == null || image.trim().isEmpty()) {
            finish();
            return;
        }
        caption = getIntent().getStringExtra(EXTRA_CAPTION);
        if (caption == null) caption = "";

        ((TextView) findViewById(R.id.headerTitle)).setText("Confirm your meal");
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        ImageUtil.load((ImageView) findViewById(R.id.heroImage), image);

        itemsContainer = findViewById(R.id.itemsContainer);
        addPanel = findViewById(R.id.addPanel);
        addOptions = findViewById(R.id.addOptions);
        liveKcal = findViewById(R.id.liveKcal);
        estimateButton = findViewById(R.id.estimateButton);
        loadingBox = findViewById(R.id.loadingBox);
        resultBox = findViewById(R.id.resultBox);
        errorBox = findViewById(R.id.errorBox);
        errorMessage = findViewById(R.id.errorMessage);

        findViewById(R.id.addButton).setOnClickListener(v -> showAddPanel());
        findViewById(R.id.addCancel).setOnClickListener(v -> addPanel.setVisibility(View.GONE));
        estimateButton.setOnClickListener(v -> estimate());
        findViewById(R.id.retryButton).setOnClickListener(v -> {
            if (viewModel != null) viewModel.retry();
        });

        if (savedInstanceState != null) {
            analysis = (MealAnalysis) savedInstanceState.getSerializable("confirmedAnalysis");
        }
        if (analysis != null) {
            // Coming back from recreation after a successful analysis: keep the
            // user's portion edits instead of re-sending the photo.
            restoredFromState = true;
            items.addAll(analysis.items);
            showAnalysis();
            return;
        }

        showLoading();
        viewModel = new ViewModelProvider(this).get(RecognitionViewModel.class);
        viewModel.state().observe(this, state -> {
            if (state == null || restoredFromState || state.status == RecognitionViewModel.Status.IDLE) {
                return;
            }
            switch (state.status) {
                case LOADING:
                    showLoading();
                    break;
                case SUCCESS:
                    applyAnalysis(state.analysis);
                    break;
                case ERROR:
                    showError(state.error);
                    break;
                default:
                    break;
            }
        });
        RecognitionViewModel.State current = viewModel.state().getValue();
        if (current == null || current.status == RecognitionViewModel.Status.IDLE) {
            viewModel.analyze(image, caption);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (analysis != null) {
            outState.putSerializable("confirmedAnalysis",
                    new MealAnalysis(analysis.title, analysis.titleAr, analysis.confidence, items,
                            analysis.summary, analysis.caption, analysis.aiFlags));
        }
        super.onSaveInstanceState(outState);
    }

    // ------------------------------------------------------------------ states

    private void showLoading() {
        loadingBox.setVisibility(View.VISIBLE);
        resultBox.setVisibility(View.GONE);
        errorBox.setVisibility(View.GONE);
        estimateButton.setEnabled(false);
        TextView loadingText = findViewById(R.id.loadingText);
        loadingText.setText(ImageUtil.DEMO.equals(image)
                ? "Loading demo recognition…"
                : "Analyzing your photo with DeepSeek…");
    }

    private void showError(String message) {
        loadingBox.setVisibility(View.GONE);
        resultBox.setVisibility(View.GONE);
        errorBox.setVisibility(View.VISIBLE);
        estimateButton.setEnabled(false);
        errorMessage.setText(message == null || message.isEmpty()
                ? "Recognition failed. Please retry."
                : message);
    }

    private void applyAnalysis(MealAnalysis result) {
        if (result == null) {
            showError("Recognition returned no result. Please retry.");
            return;
        }
        analysis = result;
        items.clear();
        items.addAll(result.items);
        recognised.clear();
        for (DetectedItem item : result.items) recognised.add(item.ingredient());
        showAnalysis();
    }

    private void showAnalysis() {
        loadingBox.setVisibility(View.GONE);
        errorBox.setVisibility(View.GONE);
        resultBox.setVisibility(View.VISIBLE);

        boolean demo = ImageUtil.DEMO.equals(image);
        ((TextView) findViewById(R.id.resultLabel)).setText(demo ? "DEMO RESULT" : "AI ANALYSIS");
        ((TextView) findViewById(R.id.detectedText)).setText(
                analysis.titleAr == null || analysis.titleAr.isEmpty()
                        ? analysis.title
                        : analysis.title + "  " + analysis.titleAr);
        ((TextView) findViewById(R.id.confidencePill)).setText(
                String.format(Locale.US, "%d%% match", Math.round(analysis.confidence * 100)));
        ((TextView) findViewById(R.id.resultHint)).setText(demo
                ? "Recognition is simulated: every photo uses the Masoub example. Correct the items below."
                : "Estimated by DeepSeek from your photo and caption. Correct anything that looks off.");

        TextView captionView = findViewById(R.id.captionText);
        String shownCaption = analysis.caption == null || analysis.caption.isEmpty()
                ? caption : analysis.caption;
        if (shownCaption == null || shownCaption.trim().isEmpty()) {
            captionView.setVisibility(View.GONE);
        } else {
            captionView.setVisibility(View.VISIBLE);
            captionView.setText("“" + shownCaption.trim() + "”");
        }

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
        TextView sub = v.findViewById(R.id.itemSub);
        sub.setText(ing.nameAr);
        sub.setVisibility(ing.nameAr.isEmpty() ? View.GONE : View.VISIBLE);
        EditText grams = v.findViewById(R.id.gramsInput);
        // Repeated rows share this ID. Restore their values from the saved model,
        // otherwise Android restores the last row's text into every input.
        grams.setSaveEnabled(false);
        grams.setText(String.valueOf(item.grams));
        grams.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    item.grams = Math.max(0, Integer.parseInt(s.toString().trim()));
                } catch (NumberFormatException e) {
                    item.grams = 0;
                }
                updateTotal();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

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
        for (Ingredient ing : addCandidates()) {
            boolean present = false;
            for (DetectedItem it : items) if (it.ingredientId.equals(ing.id)) present = true;
            if (present) continue;
            any = true;
            View row = inf.inflate(R.layout.item_add_option, addOptions, false);
            ((TextView) row.findViewById(R.id.optionEmoji)).setText(ing.emoji);
            ((TextView) row.findViewById(R.id.optionName)).setText(ing.name);
            row.setOnClickListener(v -> {
                items.add(DetectedItem.ofDefault(ing));
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

    /**
     * The built-in reference foods plus everything the model recognised, so a
     * removed AI ingredient can be added back.
     */
    private List<Ingredient> addCandidates() {
        Map<String, Ingredient> byId = new LinkedHashMap<>();
        for (Ingredient ing : Ingredients.all()) byId.put(ing.id, ing);
        for (Ingredient ing : recognised) byId.put(ing.id, ing);
        return new ArrayList<>(byId.values());
    }

    // ---------------------------------------------------------------- estimate

    private void estimate() {
        if (analysis == null || items.isEmpty()) return;
        MealAnalysis confirmed = new MealAnalysis(
                analysis.title, analysis.titleAr, analysis.confidence, items,
                analysis.summary, analysis.caption, analysis.aiFlags);
        android.content.Intent i = new android.content.Intent(this, SummaryActivity.class);
        i.putExtra(SummaryActivity.EXTRA_IMAGE, image);
        i.putExtra(SummaryActivity.EXTRA_ANALYSIS, confirmed);
        startActivity(i);
    }
}
