plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
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
        else -> throw GradleException("Unsupported OS")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "rinha.main"
                if (hostOs == "Linux") {
                    linkerOpts(
                        "--allow-shlib-undefined",
                        "-L/usr/lib/x86_64-linux-gnu",
                    )
                }
            }
        }
    }

    sourceSets {
        val nativeMain by getting {
            dependencies {
                // Ktor server
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
                implementation(libs.ktor.server.content.negotiation)

                // Ktor serialization
                implementation(libs.ktor.serialization.kotlinx.json)

                // Ktor client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)

                // Redis connection
                implementation(libs.kedis)
            }
        }
    }
}