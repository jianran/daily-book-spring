#!/bin/bash

# Trigger Script - Runs the application after GitHub CI/CD builds the image
# This script is called after the CI/CD workflow completes

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   Trigger Script - Post CI/CD Runner${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""

# Pull the latest image from GitHub Container Registry
echo -e "${YELLOW}Pulling latest image from GitHub Container Registry...${NC}"
REGISTRY="ghcr.io"
IMAGE_NAME="fatan/daily-book-spring"
CONTAINER_NAME="daily-book-spring"

# Get user input for image tag (optional)
IMAGE_TAG="${1:-latest}"

echo "Using image tag: $IMAGE_TAG"
echo ""

# Pull image
echo -e "${GREEN}Pulling image: ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}${NC}"
docker pull "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to pull image. Make sure you're logged into GitHub Container Registry.${NC}"
    echo "Run: docker login ghcr.io -u <your-username> -p <your-token>"
    exit 1
fi

# Stop existing container
echo -e "${GREEN}Stopping existing container...${NC}"
docker stop "$CONTAINER_NAME" 2>/dev/null || true
docker rm "$CONTAINER_NAME" 2>/dev/null || true

# Start new container with .env
ENV_FILE="${2:-.env}"
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: $ENV_FILE not found!${NC}"
    echo "Please create a .env file with your API keys."
    exit 1
fi

echo -e "${GREEN}Starting container with $ENV_FILE...${NC}"
docker run -d \
    --name "$CONTAINER_NAME" \
    -p 8080:8080 \
    --env-file "$ENV_FILE" \
    "${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

# Wait for startup
echo -e "${GREEN}Waiting for service to start...${NC}"
sleep 5

# Check health
for i in {1..10}; do
    if curl -s http://localhost:8080/api/books/health > /dev/null 2>&1; then
        echo -e "${GREEN}Service is ready!${NC}"
        break
    fi
    echo "  Waiting... ($i/10)"
    sleep 1
done

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}   Application Running!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo "Container: $CONTAINER_NAME"
echo "URL: http://localhost:8080"
echo "Image: ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
echo ""
echo "Test the API:"
echo "  curl http://localhost:8080/api/books"
echo "  curl http://localhost:8080/api/books/essay"
echo ""
echo "Send essay to Discord:"
echo "  curl -X POST http://localhost:8080/api/books/essay/discord"
echo ""
echo "View logs:"
echo "  docker logs -f $CONTAINER_NAME"
echo ""
echo "Stop container:"
echo "  docker stop $CONTAINER_NAME && docker rm $CONTAINER_NAME"
echo ""
