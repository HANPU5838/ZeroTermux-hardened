import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.net.URL
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
    id("com.android.application")
}

// ZeroTermux: bootstrap 变体（必须与 TermuxBootstrap.PackageVariant 对应，否则启动崩溃）
extra["packageVariant"] = System.getenv("TERMUX_PACKAGE_VARIANT") ?: "apt-android-7"

android {
    namespace = "com.termux"

    compileSdk = project.properties["compileSdkVersion"].toString().toInt()
    compileSdkMinor = project.properties["compileSdkMinorVersion"].toString().toInt()
    ndkVersion = System.getenv("JITPACK_NDK_VERSION") ?: project.properties["ndkVersion"].toString()

    val appVersionName = System.getenv("TERMUX_APP_VERSION_NAME") ?: ""

    defaultConfig {
        minSdk = project.properties["minSdkVersion"].toString().toInt()
        targetSdk = project.properties["targetSdkVersion"].toString().toInt()

        versionCode = 308
        versionName = "0.118.3.69"
        if (appVersionName.isNotEmpty()) versionName = appVersionName
        validateVersionName(versionName!!)

        buildConfigField("String", "TERMUX_PACKAGE_VARIANT", "\"" + extra["packageVariant"] + "\"")

        manifestPlaceholders["TERMUX_PACKAGE_NAME"] = "com.termux"
        manifestPlaceholders["TERMUX_APP_NAME"] = "Termux"
        manifestPlaceholders["TERMUX_API_APP_NAME"] = "Termux:API"
        manifestPlaceholders["TERMUX_BOOT_APP_NAME"] = "Termux:Boot"
        manifestPlaceholders["TERMUX_FLOAT_APP_NAME"] = "Termux:Float"
        manifestPlaceholders["TERMUX_STYLING_APP_NAME"] = "Termux:Styling"
        manifestPlaceholders["TERMUX_TASKER_APP_NAME"] = "Termux:Tasker"
        manifestPlaceholders["TERMUX_WIDGET_APP_NAME"] = "Termux:Widget"

        externalNativeBuild {
            ndkBuild {
                cFlags += listOf(
                    "-std=c11", "-Wall", "-Wextra", "-Werror", "-Os",
                    "-fno-stack-protector", "-Wl,--gc-sections"
                )
            }
        }
    }

    // 通过 -Darch={arm|arm64|x86|x86_64|all} 控制构建架构，默认 all 生成通用 APK
    val buildArch = System.getProperty("arch", "all")
    splits {
        abi {
            when (buildArch) {
                "universal" -> isEnable = false
                "all" -> {
                    isEnable = true
                    reset()
                    include("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
                    isUniversalApk = true
                }
                else -> {
                    isEnable = true
                    reset()
                    val abi = when (buildArch) {
                        "arm" -> "armeabi-v7a"
                        "arm64" -> "arm64-v8a"
                        "x86" -> "x86"
                        "x86_64" -> "x86_64"
                        else -> ""
                    }
                    include(abi)
                }
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEY_ALIAS") ?: "phone1"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "654321"
            storeFile = rootProject.file("phone.jks")
            storePassword = System.getenv("STORE_PASSWORD") ?: "123456"
        }
        maybeCreate("debug") {
            keyAlias = System.getenv("KEY_ALIAS") ?: "phone1"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "654321"
            storeFile = rootProject.file("phone.jks")
            storePassword = System.getenv("STORE_PASSWORD") ?: "123456"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false // Reproducible builds
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
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

    lint {
        disable.add("ProtectedPermissions")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/services/javax.annotation.processing.Processor"
            )
        }
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        val vn = android.defaultConfig.versionName
        variant.outputs.forEach { output ->
            val abi = output.filters.find { it.filterType.name == "ABI" }?.identifier
            output.outputFileName.set("ZeroTermux-" + vn + "-" + variant.buildType + "_" + (abi ?: "universal") + ".apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.viewpager)
    implementation(libs.androidx.material)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.documentfile)
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.linkify)
    implementation(libs.markwon.recycler)
    implementation(libs.guava.listenablefuture)

    implementation(project(":terminal-view"))
    implementation(project(":termux-shared"))

    implementation(fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation("cn.hotapk:fastandrutils:0.8.0") {
        exclude(group = "com.android.support")
    }
    implementation(libs.guava)
    implementation(libs.gson)
    implementation(libs.okgo)
    implementation(libs.switchbutton)

    implementation(libs.xxpermissions)
    implementation("com.daimajia.numberprogressbar:library:1.4@aar")

    implementation(libs.aria.core)
    annotationProcessor(libs.aria.compiler)

    implementation(libs.libsu.core)
    implementation(libs.libsu.service)
    implementation(libs.libsu.nio)
    implementation(libs.rootbeer)
    implementation(libs.libaums)
    implementation(libs.usbdeviceenumerator)
    implementation(libs.agentweb)
    implementation(libs.agentweb.download)
    implementation(libs.colorseekbar)
    implementation(libs.imagepicker)
    implementation(libs.smart.swipe)
    implementation(libs.smart.swipe.x)
    implementation(libs.glide)
    implementation(libs.immersionbar)
    implementation(libs.ddplist)
    implementation(libs.adblib)
    implementation(libs.commons.fileupload)
    implementation(libs.nanohttpd)
    implementation(libs.nanohttpd.websocket)
    api(libs.servlet.api)
    implementation(libs.un7zip)

    implementation(libs.androidx.swiperefreshlayout)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.zxing)
    implementation(libs.okhttp)

    implementation(libs.plugin.locale)

    implementation(libs.jsoup)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)

    coreLibraryDesugaring(libs.desugar)
    implementation(libs.editor)
    implementation(libs.editor.language.textmate)

    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.jcl.over.slf4j)
    implementation(libs.commons.net)
    implementation(files("libs/ftplet-api-1.1.1.jar"))
    implementation(files("libs/ftpserver-core-1.1.1-edited.jar"))
    implementation(files("libs/jlan-5.jar"))
    implementation(files("libs/mina-core-2.0.16.jar"))
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.reload4j)
    implementation(libs.reload4j)
    implementation(files("libs/commons-codec-1.10-rep.jar"))
}

