// settings.gradle.kts
// Gradle이 빌드를 시작할 때 가장 먼저 읽는 파일이다.
// "이 빌드에 어떤 프로젝트(모듈)가 있는지"와 "빌드 전체에 쓰일 플러그인"을 정한다.
// 실제 의존성·컴파일 설정은 build.gradle.kts에 있다.

plugins {
    // foojay toolchain resolver
    // build.gradle.kts에서 `toolchain { languageVersion = 25 }`라고 적으면 Gradle은 JDK 25를 찾는다.
    // 이 플러그인이 있으면 PC에 JDK 25가 없을 때 foojay.io(OpenJDK 배포판 목록 API)에서 자동으로 내려받는다.
    // 왜: 이 Mac에는 JDK 27만 설치되어 있다. 직접 JDK 25를 깔지 않아도 누구나 같은 버전으로 빌드되게 하려는 것.
    // 주의: 이 플러그인이 없으면 "Cannot find a Java installation ... matching languageVersion=25" 에러로 빌드가 멈춘다.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// 루트 프로젝트 이름. 빌드 결과 jar 이름(back-0.0.1-SNAPSHOT.jar)에도 쓰인다.
rootProject.name = "back"
