import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.compose.components.resources)
}

compose.desktop {
    application {
        mainClass = "com.wpt.wptaccount.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "WPT Account"
            packageVersion = "1.0.2"
            description = "WPT Account Management System"
            copyright = "© 2026 WPT"
            vendor = "WPT"
            
            windows {
                menu = true
                shortcut = true
                menuGroup = "WPT Account"
                iconFile.set(project.file("src/main/resources/icon.ico"))
                upgradeUuid = "550e8400-e29b-41d4-a716-446655440000"
            }
        }
    }
}