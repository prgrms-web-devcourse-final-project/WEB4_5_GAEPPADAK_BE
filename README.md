개발에 빠진 닭 팀입니다.

## 🛠️ 개발 환경 설정 (Development Setup)

**1️⃣ Clone the Repository**

```bash
git clone https://github.com/prgrms-web-devcourse-final-project/WEB4_5_GAEPPADAK_BE.git
```

**2️⃣ Environment Variables (.env) Setup**
✅ Using Doppler (Recommended)

> Doppler는 .env 환경 변수 파일을 안전하게 관리해주는 도구입니다.

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

**3️⃣ Run Database (MySQL via Docker Compose)**

```bash
# Start MySQL container with Docker Compose
docker-compose up -d

# Monitor logs (logs are mapped locally)
tail -f ./mysql_logs/general.log

# Stop Containers
docker-compose down

```

**4️⃣ Run Backend (Spring Boot)**

```bash
cd backend

./gradlew bootRun
```

- Port: 8080

- Swagger Docs: http://localhost:8080/swagger-ui/index.html
