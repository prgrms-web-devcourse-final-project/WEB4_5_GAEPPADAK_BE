# 기여 가이드 (Contributing)

본 문서는 개발에 빠진 닭 팀의 **kkokkio 프로젝트**에 기여하기 위한 명확한 절차와 규칙을 안내합니다. 모두가 편리하고 효율적으로 협업할 수 있도록 아래 규칙을 준수해주세요.

---

## 📌 Issue 및 PR 규칙

### 1. Issue 관리

- 작업을 시작하기 전에 **Project Kanban**에 Issue를 생성합니다.
- Issue는 **4시간 이내**에 완료할 수 있는 작은 단위로 쪼갭니다.
- Issue Estimate는 **시간 단위**로 설정합니다.

### 2. 브랜치 전략

| 브랜치                           | 목적                                             |
| -------------------------------- | ------------------------------------------------ |
| `main`                           | 배포가 이루어지는 최종 결과물 브랜치 (자동 배포) |
| `develop`                        | 개발 및 중간 병합을 위한 브랜치                  |
| `feature/{issueNo}-{issue-name}` | 각 기능 개발을 위한 작업 브랜치                  |

### 3. 개발 및 PR 생성 과정

- Issue 생성 후 해당 Issue 번호에 따라 브랜치를 생성합니다.

```bash
git checkout -b feature/{issueNo}-{issue-name}
```

- 작업 수행 후 변경 사항을 Push합니다.

```bash
git push origin feature/{issueNo}-{issue-name}
```

- 최신 변경 사항과 충돌을 방지하기 위해, PR을 생성하기 전에 반드시 `develop` 브랜치로 **rebase**합니다.

```bash
git checkout develop
git pull origin develop
git checkout feature/{issueNo}-{issue-name}
git rebase develop
```

- PR을 생성할 때는 대상 브랜치를 `develop`으로 설정하고, PR 설명란에 구현한 기능을 간단히 작성합니다.

### 4. PR 리뷰 및 병합 규칙

- PR은 병합 전에 **최소 3명 이상의 승인**이 필요합니다.
- PR이 크거나 중요한 Release일 경우 모든 팀원의 승인을 받습니다.
- PR 작성자가 직접 **`Squash and merge`** 합니다.
- PR에는 반드시 정상적으로 동작하는 코드를 포함합니다.
- 리뷰어 자동 할당 및 CI 테스트가 PR 생성 시 자동으로 실행됩니다.

---

## 📝 커밋 메시지 컨벤션

명확한 커밋 메시지는 팀원 간 원활한 소통과 작업 내역 추적을 용이하게 합니다.

### 1. 커밋 유형

| 유형               | 의미                                                  |
| ------------------ | ----------------------------------------------------- |
| `feat`             | 새로운 기능 추가                                      |
| `fix`              | 버그 수정                                             |
| `docs`             | 문서 수정                                             |
| `style`            | 코드 포매팅, 세미콜론 추가 등 코드 동작과 무관한 변경 |
| `refactor`         | 코드 리팩토링                                         |
| `test`             | 테스트 코드 추가 또는 수정                            |
| `chore`            | 패키지 매니저 등 기타 설정 변경                       |
| `design`           | 사용자 UI/UX 디자인 변경                              |
| `comment`          | 주석 추가 및 변경                                     |
| `rename`           | 파일/폴더 이름 변경 및 이동                           |
| `remove`           | 파일 삭제                                             |
| `!BREAKING CHANGE` | API 변경 등 중대한 변경 사항                          |
| `!HOTFIX`          | 긴급하게 배포할 치명적 버그 수정                      |

### 2. 커밋 메시지 형식

- 제목과 본문은 빈 행으로 구분합니다.
- 제목 첫 글자는 **대문자**로, 끝에 마침표(`.`)는 찍지 않습니다.
- 본문에는 **무엇을**, **왜** 변경했는지 명확히 설명합니다.
- 여러 변경사항이 있을 경우 글머리 기호(`-`)를 사용하여 구분합니다.

```bash
feat: 유저 로그인 기능 추가

- JWT 기반 로그인 구현
- 로그인 시 유효성 검사 추가
- 실패 케이스에 대한 예외 처리 강화
```

### 🚨 유의 사항

- 커밋 전 IDE에서 코드 포매팅 (`CTRL + ALT + L`) 필수 적용
- 명확한 변경 사항만 커밋으로 반영 (커밋 단위를 작게 유지)

```bash
git add .
git commit -m "feat: 사용자 로그인 구현"
```

- 잘못된 커밋 방지를 위해 꼼꼼히 검토 후 커밋을 진행합니다.

---

## ✨ 좋은 기여 문화를 위한 팁

- 자신의 코드가 직관적일 것이라 가정하지 말고 항상 설명을 충분히 작성하세요.
- PR과 Issue는 항상 최신 상태를 유지하고, 적극적으로 팀원과 소통하세요.

더 자세한 사항은 아래 링크를 팀원들과 함께 숙지해주세요.

- [좋은 커밋 메시지를 작성하는 방법](https://gggimmin-development-technology.tistory.com/18)
