plugins {
    application
    java
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.guava) // adjust if you have a version catalog
    implementation("com.formdev:flatlaf:3.6.2")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter("5.12.1")
        }
    }
}

application {
    mainClass.set("org.example.App")
}

// ========================
// Paths and OS detection
// ========================
val ninjaDir = layout.buildDirectory.dir("ninja-rust").get().asFile
val nativeOutputDir = file("src/main/resources/native")

val os = org.gradle.internal.os.OperatingSystem.current()
val libPattern = when {
    os.isWindows -> "*.dll"
    os.isMacOsX -> "*.dylib"
    else -> "*.so"
}

// ========================
// Clone or update Ninja repo (two tasks)
// ========================
val cloneNinjaRepo = tasks.register<Exec>("cloneNinjaRepo") {
    description = "Clone the ninja-rust repository if not present."
    workingDir = buildDir
    commandLine("git", "clone", "https://github.com/yourusername/ninja.git", ninjaDir.absolutePath)
    onlyIf { !ninjaDir.exists() }
}

val pullNinjaRepo = tasks.register<Exec>("pullNinjaRepo") {
    description = "Pull latest changes for ninja-rust repository."
    workingDir = ninjaDir
    commandLine("git", "pull")
    onlyIf { ninjaDir.exists() }
}

// ========================
// Build Rust library
// ========================
val buildRustLib = tasks.register<Exec>("buildRustLib") {
    description = "Build the Rust library in release mode."
    dependsOn(cloneNinjaRepo, pullNinjaRepo)
    workingDir = ninjaDir
    commandLine("cargo", "build", "--release")
}

// ========================
// Copy compiled library to Java resources
// ========================
val copyRustLib = tasks.register<Copy>("copyRustLib") {
    description = "Copy the compiled Rust shared library into Java resources."
    dependsOn(buildRustLib)
    from(ninjaDir.resolve("target/release")) {
        include(libPattern)
    }
    into(nativeOutputDir)
}

// ========================
// Hooking into Java compile
// ========================
tasks.named("compileJava") {
    dependsOn(copyRustLib)
}

// ========================
// Clean up the cloned repo on clean
// ========================
tasks.named("clean") {
    doLast {
        delete(ninjaDir)
    }
}
