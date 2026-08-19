package com.csnotes.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentServiceTest {

    @TempDir
    Path documentRoot;

    private DocumentService documentService;

    @BeforeEach
    void setUp() throws IOException {
        Files.createDirectories(documentRoot.resolve("데이터베이스"));
        Files.createDirectories(documentRoot.resolve("네트워크"));
        Files.createDirectories(documentRoot.resolve("apps/ignored"));
        Files.writeString(documentRoot.resolve("데이터베이스/트랜잭션.md"), "# 트랜잭션 격리 수준\n\n본문", StandardCharsets.UTF_8);
        Files.writeString(documentRoot.resolve("네트워크/TCP.md"), "제목 없는 문서", StandardCharsets.UTF_8);
        Files.writeString(documentRoot.resolve("apps/ignored/숨김.md"), "# 숨김", StandardCharsets.UTF_8);
        Files.writeString(documentRoot.resolve("README.md"), "# README", StandardCharsets.UTF_8);
        documentService = new DocumentService(documentRoot, 60_000);
    }

    @Test
    void returnsCategoriesWithDocumentCounts() {
        assertThat(documentService.findCategories())
                .extracting(DocumentModels.CategoryResponse::name)
                .containsExactly("네트워크", "데이터베이스");
    }

    @Test
    void filtersDocumentsByCategoryAndQuery() {
        var documents = documentService.findDocuments("데이터베이스", "격리");

        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().title()).isEqualTo("트랜잭션 격리 수준");
    }

    @Test
    void readsDocumentByOpaqueId() {
        var summary = documentService.findDocuments("네트워크", null).getFirst();
        var document = documentService.findDocument(summary.id());

        assertThat(document).isPresent();
        assertThat(document.orElseThrow().title()).isEqualTo("TCP");
        assertThat(document.orElseThrow().content()).contains("제목 없는 문서");
    }

    @Test
    void rejectsInvalidDocumentId() {
        assertThat(documentService.findDocument("not-valid-base64!"))
                .isEmpty();
    }

    @Test
    void keepsCachedIndexUntilExplicitRefresh() throws IOException {
        assertThat(documentService.findDocuments(null, null)).hasSize(2);

        Files.createDirectories(documentRoot.resolve("운영체제"));
        Files.writeString(documentRoot.resolve("운영체제/프로세스.md"), "# 프로세스", StandardCharsets.UTF_8);

        assertThat(documentService.findDocuments(null, null)).hasSize(2);

        documentService.refreshIndex();

        assertThat(documentService.findDocuments(null, null))
                .extracting(DocumentModels.DocumentSummaryResponse::title)
                .containsExactly("TCP", "트랜잭션 격리 수준", "프로세스");
    }

    @Test
    void refreshesChangedMetadataWithoutChangingDocumentId() throws IOException {
        var before = documentService.findDocuments("데이터베이스", null).getFirst();
        Path target = documentRoot.resolve("데이터베이스/트랜잭션.md");
        Files.writeString(target, "# ACID 트랜잭션\n\n변경된 본문입니다.", StandardCharsets.UTF_8);

        documentService.refreshIndex();
        var after = documentService.findDocuments("데이터베이스", null).getFirst();
        var detail = documentService.findDocument(after.id()).orElseThrow();

        assertThat(after.id()).isEqualTo(before.id());
        assertThat(after.title()).isEqualTo("ACID 트랜잭션");
        assertThat(detail.content()).contains("변경된 본문");
    }

    @Test
    void createsDocumentAndAddsTitleHeading() {
        var created = documentService.createDocument(new DocumentModels.CreateDocumentRequest(
                "인덱스", "데이터베이스", "B-Tree를 정리합니다.", java.util.List.of("DB", "B-Tree")
        ));

        assertThat(created.title()).isEqualTo("인덱스");
        assertThat(created.path()).isEqualTo("데이터베이스/인덱스.md");
        assertThat(created.content()).startsWith("# 인덱스\n\n");
        assertThat(created.tags()).containsExactly("DB", "B-Tree");
        assertThat(documentRoot.resolve("데이터베이스/인덱스.md"))
                .content().startsWith("---\ntitle: \"인덱스\"\ntags:\n  - \"DB\"\n  - \"B-Tree\"\n---\n# 인덱스");
    }

    @Test
    void readsFrontMatterTitleAndTagsWithoutExposingItAsContent() throws IOException {
        Files.writeString(documentRoot.resolve("네트워크/메타데이터.md"), """
                ---
                title: "HTTP 메타데이터"
                tags:
                  - "HTTP"
                  - "네트워크"
                ---
                # 이전 제목

                본문입니다.
                """, StandardCharsets.UTF_8);
        documentService.refreshIndex();

        var summary = documentService.findDocuments("네트워크", "HTTP").getFirst();
        var detail = documentService.findDocument(summary.id()).orElseThrow();

        assertThat(summary.title()).isEqualTo("HTTP 메타데이터");
        assertThat(summary.tags()).containsExactly("HTTP", "네트워크");
        assertThat(detail.content()).startsWith("# 이전 제목").doesNotContain("title:");
    }

    @Test
    void rejectsDuplicateAndUnsafeDocumentPaths() {
        assertThatThrownBy(() -> documentService.createDocument(new DocumentModels.CreateDocumentRequest(
                "트랜잭션", "데이터베이스", "본문"
        ))).isInstanceOf(DocumentConflictException.class);

        assertThatThrownBy(() -> documentService.createDocument(new DocumentModels.CreateDocumentRequest(
                "탈출", "..", "본문"
        ))).isInstanceOf(InvalidDocumentPathException.class);
    }

    @Test
    void updatesAndMovesDocumentWithNewStableResponse() {
        var before = documentService.findDocuments("데이터베이스", null).getFirst();
        var updated = documentService.updateDocument(before.id(), new DocumentModels.UpdateDocumentRequest(
                "ACID", "백엔드", "# 이전 제목\n\n새 본문", before.updatedAt()
        ));

        assertThat(updated.id()).isNotEqualTo(before.id());
        assertThat(updated.path()).isEqualTo("백엔드/ACID.md");
        assertThat(updated.content()).startsWith("# ACID\n");
        assertThat(documentRoot.resolve("데이터베이스/트랜잭션.md")).doesNotExist();
        assertThat(documentRoot.resolve("백엔드/ACID.md")).exists();
    }

    @Test
    void rejectsUpdateWhenExpectedVersionIsStale() {
        var before = documentService.findDocuments("데이터베이스", null).getFirst();

        assertThatThrownBy(() -> documentService.updateDocument(before.id(), new DocumentModels.UpdateDocumentRequest(
                before.title(), before.category(), "변경", java.time.Instant.EPOCH
        )))
                .isInstanceOf(DocumentConflictException.class)
                .hasMessageContaining("최신 내용");
    }

    @Test
    void movesDocumentToSeparateTrashAndHidesItFromDocuments() {
        var before = documentService.findDocuments("네트워크", null).getFirst();

        documentService.moveDocumentToTrash(before.id());

        assertThat(documentRoot.resolve("네트워크/TCP.md")).doesNotExist();
        assertThat(documentRoot.resolve(".trash/네트워크/TCP.md")).exists();
        assertThat(documentService.findDocuments(null, null))
                .extracting(DocumentModels.DocumentSummaryResponse::title)
                .doesNotContain("TCP");
        assertThat(documentService.findTrashDocuments())
                .extracting(DocumentModels.TrashDocumentResponse::originalPath)
                .containsExactly("네트워크/TCP.md");
    }

    @Test
    void permanentlyDeletesDocumentOnlyFromTrash() {
        var before = documentService.findDocuments("네트워크", null).getFirst();
        documentService.moveDocumentToTrash(before.id());
        var trashed = documentService.findTrashDocuments().getFirst();

        documentService.permanentlyDeleteTrashDocument(trashed.id());

        assertThat(documentRoot.resolve(".trash/네트워크/TCP.md")).doesNotExist();
        assertThat(documentService.findTrashDocuments()).isEmpty();
        assertThatThrownBy(() -> documentService.permanentlyDeleteTrashDocument(before.id()))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void restoresTrashDocumentToOriginalPath() {
        var before = documentService.findDocuments("네트워크", null).getFirst();
        documentService.moveDocumentToTrash(before.id());
        var trashed = documentService.findTrashDocuments().getFirst();

        var restored = documentService.restoreTrashDocument(trashed.id());

        assertThat(restored.path()).isEqualTo("네트워크/TCP.md");
        assertThat(documentRoot.resolve("네트워크/TCP.md")).exists();
        assertThat(documentRoot.resolve(".trash/네트워크/TCP.md")).doesNotExist();
        assertThat(documentService.findTrashDocuments()).isEmpty();
    }

    @Test
    void rejectsRestoreWhenOriginalPathAlreadyExists() throws IOException {
        var before = documentService.findDocuments("네트워크", null).getFirst();
        documentService.moveDocumentToTrash(before.id());
        var trashed = documentService.findTrashDocuments().getFirst();
        Files.createDirectories(documentRoot.resolve("네트워크"));
        Files.writeString(documentRoot.resolve("네트워크/TCP.md"), "# 새 TCP", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> documentService.restoreTrashDocument(trashed.id()))
                .isInstanceOf(DocumentConflictException.class)
                .hasMessageContaining("원래 경로");
    }

    @Test
    void buildsNestedCategoryTreeAndFiltersDescendants() throws IOException {
        Files.createDirectories(documentRoot.resolve("백엔드/Spring"));
        Files.createDirectories(documentRoot.resolve("백엔드/보안"));
        Files.writeString(documentRoot.resolve("백엔드/Spring/DI.md"), "# 의존성 주입", StandardCharsets.UTF_8);
        Files.writeString(documentRoot.resolve("백엔드/보안/JWT.md"), "# JWT", StandardCharsets.UTF_8);
        documentService.refreshIndex();

        var backend = documentService.findCategories().stream()
                .filter(category -> category.path().equals("백엔드"))
                .findFirst()
                .orElseThrow();

        assertThat(backend.documentCount()).isEqualTo(2);
        assertThat(backend.children())
                .extracting(DocumentModels.CategoryResponse::path)
                .containsExactly("백엔드/Spring", "백엔드/보안");
        assertThat(documentService.findDocuments("백엔드/Spring", null))
                .extracting(DocumentModels.DocumentSummaryResponse::title)
                .containsExactly("의존성 주입");
        assertThat(documentService.findDocuments("백엔드", null)).hasSize(2);
    }
}
