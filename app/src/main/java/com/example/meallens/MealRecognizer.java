package com.example.meallens;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;

import com.example.meallens.data.Analyzer;
import com.example.meallens.data.MealAnalysis;
import com.example.meallens.data.MealJsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * Calls the DeepSeek vision model to recognise a meal photo + caption and
 * returns a validated {@link MealAnalysis}.
 *
 * <p>Uses the cheap {@code deepseek-flash} model with image input, JSON output
 * and thinking disabled. The API key is injected into BuildConfig from .env at
 * build time; see app/build.gradle.kts for the security caveat.</p>
 */
public final class MealRecognizer {
    public static final String MODEL = "deepseek-flash";
    public static final String ENDPOINT = "https://api.deepseek.com/chat/completions";
    private static final int MAX_INGREDIENTS = 12;
    private static final int MAX_CAPTION_CHARS = 400;
    private static final int MAX_IMAGE_SIDE = 1024;

    private static final String SYSTEM_PROMPT =
            "You are MealLens, a careful nutrition-vision assistant. Identify the meal in the photo "
                    + "and break it into the visible ingredients. The user's caption is context, not a "
                    + "source of nutrition facts.\n\n"
                    + "Rules:\n"
                    + "- Identify only foods you can reasonably see or infer from the caption. If unsure, "
                    + "lower the confidence instead of inventing detail.\n"
                    + "- Estimate the edible portion in grams for each ingredient as served. Use realistic "
                    + "portions; ground estimates in visual size, count, and the plate or utensils.\n"
                    + "- Return nutrition per 100 g of each ingredient, not for the portion. Use standard "
                    + "reference values (USDA-style) for edible portions.\n"
                    + "- kcal in kcal; protein, carbs, fat, fiber and sugar in grams; sodium in milligrams. "
                    + "Values must be non-negative numbers. Ensure kcal roughly agrees with macros (4/4/9), "
                    + "within reason.\n"
                    + "- Use tags only from: gluten, dairy, nuts, high-sugar, high-gi, vegetarian, vegan, "
                    + "fried, processed. Add only tags that genuinely apply.\n"
                    + "- health_notes: 2-4 short, practical notes grounded in the identified food and "
                    + "nutrition. Use levels info, ok, warn, danger. Do not give medical diagnoses or claim "
                    + "certainty. Mention allergens only when relevant.\n"
                    + "- summary: one or two plain sentences describing the meal and its main nutritional "
                    + "trade-off.\n"
                    + "- ingredient names: concise English name, Arabic name if you know it (otherwise empty "
                    + "string), and one food emoji. Use unique English names. Return at most "
                    + MAX_INGREDIENTS + " ingredients.\n"
                    + "- If the photo contains no food, set dish.name_en to \"No meal detected\", confidence 0, "
                    + "ingredients [], and explain in summary.\n"
                    + "- Return ONLY valid JSON matching the schema exactly. No markdown, no code fences, no "
                    + "extra keys.\n\n"
                    + "JSON schema:\n"
                    + "{\n"
                    + "  \"dish\": {\"name_en\": string, \"name_ar\": string, \"confidence\": number between 0 and 1},\n"
                    + "  \"ingredients\": [\n"
                    + "    {\n"
                    + "      \"name_en\": string,\n"
                    + "      \"name_ar\": string,\n"
                    + "      \"emoji\": string,\n"
                    + "      \"grams\": number between 1 and 2000,\n"
                    + "      \"per_100g\": {\"kcal\": number, \"protein_g\": number, \"carbs_g\": number, "
                    + "\"fat_g\": number, \"fiber_g\": number, \"sugar_g\": number, \"sodium_mg\": number},\n"
                    + "      \"tags\": [string]\n"
                    + "    }\n"
                    + "  ],\n"
                    + "  \"health_notes\": [{\"level\": \"info|ok|warn|danger\", \"title\": string, "
                    + "\"detail\": string}],\n"
                    + "  \"summary\": string\n"
                    + "}\n";

    private MealRecognizer() {}

    /** Blocking call: run on a background thread. */
    public static MealAnalysis recognize(Context context, String image, String caption)
            throws RecognitionException {
        if (ImageUtil.DEMO.equals(image)) {
            // Local fixture used only by the instrumented regression tests.
            MealAnalysis demo = Analyzer.analyseImage(image);
            demo.caption = caption == null ? "" : caption;
            return demo;
        }

        String apiKey = BuildConfig.DEEPSEEK_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RecognitionException(
                    "No DeepSeek API key is configured. Add DEEPSEEK_API_KEY to .env and rebuild the app.");
        }

        String captionText = caption == null ? "" : caption.trim();
        if (captionText.length() > MAX_CAPTION_CHARS) {
            captionText = captionText.substring(0, MAX_CAPTION_CHARS);
        }

        String dataUrl = encodeImage(context, image);
        String payload = buildPayload(dataUrl, captionText);
        String content = requestContent(apiKey.trim(), payload);

