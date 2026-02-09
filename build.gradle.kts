plugins {
    kotlin("jvm") version "2.2.0"
}

group = "net.crepe"
version = "1.2.0"

repositories {
    mavenCentral()
    maven { url = uri("https://www.cursemaven.com") }
}

dependencies {
    testImplementation(kotlin("test"))
    
    compileOnly(files("libs/HytaleServer.jar"))
    // Project ID: 1431415
    compileOnly("curse.maven:hyui-1431415:7567866")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "plugin_group" to (project.findProperty("plugin_group") ?: ""),
        "plugin_name" to (project.findProperty("plugin_name") ?: ""),
        "plugin_version" to project.version.toString(),
        "server_version" to (project.findProperty("server_version") ?: ""),

        "plugin_description" to (project.findProperty("plugin_description") ?: ""),
        "plugin_website" to (project.findProperty("plugin_website") ?: ""),

        "plugin_main_entrypoint" to (project.findProperty("plugin_main_entrypoint") ?: ""),
        "plugin_author" to (project.findProperty("plugin_author") ?: "")
    )

    inputs.properties(replaceProperties)

    filesMatching("manifest.json") {
        expand(replaceProperties)
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "net.crepe.SimpleDrawerPlugin"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}