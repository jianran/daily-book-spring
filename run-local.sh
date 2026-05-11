#!/bin/bash

# Daily Book Spring AI - Local Run Script
# This script pulls the Docker image from GitHub Container Registry and runs it locally

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
REGISTRY="ghcr.io"
IMAGE_NAME="fatan/daily-book-spring"
CONTAINER_NAME="daily-book-spring"
ENV_FILE=".env"

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   Daily Book Spring AI - Local Runner${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""

# Check if .env file exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${YELLOW}Warning: $ENV_FILE not found. Creating from .env.example...${NC}"
    cp .env.example .env
    echo -e "${RED}Please edit $ENV_FILE and add your API keys before running.${NC}"
    exit 1
fi

# Check if .env has API keys configured
if grep -q "your-deepseek-api-key-here" "$ENV_FILE"; then
    echo -e "${RED}Error: Please configure your API keys in $ENV_FILE${NC}"
    echo "Edit the file and replace 'your-deepseek-api-key-here' with your actual key"
    exit 1
fi

echo -e "${GREEN}Step 1: Pulling latest Docker image...${NC}"
docker pull "${REGISTRY}/${IMAGE_NAME}:latest" 2>/dev/null || {
    echo -e "${RED}Failed to pull image. Make sure you're logged into GitHub Container Registry.${NC}"
    echo "Run: docker login ghcr.io -u <your-username> -p <your-token>"
    exit 1
}

echo -e "${GREEN}Step 2: Stopping any existing container...${NC}"
docker stop "$CONTAINER_NAME" 2>/dev/null || true
docker rm "$CONTAINER_NAME" 2>/dev/null || true

echo -e "${GREEN}Step 3: Starting container with environment variables...${NC}"
docker run -d \
    --name "$CONTAINER_NAME" \
    -p 8080:8080 \
    --env-file "$ENV_FILE" \
    --restart unless-stopped \
    "${REGISTRY}/${IMAGE_NAME}:latest"

echo -e "${GREEN}Step 4: Waiting for service to start...${NC}"
sleep 5

# Check if container is running
if docker ps --format '{{.Names}}' | grep -q "$CONTAINER_NAME"; then
    echo -e "${GREEN}Container is running!${NC}"
else
    echo -e "${RED}Container failed to start. Checking logs...${NC}"
    docker logs "$CONTAINER_NAME"
    exit 1
fi

# Wait for health check
echo -e "${GREEN}Waiting for API to be ready...${NC}"
for i in {1..30}; do
    if curl -s http://localhost:8080/api/books/health > /dev/null 2>&1; then
        echo -e "${GREEN}API is ready!${NC}"
        break
    fi
    echo -e "  Waiting... ($i/30)"
    sleep 1
done

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   Daily Book API is Running!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "Base URL: http://localhost:8080"
echo ""
echo -e "Available Endpoints:"
echo "  - GET  http://localhost:8080/api/books              # Get top books"
echo "  - GET  http://localhost:8080/api/books/health       # Health check"
echo "  - GET  http://localhost:8080/api/books/essay        # Generate essay"
echo "  - POST http://localhost:8080/api/books/essay/discord # Send to Discord"
echo ""
echo -e "Example commands:"
echo "  curl http://localhost:8080/api/books"
echo "  curl http://localhost:8080/api/books/essay"
echo "  curl -X POST http://localhost:8080/api/books/essay/discord"
echo ""
echo -e "To send essay to Discord:"
echo -e "  ${YELLOW}curl -X POST http://localhost:8080/api/books/essay/discord${NC}"
echo ""
echo -e "To stop the container:"
echo -e "  ${YELLOW}docker stop $CONTAINER_NAME && docker rm $CONTAINER_NAME${NC}"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop the container${NC}"

# Keep the script running and forward signals to docker
trap "docker stop $CONTAINER_NAME && docker rm $CONTAINER_NAME; echo ''; echo 'Stopped'; exit" INT TERM

# Wait indefinitely
wait