        try {
            MealAnalysis analysis = MealJsonParser.parse(content);
            analysis.caption = captionText;
            return analysis;
        } catch (IllegalArgumentException e) {
            throw new RecognitionException(e.getMessage(), e);
        }
    }

    // ----------------------------------------------------------------- request

    private static String buildPayload(String dataUrl, String caption) throws RecognitionException {
        try {
            JSONObject imageUrl = new JSONObject();
            imageUrl.put("url", dataUrl);
            imageUrl.put("detail", "low");

            JSONObject imagePart = new JSONObject();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrl);

            String prompt = "Analyze this meal photo.\n\nUser caption: "
                    + (caption.isEmpty() ? "No caption provided." : "\"" + caption + "\"")
                    + "\n\nReturn the JSON object described in the system prompt.";

            JSONObject textPart = new JSONObject();
            textPart.put("type", "text");
            textPart.put("text", prompt);

            JSONArray content = new JSONArray();
            content.put(textPart);
            content.put(imagePart);

            JSONObject user = new JSONObject();
            user.put("role", "user");
            user.put("content", content);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));
            messages.put(user);

            JSONObject body = new JSONObject();
            body.put("model", MODEL);
            body.put("messages", messages);
            body.put("response_format", new JSONObject().put("type", "json_object"));
            body.put("max_tokens", 1800);
            body.put("temperature", 0.2);
            body.put("thinking", new JSONObject().put("type", "disabled"));
            return body.toString();
        } catch (JSONException e) {
            throw new RecognitionException("Could not build the recognition request.", e);
        }
    }

    private static String requestContent(String apiKey, String payload) throws RecognitionException {
        // JSON mode occasionally returns an empty content string; one retry is cheap.
        for (int attempt = 0; attempt < 2; attempt++) {
            String raw = post(apiKey, payload);
            String content;
            try {
                JSONObject root = new JSONObject(raw);
                JSONArray choices = root.optJSONArray("choices");
                if (choices == null || choices.length() == 0) {
                    throw new RecognitionException("DeepSeek returned no result. Please retry.");
                }
                JSONObject message = choices.getJSONObject(0).optJSONObject("message");
                content = message == null ? "" : message.optString("content", "").trim();
            } catch (JSONException e) {
                throw new RecognitionException("DeepSeek returned an unreadable response. Please retry.", e);
            }
            if (!content.isEmpty()) return content;
            if (attempt == 0) {
                try {
                    Thread.sleep(350);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RecognitionException("Recognition was cancelled.");
                }
            }
        }
        throw new RecognitionException("DeepSeek returned an empty result. Please retry.");
    }

    private static String post(String apiKey, String payload) throws RecognitionException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(ENDPOINT).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(120000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);

            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bytes);
            }

            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String raw = readAll(stream);

            if (code < 200 || code >= 300) {
                throw new RecognitionException(httpError(code, serverMessage(raw)));
            }
            return raw;
        } catch (SocketTimeoutException e) {
            throw new RecognitionException(
                    "DeepSeek took too long to respond. Check your connection and try again.", e);
        } catch (UnknownHostException e) {
            throw new RecognitionException(
                    "No internet connection. Connect and try again.", e);
        } catch (IOException e) {
            throw new RecognitionException(
                    "Could not reach DeepSeek. Check your internet connection and retry.", e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String httpError(int code, String serverMessage) {
        switch (code) {
            case 400:
                return "DeepSeek could not process this image or caption."
                        + (serverMessage.isEmpty() ? "" : " " + serverMessage);
            case 401:
                return "DeepSeek rejected the API key. Check DEEPSEEK_API_KEY in .env and rebuild.";
            case 402:
                return "The DeepSeek account has no remaining credit.";
            case 413:
                return "This photo is too large for DeepSeek. Try a smaller image.";
            case 429:
                return "Too many recognition requests. Wait a moment and retry.";
            default:
                if (code >= 500) {
                    return "DeepSeek is temporarily unavailable. Please retry in a moment.";
                }
                return "Recognition failed (HTTP " + code + ")."
                        + (serverMessage.isEmpty() ? "" : " " + serverMessage);
        }
    }

    private static String serverMessage(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            JSONObject error = new JSONObject(raw).optJSONObject("error");
            String message = error == null ? "" : error.optString("message", "").trim();
            if (message.length() > 180) message = message.substring(0, 180) + "…";
            return message;
        } catch (JSONException e) {
            return "";
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) sb.append(buffer, 0, count);
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------- image

    private static String encodeImage(Context context, String image) throws RecognitionException {
        if (image == null || image.trim().isEmpty()) {
            throw new RecognitionException("Choose a meal photo first.");
        }
        Bitmap bitmap;
        try {
            bitmap = ImageUtil.decode(context, Uri.parse(image), MAX_IMAGE_SIDE);
        } catch (RuntimeException e) {
            throw new RecognitionException("That photo could not be opened. Choose another one.", e);
        }
        if (bitmap == null) {
            throw new RecognitionException("That photo could not be read. Choose another one.");
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 84, out);
            byte[] bytes = out.toByteArray();
            if (bytes.length == 0) {
                throw new RecognitionException("That photo could not be encoded. Choose another one.");
            }
            return "data:image/jpeg;base64,"
                    + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } finally {
            bitmap.recycle();
        }
    }

    /** User-safe failure carrying a message that can be shown directly. */
    public static class RecognitionException extends Exception {
        private static final long serialVersionUID = 1L;

        public RecognitionException(String message) {
            super(message);
        }

        public RecognitionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
