pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "trigger"
include(":app", ":core:common", ":core:ui", ":core:database", ":automation:engine", ":feature:gameassistant", ":feature:macroeditor", ":feature:scheduling", ":feature:settings", ":baselineprofile")
