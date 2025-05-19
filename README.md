# WEB4_5_GAEPPADAK_BE

프로그래머스 백엔드 데브코스 4기 5회차 4팀 최종 프로젝트

## 👥 Team Introduction

| <img src="https://avatars.githubusercontent.com/u/84301295?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/69628269?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/15260002?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/78626811?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/70743878?v=4" width="100"> | <img src="https://avatars.githubusercontent.com/u/115456416?v=4" width="100"> |
|:----------------------------------------------------------------------------:|:----------------------------------------------------------------------------:|:----------------------------------------------------------------------------:|:----------------------------------------------------------------------------:|:----------------------------------------------------------------------------:|:-----------------------------------------------------------------------------:|
|    이하은 <br> [haeun9988](https://github.com/haeun9988) <br> Product Owner     |    이화영 <br> [2hwayoung](https://github.com/2hwayoung) <br> Backend Leader    |             김경래 <br> [godaos](https://github.com/godaos) <br> 비고             |             김하연 <br> [xaxeon](https://github.com/xaxeon) <br> 비고             |   이태경 <br> [dlfjsld1](https://github.com/dlfjsld1) <br> AWS Administrator    |         윤상민 <br> [skvhffpdyd](https://github.com/skvhffpdyd) <br> 비고          |

## 📊 Project Overview

### 프로젝트 이름: **꼬끼오(KKokkio)**

다양한 뉴스 채널의 실시간 속보 데이터를 수집하고, 급상승 트렌드 키워드와 주요 이슈를 탐지하여 사용자에게 여론의 흐름을 시각화하고 요약과 알림을 제공하는 서비스

### 기획 의도

알고리즘 기반 개인화 서비스로 인해 발생하는 확증 편향과 에코챔버 문제를 해결하고자, 실시간 인기 검색어를 중심으로 다수의 사회적 관심사를 반영한 정보를 제공하여 사용자의 시야를 넓히고 관심사를 확장시키는
서비스입니다.

- 실시간 검색어를 기준으로 현재 인기 있는 주제에 대한 정보를 찾을 수 있도록 도와준다.
- 많은 서비스가 개인의 관심사에 대한 알고리즘을 바탕으로 정보를 제공하는데, 많은 사람들이 관심있는 주제를 토대로 정보를 제공
- 알고리즘에 갇힌 사회확증편향 문제를 해소
- 한정된 정보만 소비하게 되는 에코챔버 현상을 완화하고, 다양한 관점에 노출되도록 돕기 위함

### 메인 기능

- **구글 트렌드, 뉴스 Naver API에서 실시간으로 데이터 수집**
- **데이터 기반 인기 키워드 추출**
- **인기 키워드를 기반으로 출처(기사, 유튜브 링크) 제공**
- **키워드 기반 AI 내용 요약**
- **사용자 인증**
- **콘텐츠 별 사용자 댓글 기능**

### 기술적 목표

**“실시간 대용량 외부 데이터를 안정적으로 수집·가공·전달하는 백엔드 파이프라인 구축”**

- Google Open API 및 다양한 뉴스/검색 API를 활용한 실시간 외부 데이터 수집
- 비동기 메시징으로 고트래픽 상황에서도 유실 없이 처리
- 스케줄링을 활용해 데이터 수집 및 전처리 워크플로우 자동화
- 비정형 뉴스 데이터를 구조화하고 저장하는 데이터 가공 로직 설계

## 🌐 서비스 접속 주소 (Backend Endpoints)

배포된 백엔드 서버의 접속 주소입니다.

* **개발 환경 (Development)**: `https://api.deploy.kkokkio.site`
* **운영 환경 (Production)**: `https://api.prd.kkokkio.site`

Swagger 문서는 각 주소에 `/swagger-ui/index.html` 경로를 붙여 접근할 수 있습니다.

* **개발 환경 Swagger**: `https://api.deploy.kkokkio.site/swagger-ui/index.html`
* **운영 환경 Swagger**: `https://api.prd.kkokkio.site/swagger-ui/index.html`

## 🛠️ Technology Stack

#### 🎨 Frontend

<div> 
  <img src="https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white"/>
  <img src="https://img.shields.io/badge/Next.js-000000?style=for-the-badge&logo=next.js&logoColor=white"/>
  <img src="https://img.shields.io/badge/creatie.ai-5C2D91?style=for-the-badge&logoColor=white"/>
</div>

#### 🛠 Backend

<div> 
  <img src="https://img.shields.io/badge/Java-007396?style=for-the-badge&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black"/>
  <img src="https://img.shields.io/badge/Apache%20Airflow-017CEE?style=for-the-badge&logo=apacheairflow&logoColor=white"/>
</div>

#### 🗄 Database

<div> 
  <img src="https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/>
  <img src="https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white"/>
</div>

#### 🔧 IDLE & Tool

<div> 
  <img src="https://img.shields.io/badge/IntelliJ%20IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"/>
</div>

#### 📶 OPEN API

<div> 
    <img src="https://img.shields.io/badge/ChatGPT%20API-412991?style=for-the-badge&logo=openai&logoColor=white"/> 
    <img src="https://img.shields.io/badge/News%20API-0A0A0A?style=for-the-badge&logoColor=white"/>
    <img src="https://img.shields.io/badge/YouTube-FF0000?style=for-the-badge&logo=youtube&logoColor=white"/>
    <img src="https://img.shields.io/badge/Google%20Trends-4285F4?style=for-the-badge&logoColor=white"/>
    <img src="https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white"/>
</div>

#### 🚀 Deployment & Infra

<div> 
  <img src="https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black"/>
  <img src="https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white"/>
  <img src="https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white"/>
  <img src="https://img.shields.io/badge/Vercel-000000?style=for-the-badge&logo=vercel&logoColor=white"/>
  <img src="https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white"/>
  <img src="https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white"/>
</div>

#### 🖥️ Monitoring

<div> 
    <img src="https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white"/>
    <img src="https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white"/>
</div>

#### 🗂️ Version management and collaboration tools

<div> 
  <img src="https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white"/>
  <img src="https://img.shields.io/badge/Notion-%23000000.svg?style=for-the-badge&logo=notion&logoColor=white"/>
  <img src="https://img.shields.io/badge/Slack-4A154B?style=for-the-badge&logo=slack&logoColor=white)"/>
  <img src="https://img.shields.io/badge/Zep-6001D2?style=for-the-badge&logo=Zep&logoColor=white"/>
  <img src="https://img.shields.io/badge/Google%20Meet-00897B?style=for-the-badge&logo=googlemeet&logoColor=white"/>
</div>

## System Architecture & Deployment

**추가 예정**

## ERD

**추가 예정**

## 🛠️ 개발 환경 설정 (Development Setup)

**1️⃣ Clone the Repository**

```bash
git clone https://github.com/prgrms-web-devcourse-final-project/WEB4_5_GAEPPADAK_BE.git
```

**2️⃣ Environment Variables (.env) Setup**
✅ Using Doppler (Recommended)

> Doppler는 .env 환경 변수 파일을 안전하게 관리해주는 도구입니다.
> 현재 프로젝트는 backend와 infra 두 폴더 안에서 Doppler를 사용하도록 구성되어 있습니다.
> 각각의 폴더 안에서 npm run doppler 명령어를 별도로 실행해야 합니다.
> 처음 실행하는 경우, doppler setup으로 설정할 프로젝트와 환경을 먼저 선택해 주세요.

```bash
# Install Doppler CLI
brew install dopplerhq/cli/doppler

# Login & Setup
doppler login
## Select an option: Scope login to current directory
## Open the authorization page in your browser?: Y
## Your auth code is: {CODE} -> Enter in your browser

doppler setup
## Use settings from repo config file (doppler.yaml) : Y

# Run with environment loaded
npm run doppler
```

**3️⃣ Run Server & Database**

```bash
# Start MySQL & SpringBoot container with Docker Compose
npm run docker

# Monitor logs (logs are mapped locally)
tail -f ./infra/mysql_logs/general.log

# Reset Containers
npm run docker:reset

```

- Swagger Docs: http://localhost:8080/swagger-ui/index.html

**4️⃣ Generate Test Coverage Report**
✅JaCoCo를 사용하여 코드 테스트 커버리지 리포트를 생성합니다.

```bash
#자동으로 테스트를 실행 후 HTML 형식의 커버리지 리포트를 생성
#/backend 경로에서 실행
#build/jacocoHtml/index.html 경로에서 결과 확인 가능
./gradlew jacocoTestReport

```