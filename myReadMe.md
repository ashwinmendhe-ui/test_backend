## 1. install jdk 17
brew install openjdk@17

## then set path at ~/.zshrc file
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> /Users/mckinleyrice/.zshrc
export CPPFLAGS="-I/opt/homebrew/opt/openjdk@17/include"
source ~/.zshrc

sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-17.jdk

echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

## 2. install maven
brew install maven
mvn --version

## 3. install db , postgresql
brew install postgresql@14
brew services start postgresql@14
postgres --version

## 4. install redis
brew install redis
brew services start redis

## 5. install mqtt broker
brew install mosquitto
brew services start mosquitto
 # mosquitto runing manually using cmd
 mosquitto -v

# for checking all service in mac
brew services list

## 6. create project on spring initilizer
with spring 3.5.13 , java 17

## 7. change pom.xml (project object model)

## 8. Start postgres connection with current user
psql -U mckinleyrice
    # create 
    postgres=# CREATE ROLE postgres WITH LOGIN SUPERUSER PASSWORD 'Ashwin@11';
CREATE ROLE
postgres=# CREATE DATABASE "dhive-main" OWNER postgres;
CREATE DATABASE
postgres=# \q
mckinleyrice@Mckinleys-MacBook-Air ~ % 
# credential of postgres
## for connectiong 
psql -U postgres -d dhive-main
DB: dhive-main
User: postgres
Password: Ashwin@11

## 9. for running project 
cd Cloud_Service/poc
mvn spring-boot:run

Spring Boot 3.5.13 started
active profile is local
Tomcat started on port 6789
PostgreSQL connection is working
JPA initialized successfully
app fully started as DhiveApplication

Your backend is now running at:

http://localhost:6789

## 10. APIs that are directly accessible without token

These are allowed in SecurityConfig:

GET  /api/health
POST /api/v1/auth/register
POST /api/v1/auth/login

Meaning:

health check works without login
register works without token
login works without token
APIs that need token

## 11. Everything else currently needs authentication.

So for now, this one definitely needs JWT token:

GET /api/v1/test/me


## 12 when need to start app , goto
cd Cloud_Service/poc
mvn clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=local


## 13 after successful run , check health api
curl http://localhost:6789/api/health

## 14 register api
curl -i -X POST http://localhost:6789/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "one",
    "email": "one@example.com",
    "password": "Test@1one",
    "fullName": "One Test"
  }'

## 15. login api
curl -i -X POST http://localhost:6789/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "one@example.com",
    "password": "Test@1one"
  }'

## 16. if i want to run for difference env so need to create this files 
application-local.yml
application-dev.yml
application-stage.yml
application-prod.yml
# and in base file i.e. application.yml need to change config accordingly
# or For running different env we can use below command by changing env dev/stage/prod
 mvn clean install -DskipTests
 # above commnad not for always.
 mvn spring-boot:run -Dspring-boot.run.profiles=dev


 ## register
 curl -i -X POST http://localhost:6789/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "three",
    "email": "three@example.com",
    "password": "Three@3three",
    "fullName": "three three"
  }'

## login
curl -X POST http://localhost:6789/api/v1/auth/login \
-H "Content-Type: application/json" \
-d '{
  "email":"three@example.com",
  "password":"Three@3three"
}'


## test for user search
# login
# access with bearer token

curl -X GET "http://localhost:6789/api/v1/users/search" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aHJlZSIsInVzZXJJZCI6ImIyYmM3NDJkLTY5MDktNDQwZC05OTFjLTE5NWFhOGI2N2RlNiIsImVtYWlsIjoidGhyZWVAZXhhbXBsZS5jb20iLCJyb2xlcyI6WyJDT01QQU5ZX1VTRVIiXSwidG9rZW5fdHlwZSI6IkFDQ0VTUyIsImlhdCI6MTc3OTQ1NjcxMSwiZXhwIjoxNzc5NDYwMzExfQ.KOdmSCznQJwImexX8TIeAv4tfeIERU8XXrYyikkbzfo"


# search with two word
curl -X GET "http://localhost:6789/api/v1/users/search?keyword=two" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0aHJlZSIsInVzZXJJZCI6ImIyYmM3NDJkLTY5MDktNDQwZC05OTFjLTE5NWFhOGI2N2RlNiIsImVtYWlsIjoidGhyZWVAZXhhbXBsZS5jb20iLCJyb2xlcyI6WyJDT01QQU5ZX1VTRVIiXSwidG9rZW5fdHlwZSI6IkFDQ0VTUyIsImlhdCI6MTc3OTQ1NjcxMSwiZXhwIjoxNzc5NDYwMzExfQ.KOdmSCznQJwImexX8TIeAv4tfeIERU8XXrYyikkbzfo"


# swagger setup
http://localhost:6789/swagger-ui/index.html

# OpenAPI Doc
http://localhost:6789/v3/api-docs

## configure aws locally 
brew install awscli

aws --version

## for now we can connect temproriely by export keys in same teminal where server running
also check 
echo $AWS_ACCESS_KEY_ID

aws sts get-caller-identity

## we can also do
mvn clean compile

## for redis cli
redis-cli keys '*'
# also we can start terminal using
redis-cli
# its running on
127.0.0.1:6379

## for mosquitto
brew install mosquitto

# for running mosquitto, its running on 1883 port
mosquitto -v
# for 2nd terminal : testing purpose
mosquitto_sub -h localhost -p 1883 -t 'sys/product/+/status'
# 3rd terminal 
mosquitto_pub -h localhost -p 1883 \
-t 'sys/product/1234/status' \
-m '{"tid":"t1","bid":"b1","timestamp":1710000000,"data":{"sub_devices":[{"sn":"camera-001"}]}}'


## now test on BE API 
# 1. start mosquitto server for checking server lsof -i:1883
mosquitto -v
# 2. then send msg on topic , publisher
mosquitto_pub -h localhost -p 1883 \
-t 'sys/product/1234/status' \
-m '{"tid":"t1","bid":"b1","timestamp":1710000000,"data":{"sub_devices":[{"sn":"camera-001"}]}}'
# 3. Our BE server is the client , subscriber 
it will received this msg check logs 

### For stoping or offline the device send below lsg
# clean redis cache or send stop msg
redis-cli del online:1234
OR
mosquitto_pub -h localhost -p 1883 \
-t 'sys/product/1234/status' \
-m '{"tid":"t2","bid":"b2","timestamp":1710000001,"data":{}}'


## for health send status from robot/drone , in local testing
# 1. terminal start mosquitto server 
mosquitto -v

# before step two check
redis-cli keys '*'
# if anything just delete that by below command
redis-cli del online:1234
# 2. terminal our robot/drone as client publich some status 
mosquitto_pub -h localhost -p 1883 \
-t 'robot/1234/health' \
-m '{"schema":"robot-mqtt.v1","msg_id":"msg-1","robot_id":"1234","timestamp":"2026-06-05T10:00:00Z","data":{"online":true,"status":"IDLE"}}'

# 3. our spring-boot as client receives this. 



## same for offline the status
mosquitto_pub -h localhost -p 1883 \
-t 'robot/1234/health' \
-m '{"schema":"robot-mqtt.v1","msg_id":"msg-2","robot_id":"1234","timestamp":"2026-06-05T10:00:00Z","data":{"online":false}}'


## for check and test
mvn -q -DskipTests compile