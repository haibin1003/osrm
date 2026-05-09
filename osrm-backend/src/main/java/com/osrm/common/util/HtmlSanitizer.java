package com.osrm.common.util;

import java.util.regex.Pattern;

/**
 * 防 XSS 文本清洗工具：去除 script/iframe/on 事件等危险 HTML 片段。
 * 适用于普通文本字段（软件包名/描述/使用场景等）— 这些字段不应保留任何 HTML，
 * 一律剥离标签 + 转义特殊字符。
 */
public final class HtmlSanitizer {

    private static final Pattern SCRIPT_PATTERN =
            Pattern.compile("<\\s*script[^>]*>.*?<\\s*/\\s*script\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IFRAME_PATTERN =
            Pattern.compile("<\\s*iframe[^>]*>.*?<\\s*/\\s*iframe\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern STYLE_PATTERN =
            Pattern.compile("<\\s*style[^>]*>.*?<\\s*/\\s*style\\s*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ON_EVENT_PATTERN =
            Pattern.compile("\\son\\w+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PATTERN =
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTML_TAG_PATTERN =
            Pattern.compile("<[^>]+>", Pattern.DOTALL);

    private HtmlSanitizer() {}

    /**
     * 清洗纯文本字段：删除危险标签 + 剥离全部 HTML 标签。
     * 入库前调用，保证落库内容不含可执行 HTML/JS。
     */
    public static String sanitizeText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;
        result = SCRIPT_PATTERN.matcher(result).replaceAll("");
        result = IFRAME_PATTERN.matcher(result).replaceAll("");
        result = STYLE_PATTERN.matcher(result).replaceAll("");
        result = ON_EVENT_PATTERN.matcher(result).replaceAll("");
        result = JAVASCRIPT_PATTERN.matcher(result).replaceAll("");
        result = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        return result.trim();
    }
}
