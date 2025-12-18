## 연결 리스트 (Linked List)
- 리스트 내의 요소(노드)들을 포인터로 연결하여 관리하는 선형 자료구조
- 각 노드는 데이터와 다음 요소에 대한 포인터를 가지고 있음
  - 첫 번째 노드를 HEAD
  - 마지막 노드를 TAIL
- 메모리가 허용하는 한 요소를 계속 삽입 가능
- 시간 복잡도
  - 탐색 - O(N)
  - 노드 삽입, 삭제 - O(1)
- 파생된 자료구조
  - 단일 연결 리스트 (Singly Linked List)
  - 이중 연결 리스트 (Doubly Linked List, Circular Linked List)
- 배열은 순차적인 데이터가 들어가기 때문에 메모리 영역을 연속적으로 사용
- 반면, 연결 리스트는 메모리 공간에 흩어져서 존재한다는 점에서 배열과 차이가 있음
---

### 탐색, 추가, 삭제
```java
class SinglyLinkedList {

    public Node head;
    public Node tail;

    public Node insert(int newValue) {
        Node newNode = new Node(null, newValue);
        if (head == null) {
            head = newNode;
        } else {
            tail.next = newNode;
        }
        tail = newNode;

        return newNode;
    }

    public Node find(int findValue) {
        Node currentNode = head;
        while (currentNode.value != findValue) {
            currentNode = currentNode.next;
        }

        return currentNode;
    }

    public void appendNext(Node prevNode, int value) {
        prevNode.next = new Node(prevNode.next, value);
    }

    public void deleteNext(Node prevNode) {
        if (prevNode.next != null) {
            prevNode.next = prevNode.next.next;
        }
    }
}

```
- 단일 연결 리스트에서 탐색은 최악의 경우 O(N)
  - 노드를 탐색하기 위해 HEAD의 포인터부터 데이터를 찾을 때까지 다음 요소를 반복적으로 탐색하기 때문
- 삽입
  - 삽입될 위치 이전에 존재하는 노드와 신규 노드의 포인터를 조작
  - ex) 3번 위치에 신규 노드 삽입
    - 2번 위치의 노드 포인터 -> 신규 노드
    - 신규 노드 포인터 -> 3번 노드
- 삭제
  - 삭제할 노드의 이전 노드가 삭제 대상 노드의 다음 노드를 사리키도록 수정, 삭제 대상 노드를 메모리에서 제거
- **삽입과 삭제 연산 자체의 경우 시간 복잡도는 O(1)이지만, 탐색이 필요한 경우 선형 시간이 걸림**
  - ex) 값이 50인 노드 뒤에 삽입
  - ex) 3번째 노드 삭제
  - 위치를 찾는 건 O(N), 실제 삽입, 삭제 연산 자체는 O(1)