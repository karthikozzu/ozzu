# Ozzu API Stage Deploy - Quick Commands

## 1) Login to AWS
```bash
aws configure
aws sts get-caller-identity
```
Configure AWS CLI and verify your account access.

## 2) Set variables
```bash
export AWS_REGION=ap-south-1
export AWS_ACCOUNT_ID=238845541504
export ECR_REPO=ozzu-api-stage
export ECR_URI=$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO
```
Set reusable variables for ECR commands.

## 3) Login Docker to ECR
```bash
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
```
Authenticate Docker so it can push images to ECR.

## 4) Create ECR repo
```bash
aws ecr create-repository --repository-name $ECR_REPO --region $AWS_REGION
```
Create the ECR repository once if it does not already exist.

## 5) Create and enable buildx builder
```bash
docker buildx rm amd64builder || true
docker buildx create --name amd64builder --driver docker-container --use
docker buildx inspect --bootstrap
```
Prepare Docker buildx so Mac/Apple Silicon can build `linux/amd64` images for ECS.

## 6) Build and push image
```bash
docker buildx build \
  --platform linux/amd64 \
  --provenance=false \
  -t ${ECR_URI}:latest \
  --push .
```
Build the image for ECS-compatible amd64 and push it directly to ECR.

## 7) Verify image platform
```bash
docker buildx imagetools inspect $ECR_URI:latest
```
Confirm the pushed image contains `linux/amd64`.

## 8) Create deployment (first time in ECS Express Mode)
```text
AWS Console -> ECS -> Express Mode -> Create service
Image URI: 238845541504.dkr.ecr.ap-south-1.amazonaws.com/ozzu-api-stage:latest
Container port: 3001
Health check path: /actuator/health
Log group: /ecs/ozzu-api-stage
Task execution role: ecsTaskExecutionRole
Command: leave empty
```
Create the first ECS/Fargate deployment from the pushed ECR image.

## 9) Update deployment after local changes
```bash
aws ecs update-service --cluster default --service ozzu-stage-cluster --force-new-deployment --region ap-south-1
```
Trigger ECS to pull the latest image and roll a new deployment.

## 10) Check health
```bash
curl https://oz-5e60ebb7caee4d9c87cd51ea0ecfe49b.ecs.ap-south-1.on.aws/actuator/health
```
Verify the service is up and connected to PostgreSQL.

## 11) Check logs
```text
AWS Console -> CloudWatch -> Log groups -> /ecs/ozzu-api-stage
```
Open CloudWatch logs to confirm clean startup and debug failures.

## Important gotcha we hit
```text
Always use: --platform linux/amd64 --provenance=false
```
This avoids the `exec /usr/bin/sh: exec format error` issue on ECS caused by Apple Silicon image builds.
