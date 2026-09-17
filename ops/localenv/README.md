# ROBOPILOT localenv 재구축 자료 (2026-09-17)

이 폴더가 개인 Windows PC의 작업 파일을 대신하는 공유 재구축 자료다.
현재 Ubuntu 서버의 설정/운영 도구를 읽기 전용으로 수집하고 비밀값을 제거했다.
과거 일회성 patch/migration 스크립트를 재실행할 필요는 없다.
실제 서비스 변경이나 완전한 신규 설치 시험은 이번 자료 게시에서 수행하지 않았다.

## 소스 기준

- Frontend dev `4476dbfd452ead29ba3b695dedbcac90b99199c5` → 소스 보존 `6243665f9bc2ab7f3867ff1faeda36a77cdcb4a3`
- Backend dev `533235ec0a13da1b98fc56edae49d40a77ed524e` → 소스 보존 `8a523192fc96df862ea12653c1a7f6704f8b1c30`
- 이 폴더는 Backend localenv의 후속 문서/설정 커밋이다. 두 소스 보존 커밋 이후 최신 localenv를 받아야 이 자료도 포함된다.
- [Frontend localenv](https://github.com/ashwinmendhe-ui/test_frontend/tree/localenv)
- [통합 가이드](https://dhive.atlassian.net/wiki/x/GwAnFQ)
- [DB 초기화 가이드](https://dhive.atlassian.net/wiki/spaces/DHive/pages/354418740)

## 포함 자료와 범위

| 위치 | 용도 |
|---|---|
| compose.yaml.example | 6개 서비스, 포트, 네트워크, 볼륨 |
| migration/*.example | DB/Backend 환경변수 및 EMQX JWT 인증 템플릿 |
| external-web/*.conf* | HTTPS/API/WebSocket/HLS 및 최초 인증서 발급용 HTTP 설정 |
| aws-tools/aws_connection_supervisor_20260911.py | Roles Anywhere 자격증명 갱신, SSM 재연결, TCP bridge |
| aws-tools/*.config.example | 서버 credential_process와 Backend 읽기 설정 |
| aws-tools/server-identity/{policy,trust}.json | 현재 서버의 제한된 AWS IAM 정책 기준 |
| https-ip/renew.sh | HTTPS 인증서 갱신 및 Nginx reload |
| server-identity-ca/* | CA 설정 예시, leaf 확장, 서버 인증서 갱신 도구 (키 없음) |
| database/schema.sql | PostgreSQL 14의 14개 테이블/제약/인덱스 구조, 데이터 없음 |
| database/roles.sql | 신규 DB용 역할 3개 |
| frontend.production.env.example | Frontend 빌드 설정 예시 |
| MANIFEST.json, check-package.py | 파일 해시 및 정적 점검 |

현재 서버 경로 `/home/dhiveserver/robopilot-local-20260911`, IP `112.219.173.114`, Docker gateway `172.18.0.1`, UID/GID `1000:1000` 기준이다.
새 서버가 다르면 Compose·supervisor·AWS config·renew.sh·Nginx·환경파일의 경로/IP/권한을 함께 변경한다.
이는 개인 PC 경로 의존성이 아니라 배포 대상 설정이다.
Compose 예시는 수집본에서 Backend 이미지 이름을 재빌드용으로 변경하고 영상 마운트를 `data/stream-archive`로 일반화했다.
사람의 SSO 캐시인 `/home/dhiveserver/.aws` 마운트는 템플릿에서 제외했다.
변경된 템플릿 전체의 신규 서버 통합 기동은 아직 미검증이다.

## 1. 빈 설치 디렉터리 준비

현재 운영 서버에 아래를 덮어쓰지 않는다. 새로운 서버/검증 환경의 예시다.
Git, Docker Engine/Compose, Python 3, OpenSSL, cron, flock, unzip, curl이 필요하다.

```bash
git clone --branch localenv https://github.com/ashwinmendhe-ui/test_backend.git
git clone --branch localenv https://github.com/ashwinmendhe-ui/test_frontend.git
python3 test_backend/ops/localenv/check-package.py
# 새 설치 위치만 사용. 기존 서비스가 있으면 별도 검증 루트를 정한다.
R=/home/dhiveserver/robopilot-local-20260911
mkdir -p "$R"
cp -a test_backend/ops/localenv/. "$R/"
cd "$R"
cp compose.yaml.example compose.yaml
cp migration/backend.env.example migration/backend.env
cp migration/db.env.example migration/db.env
cp migration/emqx.conf.example migration/emqx.conf
mkdir -p external-web/html data/stream-archive aws-tools/auto-runtime aws-tools/bin aws-tools/server-identity https-ip/letsencrypt
cp aws-tools/backend-credentials.config.example aws-tools/auto-runtime/config
cp aws-tools/server.config.example aws-tools/robopilot-sso.config
chmod 700 migration aws-tools/auto-runtime aws-tools/server-identity server-identity-ca
chmod 600 migration/*.env migration/emqx.conf aws-tools/robopilot-sso.config
```

`REPLACE_*` 값을 실제 새 값으로 채운다. DB_PASSWORD와 POSTGRES_PASSWORD는 일치해야 한다.
JWT_SECRET와 JWT_MQTT_SECRET는 각각 강한 새 비밀값으로 준비하고, EMQX secret은 Backend JWT_MQTT_SECRET와 일치시킨다.
EMQX node.cookie는 새 임의 값으로, device_id는 이 환경에 등록할 장비 UUID로 설정한다.
`${username}`은 EMQX 자체 표현식이므로 치환하지 않는다. issuer는 Backend가 발급하는 값과 맞춘다.
내부 listener 1884는 인증 없는 Backend 전용이므로 호스트에 공개하지 않는다.
현재 토픽 ACL은 세분화되지 않았으므로 추가 장비/다중 고객 운영 시 별도 정책 검토가 필요하다.

## 2. DB 초기화 또는 기존 백업 복원

```bash
docker compose -p robopilot-local up -d db redis mqtt
docker compose -p robopilot-local exec -T db pg_isready -U postgres
# 새 빈 DB만! schema.sql은 기존 DB 업데이트용 migration이 아니다.
docker compose -p robopilot-local exec -T db psql -v ON_ERROR_STOP=1 -U postgres -d dhive-main < database/schema.sql
docker compose -p robopilot-local exec -T db psql -v ON_ERROR_STOP=1 -U postgres -d dhive-main < database/roles.sql
```

POSTGRES_USER/DB를 변경했다면 위 명령도 맞춘다. schema.sql은 pg_dump 14.24 출력이므로 호환되는 psql을 사용한다.
기존 데이터를 복구할 때는 위 초기화 대신 승인된 백업을 **새 빈 DB**에 pg_restore로 복구한다.
신규 schema에는 회사·사용자·기체·작업·영상 데이터가 없다. Backend 실행 후 문서의 초기 관리자 생성 절차를 따른다.
register API는 비밀번호를 BCrypt 처리하지만 SYS_ADMIN 권한을 자동 부여하지 않는다.
관리자가 생성한 사용자에게 user_roles의 SYS_ADMIN 역할 연결을 승인하고, 회사·현장·장비를 등록한다.
`database/register-request.json.example`을 보호된 임시 파일로 복사하고 값을 채워, Backend를 시작한 뒤
`POST http://127.0.0.1:6791/api/v1/auth/register`에 JSON으로 제출한다. 응답에는 토큰이 있으므로 공유 로그에 남기지 않는다.
승인된 초기 관리자에 한해 아래처럼 역할을 부여한 뒤 재로그인한다(임시 요청 파일은 회사 비밀정보 정책에 따라 정리):

```bash
docker compose -p robopilot-local exec -T db psql -v ON_ERROR_STOP=1 -v admin_email='ACTUAL_ADMIN_EMAIL' -U postgres -d dhive-main < database/grant-initial-admin.sql
```
workspace UUID는 유효한 환경별 값을 사용하고, SN/payload/device UUID를 실제 DJI 데이터로 맞춘다.
과거 테스트 비밀번호/사용자 해시를 재사용하지 않는다.

## 3. AWS 서버 인증과 필수 실행 도구

AI/Qwen·MediaMTX·S3는 여전히 AWS에 있다. S3 영상 쓰기는 AWS AI 서비스가 수행한다.
현재 로컬 서버 역할은 stream 읽기 및 지정 EC2 AI 7879 터널만 허용한다. mission 업로드 버킷 값은 미구현 placeholder이다.
새 서버는 AWS 관리자가 서버별 인증서/역할/profile/trust anchor를 승인해야 한다.
`policy.json`, `trust.json`, `server.config.example`의 ARN, CN, account/region, EC2 ID, 세션 이름 및 버킷 범위를 검토해 맞춘다.
SSM 문서 ROBOPILOT-Local100-AI7879는 AWS-StartPortForwardingSession 기반이며 portNumber default=7879, allowedPattern=^7879$로 제한된 Session 문서다.
Roles Anywhere profile은 3600초, acceptRoleSessionName=true이고 trust는 해당 CA anchor와 인증서 CN으로 제한한다.
자기 세션 권한 prefix와 helper --role-session-name이 일치해야 한다.

새 전용 CA를 승인받아 생성하는 환경의 OpenSSL 예시 (기존 파일 덮어쓰기 금지):

```bash
umask 077
openssl req -x509 -newkey rsa:3072 -nodes -sha256 -days 3650 -config server-identity-ca/ca.cnf -keyout server-identity-ca/ca.key -out server-identity-ca/ca.pem
openssl req -new -newkey rsa:3072 -nodes -sha256 -subj '/O=DHive/CN=robopilot-local-192-168-0-100' -keyout aws-tools/server-identity/server.key -out aws-tools/server-identity/server.csr
openssl x509 -req -in aws-tools/server-identity/server.csr -CA server-identity-ca/ca.pem -CAkey server-identity-ca/ca.key -CAcreateserial -days 365 -sha256 -extfile server-identity-ca/leaf.ext -out aws-tools/server-identity/server.pem
```

관리자는 ca.pem(공개 인증서)으로 trust anchor를 구성하고 제한된 역할/profile을 연결한다.
ca.key는 Git/Confluence나 AWS 컨테이너에 넣지 않는다. 기존 기업 CA를 쓰면 해당 관리자의 발급/갱신 절차를 따른다.
renew.sh는 이 전용 CA와 CSR/key를 전제로 하므로 기업 CA 방식에서는 교체한다.

필요한 바이너리는 Git에 올리지 않고 공식 배포본에서 설치한다:
- [AWS CLI 설치](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html): 검증 당시 2.36.42. 실행 경로 `$R/aws-tools/bin/aws`. zip 설치 시 --install-dir "$R/aws-tools/aws-cli" --bin-dir "$R/aws-tools/bin"을 사용한다.
- [Session Manager plugin](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html): 검증 당시 1.2.835.0. Linux 실행 파일이 `$R/aws-tools/bin/session-manager-plugin`에서 실행되어야 한다. 외부 절대경로 symlink는 컨테이너에 전달되지 않으므로 aws-tools 안에 실제 도구가 있어야 한다.
- signing helper 1.8.5: https://rolesanywhere.amazonaws.com/releases/1.8.5/X86_64/Linux/Amzn2023/aws_signing_helper
  SHA256 `beec9ed1c492d93db809890f16713e3556353294b823c2184ad4e891f1b2b54d` 확인 후 `$R/aws-tools/server-identity/aws_signing_helper`에 설치/chmod 700.
  현재 Ubuntu의 glibc와 맞지 않아 host가 아닌 Compose의 python:3.12-slim 컨테이너에서 실행한다.

위 모든 파일을 준비한 뒤 aws-auto를 시작한다. 사용자 uid/gid가 1000이 아니면 Compose와 쓰기 디렉터리 권한을 맞춘다.

```bash
docker compose -p robopilot-local up -d aws-auto
cat aws-tools/auto-runtime/status.json
```

credentials=ok, tunnel=listening을 확인한다. credentials.json은 출력/공유하지 않는다.
Backend는 /run/robopilot-aws에 읽기 전용 마운트한 credential_process를 사용한다.
AI bridge는 172.18.0.1:17880 → 127.0.0.1:17879 → AWS :7879이며 인터넷에 공개하지 않는다.

## 4. 빌드와 HTTP 최초 기동

Frontend .env.production.local에 frontend.production.env.example을 참고해 승인된 DJI 설정을 넣는다.
VITE_*는 번들에 포함되므로 AWS 비밀키/서버 비밀번호를 넣지 않는다.
아래는 준비한 checkout의 실제 위치에서 실행한다. Node 24, Maven/Java는 Dockerfile 기준이다.

```bash
# test_backend/Cloud_Service/poc에서
docker build -t robopilot-localenv-backend:20260917 .
# test_frontend에서 Node 24로
npm ci
VITE_API_URL=https://112.219.173.114/api npm run build -- --outDir dist-localenv-candidate
```

Frontend 후보 결과를 새 설치 루트 external-web/html에 복사한다. 기존 배포본은 먼저 백업한다.
현재 운영 번들은 HTTPS URL을 직접 수정한 이력이 있으므로 이 소스 재빌드 결과를 운영 검증 완료본이라고 가정하지 않는다.
Backend Dockerfile은 테스트를 생략하므로 별도 LocalStreamArchiveTest를 수행한다(통합 가이드 참조).
로컬 영상은 `data/stream-archive/streams/...`에 S3 object key 구조로 보관한다. 비어 있으면 S3로 fallback한다.

인증서가 아직 없는 첫 설치에서는 HTTPS conf를 바로 쓰면 Nginx가 시작되지 않는다:

```bash
cd "$R"
cp external-web/acme-bootstrap.conf external-web/default.conf
docker compose -p robopilot-local config --quiet
docker compose -p robopilot-local up -d backend web
```

## 5. HTTPS 발급 및 갱신

공유기 외부 TCP 80/443이 이 서버로 연결되어야 한다. 18080은 HTTP 호환 경로이며 80의 ACME 검증을 대신하지 못한다.
아래는 운영에 사용할 공인 IP로 발급하는 명령이다. 이메일은 관리 연락처로 교체하고 약관은 관리자가 확인한다.

```bash
docker run --rm -it -v "$R/https-ip/letsencrypt:/etc/letsencrypt" -v "$R/external-web/html:/webroot" certbot/certbot@sha256:c23159d30afdd9c97960578aa4654f5901de6cae394958f894074dedd55e599d certonly --webroot -w /webroot --preferred-profile shortlived --ip-address 112.219.173.114 --email YOUR_ADMIN_EMAIL
cp external-web/default.conf.example external-web/default.conf
docker compose -p robopilot-local up -d --force-recreate web
docker exec robopilot-dev-web nginx -t
curl --fail https://112.219.173.114/api/health
```

인증서 디렉터리 이름이 다르면 Nginx 경로를 발급 결과에 맞춘다. 단일 파일 bind mount 교체 반영을 위해 web를 재생성한다.
[Certbot IP 인증서 공식 안내](https://letsencrypt.org/2026/03/11/shorter-certs-certbot.html)를 함께 참고한다.
renew.sh 두 파일의 루트/컨테이너명을 맞추고 실행권한을 부여한다. 사용자 crontab에 기존 항목을 유지하며 다음을 추가한다:

```cron
17 */6 * * * /home/dhiveserver/robopilot-local-20260911/https-ip/renew.sh >> /home/dhiveserver/robopilot-local-20260911/https-ip/renew.log 2>&1
41 3 * * * /home/dhiveserver/robopilot-local-20260911/server-identity-ca/renew.sh >> /home/dhiveserver/robopilot-local-20260911/server-identity-ca/renew.log 2>&1
```

Docker와 cron의 부팅 시작을 확인하고 HTTPS renew --dry-run을 수행한다.
웹 로그인, DJI 초기화/MQTT, 로컬 영상/S3 영상, 작업 시작과 실제 프레임 재생, 서버 재시작 복구를 검증한다.
동일 드론을 다른 Backend에서 동시에 조작하지 않는다.

## 공유 저장소에 넣지 않는 자료와 복구 책임

개인 PC는 필수 자료 보관소가 아니다. 운영 디스크만을 유일한 백업으로 삼지도 않는다.
- 비밀번호/JWT secret/DJI 라이선스: 회사 지정 비밀정보 저장소에서 승인된 관리자에게 전달하거나 신규 발급한다.
- 서버/CA 개인키: 회사의 접근 제한된 비밀정보 백업에 보관하거나 새 신원으로 재발급한다. Confluence/Git 첨부 금지.
- 사용자·작업 이력 DB, 실제 영상: 회사 관리 백업 저장소에 별도 보관한다. 새 빈 환경 설치에는 필요 없지만 과거 이력 복구에는 필요하다.
- 신규 설치는 여기의 schema/역할로 시작할 수 있다. 과거 영상과 동일 데이터 복구를 원하면 별도 백업이 필요하다.
- 현재 회사 중앙 백업 저장소 경로/담당자는 이 작업에서 확인되지 않았다. 기존 자료가 중앙 백업되었다고 간주하지 않는다.
- 통합 가이드와 이 Git 디렉터리가 설치 설명과 비밀값 없는 실행 자료의 기준이다. Windows 작업 경로의 파일을 요청할 필요는 없다.

## 검증 한계

해시/Python 구문/비밀 패턴 점검, 운영 설정 대조, DB dump의 schema-only 여부, docker compose config --quiet 및 두 갱신 스크립트의 sh -n 검사를 통과했다.
새 서버에서의 전체 통합 설치, CA 신규 발급, 새 Frontend 빌드와 DJI 연동 검증은 이 게시 작업에서 실행하지 않았다.
임의 PC 자료 전체 업로드나 운영 설정 변경은 하지 않았다. 파일별 해시는 MANIFEST.json을 확인한다.
