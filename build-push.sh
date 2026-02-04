#!/bin/bash
# Build and push Docker image for oms-v6

set -e

PROJECT_NAME="oms-v6"
AWS_REGION="us-east-1"
ECR_REGISTRY="<YOUR_AWS_ACCOUNT_ID>.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_NAME="${ECR_REGISTRY}/${PROJECT_NAME}"

echo "Building and pushing ${PROJECT_NAME}..."

# Build application
mvn clean package -DskipTests

# Build Docker image
docker build -t ${PROJECT_NAME}:latest .

# Login to ECR
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}

# Push image
docker tag ${PROJECT_NAME}:latest ${IMAGE_NAME}:latest
docker push ${IMAGE_NAME}:latest

echo "✅ Image pushed: ${IMAGE_NAME}:latest"
