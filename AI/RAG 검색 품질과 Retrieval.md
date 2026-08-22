# RAG 검색 품질과 Retrieval

기본 RAG는 사용자의 질문을 Embedding으로 변환하고, Vector DB에서 관련 문서를 찾아 LLM에 전달하는 구조임

하지만 실제 RAG에서는 단순한 Vector Search만으로는 원하는 문서를 항상 정확하게 찾기 어려움. 따라서 **문서를 어떻게 나누고, 어떤 방식으로 검색하며, 검색된 결과를 어떻게 정제할 것인지**가 중요함

---

## 1. Dense Retrieval과 Sparse Retrieval

RAG의 검색 방식은 크게 **Dense Retrieval**과 **Sparse Retrieval**로 나눌 수 있음

### Dense Retrieval

Dense Retrieval은 텍스트를 Embedding Vector로 변환하여 **의미가 비슷한 문서를 검색하는 방식**

```text
질문
"DB 작업을 모두 성공하거나 모두 실패하게 하는 방법"

        ↓ Embedding

Query Vector

        ↓ Vector Search

"Transaction은 작업의 원자성을 보장한다."
```

질문과 문서에 같은 단어가 없어도 의미가 비슷하면 검색할 수 있다는 장점이 있음

반면 에러 코드, 클래스명, 제품명처럼 **정확한 문자열 자체가 중요한 검색에는 약할 수 있음**

### Sparse Retrieval

Sparse Retrieval은 문서에 실제로 등장하는 **키워드를 기준으로 검색하는 방식**이며 대표적으로 `BM25`가 사용됨

예를 들어 다음 검색에서는 Sparse 방식이 유리할 수 있음

```text
"Spring Security CSRF 403"
"ERR_CONNECTION_RESET"
"OpenFGA authorization model"
```

Dense와 Sparse의 차이

| 구분        | Dense     | Sparse   |
| --------- | --------- | -------- |
| 검색 기준     | 의미        | 키워드      |
| 대표 기술     | Embedding | BM25     |
| 자연어·유사 표현 | 강함        | 상대적으로 약함 |
| 정확한 문자열   | 약할 수 있음   | 강함       |

```text
Dense  → 의미가 비슷한가?
Sparse → 같은 키워드가 포함되어 있는가?
```

---

## 2. Hybrid Search

Dense와 Sparse는 각각 장단점이 있기 때문에 두 검색 방식을 함께 사용하는 것을 **Hybrid Search**라고 함

```text
                 Query
                   │
         ┌─────────┴─────────┐
         ↓                   ↓
   Dense Search        Sparse Search
   (Embedding)            (BM25)
         ↓                   ↓
         └─────────┬─────────┘
                   ↓
             결과 결합
                   ↓
             최종 검색 결과
```

예를 들어

```text
"Spring Security CSRF 403 오류"
```

라는 질문이 들어오면,

Dense Search는 `CSRF 인증 실패`, `Spring Security 보안 처리`처럼 의미적으로 관련된 문서를 찾고, Sparse Search는 `Spring Security`, `CSRF`, `403`이라는 정확한 키워드가 포함된 문서를 찾을 수 있음

따라서 Hybrid Search는 **의미 검색과 키워드 검색의 단점을 서로 보완하는 방식**

---

## 3. Reranker

Retriever가 찾은 문서가 모두 동일하게 중요한 것은 아님

따라서 검색된 후보들의 관련도를 다시 평가하여 순서를 조정하는 **Reranker**를 사용할 수 있음

```text
Query
 ↓
Retriever
 ↓
후보 문서 20개
 ↓
Reranker
 ↓
관련도 재평가
 ↓
상위 5개
 ↓
LLM
```

Retriever의 목적이 **많은 데이터에서 관련 있을 가능성이 높은 문서를 빠르게 찾는 것**이라면,

Reranker의 목적은 **검색된 후보 중 실제 질문과 가장 관련 있는 문서를 정확하게 고르는 것**

```text
Retriever → 넓게 찾는다.
Reranker  → 정확하게 고른다.
```

RAG에서는 처음부터 모든 문서를 정밀하게 비교하면 비용이 너무 크기 때문에, Retriever로 후보를 줄인 뒤 Reranker를 적용하는 구조가 일반적

---

## 4. Chunking과 Metadata

### Chunking

RAG에서는 일반적으로 문서 전체를 하나의 Vector로 저장하지 않고 **작은 문서 조각인 Chunk로 나누어 저장**

```text
Document
 ↓
Chunk 1
Chunk 2
Chunk 3
 ↓
각 Chunk Embedding
```

Chunk가 너무 크면 여러 주제가 하나의 Vector에 섞여 검색 정확도가 떨어질 수 있고, 너무 작으면 문맥이 끊길 수 있음

따라서 문서 특성에 따라 적절한 크기를 선택해야 하며, 문맥 단절을 줄이기 위해 Chunk 사이 일부 내용을 겹치게 하는 **Chunk Overlap**을 사용할 수도 있음

### Metadata

