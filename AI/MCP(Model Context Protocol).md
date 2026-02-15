### MCP (Model Context Protocol)

**정의**
- **"AI를 위한 USB 표준"**
- AI 모델이 로컬 파일, 데이터베이스, API 서버 등 외부 세계와 소통하는 방식을 표준화한 프로토콜
- 이 규격만 맞추면, 어떤 AI 모델이든 내가 만든 DB나 API를 도구(Tool)처럼 가져다 쓸 수 있음

---

### 아키텍처 (3가지 요소)

1.  **MCP Host (호스트):**
    - AI 모델을 실행하는 애플리케이션 (예: Claude Desktop App, Cursor IDE)
    - 사용자의 질문을 받고, MCP Server에게 "이거 좀 해줘"라고 요청함
2.  **MCP Server (서버):**
    - **백엔드 개발자가 주로 개발하는 영역**
    - 실제 데이터(DB, API)에 접근하는 기능을 '툴(Tool)'로 만들어 노출함
3.  **MCP Client (클라이언트):**
    - 호스트 내부에서 서버와 통신을 담당하는 모듈

---

### 실무 활용 예시

#### 예시 1: GitHub 이슈 생성 (Python)

**상황:** AI에게 "버그 리포트 좀 작성해줘"라고 말하면, 알아서 GitHub에 이슈를 생성하게 하고 싶음

**1. MCP Server 개발 (Python)**

```python
from mcp.server.fastmcp import FastMCP
import requests
import os

# 1. MCP 서버 생성
mcp = FastMCP("GitHub-Issue-Bot")

# GitHub API 토큰 (환경 변수에서 안전하게 로드)
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN")
HEADERS = {
    "Authorization": f"token {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json"
}

# 2. 툴(Tool) 정의: AI가 호출할 수 있는 함수
@mcp.tool()
def create_github_issue(repository: str, title: str, body: str) -> str:
    """
    지정된 GitHub 리포지토리에 새로운 이슈를 생성합니다.
    Args:
        repository: 이슈를 생성할 리포지토리 (예: 'owner/repo')
        title: 이슈 제목
        body: 이슈 내용
    """
    url = f"https://api.github.com/repos/{repository}/issues"
    payload = {"title": title, "body": body}
    
    response = requests.post(url, headers=HEADERS, json=payload)
    
    if response.status_code == 201:
        return f"이슈 생성 성공! URL: {response.json()['html_url']}"
    else:
        return f"이슈 생성 실패: {response.text}"

# 3. 서버 실행
if __name__ == "__main__":
    mcp.run()
```

**2. 실제 동작**
- **사용자:** "backend-cs-notes 리포에 'README 업데이트 필요'라는 제목으로 이슈 하나 만들어줘."
- **Claude (Host):** `create_github_issue(repository='629jy/backend-cs-notes', title='README 업데이트 필요', body='')` 함수를 호출
- **MCP Server:** GitHub API를 호출하여 실제로 이슈를 생성하고 결과를 반환
- **Claude:** "네, 이슈를 성공적으로 생성했습니다. 주소는 ... 입니다."

---

#### 예시 2: GitHub Pull Request 생성 (Java / Spring Boot)

**상황:** 개발자가 "feature/login 브랜치를 develop으로 PR 만들어줘"라고 AI에게 요청

**1. MCP Server 개발 (Java/Spring Boot)**
* (참고: 아직 공식 Java용 MCP 라이브러리는 없으므로, Spring Boot로 개념을 구현한 예시)

```java
// build.gradle - 의존성 추가
// implementation 'org.kohsuke:github-api:1.321'

@RestController
public class GitHubMcpController {

    // @Tool 어노테이션은 MCP 서버가 툴을 식별하는 가상의 장치
    @PostMapping("/mcp/tools/createPullRequest")
    public Map<String, String> createPullRequest(@RequestBody PullRequestDto request) {
        try {
            GitHub github = new GitHubBuilder().withOAuthToken(System.getenv("GITHUB_TOKEN")).build();
            GHRepository repo = github.getRepository(request.getRepository());
            
            GHPullRequest pr = repo.createPullRequest(
                request.getTitle(),
                request.getHead(), // 예: "feature/login"
                request.getBase(),  // 예: "develop"
                request.getBody()
            );
            
            return Map.of("result", "PR 생성 성공! URL: " + pr.getHtmlUrl().toString());
        } catch (IOException e) {
            return Map.of("error", "PR 생성 실패: " + e.getMessage());
        }
    }
}

// DTO (Data Transfer Object)
class PullRequestDto {
    private String repository; // "owner/repo"
    private String title;
    private String head;
    private String base;
    private String body;
    // Getters and Setters
}
```

**2. 실제 동작**
- **사용자:** "feature/login 브랜치를 develop으로 '로그인 기능 추가' PR 만들어줘."
- **Claude (Host):** 사용자의 말을 해석하여 `/mcp/tools/createPullRequest` 엔드포인트에 JSON 요청을 보냄
- **MCP Server (Spring Boot):** GitHub API를 호출하여 PR을 생성하고 결과를 반환
- **Claude:** "네, '로그인 기능 추가' PR을 성공적으로 생성했습니다."

---

### Q&A: 공식 서버 vs 커스텀 서버

**Q. GitHub에서 제공하는 공식 MCP 서버가 있던데, 그걸 쓰면 안 되나?**
- **A. 사용하면 됨 (그리고 그게 더 좋음)**
- GitHub, Slack, Google Drive 같은 대형 서비스들은 이미 **공식 MCP 서버**를 제공하고 있음
- 공식 서버를 사용하면 개발자가 일일이 API 연동 코드를 짤 필요 없이, 설정 파일에 등록만 하면 바로 AI가 해당 서비스의 모든 기능(이슈 생성, PR 리뷰, 파일 검색 등)을 사용할 수 있음
- **안정성**과 **유지보수** 측면에서 공식 서버가 훨씬 유리함

**Q. 그럼 언제 직접(Custom) 만드나?**
- **우리 회사만의 비즈니스 로직**이 필요할 때 만듬
- 예: "PR을 생성할 때 우리 회사 지라(Jira) 티켓 상태도 '진행 중'으로 바꿔줘" 같은 복합적인 기능은 공식 서버에 없음
- 예: 사내 레거시 DB를 조회해야 하거나, 내부 API를 호출해야 할 때는 직접 만들어야 함

**요약**
- **일반적인 기능 (GitHub, Slack 등):** 공식 MCP 서버 사용 권장
- **특수한 기능 (사내 로직, 커스텀 워크플로우):** 직접 MCP 서버 개발