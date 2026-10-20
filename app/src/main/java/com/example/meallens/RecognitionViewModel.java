package com.example.meallens;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.meallens.data.MealAnalysis;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Runs recognition off the main thread and keeps the result across activity
 * recreation (rotation) so a photo is not sent to the API twice.
 */
public class RecognitionViewModel extends AndroidViewModel {
    public enum Status { IDLE, LOADING, SUCCESS, ERROR }

    public static class State {
        public final Status status;
        public final MealAnalysis analysis;
        public final String error;

        private State(Status status, MealAnalysis analysis, String error) {
            this.status = status;
            this.analysis = analysis;
            this.error = error;
        }

        static State idle() { return new State(Status.IDLE, null, null); }
        static State loading() { return new State(Status.LOADING, null, null); }
        static State success(MealAnalysis analysis) {
            return new State(Status.SUCCESS, analysis, null);
        }
        static State error(String message) { return new State(Status.ERROR, null, message); }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>(State.idle());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile int generation;
    private Future<?> task;
    private String image;
    private String caption = "";

    public RecognitionViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<State> state() {
        return state;
    }

    public void analyze(String image, String caption) {
        if (image == null || image.trim().isEmpty()) {
            state.setValue(State.error("Choose a meal photo first."));
            return;
        }
        this.image = image;
        this.caption = caption == null ? "" : caption.trim();
        if (task != null) task.cancel(true);

        final int request = ++generation;
        state.setValue(State.loading());
        task = executor.submit(() -> {
            try {
                MealAnalysis analysis = MealRecognizer.recognize(
                        getApplication(), this.image, this.caption);
                if (request == generation) state.postValue(State.success(analysis));
            } catch (MealRecognizer.RecognitionException e) {
                if (request == generation) state.postValue(State.error(e.getMessage()));
            } catch (RuntimeException e) {
                if (request == generation) {
                    state.postValue(State.error(
                            "Recognition failed unexpectedly. Please retry."));
                }
            }
        });
    }

    public void retry() {
        if (image != null) analyze(image, caption);
    }

    @Override
    protected void onCleared() {
        generation++;
        if (task != null) task.cancel(true);
        executor.shutdownNow();
        super.onCleared();
    }
}
