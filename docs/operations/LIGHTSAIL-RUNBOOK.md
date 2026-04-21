# Lightsail 운영 가이드

## 운영 기준

- 서버 경로: `/opt/app/melissa-BE`
- 배포 브랜치: `deploy/lightsail-test`
- 실행 방식: `systemd` 서비스
- 서비스명: `melissa`
- 환경변수 파일: `/opt/app/melissa-BE/.env`
- 실행 profile: `lightsail`

## 상태 확인

```bash
sudo systemctl status melissa --no-pager
```

```bash
sudo journalctl -u melissa -n 100 --no-pager
```

```bash
curl -i http://localhost:8080/health
```

`/health`가 동작하지 않으면 현재 애플리케이션 health endpoint를 확인한다.

## 배포 절차

```bash
cd /opt/app/melissa-BE
git fetch origin
git checkout deploy/lightsail-test
git pull origin deploy/lightsail-test
./gradlew clean bootJar -x test
sudo systemctl restart melissa
sudo systemctl status melissa --no-pager
```

배포 후 로그를 확인한다.

```bash
sudo journalctl -u melissa -n 100 --no-pager
```

## 환경변수 변경

환경변수는 서버의 `.env`에서 관리한다.

```bash
cd /opt/app/melissa-BE
sudo cp .env .env.bak
sudo nano .env
```

수정 후 서비스를 재시작한다.

```bash
sudo systemctl restart melissa
sudo systemctl status melissa --no-pager
```

## S3 Access Key 로테이션

대상 IAM 사용자: `s3-accessor`

1. AWS IAM에서 새 access key를 발급한다.
2. 기존 key는 아직 비활성화하지 않는다.
3. `/opt/app/melissa-BE/.env`의 값을 교체한다.

```env
AWS_ACCESS_KEY_ID=새 Access Key ID
AWS_SECRET_ACCESS_KEY=새 Secret Access Key
```

4. 서비스를 재시작한다.

```bash
sudo systemctl restart melissa
```

5. 이미지 생성 또는 S3 업로드/조회가 정상인지 확인한다.
6. 기존 access key를 `Inactive`로 변경한다.
7. 한 번 더 S3 동작을 확인한 뒤 기존 access key를 삭제한다.

## CloudWatch Agent

CloudWatch 커스텀 메트릭은 비용 절감을 위해 비활성화한다.
현재 CloudWatch 설정은 로그 수집만 유지한다.

Agent를 직접 중지해야 하는 경우:

```bash
sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a stop
```

## systemd 서비스 파일

서비스 파일 위치:

```bash
/etc/systemd/system/melissa.service
```

현재 기준 설정:

```ini
[Unit]
Description=Melissa Backend
After=network.target

[Service]
User=ubuntu
WorkingDirectory=/opt/app/melissa-BE
EnvironmentFile=/opt/app/melissa-BE/.env
ExecStart=/usr/bin/java -Dspring.profiles.active=lightsail -jar /opt/app/melissa-BE/build/libs/melissa-diary-assistant-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

서비스 파일 변경 후:

```bash
sudo systemctl daemon-reload
sudo systemctl restart melissa
```

서버 재부팅 시 자동 시작:

```bash
sudo systemctl enable melissa
```

## 임시 nohup 실행 금지

운영 환경에서는 아래 방식으로 직접 실행하지 않는다.

```bash
nohup java -jar ... &
```

프로세스 관리는 `systemd`를 기준으로 한다.
