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

tasks.register<JavaExec>("generateM2IndependentReviewQueue") {
    group = "verification"
    description = "Export 150 authored draft recipes with full methods and independent evidence gaps"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.foodplan.domain.M2IndependentRecipeReviewExport")
    args(layout.buildDirectory.file("reports/m2/independent-review-queue.csv").get().asFile.absolutePath)
}
