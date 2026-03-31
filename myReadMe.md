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

