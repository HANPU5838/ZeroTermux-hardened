import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.termux.view"

    compileSdk = project.properties["compileSdkVersion"].toString().toInt()
    compileSdkMinor = project.properties["compileSdkMinorVersion"].toString().toInt()

    defaultConfig {
        minSdk = project.properties["minSdkVersion"].toString().toInt()
        targetSdk = project.properties["targetSdkVersion"].toString().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    implementation(libs.androidx.annotation)
    api(project(":terminal-emulator"))
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
                artifactId = "terminal-view"
                version = "0.118.3.58"
                artifact(tasks.named("sourceJar").get())
            }
        }
    }
}
