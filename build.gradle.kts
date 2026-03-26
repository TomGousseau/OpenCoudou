plugins {
    id("java")
    id("idea")
    id("eclipse")
    kotlin("jvm") version "1.9.22"
    id("net.neoforged.moddev") version "2.0.28-beta"
}

val modId = "opencomputers"
val modVersion = "3.0.0"
val minecraftVersion = "1.21.4"
val neoVersion = "21.4.0-beta"

version = modVersion
group = "li.cil.oc"
base {
    archivesName.set(modId)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
}

neoForge {
    version = neoVersion
    
    parchment {
        mappingsVersion = "2024.11.17"
        minecraftVersion = "1.21.4"
    }
    
    runs {
        register("client") {
            client()
            gameDirectory.set(file("run"))
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("server") {
            server()
            gameDirectory.set(file("run"))
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("data") {
            data()
            programArguments.addAll(
                "--mod", modId,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets["main"])
        }
    }
}

sourceSets.main.configure {
    resources.srcDir("src/generated/resources")
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://www.cursemaven.com")
}

dependencies {
    // LuaJ for embedded Lua runtime
    implementation("org.luaj:luaj-jse:3.0.1")
    
    // Kotlin coroutines for async Lua execution
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    
    // JSON handling
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.withType<ProcessResources>().configureEach {
    val properties = mapOf(
        "minecraft_version" to minecraftVersion,
        "neo_version" to neoVersion,
        "mod_id" to modId,
        "mod_version" to modVersion,
        "mod_name" to "OpenComputers Rewrite",
        "mod_license" to "MIT",
        "mod_authors" to "Original: Sangar, Vexatos, payonel; Rewrite: Community",
        "mod_description" to "Modular computers and robots programmable in Lua"
    )
    inputs.properties(properties)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(properties)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
