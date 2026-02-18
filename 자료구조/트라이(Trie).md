### 트라이 (Trie)

**정의**
- 문자열을 저장하고 검색하는 데 특화된 **트리(Tree) 형태의 자료구조**
- 이름은 "탐색(Re**trie**val)"이라는 단어에서 유래
- 각 노드가 문자를 나타내며, 루트에서부터 특정 노드까지의 경로는 하나의 문자열(주로 접두사)을 의미함

**비유: "알파벳으로 이루어진 길 찾기"**
- 루트(출발점)에서부터 단어의 각 알파벳을 따라 길을 찾아가는 것과 같음
- "CAT"이라는 단어를 저장하면, `C` -> `A` -> `T`로 이어지는 길이 생기고, `T` 노드에 "여기서 단어가 끝남"이라는 깃발을 꽂아두는 방식

---

### 트라이의 구조

- **루트 노드 (Root Node):** 비어있는 시작 노드
- **자식 노드 (Child Node):** 각 노드는 다음 문자에 해당하는 자식 노드들을 가짐 (보통 `Map<Character, TrieNode>`로 구현)
- **단어의 끝 표시 (isEndOfWord):** 특정 노드에서 단어가 끝나는지를 나타내는 boolean 플래그

---

### Java 코드 예시

```java
import java.util.HashMap;
import java.util.Map;

public class Trie {

    // 1. Trie 노드 정의
    private static class TrieNode {
        // 자식 노드들을 저장하는 맵
        private final Map<Character, TrieNode> children = new HashMap<>();
        // 해당 노드에서 끝나는 단어가 있는지 여부
        private boolean isEndOfWord;
    }

    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    // 2. 단어 삽입 (Insert)
    public void insert(String word) {
        TrieNode currentNode = root;
        for (char ch : word.toCharArray()) {
            // 문자에 해당하는 자식 노드가 없으면 새로 생성
            currentNode = currentNode.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        // 마지막 노드에 단어의 끝임을 표시
        currentNode.isEndOfWord = true;
    }

    // 3. 단어 검색 (Search)
    public boolean search(String word) {
        TrieNode currentNode = root;
        for (char ch : word.toCharArray()) {
            TrieNode node = currentNode.children.get(ch);
            // 문자에 해당하는 경로가 없으면 false
            if (node == null) {
                return false;
            }
            currentNode = node;
        }
        // 경로가 존재하더라도, 단어의 끝 표시가 있어야 완전한 단어로 인정
        return currentNode.isEndOfWord;
    }

    // 4. 접두사로 시작하는 단어 검색 (Starts With)
    public boolean startsWith(String prefix) {
        TrieNode currentNode = root;
        for (char ch : prefix.toCharArray()) {
            TrieNode node = currentNode.children.get(ch);
            // 접두사에 해당하는 경로가 없으면 false
            if (node == null) {
                return false;
            }
            currentNode = node;
        }
        // 접두사는 단어의 끝 표시와 상관없이 경로만 존재하면 true
        return true;
    }
}
```

---

### 실무 활용 예시

#### 1. 검색 엔진 자동완성 (Autocomplete)
- **상황:** 사용자가 검색창에 "back"을 입력하면 "backend", "background", "backpack" 등을 추천해줘야 함
- **동작:**
    1.  수많은 검색어를 트라이에 미리 저장해 둠
    2.  사용자가 "back"을 입력하면, 트라이에서 `b` -> `a` -> `c` -> `k` 경로를 따라 노드를 찾음
    3.  해당 노드부터 시작하는 모든 하위 경로들을 탐색하여 단어들을 조합하면, "backend", "background" 등을 빠르게 찾을 수 있음
- **왜 좋은가?** 해시 테이블이나 리스트를 쓰는 것보다 접두사 검색 속도가 압도적으로 빠름 (문자열 길이에만 비례)

#### 2. 맞춤법 검사기 (Spell Checker)
- **상황:** 사용자가 "helo"라고 오타를 냈을 때 "hello"를 추천해줘야 함
- **동작:**
    1.  사전의 모든 단어를 트라이에 저장
    2.  "helo"를 트라이에서 `search`하면 결과가 `false`로 나옴 (사전에 없는 단어)
    3.  이때 "helo"와 유사한 경로(예: 한 글자만 다른 경로)를 트라이에서 탐색하여 "hello"를 추천 단어로 제시할 수 있음

#### 3. IP 라우팅 (Longest Prefix Match)
- **상황:** 라우터는 IP 주소를 보고 어떤 경로로 데이터를 보내야 할지 결정해야 함 (가장 구체적인 경로 우선)
- **동작:** 라우팅 테이블을 트라이(정확히는 Radix Trie)로 구현하면, IP 주소(이진수 문자열)와 가장 길게 일치하는 접두사를 매우 효율적으로 찾을 수 있음

---

### 장점과 단점

**장점**
- **빠른 검색 속도:** 접두사 검색(startsWith)과 단어 검색(search)이 문자열의 길이에만 비례(O(L))하므로, 저장된 단어의 수(N)와 무관하게 매우 빠름
- **공간 효율성:** "apple", "apply", "application"처럼 공통된 접두사를 가진 단어들이 많을 경우, 접두사 부분을 공유하므로 메모리를 절약할 수 있음

**단점**
- **메모리 사용량:** 각 노드가 자식 노드에 대한 포인터(맵)를 유지해야 하므로, 공통 접두사가 없는 단어들이 많으면 오히려 메모리 사용량이 클 수 있음
- **제한적인 사용처:** 문자열 검색, 특히 접두사 관련 검색에 특화되어 있어 범용적으로 사용하기는 어려움