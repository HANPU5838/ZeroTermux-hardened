import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.termux.shared"

    compileSdk = project.properties["compileSdkVersion"].toString().toInt()
    compileSdkMinor = project.properties["compileSdkMinorVersion"].toString().toInt()
    ndkVersion = System.getenv("JITPACK_NDK_VERSION") ?: project.properties["ndkVersion"].toString()

    defaultConfig {
        minSdk = project.properties["minSdkVersion"].toString().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            ndkBuild {
                // 保持与上游一致的空 cppFlags
                cppFlags.addAll(listOf(""))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
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

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core)
    implementation(libs.androidx.material)
    implementation(libs.guava)
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.recycler)
    implementation(libs.hiddenapibypass)

    implementation(libs.androidx.window)

    // Do not increment version higher than 2.5 or there
    // will be runtime exceptions on android < 8
    // due to missing classes like java.nio.file.Path.
    implementation("commons-io:commons-io:2.5")

    implementation(project(":terminal-view"))

    implementation(libs.termux.am.library)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    coreLibraryDesugaring(libs.desugar)
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
                artifactId = "termux-shared"
                version = "0.118.3.58"
                artifact(tasks.named("sourceJar").get())
            }
        }
    }
}
