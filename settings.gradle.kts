pluginManagement {
  repositories {
    google()        // ← content filter ဖြုတ်
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "Galaxy Tunnel Next"  // ← app name နဲ့ ကိုက်အောင်

include(":app")
