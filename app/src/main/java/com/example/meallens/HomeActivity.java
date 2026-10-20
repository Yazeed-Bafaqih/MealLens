package com.example.meallens;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.meallens.auth.AuthService;
import com.example.meallens.data.DetectedItem;
import com.example.meallens.data.MealStore;
import com.example.meallens.data.SavedMeal;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Home: upload a photo + optional caption, recent meal and history. */
public class HomeActivity extends AppCompatActivity {

    private LinearLayout homeContent;
    private LinearLayout historyContent;
    private LinearLayout recentContainer;
    private LinearLayout historyContainer;
    private ImageView imagePreview;
    private TextView uploadTitle;
    private TextView uploadHint;
    private EditText captionInput;
    private Button analyzeButton;
    private String selectedImage;
    private String userName = "User";

    private ActivityResultLauncher<PickVisualMediaRequest> pickImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        EdgeToEdge.apply(this);

        if (savedInstanceState != null) {
            selectedImage = savedInstanceState.getString("selectedImage");
        }

        AuthService.User user = AuthService.currentUser(this);
        if (user == null) {
            goToLogin();
            return;
        }
        userName = user.displayName;

        ((TextView) findViewById(R.id.greetingText)).setText(greeting());
        ((TextView) findViewById(R.id.userNameText)).setText(userName);

        homeContent = findViewById(R.id.homeContent);
        historyContent = findViewById(R.id.historyContent);
        recentContainer = findViewById(R.id.recentContainer);
        historyContainer = findViewById(R.id.historyContainer);
        imagePreview = findViewById(R.id.imagePreview);
        uploadTitle = findViewById(R.id.uploadTitle);
        uploadHint = findViewById(R.id.uploadHint);
        captionInput = findViewById(R.id.captionInput);
        analyzeButton = findViewById(R.id.analyzeButton);

        pickImage = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
            if (uri == null) return;
            selectedImage = uri.toString();
            showPreview();
        });

        findViewById(R.id.uploadCard).setOnClickListener(v -> pickImage.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        analyzeButton.setOnClickListener(v -> {
            if (selectedImage == null) {
                Toast.makeText(this, "Choose a meal photo first", Toast.LENGTH_SHORT).show();
                return;
            }
            captionInput.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(captionInput.getWindowToken(), 0);
            }
            openConfirm(selectedImage, captionInput.getText().toString().trim());
        });
        findViewById(R.id.settingsButton).setOnClickListener(v -> confirmLogout());
        findViewById(R.id.tabHome).setOnClickListener(v -> showTab(true));
        findViewById(R.id.tabHistory).setOnClickListener(v -> showTab(false));

        if (selectedImage != null) showPreview();
        analyzeButton.setEnabled(selectedImage != null);
        analyzeButton.setAlpha(selectedImage != null ? 1f : 0.55f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recentContainer != null) renderMeals();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (selectedImage != null) outState.putString("selectedImage", selectedImage);
        super.onSaveInstanceState(outState);
    }

    // ----------------------------------------------------------------- actions

    private void showPreview() {
        if (selectedImage == null) return;
        imagePreview.setVisibility(View.VISIBLE);
        ImageUtil.load(imagePreview, selectedImage);
        uploadTitle.setText("Change photo");
        uploadHint.setText("Tap to choose a different picture");
        analyzeButton.setEnabled(true);
        analyzeButton.setAlpha(1f);
    }

    private void openConfirm(String image, String caption) {
        Intent i = new Intent(this, ConfirmActivity.class);
        i.putExtra(ConfirmActivity.EXTRA_IMAGE, image);
        i.putExtra(ConfirmActivity.EXTRA_CAPTION, caption);
        startActivity(i);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setMessage("Signed in as " + userName)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log out", (d, w) -> {
                    AuthService.logout(this);
                    MealStore.clear();
                    goToLogin();
                })
                .show();
    }

    private void goToLogin() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    // ------------------------------------------------------------------ render

    private void showTab(boolean home) {
        homeContent.setVisibility(home ? View.VISIBLE : View.GONE);
        historyContent.setVisibility(home ? View.GONE : View.VISIBLE);
        findViewById(R.id.tabHomeIcon).setAlpha(home ? 1f : 0.5f);
        findViewById(R.id.tabHistoryIcon).setAlpha(home ? 0.5f : 1f);
        TextView hl = findViewById(R.id.tabHomeLabel);
        TextView yl = findViewById(R.id.tabHistoryLabel);
        hl.setTextColor(getColor(home ? R.color.primary : R.color.muted));
        yl.setTextColor(getColor(home ? R.color.muted : R.color.primary));
        hl.setTypeface(null, home ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        yl.setTypeface(null, home ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);
    }

    private void renderMeals() {
        List<SavedMeal> meals = MealStore.all();
        LayoutInflater inf = LayoutInflater.from(this);

        recentContainer.removeAllViews();
        if (meals.isEmpty()) {
            recentContainer.addView(emptyView(inf, recentContainer,
                    "No meals yet. Upload a photo to see calories, macros and health flags."));
        } else {
            recentContainer.addView(mealRow(inf, recentContainer, meals.get(0)));
        }

        historyContainer.removeAllViews();
        if (meals.isEmpty()) {
            historyContainer.addView(emptyView(inf, historyContainer, "Saved meals will appear here."));
        } else {
            for (SavedMeal m : meals) historyContainer.addView(mealRow(inf, historyContainer, m));
        }
    }

    private View emptyView(LayoutInflater inf, LinearLayout parent, String text) {
        View v = inf.inflate(R.layout.item_empty, parent, false);
        ((TextView) v.findViewById(R.id.emptyText)).setText(text);
        return v;
    }

    private View mealRow(LayoutInflater inf, LinearLayout parent, SavedMeal meal) {
        View v = inf.inflate(R.layout.item_recent, parent, false);
        ImageUtil.load((ImageView) v.findViewById(R.id.recentImage), meal.imageUri);
        ((TextView) v.findViewById(R.id.recentTitle)).setText(meal.analysis.title);
        StringBuilder sb = new StringBuilder();
        for (DetectedItem it : meal.analysis.items) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(it.ingredient().name);
        }
        ((TextView) v.findViewById(R.id.recentSub)).setText(sb.toString());
        ((TextView) v.findViewById(R.id.recentMeta)).setText(
                String.format(Locale.US, "%d kcal · %s", Math.round(meal.kcal), meal.time));
        v.setOnClickListener(x -> {
            Intent i = new Intent(this, SummaryActivity.class);
            i.putExtra(SummaryActivity.EXTRA_SAVED_ID, meal.id);
            startActivity(i);
        });
        return v;
    }

    static String greeting() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h < 12) return "Good morning,";
        if (h < 18) return "Good afternoon,";
        return "Good evening,";
    }
}
