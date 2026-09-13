import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.*

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

val calendar = Calendar.getInstance()
val month = calendar.get(Calendar.MONTH) + 1
val day = calendar.get(Calendar.DAY_OF_MONTH)
val hour = calendar.get(Calendar.HOUR_OF_DAY)
val minute = calendar.get(Calendar.MINUTE)
val buildNumber = day * 1000 + hour * 60 + minute
val dynamicVersion = "1.$month.$buildNumber"

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
            packageVersion = dynamicVersion
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