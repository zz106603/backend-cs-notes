package com.csnotes.document;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DocumentService documentService;

    @Test
    void createsDocumentWithCreatedStatus() throws Exception {
        var response = new DocumentModels.DocumentDetailResponse(
                "document-id", "인덱스", "데이터베이스", "데이터베이스/인덱스.md",
                "# 인덱스\n", Instant.parse("2026-08-19T00:00:00Z")
        );
        when(documentService.createDocument(any())).thenReturn(response);

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"인덱스","category":"데이터베이스","content":"# 인덱스"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("document-id"))
                .andExpect(jsonPath("$.path").value("데이터베이스/인덱스.md"));
    }

    @Test
    void rejectsInvalidCreateRequest() throws Exception {
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","category":"","content":"본문"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("입력한 문서 정보를 확인해 주세요."));
    }

    @Test
    void returnsConflictWhenTrashTargetAlreadyExists() throws Exception {
        doThrow(new DocumentConflictException("휴지통에 동일한 문서가 있습니다."))
                .when(documentService).moveDocumentToTrash("document-id");

        mockMvc.perform(delete("/api/documents/document-id"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("휴지통에 동일한 문서가 있습니다."));
    }

    @Test
    void listsAndPermanentlyDeletesTrashDocument() throws Exception {
        when(documentService.findTrashDocuments()).thenReturn(List.of(
                new DocumentModels.TrashDocumentResponse(
                        "trash-id", "TCP", "네트워크/TCP.md", Instant.parse("2026-08-19T01:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/trash"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].originalPath").value("네트워크/TCP.md"));

        mockMvc.perform(delete("/api/trash/trash-id"))
                .andExpect(status().isNoContent());
        verify(documentService).permanentlyDeleteTrashDocument("trash-id");
    }

    @Test
    void restoresTrashDocument() throws Exception {
        var response = new DocumentModels.DocumentDetailResponse(
                "restored-id", "TCP", "네트워크", "네트워크/TCP.md",
                "# TCP\n", Instant.parse("2026-08-19T02:00:00Z")
        );
        when(documentService.restoreTrashDocument("trash-id")).thenReturn(response);

        mockMvc.perform(post("/api/trash/trash-id/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("restored-id"))
                .andExpect(jsonPath("$.path").value("네트워크/TCP.md"));
    }
}