tasks.register("versionName") {
    doLast {
        print(android.defaultConfig.versionName)
    }
}

fun validateVersionName(versionName: String) {
    // ZeroTermux: 语义化版本校验已关闭（上游逻辑保留在注释中）
}

fun downloadBootstrap(arch: String, expectedChecksum: String, version: String) {
    val digest = MessageDigest.getInstance("SHA-256")

    val localUrl = "src/main/cpp/bootstrap-$arch.zip"
    val file = File(projectDir, localUrl)
    if (file.exists()) {
        val buffer = ByteArray(8192)
        FileInputStream(file).use { input ->
            while (true) {
                val readBytes = input.read(buffer)
                if (readBytes < 0) break
                digest.update(buffer, 0, readBytes)
            }
        }
        var checksum = BigInteger(1, digest.digest()).toString(16)
        while (checksum.length < 64) {
            checksum = "0$checksum"
        }
        if (checksum == expectedChecksum) {
            return
        } else {
            logger.quiet("Deleting old local file with wrong hash: $localUrl: expected: $expectedChecksum, actual: $checksum")
            file.delete()
        }
    }

    val remoteUrl = "https://github.com/termux/termux-packages/releases/download/bootstrap-$version/bootstrap-$arch.zip"
    logger.quiet("Downloading $remoteUrl ...")

    file.parentFile.mkdirs()

    val connection = URL(remoteUrl).openConnection()
    connection.setInstanceFollowRedirects(true)
    BufferedOutputStream(FileOutputStream(file)).use { out ->
        DigestInputStream(connection.getInputStream(), digest).use { digestStream ->
            digestStream.copyTo(out)
        }
    }

    var checksum = BigInteger(1, digest.digest()).toString(16)
    while (checksum.length < 64) {
        checksum = "0$checksum"
    }
    if (checksum != expectedChecksum) {
        file.delete()
        throw GradleException("Wrong checksum for $remoteUrl: expected: $expectedChecksum, actual: $checksum")
    }
}

tasks.named("clean") {
    doLast {
        val tree = fileTree(File(projectDir, "src/main/cpp"))
        tree.include("bootstrap-*.zip")
        tree.forEach { it.delete() }
    }
}

tasks.register("downloadBootstraps") {
    doLast {
        // 上游 bootstrap 已升级到 2026.11.08-r1（Termux 官方每周滚动发布）
        val version = "2026.11.08-r1+apt.android-7"
        downloadBootstrap("aarch64", "ad0df6ac4e8524684ac0b25ccf41459b0aaa515da9557a238d497b44f9034e46", version)
        downloadBootstrap("arm", "12296b1e82c192ac8c59e69b2b18cd84c5f56c389c056f0502e227dbe841232e", version)
        downloadBootstrap("i686", "bb72be24209d636087faf6dbe742e17ac0dca17fc28cc13b1d235c983ca7dfd2", version)
        downloadBootstrap("x86_64", "41453bd2ec3f9690e2474085fbfe8d3406476b3cdd9627502d0ae4fb0d3eecb0", version)
    }
}

afterEvaluate {
    // 原生编译会把 bootstrap-*.zip 用 .incbin 直接嵌进 .so，执行时机比 Java 编译更早；
    // 只挂 JavaWithJavac 会漏，CI 上会报 "Could not find incbin file"。
    tasks.configureEach {
        val taskName = name
        if (taskName.endsWith("JavaWithJavac") ||
            taskName.endsWith("JavaWithJavacCreation") ||
            taskName == "preBuild" ||
            taskName.lowercase().contains("externalnativebuild")
        ) {
            dependsOn(tasks.named("downloadBootstraps"))
        }
    }
}
