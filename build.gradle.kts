
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "com.felipebz.zpa"
version = "1.0.0-SNAPSHOT"

plugins {
    kotlin("jvm").version("2.4.21")
    application
    id("org.jreleaser") version "1.27.0"
    id("com.diffplug.spotless") version "8.10.4"
    id("org.sonarqube") version "7.5.0.8588"
}

sonar {
    properties {
        property("sonar.projectKey", "com.felipebz.zpa:zpa-forms-extractor")
        property("sonar.projectName", "ZPA Forms Extractor")
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        val lines = rootProject.file("LICENSE_HEADER").readLines().filter { it.isNotBlank() }
        licenseHeader(lines.joinToString("", prefix = "//\n", postfix = "//\n\n") { "// $it\n" })
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(fileTree("libs").include("*.jar"))
    implementation("com.github.ajalt:clikt:2.8.0")
    implementation("com.google.code.gson:gson:2.15.0")
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.assertj:assertj-core:3.27.7")
}

configurations {
    // include compileOnly dependencies during test
    testImplementation {
        extendsFrom(configurations.compileOnly.get())
    }
}

application {
    mainClass.set("zpa.forms.extractor.MainKt")
}

// The JDAPI jar is proprietary and is not bundled: users drop it into the distribution's "libs" folder.
tasks.startScripts {
    doLast {
        unixScript.writeText(unixScript.readText()
            .replaceFirst("CLASSPATH=\$APP_HOME/lib/", "CLASSPATH=\$APP_HOME/libs/*:\$APP_HOME/lib/"))
        windowsScript.writeText(windowsScript.readText()
            .replaceFirst("set CLASSPATH=%APP_HOME%\\lib\\", "set CLASSPATH=%APP_HOME%\\libs\\*;%APP_HOME%\\lib\\"))
    }
}

distributions {
    main {
        contents {
            from("libs") {
                include(".gitkeep")
                into("libs")
            }
        }
    }
}

jreleaser {
    project {
        description.set("A code extractor for Oracle Forms modules.")
        authors.set(listOf("felipebz"))
        license.set("Apache-2.0")
        links {
            homepage.set("https://zpa.felipebz.com")
        }
        inceptionYear.set("2019")
    }
    release {
        github {
            overwrite.set(true)
            draft.set(true)
            changelog {
                formatted.set(org.jreleaser.model.Active.ALWAYS)
                preset.set("conventional-commits")
                format.set("- {{commitShortHash}} {{commitTitle}}")
                contributors {
                    enabled.set(false)
                }
                hide {
                    uncategorized.set(true)
                }
            }
        }
    }
    distributions {
        create("zpa-forms-extractor") {
            artifact {
                path.set(file("build/distributions/{{distributionName}}-{{projectVersion}}.zip"))
            }
        }
    }
}