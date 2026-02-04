@echo off
REM Deployment script for oms-v6 to AWS EKS (Windows)

setlocal EnableDelayedExpansion

SET PROJECT_NAME=oms-v6
SET AWS_REGION=us-east-1
SET ECR_REGISTRY=<YOUR_AWS_ACCOUNT_ID>.dkr.ecr.%AWS_REGION%.amazonaws.com
SET IMAGE_NAME=%ECR_REGISTRY%/%PROJECT_NAME%
SET EKS_CLUSTER_NAME=<YOUR_EKS_CLUSTER_NAME>

echo ==========================================
echo Deploying %PROJECT_NAME% to AWS EKS
echo ==========================================

echo Step 1: Building Spring Boot application...
call mvn clean package -DskipTests
if %ERRORLEVEL% NEQ 0 goto error

echo Step 2: Building Docker image...
docker build -t %PROJECT_NAME%:latest .
if %ERRORLEVEL% NEQ 0 goto error

echo Step 3: Authenticating with AWS ECR...
for /f "tokens=*" %%i in ('aws ecr get-login-password --region %AWS_REGION%') do set ECR_PASSWORD=%%i
echo %ECR_PASSWORD% | docker login --username AWS --password-stdin %ECR_REGISTRY%
if %ERRORLEVEL% NEQ 0 goto error

echo Step 4: Pushing image to ECR...
docker tag %PROJECT_NAME%:latest %IMAGE_NAME%:latest
docker push %IMAGE_NAME%:latest
if %ERRORLEVEL% NEQ 0 goto error

echo Step 5: Updating kubeconfig...
aws eks update-kubeconfig --name %EKS_CLUSTER_NAME% --region %AWS_REGION%
if %ERRORLEVEL% NEQ 0 goto error

echo Step 6: Deploying to Kubernetes...
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml -n %PROJECT_NAME%-namespace
kubectl apply -f k8s/deployment.yaml -n %PROJECT_NAME%-namespace
kubectl apply -f k8s/service.yaml -n %PROJECT_NAME%-namespace
kubectl apply -f k8s/hpa.yaml -n %PROJECT_NAME%-namespace
if %ERRORLEVEL% NEQ 0 goto error

echo ==========================================
echo Deployment completed successfully!
echo ==========================================
kubectl get all -n %PROJECT_NAME%-namespace
goto end

:error
echo ==========================================
echo ERROR: Deployment failed!
echo ==========================================
exit /b 1

:end
endlocal
