package com.shiji.guard;

public final class AiHelper {

    public interface Callback {
        void onText(String text);

        void onError(String message);
    }

    private AiHelper() {
    }

    public static void generate(String cacheKey, String payload, boolean daily, boolean force,
                                AiStyle.Profile profile, Callback cb) {
        App app = App.get();
        if (profile == null) {
            cb.onError("评价风格未初始化");
            return;
        }
        if (profile.customEmpty()) {
            cb.onError("自定义风格还没填写，请到设置补充");
            return;
        }
        String key = app.prefs().apiKey();
        if (key == null || key.trim().isEmpty()) {
            cb.onError("请先在「设置」里填写 API Key");
            return;
        }
        app.io(() -> {
            try {
                String system = profile.systemPrompt(daily);
                String user = payload;
                if (force) {
                    user = user + "\n\n这是重新生成，请换一个观察角度，避免与上一版重复。";
                }
                String text = AiClient.complete(app.prefs(), system, user, profile.maxTokens());
                app.repo().aiPut(cacheKey, text);
                app.ui(() -> cb.onText(text));
            } catch (Throwable t) {
                String msg = t.getMessage();
                if (msg == null || msg.trim().isEmpty()) msg = t.getClass().getSimpleName();
                String finalMsg = msg;
                app.ui(() -> cb.onError(finalMsg));
            }
        });
    }

    public static void test(Callback cb) {
        App app = App.get();
        if (app.prefs().apiKey().trim().isEmpty()) {
            cb.onError("请先填写 API Key");
            return;
        }
        app.io(() -> {
            try {
                String t = AiClient.complete(app.prefs(), "你是接口连通性测试助手。", "只回复四个字：接口可用", 16);
                app.ui(() -> cb.onText(t));
            } catch (Throwable t) {
                String msg = t.getMessage();
                if (msg == null || msg.trim().isEmpty()) msg = t.getClass().getSimpleName();
                String finalMsg = msg;
                app.ui(() -> cb.onError(finalMsg));
            }
        });
    }
}
