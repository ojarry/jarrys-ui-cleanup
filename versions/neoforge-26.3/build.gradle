plugins {
	id 'java-library'
	id 'net.neoforged.moddev' version '2.0.148'
}

version = "${mod_version}+${minecraft_version}"
group = 'net.jarry.uicleanup'
base { archivesName = 'jarrys-ui-cleanup-neoforge' }

def root = file('../..')

sourceSets {
	main {
		java.srcDirs = [file("${root}/common/src/main/java"), file("${root}/neoforge/src/main/java")]
		resources.srcDirs = [file("${root}/common/src/main/resources"), file("${root}/generated/${minecraft_version}")]
	}
	test {
		java.srcDirs = [file("${root}/common/src/test/java")]
	}
}

java.toolchain.languageVersion = JavaLanguageVersion.of(25)

neoForge {
	version = project.neo_version
	runs {
		client {
			client()
			gameDirectory = project.file('run-client')
			systemProperty 'jarrys_ui_cleanup.clientselftest', 'true'
			jvmArguments.add('-Xmx2G')
		}
	}
	mods {
		jarrys_ui_cleanup { sourceSet sourceSets.main }
	}
	unitTest {
		enable()
		testedMod = mods.jarrys_ui_cleanup
	}
}

dependencies {
	testImplementation platform('org.junit:junit-bom:5.11.4')
	testImplementation 'org.junit.jupiter:junit-jupiter'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test { useJUnitPlatform(); systemProperty 'uic.root', root.absolutePath; systemProperty 'uic.mc', minecraft_version }

var generateModMetadata = tasks.register("generateModMetadata", ProcessResources) {
	var replaceProperties = [
		minecraft_version_range: minecraft_version_range,
		neo_version            : neo_version,
		mod_version            : project.version
	]
	inputs.properties replaceProperties
	expand replaceProperties
	from "${root}/neoforge/templates"
	into "build/generated/sources/modMetadata"
}
sourceSets.main.resources.srcDir generateModMetadata
neoForge.ideSyncTask generateModMetadata

tasks.withType(JavaCompile).configureEach {
	it.options.release = 25
	it.options.encoding = 'UTF-8'
	it.options.compilerArgs << '-Xlint:deprecation'
}

jar { from(file("${root}/common/LICENSE")) { rename { 'LICENSE.txt' } } }
