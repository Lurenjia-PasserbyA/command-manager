plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.gradleup.shadow") version "9.2.0"
}

group = "org.passerbya"
version = "0.02.0000-alpha"

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.passerbya.MainGUI")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0-rc2")
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("net.java.dev.jna:jna-platform:5.19.1")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// shadow 插件会自动把 JavaFX 和所有依赖都打进 JAR
// 执行 ./gradlew shadowJar 即可
tasks.shadowJar {
    archiveClassifier.set("fat")
    manifest {
        attributes["Main-Class"] = "org.passerbya.MainGUI"
    }
}