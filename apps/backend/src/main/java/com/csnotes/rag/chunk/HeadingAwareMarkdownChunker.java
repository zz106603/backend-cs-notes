package com.csnotes.rag.chunk;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Markdown 제목 계층을 먼저 나누고, 긴 섹션만 블록 단위로 추가 분할한다.
 * 코드 블록과 표는 의미가 깨지지 않도록 목표 크기를 넘더라도 하나로 유지한다.
 */
public final class HeadingAwareMarkdownChunker implements MarkdownChunker {
    private final int maxCharacters;

    public HeadingAwareMarkdownChunker(int maxCharacters) {
        if (maxCharacters < 200) throw new IllegalArgumentException("maxCharacters must be at least 200");
        this.maxCharacters = maxCharacters;
    }

    /** 문서 메타데이터를 각 Chunk에 복사하고 동기화용 해시와 안정적인 ID를 만든다. */
    @Override
    public List<DocumentChunk> chunk(ChunkSourceDocument document) {
        List<Section> sections = splitSections(document.content());
        List<DocumentChunk> chunks = new ArrayList<>();
        int sequence = 0;
        for (Section section : sections) {
            for (String content : splitSection(section)) {
                if (content.isBlank()) continue;
                String contentHash = sha256(content);
                String id = sha256(document.documentId() + "\n" + String.join(" > ", section.path())
                        + "\n" + sequence + "\n" + contentHash);
                chunks.add(new DocumentChunk(id, document.documentId(), document.title(), document.path(),
                        document.tags(), section.path(), sequence++, content, contentHash));
            }
        }
        return List.copyOf(chunks);
    }

    private List<Section> splitSections(String markdown) {
        List<Section> sections = new ArrayList<>();
        List<String> hierarchy = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        List<String> currentPath = List.of();
        boolean inFence = false;

        for (String line : markdown.split("\n", -1)) {
            String trimmed = line.trim();
            if (isFence(trimmed)) inFence = !inFence;
            int headingLevel = inFence ? 0 : headingLevel(line);
            if (headingLevel > 0) {
                addSection(sections, currentPath, lines);
                while (hierarchy.size() >= headingLevel) hierarchy.removeLast();
                while (hierarchy.size() < headingLevel - 1) hierarchy.add("");
                hierarchy.add(line.substring(headingLevel + 1).trim());
                currentPath = hierarchy.stream().filter(value -> !value.isBlank()).toList();
                lines = new ArrayList<>();
            }
            lines.add(line);
        }
        addSection(sections, currentPath, lines);
        return sections;
    }

    private List<String> splitSection(Section section) {
        List<String> blocks = blocks(section.content());
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String block : blocks) {
            if (!current.isEmpty() && current.length() + 2 + block.length() > maxCharacters) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            if (block.length() > maxCharacters && !isAtomicBlock(block)) {
                for (String part : splitLongBlock(block)) {
                    if (!current.isEmpty()) {
                        chunks.add(current.toString().trim());
                        current = new StringBuilder();
                    }
                    chunks.add(part);
                }
            } else {
                if (!current.isEmpty()) current.append("\n\n");
                current.append(block);
            }
        }
        if (!current.isEmpty()) chunks.add(current.toString().trim());
        return chunks;
    }

    /** 빈 줄을 경계로 나누되 fenced code block 내부의 빈 줄은 분할하지 않는다. */
    private List<String> blocks(String content) {
        List<String> result = new ArrayList<>();
        StringBuilder block = new StringBuilder();
        boolean inFence = false;
        for (String line : content.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isBlank() && !inFence) {
                if (!block.isEmpty()) {
                    result.add(block.toString().trim());
                    block = new StringBuilder();
                }
                continue;
            }
            if (!block.isEmpty()) block.append('\n');
            block.append(line);
            if (isFence(trimmed)) inFence = !inFence;
        }
        if (!block.isEmpty()) result.add(block.toString().trim());
        return result;
    }

    private List<String> splitLongBlock(String block) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < block.length()) {
            int end = Math.min(start + maxCharacters, block.length());
            if (end < block.length()) {
                int newline = block.lastIndexOf('\n', end);
                int sentence = Math.max(block.lastIndexOf(". ", end), block.lastIndexOf("다. ", end));
                int boundary = Math.max(newline, sentence < 0 ? -1 : sentence + 1);
                if (boundary > start + maxCharacters / 2) end = boundary;
            }
            parts.add(block.substring(start, end).trim());
            start = end;
            while (start < block.length() && Character.isWhitespace(block.charAt(start))) start++;
        }
        return parts;
    }

    private boolean isAtomicBlock(String block) {
        String trimmed = block.stripLeading();
        return trimmed.startsWith("```") || trimmed.startsWith("~~~") || trimmed.startsWith("|");
    }

    private void addSection(List<Section> sections, List<String> path, List<String> lines) {
        String content = String.join("\n", lines).trim();
        if (!content.isBlank()) sections.add(new Section(List.copyOf(path), content));
    }

    private int headingLevel(String line) {
        int level = 0;
        while (level < line.length() && level < 6 && line.charAt(level) == '#') level++;
        return level > 0 && level < line.length() && line.charAt(level) == ' ' ? level : 0;
    }

    private boolean isFence(String line) {
        return line.startsWith("```") || line.startsWith("~~~");
    }

    /** 같은 입력에는 항상 같은 ID가 나오도록 SHA-256을 사용한다. */
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record Section(List<String> path, String content) {
    }
}
