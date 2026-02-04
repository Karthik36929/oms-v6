#!/bin/bash
# Deployment script for oms-v6 to AWS EKS

set -e

# Configuration
PROJECT_NAME="oms-v6"
AWS_REGION="us-east-1"  # Change this to your AWS region
ECR_REGISTRY="<YOUR_AWS_ACCOUNT_ID>.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_NAME="${ECR_REGISTRY}/${PROJECT_NAME}"
EKS_CLUSTER_NAME="<YOUR_EKS_CLUSTER_NAME>"

echo "=========================================="
echo "Deploying ${PROJECT_NAME} to AWS EKS"
echo "=========================================="

# Step 1: Build the application
echo "Step 1: Building Spring Boot application..."
mvn clean package -DskipTests
echo "✅ Build completed"

# Step 2: Build Docker image
echo "Step 2: Building Docker image..."
docker build -t ${PROJECT_NAME}:latest .
echo "✅ Docker image built"

# Step 3: Authenticate with ECR
echo "Step 3: Authenticating with AWS ECR..."
aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
echo "✅ ECR authentication successful"

# Step 4: Create ECR repository if it doesn't exist
echo "Step 4: Checking ECR repository..."
aws ecr describe-repositories --repository-names ${PROJECT_NAME} --region ${AWS_REGION} 2>/dev/null || \
  aws ecr create-repository --repository-name ${PROJECT_NAME} --region ${AWS_REGION}
echo "✅ ECR repository ready"

# Step 5: Tag and push image
echo "Step 5: Pushing image to ECR..."
docker tag ${PROJECT_NAME}:latest ${IMAGE_NAME}:latest
docker tag ${PROJECT_NAME}:latest ${IMAGE_NAME}:$(date +%Y%m%d-%H%M%S)
docker push ${IMAGE_NAME}:latest
docker push ${IMAGE_NAME}:$(date +%Y%m%d-%H%M%S)
echo "✅ Image pushed to ECR"

# Step 6: Update kubeconfig
echo "Step 6: Updating kubeconfig..."
aws eks update-kubeconfig --name ${EKS_CLUSTER_NAME} --region ${AWS_REGION}
echo "✅ Kubeconfig updated"

# Step 7: Update deployment YAML with ECR registry
echo "Step 7: Updating Kubernetes manifests..."
sed -i "s|<YOUR_ECR_REGISTRY>|${ECR_REGISTRY}|g" k8s/deployment.yaml
echo "✅ Manifests updated"

# Step 8: Apply Kubernetes manifests
echo "Step 8: Deploying to Kubernetes..."
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml -n ${PROJECT_NAME}-namespace
kubectl apply -f k8s/deployment.yaml -n ${PROJECT_NAME}-namespace
kubectl apply -f k8s/service.yaml -n ${PROJECT_NAME}-namespace
kubectl apply -f k8s/hpa.yaml -n ${PROJECT_NAME}-namespace
echo "✅ Kubernetes resources applied"

# Step 9: Wait for deployment to be ready
echo "Step 9: Waiting for deployment to be ready..."
kubectl rollout status deployment/${PROJECT_NAME}-deployment -n ${PROJECT_NAME}-namespace --timeout=5m
echo "✅ Deployment ready"

# Step 10: Get service details
echo "Step 10: Getting service information..."
echo ""
echo "=========================================="
echo "Deployment Summary"
echo "=========================================="
kubectl get all -n ${PROJECT_NAME}-namespace
echo ""
echo "Service Endpoint:"
kubectl get svc ${PROJECT_NAME}-service -n ${PROJECT_NAME}-namespace -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
echo ""
echo "=========================================="
echo "✅ Deployment completed successfully!"
echo "=========================================="
