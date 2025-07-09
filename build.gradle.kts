val ktor_version: String by project

plugins {
    kotlin("multiplatform") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    id("app.cash.sqldelight") version "2.1.0"
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

kotlin {
    val hostOs = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    val nativeTarget = when {
        hostOs == "Mac OS X" && arch == "x86_64" -> macosX64("native")
        hostOs == "Mac OS X" && arch == "aarch64" -> macosArm64("native")
        hostOs == "Linux" -> linuxX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
//                if (hostOs == "Linux") {
//                    linkerOpts(
//                        "--allow-shlib-undefined",
//                        "-L/usr/lib/x86_64-linux-gnu",
//                        "-lsqlite3"
//                    )
//                }
            }
        }
    }
    sourceSets {
        val nativeMain by getting {
            dependencies {
                implementation("io.ktor:ktor-serialization-kotlinx-json:${ktor_version}")
                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-cio:$ktor_version")
                implementation("io.ktor:ktor-server-content-negotiation:${ktor_version}")

                implementation("io.ktor:ktor-client-core:${ktor_version}")
                implementation("io.ktor:ktor-client-cio:${ktor_version}")
                implementation("io.ktor:ktor-client-content-negotiation:${ktor_version}")

                implementation("app.cash.sqldelight:coroutines-extensions:2.1.0")
                implementation("io.github.smyrgeorge:sqlx4k-postgres:0.61.0")
                implementation("io.github.smyrgeorge:sqlx4k-sqldelight:0.46.0")
            }
        }
        val nativeTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.ktor:ktor-server-test-host:$ktor_version")
            }
        }
    }
}

sqldelight {
    linkSqlite = false
    databases.register("AppDatabase") {
        generateAsync = true
        packageName = "rinha"
        dialect("io.github.smyrgeorge:sqlx4k-sqldelight-dialect-postgres:0.46.0")
    }
}