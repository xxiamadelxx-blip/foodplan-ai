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

tasks.register<JavaExec>("exportM3ActualWeek") {
    group = "verification"
    description = "Export the real production-planned seven-day meals and complete price-UNKNOWN shopping list"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.foodplan.domain.M3ActualWeekExport")
    args(
        layout.buildDirectory.file("reports/m3/actual-week-meals.csv").get().asFile.absolutePath,
        layout.buildDirectory.file("reports/m3/actual-week-shopping.csv").get().asFile.absolutePath
    )
}

tasks.register<JavaExec>("generateM2IndependentReviewQueue") {
    group = "verification"
    description = "Export 150 authored draft recipes with full methods and independent evidence gaps"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.foodplan.domain.M2IndependentRecipeReviewExport")
    args(layout.buildDirectory.file("reports/m2/independent-review-queue.csv").get().asFile.absolutePath)
}


tasks.register<JavaExec>("generateM2DocumentaryReviewQueue") {
    group = "verification"
    description = "Export 150 authored recipes for sourced documentary method/nutrition checks, no physical cooking required"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.foodplan.domain.M2DocumentaryReviewQueue")
    args(layout.buildDirectory.file("reports/m2/documentary-review-queue.csv").get().asFile.absolutePath)
}
