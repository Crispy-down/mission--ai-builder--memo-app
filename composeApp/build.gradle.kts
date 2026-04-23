import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.openpdf)
            implementation(libs.openpdf.fontsExtra)
        }
    }
}


val iconsDir = project.file("icons")
val icoFile = File(iconsDir, "icon.ico")
val icnsFile = File(iconsDir, "icon.icns")
val pngFile = File(iconsDir, "icon.png")

tasks.register<JavaExec>("generateAppIcons") {
    group = "distribution"
    description = "Generate .ico, .icns, .png application icons for native distribution"

    val jvmMain = kotlin.jvm().compilations.getByName("main")
    classpath = files(jvmMain.output.classesDirs, jvmMain.runtimeDependencyFiles)
    mainClass.set("org.example.project.tools.IconGeneratorKt")
    args = listOf(iconsDir.absolutePath)

    dependsOn("compileKotlinJvm")
    outputs.files(icoFile, icnsFile, pngFile)
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Good Vibe Memo"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(icoFile)
                menuGroup = "Good Vibe Memo"
                upgradeUuid = "9E0D2F3A-4C7B-4F5A-9E5C-2C5B6F8F9D11"
            }
            macOS {
                iconFile.set(icnsFile)
                bundleID = "com.goodvibe.memo"
            }
            linux {
                iconFile.set(pngFile)
            }
        }
    }
}

afterEvaluate {
    tasks.matching { task ->
        task.name.startsWith("package") ||
            task.name == "createDistributable" ||
            task.name == "runDistributable"
    }.configureEach {
        dependsOn("generateAppIcons")
    }
}
