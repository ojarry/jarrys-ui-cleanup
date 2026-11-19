#!/usr/bin/env python3
"""Writes versions/<loader>-<mc>/ Gradle projects (same verified loader versions as the Bible builds)."""
import shutil
from pathlib import Path
root = Path(__file__).resolve().parent
FABRIC = {
 "26.3": dict(api="0.166.2+26.3", dep="~26.3", modmenu="modmenu-21.0.1.jar"),
 "26.2": dict(api="0.161.0+26.2", dep="~26.2", modmenu="modmenu-20.0.3.jar"),
}
NEO = {"26.3": "26.3.0.113", "26.2": "26.2.0.95"}
wrapper = root / "versions/fabric-26.3"

def common(d):
    d.mkdir(parents=True, exist_ok=True)
    (d / "gradle/wrapper").mkdir(parents=True, exist_ok=True)
    for f in ("gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar", "gradle/wrapper/gradle-wrapper.properties"):
        shutil.copy2(wrapper / f, d / f)
    (d / "gradlew").chmod(0o755)

for mc, c in FABRIC.items():
    d = root / f"versions/fabric-{mc}"
    common(d)
    (d / "build.gradle").write_text((root / "fabric.build.gradle.tpl").read_text())
    (d / "settings.gradle").write_text("""pluginManagement {
	repositories {
		maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
		mavenCentral()
		gradlePluginPortal()
	}
}
rootProject.name = 'jarrys-ui-cleanup-fabric'
""")
    (d / "gradle.properties").write_text(f"""org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=false
org.gradle.configuration-cache=false
minecraft_version={mc}
loader_version=0.20.1
loom_version=1.19-SNAPSHOT
mod_version=1.0.2
fabric_api_version={c['api']}
mc_dep={c['dep']}
modmenu_jar={c['modmenu']}
""")
    print("fabric", mc)

for mc, neo in NEO.items():
    d = root / f"versions/neoforge-{mc}"
    common(d)
    (d / "build.gradle").write_text((root / "neoforge.build.gradle.tpl").read_text())
    (d / "settings.gradle").write_text("""pluginManagement {
	repositories {
		gradlePluginPortal()
	}
}

plugins {
	id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}

rootProject.name = 'jarrys-ui-cleanup-neoforge'
""")
    (d / "gradle.properties").write_text(f"""org.gradle.jvmargs=-Xmx3G
org.gradle.parallel=false
org.gradle.configuration-cache=false
org.gradle.daemon=false
minecraft_version={mc}
minecraft_version_range=[{mc}]
neo_version={neo}
mod_version=1.0.2
""")
    print("neoforge", mc, neo)
