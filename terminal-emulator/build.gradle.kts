import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.termux.emulator"

    compileSdk = project.properties["compileSdkVersion"].toString().toInt()
    compileSdkMinor = project.properties["compileSdkMinorVersion"].toString().toInt()
    ndkVersion = System.getenv("JITPACK_NDK_VERSION") ?: project.properties["ndkVersion"].toString()

    defaultConfig {
        minSdk = project.properties["minSdkVersion"].toString().toInt()
        targetSdk = project.properties["targetSdkVersion"].toString().toInt()

        externalNativeBuild {
            ndkBuild {
                cFlags += listOf(
                    "-std=c11", "-Wall", "-Wextra", "-Werror", "-Os",
                    "-fno-stack-protector", "-Wl,--gc-sections"
                )
            }
        }

        ndk {
            val buildArch = System.getProperty("arch", "all")
            if (buildArch == "universal" || buildArch == "all") {
                abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
            } else {
                val abi = when (buildArch) {
                    "arm" -> "armeabi-v7a"
                    "arm64" -> "arm64-v8a"
                    "x86" -> "x86"
                    "x86_64" -> "x86_64"
                    else -> ""
                }
                abiFilters += abi
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    publishing {
        multipleVariants {
            withSourcesJar()
            withJavadocJar()
            allVariants()
        }
    }
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("started", "passed", "skipped", "failed")
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    testImplementation(libs.junit)
}

tasks.register<Jar>("sourceJar") {
    from(android.sourceSets.getByName("main").java.srcDirs)
    archiveClassifier.set("sources")
}

afterEvaluate {
    publishing {
        publications {
            // Creates a Maven publication called "release".
            create<MavenPublication>("release") {
                from(components["default"])
                groupId = "com.termux"
                artifactId = "terminal-emulator"
                version = "0.118.3.58"
                artifact(tasks.named("sourceJar").get())
            }
        }
    }
}
