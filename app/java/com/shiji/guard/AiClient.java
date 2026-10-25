package com.shiji.guard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class AiClient {

    private AiClient() {
    }

    public static String complete(Prefs prefs, String system, String user, int maxTokens) throws Exception {
        String key = prefs.apiKey() == null ? "" : prefs.apiKey().trim();
        if (key.isEmpty()) throw new IllegalStateException("还没有填写 API Key");

        String base = prefs.baseUrl();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        String url = base.contains("/chat/completions") ? base : base + "/chat/completions";

        JSONObject body = new JSONObject();
        body.put("model", prefs.model());
        body.put("temperature", 1.05);
        body.put("max_tokens", maxTokens);
        body.put("stream", false);
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", system));
        messages.put(new JSONObject().put("role", "user").put("content", user));
        body.put("messages", messages);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Authorization", "Bearer " + key);
        conn.setRequestProperty("Accept", "application/json");
        try {
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }
            int code = conn.getResponseCode();
            InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
            String raw = read(in);
            if (code < 200 || code >= 300) {
                throw new IOException("接口返回 " + code + (raw.isEmpty() ? "" : " · " + trim(raw, 160)));
            }
            JSONObject obj = new JSONObject(raw);
            JSONArray choices = obj.optJSONArray("choices");
            if (choices == null || choices.length() == 0) throw new IOException("接口没有返回内容");
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            String text = message == null ? "" : message.optString("content", "");
            if (text.trim().isEmpty()) throw new IOException("接口返回为空");
            return text.trim();
        } finally {
            conn.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        if (in == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private static String trim(String s, int max) {
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
