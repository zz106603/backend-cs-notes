package com.csnotes.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

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
}
