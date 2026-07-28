plugins {
    id("dev.kikugie.loom-back-compat")
    id("maven-publish")
}

version = "${property("mod.version")}+${sc.current.version}"
group = property("mod.group") as String
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = JavaVersion.toVersion(sc.properties.get<String>("mod.java_major"))

val is1214 = sc.current.version == "1.21.4" || sc.current.version == "1.21.11"
val only1214 = sc.current.version == "1.21.4"
val only262 = sc.current.version == "26.2"
sc.replacements {
    regex {
        direction.set(only262)
        replace("""\.setScreen\(""", ".setScreenAndShow(", """\.setScreenAndShow\(""", ".setScreen(")
    }
    regex {
        direction.set(only262)
        replace("""\.getMainCamera\(\)""", ".mainCamera()", """\.mainCamera\(\)""", ".getMainCamera()")
    }
    string {
        direction.set(only1214)
        replace("Identifier", "ResourceLocation")
    }
    string {
        direction.set(is1214)
        replace("GuiGraphicsExtractor", "GuiGraphics")
    }
    string {
        direction.set(only1214)
        replace("AvatarRenderState", "PlayerRenderState")
    }
    regex {
        direction.set(only1214)
        replace("""AvatarRenderer(?!Mixin)""", "PlayerRenderer", """PlayerRenderer(?!Mixin)""", "AvatarRenderer")
    }
    string {
        direction.set(only1214)
        replace("RenderTypes", "RenderType")
    }
    string {
        direction.set(is1214)
        replace("KeyMappingHelper", "KeyBindingHelper")
    }
    string {
        direction.set(is1214)
        replace("registerKeyMapping", "registerKeyBinding")
    }
    regex {
        direction.set(is1214)
        replace("""\.text\(""", ".drawString(", """\.drawString\(""", ".text(")
    }
    regex {
        direction.set(is1214)
        replace("""\.centeredText\(""", ".drawCenteredString(", """\.drawCenteredString\(""", ".centeredText(")
    }
    regex {
        direction.set(is1214)
        replace("""\.outline\(""", ".renderOutline(", """\.renderOutline\(""", ".outline(")
    }
    regex {
        direction.set(is1214)
        replace("""\.itemDecorations\(""", ".renderItemDecorations(", """\.renderItemDecorations\(""", ".itemDecorations(")
    }
    regex {
        direction.set(only1214)
        replace("""\.position\(\)""", ".getPosition()", """\.getPosition\(\)""", ".position()")
    }
    regex {
        direction.set(only1214)
        replace("""\.xRot\(\)""", ".getXRot()", """\.getXRot\(\)""", ".xRot()")
    }
    regex {
        direction.set(only1214)
        replace("""\.yRot\(\)""", ".getYRot()", """\.getYRot\(\)""", ".yRot()")
    }
    regex {
        direction.set(only1214)
        replace("""getGameProfile\(\)\.name\(\)""", "getGameProfile().getName()", """getGameProfile\(\)\.getName\(\)""", "getGameProfile().name()")
    }
    regex {
        direction.set(only1214)
        replace("""getProfile\(\)\.name\(\)""", "getProfile().getName()", """getProfile\(\)\.getName\(\)""", "getProfile().name()")
    }
    regex {
        direction.set(only1214)
        replace("""getProfile\(\)\.id\(\)""", "getProfile().getId()", """getProfile\(\)\.getId\(\)""", "getProfile().id()")
    }
    regex {
        direction.set(is1214)
        replace("""\.item\(""", ".renderItem(", """\.renderItem\(""", ".item(")
    }
}

repositories {
}

loom {
    splitEnvironmentSourceSets()
    // Only wired in for the versions that use the NanoVG ClickGUI (GlDevice/GpuDevice.backend
    // access widened for NVGPIPRenderer) - 1.21.4/1.21.11's Loom setup expects a differently
    // namespaced widener file and fails configuration entirely if this is set unconditionally.
    if (!is1214) {
        accessWidenerPath = rootProject.file("src/main/resources/runal.accesswidener")
    }

    mods {
        register("runal") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader") as String}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")

    implementation("io.github.llamalad7:mixinextras-fabric:0.5.2")
    annotationProcessor("io.github.llamalad7:mixinextras-fabric:0.5.2")

    // NanoVG-based ClickGUI rendering (NVGRenderer). Pulled in for every version because
    // NVGRenderer.java itself isn't Stonecutter-gated (it has no version-specific Mojang
    // API calls), so it needs to compile everywhere even though only 26.1.2/26.2 actually
    // wire it up to the screen (see RunalScreen's `is1214` branch / NVGPIPRenderer, which
    // *is* gated, since only those versions expose the PictureInPictureRenderState/
    // GuiRenderState APIs it depends on).
    run {
        val nanoVGVersion = "3.4.1"
        implementation("org.lwjgl:lwjgl-nanovg:$nanoVGVersion")
        include("org.lwjgl:lwjgl-nanovg:$nanoVGVersion")

        listOf("windows", "windows-arm64", "linux-arm64", "linux", "macos", "macos-arm64").forEach { os ->
            implementation("org.lwjgl:lwjgl-nanovg:$nanoVGVersion:natives-$os")
            include("org.lwjgl:lwjgl-nanovg:$nanoVGVersion:natives-$os")
        }
    }
}

java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava

    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = requiredJava.majorVersion.toInt()
}

tasks.withType<ProcessResources>().configureEach {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft" to sc.properties.get<String>("mod.mc_compat"),
        "javaDepends" to sc.properties.get<String>("mod.java_depends")
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }

    val mixinJava = sc.properties.get<String>("mod.java_level")
    inputs.property("mixinJava", mixinJava)
    filesMatching("*.mixins.json") { expand("java" to mixinJava) }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.name}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
    }
}
