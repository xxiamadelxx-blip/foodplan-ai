plugins { id("org.jetbrains.kotlin.jvm") }
kotlin { jvmToolchain(17) }
dependencies { testImplementation("junit:junit:4.13.2") }
tasks.test { useJUnit() }

tasks.register<JavaExec>("generateM2RecipeAudit") {
    group = "verification"
    description = "Export an unverified human-review M2 ingredient and recipe evidence CSV"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.foodplan.domain.M2RecipeAuditExport")
    args(layout.buildDirectory.file("reports/m2/recipe-review.csv").get().asFile.absolutePath)
}
