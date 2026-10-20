package com.example.meallens;

import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.meallens.data.MealStore;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Regression coverage for confirmation edits, Android recreation and saving a meal. */
@RunWith(AndroidJUnit4.class)
public class SprintTwoFlowTest {
    private ActivityScenario<ConfirmActivity> openMeal() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, ConfirmActivity.class);
        intent.putExtra(ConfirmActivity.EXTRA_IMAGE, ImageUtil.DEMO);
        ActivityScenario<ConfirmActivity> scenario = ActivityScenario.launch(intent);
        AtomicBoolean ready = new AtomicBoolean();
        long deadline = android.os.SystemClock.uptimeMillis() + 10000;
        while (!ready.get() && android.os.SystemClock.uptimeMillis() < deadline) {
            scenario.onActivity(a -> ready.set(a.findViewById(R.id.estimateButton).isEnabled()));
            if (!ready.get()) Thread.sleep(100);
        }
        assertTrue("Demo analysis should finish", ready.get());
        return scenario;
    }

    @Test
    public void typingPortionUpdatesCaloriesWithoutLeavingFieldAndSurvivesRecreation() throws Exception {
        try (ActivityScenario<ConfirmActivity> scenario = openMeal()) {
            scenario.onActivity(a -> {
                LinearLayout rows = a.findViewById(R.id.itemsContainer);
                EditText grams = rows.getChildAt(0).findViewById(R.id.gramsInput);
                grams.requestFocus();
                grams.setText("200");
                assertEquals("1043 kcal", ((TextView) a.findViewById(R.id.liveKcal)).getText().toString());
            });
            scenario.recreate();
            scenario.onActivity(a -> {
                LinearLayout rows = a.findViewById(R.id.itemsContainer);
                assertEquals("200", ((EditText) rows.getChildAt(0).findViewById(R.id.gramsInput))
                        .getText().toString());
                assertEquals("1043 kcal", ((TextView) a.findViewById(R.id.liveKcal)).getText().toString());
            });
        }
    }

    @Test
    public void removingAllIngredientsDisablesEstimateAndAddingOneEnablesIt() throws Exception {
        try (ActivityScenario<ConfirmActivity> scenario = openMeal()) {
            scenario.onActivity(a -> {
                LinearLayout rows = a.findViewById(R.id.itemsContainer);
                while (rows.getChildCount() > 0) {
                    rows.getChildAt(0).findViewById(R.id.removeButton).performClick();
                }
                assertTrue(!a.findViewById(R.id.estimateButton).isEnabled());
                a.findViewById(R.id.addButton).performClick();
                ((LinearLayout) a.findViewById(R.id.addOptions)).getChildAt(0).performClick();
                assertEquals(1, rows.getChildCount());
                assertTrue(a.findViewById(R.id.estimateButton).isEnabled());
            });
        }
    }

    @Test
    public void editedPortionReachesSummaryAndSavedHistory() throws Exception {
        MealStore.clear();
        try (ActivityScenario<ConfirmActivity> scenario = openMeal()) {
            scenario.onActivity(a -> {
                LinearLayout rows = a.findViewById(R.id.itemsContainer);
                EditText grams = rows.getChildAt(0).findViewById(R.id.gramsInput);
                grams.requestFocus();
                grams.setText("200");
            });
            onView(withId(R.id.estimateButton)).perform(click());
            onView(withId(R.id.kcalText)).check(matches(withText("1043")));
            onView(withId(R.id.saveButton)).perform(click());
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                assertEquals(1, MealStore.all().size());
                assertEquals(200, MealStore.latest().analysis.items.get(0).grams);
            });
        } finally {
            MealStore.clear();
        }
    }
}
