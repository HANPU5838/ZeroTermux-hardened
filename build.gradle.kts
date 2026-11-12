import org.gradle.api.tasks.Delete

buildscript {
    repositories {
        mavenCentral()
        google()
        // ZeroTermux add {@
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.eclipse.org/content/repositories/paho-snapshots/") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        // @}
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.5.1")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.eclipse.org/content/repositories/paho-snapshots/") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

// ZeroTermux: 所有模块的 build 产物统一输出到项目内「垃圾箱/build」，保持工程根目录干净
allprojects {
    layout.buildDirectory.set(rootProject.file("垃圾箱/build/$name"))
}

tasks.register<Delete>("clean") {
    delete(rootProject.file("垃圾箱/build"))
}
