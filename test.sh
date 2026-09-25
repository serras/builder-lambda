set -e
./gradlew publish -PonlyLocal=true -Pversion=10.0-test
cd gradle-test
./gradlew build
cd ..