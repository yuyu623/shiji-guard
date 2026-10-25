package com.shiji.guard;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * AI 评价风格：把「怎么说」和「说什么」拆开。
 * 风格、强度、关注点、长度、自定义描述会组成 prompt，并参与点评缓存 key。
 */
public final class AiStyle {

    public static final String ID_DIRECT = "direct";
    public static final String ID_GENTLE = "gentle";
    public static final String ID_ANALYST = "analyst";
    public static final String ID_HUMOR = "humor";
    public static final String ID_COACH = "coach";
    public static final String ID_CUSTOM = "custom";

    public static final String[] IDS = {ID_DIRECT, ID_GENTLE, ID_ANALYST, ID_HUMOR, ID_COACH, ID_CUSTOM};
    public static final String[] NAMES = {"直白批评", "温柔陪伴", "冷静分析", "幽默反讽", "教练模式", "自定义"};

    public static final String[] FOCUS_IDS = {"structure", "night", "delay", "single", "action"};
    public static final String[] FOCUS_NAMES = {"时间结构", "熬夜", "拖延", "单应用", "行动建议"};

    public static final String[] LENGTH_NAMES = {"一句话", "短评", "详细"};

    private static final String[] DESCRIPTIONS = {
            "像关系很近、说话很直的朋友。先点出最扎眼的问题，再给一个不太好受但有用的建议。不铺垫，不绕弯。",
            "像耐心的伴侣或朋友。先接住今天的难处，再提一个很小、做得到的改变。不指责，不说教。",
            "像冷静的数据分析师。只谈结构、占比、趋势和因果关系，少用情绪词，最后给一个基于数据的调整。",
            "像一个损友，用反讽、夸张、网络用语和热梗吐槽，但只吐槽行为，不否定整个人。梗要自然，不要堆砌，不要用过时或冒犯性的梗。",
            "像行动教练。先给一句结论，再给一个今天就能执行的具体动作，最好带上时间和场景。",
            ""
    };

    private static final String[] INTENSITY_PROMPTS = {
            "非常温和：几乎不批评，以理解和鼓励为主，最多轻轻提一个建议。",
            "温和：先肯定，再温和指出一个小问题。",
            "偏温和：正常交流，指出问题但不加重语气。",
            "直接：不绕弯，明确指出主要问题，语气平实。",
            "偏严厉：措辞更硬，明确表达不满，但仍然克制。",
            "严厉：不留情面，重点批评具体行为，给出硬建议。",
            "很严厉：非常直接、强烈不满，可以尖锐，但绝不侮辱人格、不攻击外貌或身份。"
    };

    private static final String[] INTENSITY_SHORT = {"很温和", "温和", "偏温和", "直接", "偏严厉", "严厉", "很严厉"};

    private static final String[] FOCUS_PROMPTS = {
            "时间结构：关注使用时段、碎片化程度、连续使用长度。",
            "熬夜：关注睡前和凌晨的使用，以及它对睡眠的挤占。",
            "拖延：关注用娱乐应用回避正事的迹象。",
            "单应用：关注某一个应用占比过高、反复打开。",
            "行动建议：最后落到一个具体、当天能做的动作。"
    };

    private static final String[] LENGTH_PROMPTS = {
            "只写 1 句，35 字以内，直接给结论。",
            "2 到 3 句，90 字以内，短评。",
            "4 到 6 句，180 字以内，可以有更完整的观察和建议。"
    };

    private AiStyle() {
    }

    public static final class Profile {
        public final String id;
        public final int intensity;
        public final String focus;
        public final int length;
        public final String custom;

        Profile(String id, int intensity, String focus, int length, String custom) {
            this.id = id;
            this.intensity = intensity;
            this.focus = focus;
            this.length = length;
            this.custom = custom;
        }

        public String name() {
            return AiStyle.name(id);
        }

        public String badge() {
            return name() + " · " + intensity + "/7";
        }

        public boolean customEmpty() {
            return ID_CUSTOM.equals(id) && custom.isEmpty();
        }

        public String signature() {
            String customHash = ID_CUSTOM.equals(id) ? Integer.toHexString(custom.hashCode()) : "0";
            return "v2|" + id + "|" + intensity + "|" + focus + "|" + length + "|" + customHash;
        }

        public int maxTokens() {
            if (length == 0) return 160;
            if (length == 2) return 560;
            return 320;
        }