Chunk에는 본문과 Embedding뿐 아니라 다음과 같은 부가 정보를 함께 저장할 수 있음

```text
content
embedding
documentId
title
category
source
permission
```

이를 **Metadata**라고 함

Metadata를 이용하면 검색 전에 범위를 제한할 수 있음

예를 들어 Database 문서만 검색한다면 다음과 같은 조건을 적용할 수 있음

```sql
WHERE category = 'database'
```

기업 내부 RAG에서는 권한이 없는 문서가 검색되지 않도록 `permission`, `tenantId` 등의 Metadata를 이용하기도 함

---

## 5. Vector Index

Vector가 적을 때는 모든 Vector와 Query Vector를 비교할 수 있지만, 데이터가 많아질수록 전체 검색 비용이 커짐

따라서 대규모 Vector Search에서는 **ANN(Approximate Nearest Neighbor)** 방식의 Index를 사용함

pgvector에서는 대표적으로 다음 두 방식

### HNSW

Vector 간의 관계를 Graph 구조로 만들어 가까운 Vector를 빠르게 탐색

```sql
CREATE INDEX document_embedding_idx
ON document_chunks
USING hnsw (embedding vector_cosine_ops);
```

검색 속도와 검색 품질이 좋은 편이지만 Index 생성과 저장에 더 많은 자원이 필요할 수 있음

### IVFFlat

Vector들을 여러 그룹으로 나누고 Query와 가까운 그룹을 중심으로 검색하는 방식

백엔드 개발자 입장에서는 내부 알고리즘보다 다음 정도를 이해하는 것이 중요함

> **Vector 데이터가 많아지면 모든 Vector를 비교하는 것은 비효율적이므로 ANN Index를 사용하며, Index 설정에 따라 검색 속도와 검색 정확도 사이에 Trade-off가 발생한다.**

---

## 6. Retrieval 성능 평가

RAG의 답변이 잘못되었다고 해서 항상 LLM의 문제는 아님

필요한 문서를 Retrieval 단계에서 찾지 못했다면 LLM도 정확한 답변을 만들기 어려움

따라서 검색 품질 자체를 평가해야 함

### Recall@K

필요한 문서가 상위 K개의 검색 결과 안에 포함되어 있는지를 평가함

```text
Recall@5
→ 필요한 문서가 Top 5 안에 들어왔는가?
```

### Precision@K

상위 K개의 검색 결과 중 실제 질문과 관련 있는 문서가 얼마나 포함되어 있는지를 평가함


```text
Recall
→ 필요한 문서를 놓치지 않았는가?

Precision
→ 가져온 문서가 실제로 관련 있는가?
```

일반적으로 Retriever 단계에서는 필요한 문서를 놓치지 않도록 **Recall을 확보하고**, 이후 Reranker를 통해 **Precision을 높이는 방향**으로 검색 구조를 설계할 수 있음

---

## 7. 전체 흐름 정리

```text
Question
 ↓
Embedding
 ↓
Vector Search
 ↓
LLM
 ↓
Answer
```

검색 품질까지 고려하면 다음과 같이 확장

```text
                    [Indexing]

Document
 ↓
Chunking
 ↓
Embedding
 ↓
Vector DB / Search Index


                    [Retrieval]

User Query
 ↓
       ┌─────────────────┐
       ↓                 ↓
Dense Retrieval    Sparse Retrieval
       ↓                 ↓
       └────────┬────────┘
                ↓
          Hybrid Search
                ↓
             Reranker
                ↓
              Top-K
                ↓
            LLM Context
                ↓
               LLM
                ↓
              Answer
```



> **RAG의 품질은 단순히 좋은 LLM을 사용하는 것으로 결정되지 않는다. 문서를 적절하게 Chunking하고, Dense와 Sparse 검색을 조합하고, Reranker를 통해 필요한 문서를 선별하여 LLM에게 정확한 Context를 제공하는 전체 Retrieval 과정이 중요하다.**

---

## 핵심 용어 요약

| 용어                 | 의미                           |
| ------------------ | ---------------------------- |
| Dense Retrieval    | Embedding 기반 의미 검색           |
| Sparse Retrieval   | BM25 기반 키워드 검색               |
| Hybrid Search      | Dense + Sparse 검색            |
| Reranker           | 검색된 후보의 관련도를 다시 평가           |
| Chunking           | 문서를 검색 가능한 작은 단위로 분리         |
| Metadata           | Chunk에 함께 저장하는 부가 정보         |
| Metadata Filtering | Metadata 조건으로 검색 범위를 제한      |
| Top-K              | 관련도가 높은 상위 K개의 검색 결과         |
| ANN                | 가까운 Vector를 빠르게 찾는 근사 검색     |
| HNSW               | Graph 기반 ANN Index           |
| IVFFlat            | Vector를 그룹화하여 검색하는 ANN Index |
| Recall             | 필요한 문서를 얼마나 놓치지 않고 검색했는지     |
| Precision          | 검색한 문서 중 실제 관련 문서의 비율        |
