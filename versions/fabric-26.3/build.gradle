plugins {
	id 'net.fabricmc.fabric-loom' version "${loom_version}"
}

version = "${project.mod_version}+${project.minecraft_version}"
group = 'net.jarry.uicleanup'
base { archivesName = 'jarrys-ui-cleanup-fabric' }

def root = file('../..')

sourceSets {
	main {
		java.srcDirs = [file("${root}/common/src/main/java"), file("${root}/fabric/src/main/java")]
		resources.srcDirs = [file("${root}/common/src/main/resources"), file("${root}/fabric/src/main/resources"), file("${root}/generated/${project.minecraft_version}")]
	}
	test {
		java.srcDirs = [file("${root}/common/src/test/java")]
	}
}

loom {
	mods {
		"jarrys_ui_cleanup" { sourceSet sourceSets.main }
	}
	runs {
		client {
			runDir = 'run-client'
			vmArg '-Djarrys_ui_cleanup.clientselftest=true'
			vmArg '-Xmx2G'
		}
	}
}

dependencies {
	minecraft "com.mojang:minecraft:${project.minecraft_version}"
	implementation "net.fabricmc:fabric-loader:${project.loader_version}"
	implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
	compileOnly files("${root}/libs/${project.modmenu_jar}")
	if (project.findProperty('with_modmenu') == 'true') {
		runtimeOnly files("${root}/libs/${project.modmenu_jar}")
	}
	if (project.findProperty('with_ncr') == 'true') {
		runtimeOnly files("${root}/libs/NoChatReports-FABRIC-26.2-v2.20.2.jar")
	}
	testImplementation platform('org.junit:junit-bom:5.11.4')
	testImplementation 'org.junit.jupiter:junit-jupiter'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test { useJUnitPlatform(); systemProperty 'uic.root', root.absolutePath; systemProperty 'uic.mc', project.minecraft_version }

processResources {
	def props = [version: project.version, mc_dep: project.mc_dep]
	inputs.properties props
	filesMatching("fabric.mod.json") { expand props }
}

tasks.withType(JavaCompile).configureEach { it.options.release = 25; it.options.encoding = 'UTF-8'; it.options.compilerArgs << '-Xlint:deprecation' }
java { sourceCompatibility = JavaVersion.VERSION_25; targetCompatibility = JavaVersion.VERSION_25 }
jar { from(file("${root}/common/LICENSE")) }
