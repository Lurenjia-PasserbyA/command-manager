plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    // Shadow: 把 JavaFX + Jackson 一起打进单个可执行 jar。
    // 9.x 是 com.gradleup.shadow（8.3 之后从 com.github.johnrengelman.shadow 改名过来）
    id("com.gradleup.shadow") version "9.6.1"
}

group = "org.passerbya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass.set("org.passerbya.Main")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// ---------- Fat jar ----------
// ./gradlew fatJar  ->  build/libs/command-manager-1.0-SNAPSHOT-all.jar
//
// JavaFX 的原生库（.dll）就打包在 javafx-graphics-*-win.jar 里面，JavaFX 运行时
// 会自己把它们解压到临时目录再用，所以 fat jar 不需要额外处理原生库。
// 前提是平台 jar（-win）在依赖图里 —— JavaFX 插件会按当前系统自动加上。
tasks.shadowJar {
    archiveClassifier.set("all")

    manifest {
        attributes(
            "Main-Class" to "org.passerbya.Main",
            "Implementation-Title" to "Command Manager",
            "Implementation-Version" to project.version,
        )
    }

    // 合并 ServiceLoader 的 provider 文件。
    // 不合并的话，多个依赖各自带 META-INF/services 时只会剩一个，运行期才发现缺实现。
    mergeServiceFiles()

    // module-info.class 在 fat jar 里是多余的，而且多个模块的会互相冲突
    exclude("module-info.class", "META-INF/versions/*/module-info.class")

    // 依赖 jar 的签名文件必须去掉，否则合并后 JVM 会报
    // "Invalid signature file digest for Manifest main attributes"
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

// 用一个语义清楚的名字，省得每次都要写 shadowJar
tasks.register("fatJar") {
    group = "build"
    description = "打包成单个可执行 jar（含 JavaFX 与 Jackson）"
    dependsOn(tasks.shadowJar)
}
