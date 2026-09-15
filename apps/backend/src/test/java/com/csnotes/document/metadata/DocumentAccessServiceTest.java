package com.csnotes.document.metadata;

import com.csnotes.document.DocumentModels;
import com.csnotes.document.DocumentNotFoundException;
import com.csnotes.document.DocumentService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentAccessServiceTest {
    private final DocumentService documentService = mock(DocumentService.class);
    private final DocumentMetadataRepository repository = mock(DocumentMetadataRepository.class);
    private final DocumentAccessService service = new DocumentAccessService(documentService, repository);
    private final UUID userId = UUID.randomUUID();

    @Test
    void 목록에는_소유하거나_공개된_문서만_반환한다() {
        var mine = summary("mine", "백엔드", "백엔드/내 문서.md");
        var publicDocument = summary("public", "백엔드/Spring", "백엔드/Spring/공개.md");
        var privateDocument = summary("private", "데이터베이스", "데이터베이스/비공개.md");
        when(repository.findReadableSourceDocumentIds(userId)).thenReturn(Set.of("mine", "public"));
        when(documentService.findDocuments(null, null)).thenReturn(List.of(mine, publicDocument, privateDocument));

        List<DocumentModels.DocumentSummaryResponse> result = service.findReadableDocuments(userId, null, null);

        assertThat(result).extracting(DocumentModels.DocumentSummaryResponse::id)
                .containsExactly("mine", "public");
    }

    @Test
    void 폴더_문서_개수는_읽을_수_있는_문서만으로_계산한다() {
        var mine = summary("mine", "백엔드/Spring", "백엔드/Spring/내 문서.md");
        var hidden = summary("hidden", "백엔드", "백엔드/숨김.md");
        var spring = new DocumentModels.CategoryResponse("Spring", "백엔드/Spring", 1, List.of());
        var backend = new DocumentModels.CategoryResponse("백엔드", "백엔드", 2, List.of(spring));
        when(repository.findReadableSourceDocumentIds(userId)).thenReturn(Set.of("mine"));
        when(documentService.findDocuments(null, null)).thenReturn(List.of(mine, hidden));
        when(documentService.findCategories()).thenReturn(List.of(backend));

        List<DocumentModels.CategoryResponse> result = service.findReadableCategories(userId);

        assertThat(result.getFirst().documentCount()).isEqualTo(1);
        assertThat(result.getFirst().children().getFirst().documentCount()).isEqualTo(1);
    }

    @Test
    void 다른_사용자의_비공개_문서는_존재를_숨기기_위해_찾을_수_없음으로_처리한다() {
        when(repository.canRead(userId, "private")).thenReturn(false);

        assertThatThrownBy(() -> service.requireReadable(userId, "private"))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessage("문서를 찾을 수 없습니다.");
    }

    @Test
    void 공개_문서라도_소유자가_아니면_수정할_수_없다() {
        when(repository.isOwner(userId, "public", false)).thenReturn(false);

        assertThatThrownBy(() -> service.requireOwner(userId, "public"))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void 다른_사용자의_문서가_포함된_폴더는_이동할_수_없다() {
        when(documentService.findDocuments("백엔드", null)).thenReturn(List.of(
                summary("mine", "백엔드", "백엔드/내 문서.md"),
                summary("other", "백엔드", "백엔드/다른 문서.md")
        ));
        when(repository.findOwnedSourceDocumentIds(userId, false)).thenReturn(Set.of("mine"));

        assertThatThrownBy(() -> service.requireCategoryOwner(userId, "백엔드"))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    private DocumentModels.DocumentSummaryResponse summary(String id, String category, String path) {
        return new DocumentModels.DocumentSummaryResponse(id, id, category, path, Instant.EPOCH);
    }
}
