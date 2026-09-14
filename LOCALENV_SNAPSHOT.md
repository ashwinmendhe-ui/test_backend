# localenv 변경 보존 — 2026-09-14

현재 로컬 운영 소스를 원래 dev 커밋 위에 보존한 브랜치다. 최신 dev 병합, AWS/local 환경 분리, AWS 배포는 이번 작업에 포함하지 않았다. 이후 변경은 원본 dev와 PR로 상호 반영할 수 있도록 이력을 유지한다.

새 운영 서버 소스에는 .git이 없어 이전 테스트 서버의 dev 이력과 현재 운영 서버 소스를 대조했다. 비밀 환경파일, 개인키, AWS 자격증명, DB·영상 데이터, 의존성/빌드 산출물, .before 백업 및 RP-178 임시 진단 코드는 추가하지 않았다. 기존 저장소에 포함된 파일은 별도의 정리 대상으로 남겼다.

이 브랜치는 소스 보존본이며 전체 서버 복구 패키지가 아니다. Docker/Nginx/MQTT/AWS 인증 설정과 DB·영상 백업은 별도로 관리한다. 운영 서버와 AWS 배포는 변경하지 않았다.

## 기준과 보존 내용
- 기준 dev: 533235ec0a13da1b98fc56edae49d40a77ed524e.
- S3PresignService.java: app.storage.local-stream-dir에 파일이 있으면 로컬 파일을 먼저 읽고, 없으면 기존 S3 경로 사용.
- 정규화된 경로 및 실제 심볼릭 링크 경로가 저장 루트 밖으로 벗어나지 않도록 확인.
- LocalStreamArchiveTest.java: 로컬 읽기, 존재 확인, 상위 경로/심볼릭 링크 이탈 및 없는 파일 검증.

검증: 동일 운영 서버 소스에서 mvn -q -Dtest=LocalStreamArchiveTest test 성공. 환경 분리나 동작 변경을 추가하지 않았다.
