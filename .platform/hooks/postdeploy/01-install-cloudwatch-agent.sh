#!/bin/bash
set -e

# CloudWatch Agent 설치 및 구성 스크립트
# 이 스크립트는 배포 완료 후 자동으로 실행됩니다.

AGENT_BIN="/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent"
AGENT_CTL="/opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl"
CONFIG_SOURCE="/var/app/current/.platform/cloudwatch/cloudwatch-config.json"
CONFIG_TARGET="/opt/aws/amazon-cloudwatch-agent/etc/config.json"

echo "=========================================="
echo "CloudWatch Agent Installation Script"
echo "=========================================="

# 1. CloudWatch Agent 설치 확인 및 설치
if [ ! -f "$AGENT_BIN" ]; then
    echo "[1/4] Downloading CloudWatch Agent..."
    wget -q https://s3.amazonaws.com/amazoncloudwatch-agent/amazon_linux/amd64/latest/amazon-cloudwatch-agent.rpm -O /tmp/amazon-cloudwatch-agent.rpm
    
    echo "[1/4] Installing CloudWatch Agent..."
    rpm -U /tmp/amazon-cloudwatch-agent.rpm
    rm -f /tmp/amazon-cloudwatch-agent.rpm
    echo "✓ CloudWatch Agent installed successfully"
else
    echo "[1/4] CloudWatch Agent already installed"
fi

# 2. Config 파일 복사
if [ ! -f "$CONFIG_SOURCE" ]; then
    echo "[2/4] ERROR: Config file not found at $CONFIG_SOURCE"
    echo "⚠️  Skipping CloudWatch Agent configuration"
    exit 0
fi

echo "[2/4] Copying CloudWatch configuration..."
mkdir -p /opt/aws/amazon-cloudwatch-agent/etc
cp "$CONFIG_SOURCE" "$CONFIG_TARGET"
echo "✓ Configuration copied successfully"

# 3. Agent 중지 (이미 실행 중인 경우)
echo "[3/4] Stopping existing CloudWatch Agent..."
$AGENT_CTL -a stop 2>/dev/null || echo "  (No running agent found)"

# 4. Agent 시작
echo "[4/4] Starting CloudWatch Agent with custom config..."
if $AGENT_CTL -a fetch-config -m ec2 -s -c "file:$CONFIG_TARGET" 2>&1; then
    echo "=========================================="
    echo "✓ CloudWatch Agent started successfully!"
    echo "  Namespace: Melissa/Backend"
    echo "  Metrics: Memory, Swap, Disk, CPU"
    echo "  Interval: 60 seconds"
    echo "=========================================="
else
    echo "=========================================="
    echo "⚠️  WARNING: CloudWatch Agent failed to start"
    echo ""
    echo "This is usually due to missing IAM permissions."
    echo "Please ensure the EC2 instance role has the following policy attached:"
    echo "  - CloudWatchAgentServerPolicy"
    echo ""
    echo "AWS Console: IAM → Roles → [EB Instance Role] → Attach policies"
    echo "=========================================="
    # 배포는 계속 진행 (Agent 실패로 앱 배포를 막지 않음)
    exit 0
fi

# 5. Agent 상태 확인
echo ""
echo "Agent Status:"
$AGENT_CTL -a query -m ec2 -c default -s || true

exit 0

