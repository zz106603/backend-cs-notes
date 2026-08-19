package com.csnotes.document;

import java.util.ArrayList;
import java.util.List;

/** Markdown 본문과 검색 메타데이터(title, tags)를 분리하고 다시 직렬화한다. */
final class MarkdownFrontMatter {
    private MarkdownFrontMatter() {
    }

    /** front matter가 없는 기존 문서는 전체 원문을 본문으로 반환한다. */
    static Parsed parse(String source) {
        String normalized = source == null ? "" : source.replace("\r\n", "\n");
        if (!normalized.startsWith("---\n")) return new Parsed(null, List.of(), normalized);
        int closing = normalized.indexOf("\n---\n", 4);
        if (closing < 0) return new Parsed(null, List.of(), normalized);

        String title = null;
        List<String> tags = new ArrayList<>();
        boolean readingTags = false;
        for (String line : normalized.substring(4, closing).split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("title:")) {
                title = unquote(trimmed.substring(6).trim());
                readingTags = false;
            } else if (trimmed.equals("tags:")) {
                readingTags = true;
            } else if (readingTags && trimmed.startsWith("- ")) {
                String tag = unquote(trimmed.substring(2).trim());
                if (!tag.isBlank()) tags.add(tag);
            } else if (!trimmed.isBlank()) {
                readingTags = false;
            }
        }
        return new Parsed(title, List.copyOf(tags), normalized.substring(closing + 5));
    }

    static String render(String title, List<String> tags, String body) {
        StringBuilder result = new StringBuilder("---\n");
        result.append("title: \"").append(escape(title)).append("\"\n");
        result.append("tags:\n");
        tags.forEach(tag -> result.append("  - \"").append(escape(tag)).append("\"\n"));
        return result.append("---\n").append(body).toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return value;
    }

    record Parsed(String title, List<String> tags, String body) {
    }
}
