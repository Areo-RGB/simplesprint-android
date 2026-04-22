plugins {
    id("com.github.ben-manes.versions")
    id("io.gitlab.arturbosch.detekt") apply false
    id("org.jlleitschuh.gradle.ktlint") apply false
}

val appModule = ":app"

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    plugins.withId("com.android.application") {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    }
    plugins.withId("com.android.library") {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
    }
    plugins.withId("io.gitlab.arturbosch.detekt") {
        extensions.configure(io.gitlab.arturbosch.detekt.extensions.DetektExtension::class.java) {
            buildUponDefaultConfig = true
            allRules = false
            parallel = true
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            baseline = file("$projectDir/detekt-baseline.xml")
            basePath = rootDir.absolutePath
        }
        tasks.withType(io.gitlab.arturbosch.detekt.Detekt::class.java).configureEach {
            reports {
                xml.required.set(true)
                html.required.set(true)
                sarif.required.set(true)
                txt.required.set(false)
                md.required.set(false)
            }
        }
    }
    plugins.withId("org.jlleitschuh.gradle.ktlint") {
        extensions.configure(org.jlleitschuh.gradle.ktlint.KtlintExtension::class.java) {
            android.set(true)
            outputToConsole.set(true)
            ignoreFailures.set(false)
            baseline.set(file("$projectDir/ktlint-baseline.xml"))
            reporters {
                reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
                reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.SARIF)
            }
            filter {
                exclude("**/build/**")
                exclude("**/generated/**")
            }
        }
    }
}

tasks.register("agentQuickCheck") {
    group = "verification"
    description = "Runs fast style and static analysis checks for automated agents."
    dependsOn(
        "$appModule:ktlintCheck",
        "$appModule:detekt",
    )
}

tasks.register("agentCheck") {
    group = "verification"
    description = "Runs the full agent quality gate, including Android lint."
    dependsOn(
        "agentQuickCheck",
        "$appModule:lintDebug",
    )
}

tasks.register("agentFix") {
    group = "formatting"
    description = "Applies safe auto-fixes before re-running checks."
    dependsOn(
        "$appModule:ktlintFormat",
        "$appModule:lintFixDebug",
    )
}

tasks.register("agentRefreshBaselines") {
    group = "verification"
    description = "Regenerates detekt/ktlint baselines to re-snapshot accepted legacy issues."
    dependsOn(
        "$appModule:detektBaseline",
        "$appModule:ktlintGenerateBaseline",
    )
}

tasks.register("agentReportPaths") {
    group = "help"
    description = "Prints report locations for parser-friendly agent output collection."
    doLast {
        println("Ktlint reports: app/build/reports/ktlint/")
        println("Detekt reports: app/build/reports/detekt/")
        println("Android lint reports: app/build/reports/lint-results-debug.*")
    }
}

subprojects {
    project.evaluationDependsOn(":app")
    plugins.withId("com.android.library") {
        if (name == "camera_android_camerax") {
            dependencies.add(
                "implementation",
                "androidx.concurrent:concurrent-futures:1.2.0",
            )
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
