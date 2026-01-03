## CSRF 공격

#### 사이트 간 요청 위조(Cross-site Request Forgery, CSRF) 공격

- 사용자가 자신의 의지와 상관없이 공격자가 의도한 행위를 특정 웹사이트에 요청하도록 하는 것
- ex)
    - 특정 사용자가 매일메일 서비스에서 로그인을 수행
    - 서버는 해당 사용자에 대한 세션 ID를 `Set-Cookie` 헤더에 담아서 응답함
    - 클라이언트는 쿠키를 저장하고 요청마다 자동으로 전달
    - 이러한 사용자를 대상으로 공격자는 악성 스크립트가 담긴 페이지에 접속하도록 유도
        - 악성 스크립트가 포함된 메일이나 게시글을 작성
        - 악성 스크립트가 포함된 공격자 사이트 접속 링크를 전달하는 것
    - 사용자가 악성 스크립트가 포함된 페이지에 접속하게 되면 악성 스크립트가 실행됨
    - 사용자의 의도와 상관없는 특정한 요청(결제, 비밀번호 변경)을 공격 대상 서버로 보내도록 구현되어 있음
    - 해당 요청은 브라우저에 의해서 자동으로 쿠키에 세션 ID가 함께 전달됨
    - ex) `<img src ="https://maeil-mail.com/member/changePassword?newValue=1234" />`
    - 공격자 사이트에 방문한 사용자는 자신의 의지와 무관하게 img 태그로 인해 세션 ID가 포함된 쿠키와 함께 비밀번호 변경 요청을 매일메일 서버로 전달함
---
### CSRF 공격은 어떻게 방어할 수 있나?

#### 교차 출처인 상황에서의 요청을 막는 방식으로 CSRF를 방어

- HTTP 헤더 중에 하나인 Referer 요청 헤더를 사용하는 방법
    - Referer 요청 헤더로 현재 요청을 보낸 페이지 주소를 알 수 있음
    - 해당 주소와 Host(서버의 도메인 이름) 헤더를 비교하여 다른 경우, 예외를 발생
    - Referer 요청 헤더는 조작될 수 있다는 점에서 한계가 있음
- 템플릿 엔진 기술(JSP, 타임리프, Pug, Ejs 등)을 사용하는 경우라면 CSRF 토큰 방법
    - 페이지를 생성하기 이전에 사용자 세션에 임의의 CSRF 토큰을 저장
    - 특정 API 요청에 대한 제출 폼을 생성할 때 해당 CSRF 토큰값이 설정된 imput 태그를 추가
    - `<input type = "hidden" name = "csrf_token" value = "csrf_token_12341234" />`
    - 실제로 요청이 전달될 때, 해당 input 태그의 CSRF 토큰과 사용자 세션 내부에 존재하는 CSRF 토큰의 일치 여부를 판단
- 이 외
    - SameSite 쿠키를 사용하여 크로스 사이트에 대한 쿠키 전송을 제어
    - 브라우저늬 SOP(Same Origin Policy) 정책을 사용하고 CORS 설정으로 교차 출처 접근을 일부분 허용하는 방식으로도 방어 가능

ex)
- 정상 로그인
  - `https://example.com` 로그인
  - 서버 -> `Set-Cookie: JSESSIONID=abc`
  - 브라우저 -> `example.com` 전용 쿠키로 저장
- 악성 사이트 방문
  - `https://evil-site.com` 접속
  - 페이지 내부
  - `<button onclick="location.href='https://example.com/member/changePassword?newValue=1234'"> 이벤트 참여 </button>`
- 버튼 클릭
  - 브라우저가 `example`으로 요청을 보냄
  - 비밀번호가 변경되는 악용 사례 발생
