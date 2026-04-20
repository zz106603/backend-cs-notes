# RAG (Retrieval-Augmented Generation, 검색 증강 생성)

**정의**
- **"검색(Retrieval)을 통해 얻은 정보를 바탕으로(Augmented) 답변을 생성(Generation)하는 기술"**
- LLM(거대언어모델)이 학습하지 않은 **최신 정보**나 **기업 내부의 비공개 데이터**를 활용하여 정확한 답변을 생성하도록 돕는 프레임워크입니다.

---

## 1. 왜 필요한가요? (LLM의 한계 극복)

1.  **환각(Hallucination) 방지:** 근거 자료(Context)를 제공하여 사실이 아닌 내용을 지어내는 것을 억제합니다.
2.  **최신 정보 반영:** 학습 시점 이후의 정보도 검색을 통해 답변할 수 있습니다.
3.  **데이터 보안:** 기밀 데이터를 모델에 학습시키지 않고도 보안이 유지된 상태로 검색하여 답변을 생성합니다.

---

## 2. 동작 원리 (Step-by-Step)

1.  **임베딩 (Embedding):** 사용자의 질문을 숫자 벡터(Vector)로 변환합니다.
2.  **검색 (Retrieval):** 벡터 DB에서 질문과 의미가 가장 유사한 문서 조각(Chunk)들을 찾아냅니다.
3.  **프롬프트 구성 (Augmentation):** 검색된 문서 조각을 질문과 합쳐서 "이 문서를 참고해서 답변해줘"라는 프롬프트를 만듭니다.
4.  **답변 생성 (Generation):** LLM이 완성된 프롬프트를 보고 정확한 답변을 생성합니다.

---

## 3. 실무 코드 예시

백엔드 개발자가 실제로 RAG 시스템을 구현할 때의 핵심 로직입니다. (Python/LangChain 기준)

### 1) 문서 인덱싱 (Indexing)
문서를 조각내고 벡터 DB에 저장하는 과정입니다.

```python
from langchain.text_splitter import CharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_openai import OpenAIEmbeddings

# 1. 문서 로드 및 쪼개기 (Chunking)
text_splitter = CharacterTextSplitter(chunk_size=500, chunk_overlap=50)
documents = text_splitter.split_text(raw_company_policy)

# 2. 임베딩 및 벡터 DB 저장
vector_db = FAISS.from_texts(documents, OpenAIEmbeddings())
```

### 2) 서비스 로직 (Retrieval & Generation)
사용자 질문에 대해 검색하고 답변을 생성하는 API 내부 로직입니다.

```python
from langchain_openai import ChatOpenAI
from langchain.chains import RetrievalQA

# 1. 검색기(Retriever) 설정 (유사도 높은 상위 3개 추출)
retriever = vector_db.as_retriever(search_kwargs={"k": 3})

# 2. RAG 체인 구성
qa_chain = RetrievalQA.from_chain_type(
    llm=ChatOpenAI(model="gpt-4o"),
    retriever=retriever,
    return_source_documents=True # 답변의 근거가 된 원문도 함께 반환
)

# 3. 질문 실행
result = qa_chain.invoke({"query": "우리 회사 연차 규정 알려줘"})
print(result["result"]) # LLM의 답변
```

### 3) DB 쿼리 예시 (pgvector 사용 시)
PostgreSQL의 `pgvector` 확장을 사용하여 벡터 유사도 검색을 수행하는 SQL입니다.

```sql
-- 사용자의 질문 벡터와 가장 유사한 문서 상위 5개를 코사인 유사도 기반으로 검색
SELECT content, 1 - (embedding <=> '[0.123, 0.456, ...]') AS similarity
FROM documents
ORDER BY similarity DESC
LIMIT 5;
```

---

## 4. 핵심 기술 요소

*   **임베딩 모델:** `text-embedding-3-small` (OpenAI), `HuggingFace` 오픈소스 모델 등
*   **벡터 데이터베이스 (Vector DB):**
    *   **Pinecone:** 관리형 서비스(SaaS), 빠른 도입이 필요할 때.
    *   **pgvector:** 기존 PostgreSQL 인프라를 그대로 활용하고 싶을 때.
    *   **Elasticsearch:** 키워드 검색과 벡터 검색을 합친 **하이브리드 검색**이 필요할 때.

### 요약

| 구분 | 일반 LLM | RAG (검색 증강 생성) |
| :--- | :--- | :--- |
| **지식 출처** | 학습된 데이터 (과거) | **외부 데이터베이스 (실시간)** |
| **정확도** | 환각 발생 가능성 높음 | **근거 기반이라 정확함** |
| **비용** | 파인튜닝(재학습) 비쌈 | **비교적 저렴하고 구축 쉬움** |
| **주요 기술** | 프롬프트 엔지니어링 | **임베딩, 벡터 DB, 검색** |
