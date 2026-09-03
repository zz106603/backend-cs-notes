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
    void 문서_개수와_함께_카테고리를_반환한다() {
        assertThat(documentService.findCategories())
                .extracting(DocumentModels.CategoryResponse::name)
                .containsExactly("네트워크", "데이터베이스");
    }

    @Test
    void 빈_폴더를_생성하고_카테고리_트리에_노출한다() {
        var created = documentService.createCategory(
                new DocumentModels.CreateCategoryRequest("백엔드/Spring"));

        assertThat(created.path()).isEqualTo("백엔드/Spring");
        assertThat(created.documentCount()).isZero();
        assertThat(documentRoot.resolve("백엔드/Spring")).isDirectory();
        assertThat(documentRoot.resolve("백엔드/Spring/.gitkeep")).exists();
        assertThat(documentService.findCategories())
                .extracting(DocumentModels.CategoryResponse::path)
                .contains("백엔드");
    }

    @Test
    void 폴더를_수정하면_하위_문서와_폴더를_함께_이동한다() throws IOException {
        Files.createDirectories(documentRoot.resolve("백엔드/Spring/JPA"));
        Files.writeString(documentRoot.resolve("백엔드/Spring/JPA/지연 로딩.md"),
                "# 지연 로딩", StandardCharsets.UTF_8);
        documentService.refreshIndex();

        var updated = documentService.updateCategory(
                new DocumentModels.UpdateCategoryRequest("백엔드/Spring", "프레임워크/Spring"));

        assertThat(updated.path()).isEqualTo("프레임워크/Spring");
        assertThat(updated.documentCount()).isEqualTo(1);
        assertThat(documentRoot.resolve("백엔드/Spring")).doesNotExist();
        assertThat(documentRoot.resolve("프레임워크/Spring/JPA/지연 로딩.md")).exists();
        assertThat(documentService.findDocuments("프레임워크/Spring", null))
                .extracting(DocumentModels.DocumentSummaryResponse::title)
                .containsExactly("지연 로딩");
    }

    @Test
    void 폴더를_자기_하위로_이동하거나_기존_폴더와_겹치게_수정하지_못한다() throws IOException {
        Files.createDirectories(documentRoot.resolve("백엔드/Spring"));
        documentService.refreshIndex();

        assertThatThrownBy(() -> documentService.updateCategory(
                new DocumentModels.UpdateCategoryRequest("백엔드", "백엔드/Spring/하위")))
                .isInstanceOf(InvalidDocumentPathException.class);
        assertThatThrownBy(() -> documentService.updateCategory(
                new DocumentModels.UpdateCategoryRequest("백엔드", "데이터베이스")))
                .isInstanceOf(DocumentConflictException.class);
    }

    @Test
    void 카테고리와_검색어로_문서를_필터링한다() {
        var documents = documentService.findDocuments("데이터베이스", "격리");

        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().title()).isEqualTo("트랜잭션 격리 수준");
    }

    @Test
    void 불투명_아이디로_문서를_조회한다() {
        var summary = documentService.findDocuments("네트워크", null).getFirst();
        var document = documentService.findDocument(summary.id());

        assertThat(document).isPresent();
        assertThat(document.orElseThrow().title()).isEqualTo("TCP");
        assertThat(document.orElseThrow().content()).contains("제목 없는 문서");
    }

    @Test
    void 올바르지_않은_문서_아이디를_거부한다() {
        assertThat(documentService.findDocument("not-valid-base64!"))
                .isEmpty();
    }

    @Test
    void 명시적으로_갱신할_때까지_캐시된_인덱스를_유지한다() throws IOException {
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
    void 문서_아이디를_유지하면서_변경된_메타데이터를_갱신한다() throws IOException {
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
    void 문서를_생성하고_제목_헤딩을_추가한다() {
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
    void 프론트_매터의_제목과_태그를_본문에_노출하지_않고_읽는다() throws IOException {
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
    void 본문을_검색하고_일치하는_문맥을_반환한다() {
        var documents = documentService.findDocuments(null, "본문");

        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().title()).isEqualTo("트랜잭션 격리 수준");
        assertThat(documents.getFirst().excerpt()).contains("본문");
    }

    @Test
    void 태그를_검색하고_정확한_태그_일치를_본문_일치보다_우선한다() throws IOException {
        Files.writeString(documentRoot.resolve("네트워크/태그.md"), """
                ---
                title: "네트워크 기초"
                tags:
                  - "격리"
                ---
                # 네트워크 기초

                연결에 대한 설명입니다.
                """, StandardCharsets.UTF_8);
        documentService.refreshIndex();

        var documents = documentService.findDocuments(null, "격리");

        assertThat(documents)
                .extracting(DocumentModels.DocumentSummaryResponse::title)
                .containsExactly("네트워크 기초", "트랜잭션 격리 수준");
        assertThat(documents.getFirst().excerpt()).isNull();
    }

    @Test
    void 중복되거나_안전하지_않은_문서_경로를_거부한다() {
        assertThatThrownBy(() -> documentService.createDocument(new DocumentModels.CreateDocumentRequest(
                "트랜잭션", "데이터베이스", "본문"
        ))).isInstanceOf(DocumentConflictException.class);

        assertThatThrownBy(() -> documentService.createDocument(new DocumentModels.CreateDocumentRequest(
                "탈출", "..", "본문"
        ))).isInstanceOf(InvalidDocumentPathException.class);
    }

    @Test
    void 문서를_수정하고_이동한_결과를_반환한다() {
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
    void 본문을_다시_저장하지_않고_문서를_다른_폴더로_이동한다() {
        var before = documentService.findDocuments("데이터베이스", null).getFirst();
        var moved = documentService.moveDocument(before.id(), new DocumentModels.MoveDocumentRequest(
                "백엔드/데이터", before.updatedAt()));

        assertThat(moved.id()).isNotEqualTo(before.id());
        assertThat(moved.path()).isEqualTo("백엔드/데이터/트랜잭션.md");
        assertThat(moved.content()).contains("본문");
        assertThat(documentRoot.resolve("데이터베이스/트랜잭션.md")).doesNotExist();
        assertThat(documentRoot.resolve("백엔드/데이터/트랜잭션.md")).exists();
    }

    @Test
    void 예상_버전이_오래되었으면_수정을_거부한다() {
        var before = documentService.findDocuments("데이터베이스", null).getFirst();

        assertThatThrownBy(() -> documentService.updateDocument(before.id(), new DocumentModels.UpdateDocumentRequest(
                before.title(), before.category(), "변경", java.time.Instant.EPOCH
        )))
                .isInstanceOf(DocumentConflictException.class)
                .hasMessageContaining("최신 내용");
    }

    @Test
    void 문서를_별도_휴지통으로_이동하고_목록에서_숨긴다() {
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
    void 휴지통에_있는_문서만_영구_삭제한다() {
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
    void 휴지통_문서를_원래_경로로_복원한다() {
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
    void 원래_경로에_문서가_있으면_복원을_거부한다() throws IOException {
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
    void 중첩_카테고리_트리를_만들고_하위_문서를_필터링한다() throws IOException {
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
