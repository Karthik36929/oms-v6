#!/bin/bash
# Undeploy script for oms-v6 from AWS EKS

set -e

PROJECT_NAME="oms-v6"
AWS_REGION="us-east-1"
EKS_CLUSTER_NAME="<YOUR_EKS_CLUSTER_NAME>"

echo "=========================================="
echo "Undeploying ${PROJECT_NAME} from AWS EKS"
echo "=========================================="

# Update kubeconfig
aws eks update-kubeconfig --name ${EKS_CLUSTER_NAME} --region ${AWS_REGION}

# Delete all resources
kubectl delete -f k8s/hpa.yaml -n ${PROJECT_NAME}-namespace --ignore-not-found=true
kubectl delete -f k8s/service.yaml -n ${PROJECT_NAME}-namespace --ignore-not-found=true
kubectl delete -f k8s/deployment.yaml -n ${PROJECT_NAME}-namespace --ignore-not-found=true
kubectl delete -f k8s/configmap.yaml -n ${PROJECT_NAME}-namespace --ignore-not-found=true
kubectl delete -f k8s/namespace.yaml --ignore-not-found=true

echo "✅ All resources removed"
echo "=========================================="
