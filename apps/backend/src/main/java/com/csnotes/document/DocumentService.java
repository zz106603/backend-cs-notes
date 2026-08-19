package com.csnotes.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(".git", ".idea", ".agents", ".codex", "apps");
    private static final Comparator<DocumentMetadata> TITLE_ORDER =
            Comparator.comparing(DocumentMetadata::title, String.CASE_INSENSITIVE_ORDER);

    private final Path documentRoot;
    private final long refreshIntervalNanos;
    private final Object refreshLock = new Object();

    private volatile DocumentIndex index = DocumentIndex.empty();
    private volatile long lastRefreshNanos;

    public DocumentService(
            @Value("${cs-notes.documents.root}") Path documentRoot,
            @Value("${cs-notes.documents.refresh-interval-ms:2000}") long refreshIntervalMs
    ) {
        this.documentRoot = documentRoot.toAbsolutePath().normalize();
        this.refreshIntervalNanos = TimeUnit.MILLISECONDS.toNanos(Math.max(0, refreshIntervalMs));
    }

    public List<DocumentModels.CategoryResponse> findCategories() {
        return currentIndex().categories();
    }

    public List<DocumentModels.DocumentSummaryResponse> findDocuments(String category, String query) {
        String normalizedQuery = normalize(query);
        Predicate<DocumentMetadata> categoryFilter = document -> category == null
                || category.isBlank()
                || document.category().equalsIgnoreCase(category.trim());
        Predicate<DocumentMetadata> queryFilter = document -> normalizedQuery.isBlank()
                || document.normalizedTitle().contains(normalizedQuery)
                || document.normalizedPath().contains(normalizedQuery);

        return currentIndex().documents().stream()
                .filter(categoryFilter.and(queryFilter))
                .map(DocumentMetadata::toSummary)
                .toList();
    }

    public Optional<DocumentModels.DocumentDetailResponse> findDocument(String id) {
        DocumentMetadata metadata = currentIndex().documentsById().get(id);
        if (metadata == null) {
            return Optional.empty();
        }

        Path target = documentRoot.resolve(metadata.relativePath()).normalize();
        if (!target.startsWith(documentRoot) || !isReadableMarkdown(target)) {
            return Optional.empty();
        }

        try {
            String content = Files.readString(target, StandardCharsets.UTF_8);
            return Optional.of(metadata.toDetail(content));
        } catch (IOException exception) {
            throw new DocumentReadException("문서를 읽는 중 오류가 발생했습니다: " + metadata.relativePath(), exception);
        }
    }

    /** 웹 편집이나 외부 파일 변경 후 다음 조회 전에 인덱스를 즉시 갱신할 때 사용한다. */
    public void refreshIndex() {
        synchronized (refreshLock) {
            index = buildIndex(index);
            lastRefreshNanos = System.nanoTime();
        }
    }

    private DocumentIndex currentIndex() {
        long now = System.nanoTime();
        if (index.initialized() && now - lastRefreshNanos < refreshIntervalNanos) {
            return index;
        }

        synchronized (refreshLock) {
            now = System.nanoTime();
            if (!index.initialized() || now - lastRefreshNanos >= refreshIntervalNanos) {
                index = buildIndex(index);
                lastRefreshNanos = now;
            }
            return index;
        }
    }

    private DocumentIndex buildIndex(DocumentIndex previousIndex) {
        if (!Files.isDirectory(documentRoot)) {
            throw new DocumentReadException("문서 루트를 찾을 수 없습니다: " + documentRoot);
        }

        Map<String, DocumentMetadata> previousByPath = previousIndex.documents().stream()
                .collect(Collectors.toMap(DocumentMetadata::relativePath, document -> document));

        try {
            List<DocumentMetadata> documents = scanDocumentPaths().stream()
                    .map(path -> loadMetadata(path, previousByPath))
                    .sorted(TITLE_ORDER)
                    .toList();

            Map<String, DocumentMetadata> documentsById = documents.stream()
                    .collect(Collectors.toUnmodifiableMap(DocumentMetadata::id, document -> document));
            Map<String, Long> categoryCounts = documents.stream()
                    .collect(Collectors.groupingBy(DocumentMetadata::category, Collectors.counting()));
            List<DocumentModels.CategoryResponse> categories = categoryCounts.entrySet().stream()
                    .map(entry -> new DocumentModels.CategoryResponse(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(DocumentModels.CategoryResponse::name, String.CASE_INSENSITIVE_ORDER))
                    .toList();

            return new DocumentIndex(documents, documentsById, categories, true);
        } catch (IOException exception) {
            throw new DocumentReadException("문서 목록을 불러오는 중 오류가 발생했습니다.", exception);
        }
    }

    private List<Path> scanDocumentPaths() throws IOException {
        List<Path> documents = new ArrayList<>();
        Files.walkFileTree(documentRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                if (!directory.equals(documentRoot) && isInExcludedDirectory(directory)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (!documentRoot.equals(file.getParent()) && isReadableMarkdown(file)) {
                    documents.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return documents;
    }

    private DocumentMetadata loadMetadata(Path path, Map<String, DocumentMetadata> previousByPath) {
        try {
            String relativePath = toRelativePath(path);
            Instant updatedAt = Files.getLastModifiedTime(path).toInstant();
            long size = Files.size(path);
            DocumentMetadata cached = previousByPath.get(relativePath);

            if (cached != null && cached.updatedAt().equals(updatedAt) && cached.size() == size) {
                return cached;
            }

            String fallbackTitle = path.getFileName().toString().replaceFirst("(?i)\\.md$", "");
            String title = readTitle(path).orElse(fallbackTitle);
            String category = relativePath.substring(0, relativePath.indexOf('/'));
            String id = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(relativePath.getBytes(StandardCharsets.UTF_8));

            return new DocumentMetadata(
                    id, title, category, relativePath, normalize(title), normalize(relativePath), updatedAt, size
            );
        } catch (IOException exception) {
            throw new DocumentReadException("문서 메타데이터를 읽는 중 오류가 발생했습니다: " + path, exception);
        }
    }

    private Optional<String> readTitle(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("# ") && !trimmed.substring(2).isBlank()) {
                    return Optional.of(trimmed.substring(2).trim());
                }
            }
            return Optional.empty();
        }
    }

    private String toRelativePath(Path path) {
        return documentRoot.relativize(path).toString().replace('\\', '/');
    }

    private boolean isInExcludedDirectory(Path path) {
        Path relativePath = documentRoot.relativize(path);
        return relativePath.getNameCount() >= 1 && EXCLUDED_DIRECTORIES.contains(relativePath.getName(0).toString());
    }

    private boolean isReadableMarkdown(Path path) {
        return Files.isRegularFile(path)
                && Files.isReadable(path)
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record DocumentIndex(
            List<DocumentMetadata> documents,
            Map<String, DocumentMetadata> documentsById,
            List<DocumentModels.CategoryResponse> categories,
            boolean initialized
    ) {
        private static DocumentIndex empty() {
            return new DocumentIndex(List.of(), Map.of(), List.of(), false);
        }
    }

    private record DocumentMetadata(
            String id,
            String title,
            String category,
            String relativePath,
            String normalizedTitle,
            String normalizedPath,
            Instant updatedAt,
            long size
    ) {
        private DocumentModels.DocumentSummaryResponse toSummary() {
            return new DocumentModels.DocumentSummaryResponse(id, title, category, relativePath, updatedAt);
        }

        private DocumentModels.DocumentDetailResponse toDetail(String content) {
            return new DocumentModels.DocumentDetailResponse(id, title, category, relativePath, content, updatedAt);
        }
    }
}
