package com.csnotes.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.FileVisitResult;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final String TRASH_DIRECTORY = ".trash";
    private static final String EMPTY_DIRECTORY_MARKER = ".gitkeep";
    private static final Set<String> EXCLUDED_DIRECTORIES = Set.of(
            ".git", ".gradle", ".idea", ".agents", ".codex", ".trash", "apps", "build", "docs", "gradle"
    );
    private static final Set<String> WINDOWS_RESERVED_NAMES = Set.of(
            "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    );
    private static final Pattern INVALID_PATH_CHARACTERS = Pattern.compile("[<>:\"/\\\\|?*\\p{Cntrl}]");
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

    public DocumentModels.CategoryResponse createCategory(DocumentModels.CreateCategoryRequest request) {
        synchronized (refreshLock) {
            String category = validateCategoryPath(request.path());
            Path target = resolveCategoryPath(category);
            if (Files.exists(target)) {
                throw new DocumentConflictException("같은 경로의 폴더가 이미 있습니다.");
            }

            try {
                ensureCreatableDirectoryInsideRoot(target);
                Files.createDirectories(target);
                ensureRealDirectoryInsideRoot(target);
                Files.writeString(target.resolve(EMPTY_DIRECTORY_MARKER), "", StandardCharsets.UTF_8);
                rebuildAfterMutation();
                return findCategory(category);
            } catch (IOException exception) {
                throw new DocumentReadException("폴더를 생성하는 중 오류가 발생했습니다.", exception);
            }
        }
    }

    /** 폴더 자체를 이동하므로 하위 폴더와 문서도 새 경로로 함께 이동한다. */
    public DocumentModels.CategoryResponse updateCategory(DocumentModels.UpdateCategoryRequest request) {
        synchronized (refreshLock) {
            String category = validateCategoryPath(request.path());
            String newCategory = validateCategoryPath(request.newPath());
            Path source = resolveCategoryPath(category);
            Path target = resolveCategoryPath(newCategory);
            if (!Files.isDirectory(source) || Files.isSymbolicLink(source)) {
                throw new DocumentNotFoundException("수정할 폴더를 찾을 수 없습니다.");
            }
            if (source.equals(target)) {
                return findCategory(category);
            }
            if (target.startsWith(source)) {
                throw new InvalidDocumentPathException("폴더를 자기 하위 경로로 이동할 수 없습니다.");
            }
            if (Files.exists(target)) {
                throw new DocumentConflictException("이동할 위치에 동일한 폴더가 이미 있습니다.");
            }

            try {
                ensureCreatableDirectoryInsideRoot(target.getParent());
                Files.createDirectories(target.getParent());
                ensureRealParentInsideRoot(target);
                moveWithoutReplacing(source, target);
                rebuildAfterMutation();
                return findCategory(newCategory);
            } catch (IOException exception) {
                throw new DocumentReadException("폴더를 수정하는 중 오류가 발생했습니다.", exception);
            }
        }
    }

    /** 제목·경로·태그·본문의 관련도 점수를 계산해 검색 결과를 정렬한다. */
    public List<DocumentModels.DocumentSummaryResponse> findDocuments(String category, String query) {
        String normalizedQuery = normalize(query);
        Predicate<DocumentMetadata> categoryFilter = document -> category == null
                || category.isBlank()
                || document.category().equalsIgnoreCase(category.trim())
                || document.category().toLowerCase(Locale.ROOT)
                .startsWith(category.trim().toLowerCase(Locale.ROOT) + "/");
        var documents = currentIndex().documents().stream().filter(categoryFilter);
        if (normalizedQuery.isBlank()) {
            return documents.map(document -> document.toSummary(null)).toList();
        }

        return documents
                .map(document -> new SearchResult(document, searchScore(document, normalizedQuery)))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparingInt(SearchResult::score).reversed()
                        .thenComparing(result -> result.document().title(), String.CASE_INSENSITIVE_ORDER))
                .map(result -> result.document().toSummary(buildExcerpt(result.document(), normalizedQuery)))
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
            String content = MarkdownFrontMatter.parse(Files.readString(target, StandardCharsets.UTF_8)).body();
            return Optional.of(metadata.toDetail(content));
        } catch (IOException exception) {
            throw new DocumentReadException("문서를 읽는 중 오류가 발생했습니다: " + metadata.relativePath(), exception);
        }
    }

    public DocumentModels.DocumentDetailResponse createDocument(DocumentModels.CreateDocumentRequest request) {
        synchronized (refreshLock) {
            String title = validatePathSegment(request.title(), "제목");
            String category = validateCategoryPath(request.category());
            Path target = resolveDocumentPath(category, title);
            if (Files.exists(target)) {
                throw new DocumentConflictException("같은 카테고리에 동일한 이름의 문서가 이미 있습니다.");
            }

            List<String> tags = normalizeTags(request.tags());
            String content = withFrontMatter(title, tags, request.content());
            try {
                Files.createDirectories(target.getParent());
                ensureRealParentInsideRoot(target);
                writeAtomically(target, content);
                rebuildAfterMutation();
                return detailForPath(target, content);
            } catch (IOException exception) {
                throw new DocumentReadException("문서를 저장하는 중 오류가 발생했습니다.", exception);
            }
        }
    }

    public DocumentModels.DocumentDetailResponse updateDocument(
            String id,
            DocumentModels.UpdateDocumentRequest request
    ) {
        synchronized (refreshLock) {
            DocumentMetadata metadata = currentIndex().documentsById().get(id);
            if (metadata == null) {
                throw new DocumentNotFoundException("수정할 문서를 찾을 수 없습니다.");
            }

            Path source = documentRoot.resolve(metadata.relativePath()).normalize();
            if (!isReadableMarkdown(source)) {
                throw new DocumentNotFoundException("수정할 문서 파일을 찾을 수 없습니다.");
            }

            try {
                Instant actualUpdatedAt = Files.getLastModifiedTime(source).toInstant();
                if (!actualUpdatedAt.equals(request.expectedUpdatedAt())) {
                    throw new DocumentConflictException("다른 곳에서 문서가 수정되었습니다. 최신 내용을 다시 불러와 주세요.");
                }

                String title = validatePathSegment(request.title(), "제목");
                String category = validateCategoryPath(request.category());
                Path target = resolveDocumentPath(category, title);
                if (!source.equals(target) && Files.exists(target)) {
                    throw new DocumentConflictException("이동할 위치에 동일한 이름의 문서가 이미 있습니다.");
                }

                if (!source.equals(target)) {
                    Files.createDirectories(target.getParent());
                    ensureRealParentInsideRoot(target);
                    moveWithoutReplacing(source, target);
                }

                List<String> tags = normalizeTags(request.tags());
                String content = withFrontMatter(title, tags, request.content());
                writeAtomically(target, content);
                rebuildAfterMutation();
                return detailForPath(target, content);
            } catch (DocumentConflictException | InvalidDocumentPathException exception) {
                throw exception;
            } catch (IOException exception) {
                throw new DocumentReadException("문서를 수정하는 중 오류가 발생했습니다.", exception);
            }
        }
    }

    public DocumentModels.DocumentDetailResponse moveDocument(
            String id,
            DocumentModels.MoveDocumentRequest request
    ) {
        synchronized (refreshLock) {
            DocumentMetadata metadata = currentIndex().documentsById().get(id);
            if (metadata == null) {
                throw new DocumentNotFoundException("이동할 문서를 찾을 수 없습니다.");
            }
            Path source = documentRoot.resolve(metadata.relativePath()).normalize();
            if (!isReadableMarkdown(source)) {
                throw new DocumentNotFoundException("이동할 문서 파일을 찾을 수 없습니다.");
            }

            try {
                Instant actualUpdatedAt = Files.getLastModifiedTime(source).toInstant();
                if (!actualUpdatedAt.equals(request.expectedUpdatedAt())) {
                    throw new DocumentConflictException("다른 곳에서 문서가 수정되었습니다. 최신 내용을 다시 불러와 주세요.");
                }
                String category = validateCategoryPath(request.category());
                Path target = resolveCategoryPath(category).resolve(source.getFileName()).normalize();
                ensureInsideDocumentRoot(target);
                if (source.equals(target)) {
                    return detailForPath(source, Files.readString(source, StandardCharsets.UTF_8));
                }
                if (Files.exists(target)) {
                    throw new DocumentConflictException("이동할 위치에 동일한 이름의 문서가 이미 있습니다.");
                }

                ensureCreatableDirectoryInsideRoot(target.getParent());
                Files.createDirectories(target.getParent());
                ensureRealParentInsideRoot(target);
                moveWithoutReplacing(source, target);
                Files.setLastModifiedTime(target, FileTime.from(Instant.now()));
                String content = Files.readString(target, StandardCharsets.UTF_8);
                rebuildAfterMutation();
                return detailForPath(target, content);
            } catch (DocumentConflictException | InvalidDocumentPathException exception) {
                throw exception;
            } catch (IOException exception) {
                throw new DocumentReadException("문서를 이동하는 중 오류가 발생했습니다.", exception);
            }
        }
    }

    public void moveDocumentToTrash(String id) {
        synchronized (refreshLock) {
            DocumentMetadata metadata = currentIndex().documentsById().get(id);
            if (metadata == null) {
                throw new DocumentNotFoundException("휴지통으로 이동할 문서를 찾을 수 없습니다.");
            }

            Path source = documentRoot.resolve(metadata.relativePath()).normalize();
            if (!isReadableMarkdown(source)) {
                throw new DocumentNotFoundException("휴지통으로 이동할 문서 파일을 찾을 수 없습니다.");
            }
            Path target = trashRoot().resolve(metadata.relativePath()).normalize();
            ensureInsideDocumentRoot(target);
            if (Files.exists(target)) {
                throw new DocumentConflictException("휴지통에 동일한 경로의 문서가 이미 있습니다.");
            }

            try {
                Files.createDirectories(target.getParent());
                ensureRealParentInsideRoot(target);
                moveWithoutReplacing(source, target);
                Files.setLastModifiedTime(target, FileTime.from(Instant.now()));
                rebuildAfterMutation();
            } catch (IOException exception) {
                throw new DocumentReadException("문서를 휴지통으로 이동하는 중 오류가 발생했습니다.", exception);
            }
        }
    }

    public List<DocumentModels.TrashDocumentResponse> findTrashDocuments() {
        Path trashRoot = trashRoot();
        if (!Files.isDirectory(trashRoot)) {
            return List.of();
        }

        try {
            List<Path> trashedFiles = new ArrayList<>();
            Files.walkFileTree(trashRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (isReadableMarkdown(file)) {
                        trashedFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            return trashedFiles.stream()
                    .map(this::toTrashResponse)
                    .sorted(Comparator.comparing(DocumentModels.TrashDocumentResponse::deletedAt).reversed())
                    .toList();
        } catch (IOException exception) {
            throw new DocumentReadException("휴지통 문서 목록을 불러오는 중 오류가 발생했습니다.", exception);
        }
    }

    public void permanentlyDeleteTrashDocument(String id) {
        String originalPath = decodeId(id)
                .orElseThrow(() -> new DocumentNotFoundException("삭제할 휴지통 문서를 찾을 수 없습니다."));
        Path target = trashRoot().resolve(originalPath).normalize();
        if (!target.startsWith(trashRoot()) || !isReadableMarkdown(target)) {
            throw new DocumentNotFoundException("삭제할 휴지통 문서를 찾을 수 없습니다.");
        }

        try {
            Files.delete(target);
            removeEmptyTrashParents(target.getParent());
        } catch (IOException exception) {
            throw new DocumentReadException("휴지통 문서를 영구 삭제하는 중 오류가 발생했습니다.", exception);
        }
    }

    public DocumentModels.DocumentDetailResponse restoreTrashDocument(String id) {
        synchronized (refreshLock) {
            String originalPath = decodeId(id)
                    .orElseThrow(() -> new DocumentNotFoundException("복원할 휴지통 문서를 찾을 수 없습니다."));
            Path source = trashRoot().resolve(originalPath).normalize();
            if (!source.startsWith(trashRoot()) || !isReadableMarkdown(source)) {
                throw new DocumentNotFoundException("복원할 휴지통 문서를 찾을 수 없습니다.");
            }

            Path target = documentRoot.resolve(originalPath).normalize();
            ensureInsideDocumentRoot(target);
            if (Files.exists(target)) {
                throw new DocumentConflictException("원래 경로에 동일한 문서가 있어 복원할 수 없습니다.");
            }

            try {
                Files.createDirectories(target.getParent());
                ensureRealParentInsideRoot(target);
                moveWithoutReplacing(source, target);
                Files.setLastModifiedTime(target, FileTime.from(Instant.now()));
                removeEmptyTrashParents(source.getParent());
                rebuildAfterMutation();
                return detailForPath(target, Files.readString(target, StandardCharsets.UTF_8));
            } catch (IOException exception) {
                throw new DocumentReadException("휴지통 문서를 복원하는 중 오류가 발생했습니다.", exception);
            }
        }
    }

    /** 웹 편집이나 외부 파일 변경 후 다음 조회 전에 인덱스를 즉시 갱신할 때 사용한다. */
    public void refreshIndex() {
        synchronized (refreshLock) {
            index = buildIndex(index);
            lastRefreshNanos = System.nanoTime();
        }
    }

    private Path resolveDocumentPath(String category, String title) {
        Path target = documentRoot.resolve(category).resolve(title + ".md").normalize();
        ensureInsideDocumentRoot(target);
        return target;
    }

    private Path resolveCategoryPath(String category) {
        Path target = documentRoot.resolve(category).normalize();
        ensureInsideDocumentRoot(target);
        return target;
    }

    private void ensureInsideDocumentRoot(Path path) {
        if (!path.startsWith(documentRoot) || path.equals(documentRoot)) {
            throw new InvalidDocumentPathException("문서 저장 경로가 올바르지 않습니다.");
        }
    }

    private void ensureRealParentInsideRoot(Path target) throws IOException {
        Path realRoot = documentRoot.toRealPath();
        Path realParent = target.getParent().toRealPath();
        if (!realParent.startsWith(realRoot)) {
            throw new InvalidDocumentPathException("문서 저장 경로가 문서 루트 밖을 가리키고 있습니다.");
        }
    }

    private void ensureRealDirectoryInsideRoot(Path directory) throws IOException {
        if (!directory.toRealPath().startsWith(documentRoot.toRealPath())) {
            throw new InvalidDocumentPathException("폴더 경로가 문서 루트 밖을 가리키고 있습니다.");
        }
    }

    /** createDirectories가 기존 심볼릭 링크를 따라 루트 밖에 폴더를 만들기 전에 가장 가까운 상위 경로를 검사한다. */
    private void ensureCreatableDirectoryInsideRoot(Path directory) throws IOException {
        Path existing = directory;
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing == null || Files.isSymbolicLink(existing)
                || !existing.toRealPath().startsWith(documentRoot.toRealPath())) {
            throw new InvalidDocumentPathException("폴더 경로가 문서 루트 밖을 가리키고 있습니다.");
        }
    }

    private String validatePathSegment(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank() || trimmed.equals(".") || trimmed.equals("..")) {
            throw new InvalidDocumentPathException(fieldName + "을(를) 입력해 주세요.");
        }
        if (trimmed.endsWith(".") || trimmed.endsWith(" ") || INVALID_PATH_CHARACTERS.matcher(trimmed).find()) {
            throw new InvalidDocumentPathException(fieldName + "에 파일 경로로 사용할 수 없는 문자가 있습니다.");
        }
        String baseName = trimmed.contains(".") ? trimmed.substring(0, trimmed.indexOf('.')) : trimmed;
        if (WINDOWS_RESERVED_NAMES.contains(baseName.toUpperCase(Locale.ROOT))) {
            throw new InvalidDocumentPathException(fieldName + "에 운영체제 예약어를 사용할 수 없습니다.");
        }
        String normalizedSegment = trimmed.toLowerCase(Locale.ROOT);
        if (fieldName.equals("카테고리") && EXCLUDED_DIRECTORIES.contains(normalizedSegment)) {
            throw new InvalidDocumentPathException("해당 카테고리는 사용할 수 없습니다.");
        }
        return trimmed;
    }

    private String validateCategoryPath(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("/") || trimmed.endsWith("/") || trimmed.contains("//")) {
            throw new InvalidDocumentPathException("카테고리 경로가 올바르지 않습니다.");
        }
        return java.util.Arrays.stream(trimmed.split("/", -1))
                .map(segment -> validatePathSegment(segment, "카테고리"))
                .collect(Collectors.joining("/"));
    }

    private String withTitleHeading(String title, String content) {
        String normalizedContent = content == null ? "" : content.replace("\r\n", "\n");
        String[] lines = normalizedContent.split("\n", -1);
        boolean inCodeFence = false;
        for (int index = 0; index < lines.length; index++) {
            String trimmed = lines[index].trim();
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inCodeFence = !inCodeFence;
                continue;
            }
            if (!inCodeFence && trimmed.startsWith("# ")) {
                lines[index] = "# " + title;
                return ensureTrailingNewline(String.join("\n", lines));
            }
        }
        return normalizedContent.isBlank()
                ? "# " + title + "\n"
                : ensureTrailingNewline("# " + title + "\n\n" + normalizedContent);
    }

    private String withFrontMatter(String title, List<String> tags, String content) {
        String body = MarkdownFrontMatter.parse(content).body();
        return MarkdownFrontMatter.render(title, tags, withTitleHeading(title, body));
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) return List.of();
        List<String> normalized = tags.stream().map(String::trim).filter(tag -> !tag.isBlank()).distinct().toList();
        if (normalized.size() > 10 || normalized.stream().anyMatch(tag -> tag.length() > 30)) {
            throw new InvalidDocumentPathException("태그는 최대 10개, 각 30자까지 입력할 수 있습니다.");
        }
        return normalized;
    }

    private String ensureTrailingNewline(String content) {
        return content.endsWith("\n") ? content : content + "\n";
    }

    private void writeAtomically(Path target, String content) throws IOException {
        Path temporaryFile = Files.createTempFile(target.getParent(), ".cs-notes-", ".tmp");
        try {
            Files.writeString(temporaryFile, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private void moveWithoutReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target);
        }
    }

    private void rebuildAfterMutation() {
        index = buildIndex(DocumentIndex.empty());
        lastRefreshNanos = System.nanoTime();
    }

    private DocumentModels.DocumentDetailResponse detailForPath(Path path, String content) {
        String relativePath = toRelativePath(path);
        String id = encodeId(relativePath);
        DocumentMetadata metadata = index.documentsById().get(id);
        if (metadata == null) {
            throw new DocumentReadException("저장한 문서를 인덱스에서 찾을 수 없습니다.");
        }
        return metadata.toDetail(MarkdownFrontMatter.parse(content).body());
    }

    /** 요청마다 전체 파일을 읽지 않고 갱신 주기 동안 메모리 인덱스를 재사용한다. */
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

    /** 파일의 수정 시각과 크기가 같으면 기존 메타데이터를 재사용해 인덱싱 비용을 줄인다. */
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
            List<DocumentModels.CategoryResponse> categories = buildCategoryTree(documents, scanCategoryPaths());

            return new DocumentIndex(documents, documentsById, categories, true);
        } catch (IOException exception) {
            throw new DocumentReadException("문서 목록을 불러오는 중 오류가 발생했습니다.", exception);
        }
    }

    private List<DocumentModels.CategoryResponse> buildCategoryTree(
            List<DocumentMetadata> documents,
            Set<String> directoryPaths
    ) {
        Set<String> categoryPaths = new HashSet<>(directoryPaths);
        for (DocumentMetadata document : documents) {
            String[] segments = document.category().split("/");
            StringBuilder path = new StringBuilder();
            for (String segment : segments) {
                if (!path.isEmpty()) {
                    path.append('/');
                }
                path.append(segment);
                categoryPaths.add(path.toString());
            }
        }
        return buildCategoryChildren("", categoryPaths, documents);
    }

    /** 문서가 아직 없는 새 폴더도 관리 화면과 사이드바에 노출하기 위해 실제 디렉터리를 수집한다. */
    private Set<String> scanCategoryPaths() throws IOException {
        Set<String> categories = new HashSet<>();
        Files.walkFileTree(documentRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                if (directory.equals(documentRoot)) return FileVisitResult.CONTINUE;
                if (isInExcludedDirectory(directory)) return FileVisitResult.SKIP_SUBTREE;
                categories.add(toRelativePath(directory));
                return FileVisitResult.CONTINUE;
            }
        });
        return categories;
    }

    private DocumentModels.CategoryResponse findCategory(String path) {
        return findCategory(currentIndex().categories(), path)
                .orElseThrow(() -> new DocumentReadException("변경한 폴더를 인덱스에서 찾을 수 없습니다."));
    }

    private Optional<DocumentModels.CategoryResponse> findCategory(
            List<DocumentModels.CategoryResponse> categories,
            String path
    ) {
        for (DocumentModels.CategoryResponse category : categories) {
            if (category.path().equals(path)) return Optional.of(category);
            Optional<DocumentModels.CategoryResponse> child = findCategory(category.children(), path);
            if (child.isPresent()) return child;
        }
        return Optional.empty();
    }

    private List<DocumentModels.CategoryResponse> buildCategoryChildren(
            String parentPath,
            Set<String> categoryPaths,
            List<DocumentMetadata> documents
    ) {
        return categoryPaths.stream()
                .filter(path -> parentCategoryPath(path).equals(parentPath))
                .map(path -> new DocumentModels.CategoryResponse(
                        categoryName(path),
                        path,
                        documents.stream().filter(document -> isInCategory(document.category(), path)).count(),
                        buildCategoryChildren(path, categoryPaths, documents)
                ))
                .sorted(Comparator.comparing(DocumentModels.CategoryResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private boolean isInCategory(String documentCategory, String categoryPath) {
        return documentCategory.equals(categoryPath) || documentCategory.startsWith(categoryPath + "/");
    }

    private String parentCategoryPath(String categoryPath) {
        int separator = categoryPath.lastIndexOf('/');
        return separator < 0 ? "" : categoryPath.substring(0, separator);
    }

    private String categoryName(String categoryPath) {
        int separator = categoryPath.lastIndexOf('/');
        return separator < 0 ? categoryPath : categoryPath.substring(separator + 1);
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

            String source = Files.readString(path, StandardCharsets.UTF_8);
            MarkdownFrontMatter.Parsed frontMatter = MarkdownFrontMatter.parse(source);
            String fallbackTitle = path.getFileName().toString().replaceFirst("(?i)\\.md$", "");
            String title = Optional.ofNullable(frontMatter.title()).filter(value -> !value.isBlank())
                    .or(() -> readTitle(frontMatter.body())).orElse(fallbackTitle);
            String category = relativePath.substring(0, relativePath.lastIndexOf('/'));
            String id = encodeId(relativePath);

            return new DocumentMetadata(
                    id, title, category, relativePath, normalize(title), normalize(relativePath),
                    frontMatter.tags(), frontMatter.tags().stream().map(this::normalize).toList(),
                    frontMatter.body(), normalize(frontMatter.body()), updatedAt, size
            );
        } catch (IOException exception) {
            throw new DocumentReadException("문서 메타데이터를 읽는 중 오류가 발생했습니다: " + path, exception);
        }
    }

    private Optional<String> readTitle(Path path) throws IOException {
        MarkdownFrontMatter.Parsed parsed = MarkdownFrontMatter.parse(Files.readString(path, StandardCharsets.UTF_8));
        return Optional.ofNullable(parsed.title()).filter(value -> !value.isBlank()).or(() -> readTitle(parsed.body()));
    }

    private Optional<String> readTitle(String content) {
        return content.lines().map(String::trim)
                .filter(line -> line.startsWith("# ") && !line.substring(2).isBlank())
                .map(line -> line.substring(2).trim()).findFirst();
    }

    private int searchScore(DocumentMetadata document, String query) {
        int score = 0;
        if (document.normalizedTitle().equals(query)) score += 120;
        else if (document.normalizedTitle().contains(query)) score += 80;
        if (document.normalizedTags().contains(query)) score += 100;
        else if (document.normalizedTags().stream().anyMatch(tag -> tag.contains(query))) score += 60;
        if (document.normalizedPath().contains(query)) score += 35;
        return score + Math.min(countOccurrences(document.normalizedContent(), query), 5) * 10;
    }

    private int countOccurrences(String content, String query) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = content.indexOf(query, fromIndex)) >= 0) {
            count++;
            fromIndex += query.length();
        }
        return count;
    }

    private String buildExcerpt(DocumentMetadata document, String query) {
        int matchIndex = document.normalizedContent().indexOf(query);
        if (matchIndex < 0) return null;
        int start = Math.max(0, matchIndex - 70);
        int end = Math.min(document.content().length(), matchIndex + query.length() + 110);
        String excerpt = document.content().substring(start, end)
                .replaceAll("[#>*_`~\\[\\]()]", " ")
                .replaceAll("\\s+", " ").trim();
        return (start > 0 ? "…" : "") + excerpt + (end < document.content().length() ? "…" : "");
    }

    private String toRelativePath(Path path) {
        return documentRoot.relativize(path).toString().replace('\\', '/');
    }

    private String encodeId(String relativePath) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(relativePath.getBytes(StandardCharsets.UTF_8));
    }

    private Optional<String> decodeId(String id) {
        try {
            return Optional.of(new String(Base64.getUrlDecoder().decode(id), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Path trashRoot() {
        return documentRoot.resolve(TRASH_DIRECTORY).normalize();
    }

    private DocumentModels.TrashDocumentResponse toTrashResponse(Path file) {
        try {
            String originalPath = trashRoot().relativize(file).toString().replace('\\', '/');
            String fallbackTitle = file.getFileName().toString().replaceFirst("(?i)\\.md$", "");
            String title = readTitle(file).orElse(fallbackTitle);
            return new DocumentModels.TrashDocumentResponse(
                    encodeId(originalPath), title, originalPath, Files.getLastModifiedTime(file).toInstant()
            );
        } catch (IOException exception) {
            throw new DocumentReadException("휴지통 문서 정보를 읽는 중 오류가 발생했습니다.", exception);
        }
    }

    private void removeEmptyTrashParents(Path directory) throws IOException {
        Path trashRoot = trashRoot();
        Path current = directory;
        while (current != null && !current.equals(trashRoot) && current.startsWith(trashRoot)) {
            try (var entries = Files.list(current)) {
                if (entries.findAny().isPresent()) {
                    return;
                }
            }
            Files.deleteIfExists(current);
            current = current.getParent();
        }
    }

    private boolean isInExcludedDirectory(Path path) {
        Path relativePath = documentRoot.relativize(path);
        return relativePath.getNameCount() >= 1
                && EXCLUDED_DIRECTORIES.contains(relativePath.getName(0).toString().toLowerCase(Locale.ROOT));
    }

    private boolean isReadableMarkdown(Path path) {
        return Files.isRegularFile(path)
                && !Files.isSymbolicLink(path)
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
            List<String> tags,
            List<String> normalizedTags,
            String content,
            String normalizedContent,
            Instant updatedAt,
            long size
    ) {
        private DocumentModels.DocumentSummaryResponse toSummary(String excerpt) {
            return new DocumentModels.DocumentSummaryResponse(id, title, category, relativePath, updatedAt, tags, excerpt);
        }

        private DocumentModels.DocumentDetailResponse toDetail(String content) {
            return new DocumentModels.DocumentDetailResponse(id, title, category, relativePath, content, updatedAt, tags);
        }
    }

    private record SearchResult(DocumentMetadata document, int score) {
    }
}
