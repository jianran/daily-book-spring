# CI/CD Guide - GitHub Actions + Local Docker Run

This guide explains how to use the CI/CD pipeline to build the Docker image and run it locally.

## Overview

```
GitHub Push → GitHub Actions (Build) → ghcr.io/jianran/daily-book-spring
                                                   ↓
                                            Local Docker Run
                                                   ↓
                                         curl to trigger essay
```

## Step 1: Configure GitHub Secrets

Before the CI/CD can build the image, you need to configure GitHub secrets:

1. Go to your repository on GitHub
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**

Add these secrets:
- `DEEPSEEK_API_KEY` - Your DeepSeek API key
- `DISCORD_WEBHOOK_URL` - Your Discord webhook URL (optional)

## Step 2: Push to Trigger CI/CD

```bash
cd daily-book-spring
git add .
git commit -m "Add Discord integration and CI/CD setup"
git push origin main
```

The GitHub Actions workflow will automatically run on push.

## Step 3: Monitor CI/CD Progress

1. Go to **Actions** tab in your repository
2. Click on the workflow run
3. Watch the progress:
   - **Build and Test** → Compiles and tests code
   - **Docker Build** → Builds and pushes to ghcr.io
   - **Security Scan** → OWASP dependency check

## Step 4: Pull and Run Locally

After the CI/CD completes successfully:

### Option A: Using the Trigger Script (Recommended)

```bash
# Login to GitHub Container Registry
docker login ghcr.io -u <your-github-username> -p <your-github-token>

# Run the trigger script
./trigger-run.sh latest
```

### Option B: Manual Steps

```bash
# Login to GitHub Container Registry
docker login ghcr.io -u <your-github-username> -p <your-github-token>

# Pull the latest image
docker pull ghcr.io/jianran/daily-book-spring:latest

# Start with environment variables
docker run -d \
    --name daily-book-spring \
    -p 8080:8080 \
    --env-file .env \
    ghcr.io/jianran/daily-book-spring:latest

# Wait for startup
sleep 5

# Test the API
curl http://localhost:8080/api/books/health
```

## Step 5: Trigger the Essay

Once the container is running:

```bash
# Get top books
curl http://localhost:8080/api/books

# Generate essay
curl http://localhost:8080/api/books/essay

# Generate and send to Discord DM
curl -X POST http://localhost:8080/api/books/essay/discord
```

## Workflow Modes

### Development Mode
```bash
# Push to develop branch
git checkout -b feature/discord-integration
git push origin develop
```

### Production Mode
```bash
# Push to main branch
git checkout main
git push origin main
```

Only pushes to `main` will build and push the Docker image.

## Troubleshooting

### Image Not Found
```bash
# Make sure you're logged in
docker login ghcr.io -u <your-github-username> -p <your-github-token>

# Check available tags
docker pull ghcr.io/jianran/daily-book-spring:latest
```

### Container Won't Start
```bash
# Check logs
docker logs daily-book-spring

# Check if port is in use
lsof -i :8080
```

### API Not Responding
```bash
# Wait a bit longer for startup
sleep 10

# Check container status
docker ps

# Restart container
docker restart daily-book-spring
```

## Cleanup

```bash
# Stop and remove container
docker stop daily-book-spring
docker rm daily-book-spring
```

## Next Steps

After successfully running locally:

1. **Test the Discord integration** - Call the Discord endpoint
2. **Monitor logs** - Use `docker logs -f daily-book-spring`
3. **Deploy to production** - Follow deployment instructions if needed
