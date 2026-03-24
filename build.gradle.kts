plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version("8.5.2").apply(false)
    id("com.android.library").version("8.5.2").apply(false)
    kotlin("android").version("1.9.24").apply(false)
    kotlin("multiplatform").version("1.9.24").apply(false)
    id("com.google.gms.google-services").version("4.4.2").apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
