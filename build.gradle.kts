// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// ==================== CHANGELOG VALIDATION TASK ====================
tasks.register("validateChangelog") {
    description = "Validates that CHANGELOG_APP.md is updated with current version"
    doLast {
        val changelogFile = File(rootDir, "CHANGELOG_APP.md")
        val buildGradleFile = File(rootDir, "app/build.gradle.kts")
        
        if (!changelogFile.exists()) {
            throw GradleException("❌ CHANGELOG_APP.md not found!")
        }
        
        // Extract version from build.gradle.kts
        val buildGradleContent = buildGradleFile.readText()
        val versionNameRegex = """versionName\s*=\s*"([^"]+)"""".toRegex()
        val versionCodeRegex = """versionCode\s*=\s*(\d+)""".toRegex()
        
        val versionName = versionNameRegex.find(buildGradleContent)?.groupValues?.get(1)
            ?: throw GradleException("❌ versionName not found in build.gradle.kts")
        val versionCode = versionCodeRegex.find(buildGradleContent)?.groupValues?.get(1)
            ?: throw GradleException("❌ versionCode not found in build.gradle.kts")
        
        // Check if changelog has this version
        val changelogContent = changelogFile.readText()
        val changelogHasVersion = changelogContent.contains("## v$versionName")
        
        if (!changelogHasVersion) {
            throw GradleException("""
                ❌ CHANGELOG_APP.md is out of sync!
                
                📦 Version in build.gradle.kts: v$versionName (code: $versionCode)
                📝 Missing in CHANGELOG_APP.md
                
                Please update CHANGELOG_APP.md with:
                
                ## v$versionName — [Descripción de cambios]
                > 📅 ${java.time.LocalDate.now()} · [Resumen]
                
                ### ✨ Cambios
                - [Tu cambio aquí]
            """.trimIndent())
        }
        
        println("✅ Changelog is up to date (v$versionName - code: $versionCode)")
    }
}

// Hook preBuild para validar changelog
tasks.register("checkChangelogBeforeBuild") {
    dependsOn("validateChangelog")
    description = "Pre-build changelog validation"
}

// Ejecutar validación antes del build
project(":app").tasks.whenTaskAdded { task ->
    if (task.name == "preBuild") {
        task.dependsOn("checkChangelogBeforeBuild")
    }
}