        public String systemPrompt(boolean daily) {
            StringBuilder sb = new StringBuilder();
            sb.append("你是「时迹」里的时间观察员。你只能看到这台手机的应用使用时长数据，看不到屏幕内容。\n\n");

            sb.append("【风格】").append(name()).append('\n');
            if (ID_CUSTOM.equals(id)) {
                if (custom.isEmpty()) {
                    sb.append("用户还没有填写自定义描述，请按直白批评的风格写。\n");
                } else {
                    sb.append("用户自定义风格：").append(custom).append('\n');
                    sb.append("按这句话描述的语气写，但不要违反下面的底线。\n");
                }
            } else {
                sb.append(DESCRIPTIONS[indexOf(id)]).append('\n');
            }

            sb.append("\n【强度】").append(intensity).append("/7\n");
            sb.append(INTENSITY_PROMPTS[intensity - 1]).append('\n');
            sb.append("强度决定措辞的直接程度；强度再高也不等于可以人身攻击。\n");

            sb.append("\n【关注点】\n").append(focusInstructions()).append('\n');

            sb.append("\n【长度】").append(LENGTH_PROMPTS[length]).append('\n');

            sb.append("\n【写作底线】\n")
                    .append("- 只基于给出的数据，不虚构，不脑补屏幕内容。\n")
                    .append("- 批评行为和使用结构，不攻击人格、外貌、身份、家庭；不羞辱、不歧视。\n")
                    .append("- 不做医疗或心理诊断，不鼓励自伤、极端行为或危险做法。\n")
                    .append("- 不要复述所有数字，挑 1 到 2 个最有信息量的。\n")
                    .append("- 只输出点评正文：不要标题、列表、编号、markdown、引号包裹、表情符号；不要以「根据数据」开头。\n")
                    .append("- 使用简体中文。\n");

            sb.append("\n本次写").append(daily ? "今天" : "这一个周期").append("的点评，只输出正文。");
            return sb.toString();
        }

        private String focusInstructions() {
            if (focus == null || focus.isEmpty()) {
                return "- 没有特别指定，挑数据里最值得说的一点。";
            }
            StringBuilder sb = new StringBuilder();
            String[] parts = focus.split(",");
            for (String p : parts) {
                int i = indexOfFocus(p);
                if (i >= 0) sb.append("- ").append(FOCUS_PROMPTS[i]).append('\n');
            }
            if (sb.length() == 0) return "- 没有特别指定，挑数据里最值得说的一点。";
            return sb.toString();
        }
    }

    public static Profile current(Prefs p) {
        String id = p.aiStyle();
        if (indexOf(id) < 0) id = ID_DIRECT;
        return new Profile(id, p.aiIntensity(), normalizeFocus(p.aiFocus()), p.aiLength(), p.aiCustom());
    }

    public static String name(String id) {
        int i = indexOf(id);
        return i >= 0 ? NAMES[i] : NAMES[0];
    }

    public static String description(String id) {
        int i = indexOf(id);
        return i >= 0 ? DESCRIPTIONS[i] : DESCRIPTIONS[0];
    }

    public static int indexOf(String id) {
        if (id == null) return -1;
        for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return i;
        return -1;
    }

    private static int indexOfFocus(String id) {
        for (int i = 0; i < FOCUS_IDS.length; i++) if (FOCUS_IDS[i].equals(id)) return i;
        return -1;
    }

    public static String intensityLabel(int v) {
        int i = Math.max(1, Math.min(7, v)) - 1;
        return (i + 1) + " / 7 · " + INTENSITY_SHORT[i];
    }

    public static String focusCsv(Set<String> set) {
        StringBuilder sb = new StringBuilder();
        for (String id : FOCUS_IDS) {
            if (set != null && set.contains(id)) {
                if (sb.length() > 0) sb.append(',');
                sb.append(id);
            }
        }
        return sb.toString();
    }

    public static LinkedHashSet<String> parseFocus(String csv) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (csv == null || csv.isEmpty()) return set;
        for (String p : csv.split(",")) {
            if (indexOfFocus(p) >= 0) set.add(p);
        }
        return set;
    }

    public static String normalizeFocus(String csv) {
        return focusCsv(parseFocus(csv));
    }

    public static boolean canAuto(Profile p, Prefs prefs) {
        if (p == null || p.customEmpty()) return false;
        if (prefs.apiKey() == null || prefs.apiKey().trim().isEmpty()) return false;
        long at = prefs.aiStyleChangedAt();
        if (at <= 0) return false;
        return System.currentTimeMillis() - at < 24 * 60 * 60 * 1000L;
    }

    public static void markChanged(Prefs prefs, Repo repo) {
        prefs.setAiStyleChangedAt(System.currentTimeMillis());
        try {
            repo.clearAi();
        } catch (Throwable ignored) {
        }
    }

    public static String cacheKey(String periodKey, Profile p) {
        return periodKey + "|" + p.signature();
    }
}
