# Daily Book Spring AI

A Java Spring Boot application that fetches daily top 10 popular books from Google Books API, uses DeepSeek AI to generate a 10-minute essay about a selected book, and provides an Apple Music link for audio reading.

## Features

- **Multi-Category Book Discovery**: Fetches top books from History, AI, Fiction, and Non-fiction categories
- **AI-Powered Essay Generation**: Uses DeepSeek AI to create engaging 10-minute reading essays (~1500-1800 words)
- **Apple Music Integration**: Generates iOS deep links for audio playback
- **RESTful API**: Well-documented endpoints for integration
- **Docker Support**: Containerized deployment with health checks
- **GitHub CI/CD**: Automated build, test, and deployment pipeline

## Prerequisites

- Java 17+
- Maven 3.6+
- DeepSeek API Key ([Get yours here](https://deepseek.com))
- Docker (optional, for containerized deployment)

## Quick Start

### 1. Clone and Configure

```bash
cd daily-book-spring
cp .env.example .env
```

Edit `.env` and add your DeepSeek API key:
```
DEEPSEEK_API_KEY=your-api-key-here
```

### 2. Run with Maven

```bash
mvn spring-boot:run
```

### 3. Run with Docker

```bash
docker-compose up --build
```

## API Endpoints

### Health Check
```bash
GET /api/books/health
```

### Get Top Books
```bash
GET /api/books?categories=history,ai,fiction,non_fiction
```

### Generate Daily Essay
```bash
GET /api/books/essay
```

### Generate Essay for Specific Book
```bash
GET /api/books/essay?category=history&rank=1
```

### Get Available Categories
```bash
GET /api/books/categories
```

### Generate Essay and Send to Discord DM
```bash
POST /api/books/essay/discord
```

## Discord Integration

To send essays directly to your Discord DM, configure the following environment variables:

```bash
# Option 1: Using Discord Webhook (Easiest)
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/YOUR_WEBHOOK_ID/YOUR_WEBHOOK_TOKEN

# Option 2: Using Bot + Direct Message
DISCORD_USER_ID=your-discord-user-id
DISCORD_BOT_TOKEN=your-bot-token
```

For detailed setup instructions, see [`DISCORD_SETUP.md`](DISCORD_SETUP.md).

Once configured, call:
```bash
curl -X POST http://localhost:8080/api/books/essay/discord
```

The essay will be sent to your Discord DM with:
- Book details and title
- The 10-minute reading essay
- Apple Music link for background study music

## Response Example

```json
{
  "status": "success",
  "message": "Daily essay generated for 'The AI Revolution' by John Smith",
  "essay": {
    "id": "essay_1234567890",
    "selectedBook": {
      "title": "The AI Revolution",
      "author": "John Smith",
      "category": "ai",
      "rank": 1
    },
    "title": "A 10-minute Essay: The AI Revolution",
    "content": "A DECADE-LONG EXPLORATION: UNDERSTANDING THE AI REVOLUTION BY JOHN SMITH...",
    "appleMusicLink": {
      "url": "https://music.apple.com/search?q=essay+THE+AI+REVOLUTION",
      "deepLink": "music://search?q=essay+THE+AI+REVOLUTION"
    },
    "estimatedReadingTimeMinutes": 10
  }
}
```

## Project Structure

```
daily-book-spring/
├── src/main/java/com/dailybook/
│   ├── DailyBookApplication.java
│   ├── controller/
│   │   └── BookController.java
│   ├── service/
│   │   ├── GoogleBooksService.java
│   │   ├── DeepSeekAiService.java
│   │   └── BookService.java
│   ├── model/
│   │   ├── Book.java
│   │   └── Essay.java
│   └── dto/
│       ├── BookSearchRequest.java
│       └── BookEssayResponse.java
├── src/main/resources/
│   └── application.properties
├── .github/workflows/
│   └── ci-cd.yml
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

## GitHub Actions CI/CD

### Workflow Overview

The `.github/workflows/ci-cd.yml` workflow includes:

1. **Build & Test**: Compiles code and runs unit tests
2. **Docker Build**: Builds and pushes container to GitHub Container Registry (ghcr.io)
3. **Security Scan**: OWASP Dependency Check for vulnerabilities
4. **Deploy**: Optional deployment to production environment

### Manual Trigger

You can manually trigger the CI/CD pipeline from GitHub:

1. Go to your repository on GitHub
2. Click **Actions** tab
3. Select **Daily Book Spring AI CI/CD** workflow
4. Click **Run workflow**
5. Choose the branch and click **Run workflow**

### After CI/CD Completes

Once the GitHub Actions workflow finishes building the Docker image:

```bash
# Option 1: Use the trigger script
./trigger-run.sh latest

# Option 2: Manual steps
docker login ghcr.io -u <your-username> -p <your-token>
docker pull ghcr.io/jianran/daily-book-spring:latest
./run-local.sh
```

### Workflow Events

The workflow triggers on:
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop` branches
- Manual trigger (workflow_dispatch)

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DEEPSEEK_API_KEY` | Your DeepSeek AI API key | `your-api-key-here` |
| `server.port` | Application port | `8080` |
| `spring.ai.deepseek.chat.options.model` | AI model to use | `deepseek-chat` |
| `spring.ai.deepseek.chat.options.temperature` | AI creativity (0-1) | `0.7` |

## License

MIT License

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
