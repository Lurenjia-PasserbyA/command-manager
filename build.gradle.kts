plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "org.passerbya"
version = "1.00.0000-SNAPSHOT"

repositories {
    mavenCentral()
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0-rc2")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "org.passerbya.MainGUI"
}

tasks.withType<JavaExec> {
    val javafxModules = sourceSets["main"].runtimeClasspath
        .filter { it.name.contains("javafx") }
        .joinToString(File.pathSeparator) { it.absolutePath }

    jvmArgs = listOf(
        "--module-path", javafxModules,
        "--add-modules", "javafx.controls,javafx.fxml"
    )
}

tasks.processResources {
}