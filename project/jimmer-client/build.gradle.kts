plugins {
    id("kotlin-convention")
    alias(libs.plugins.ksp)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(projects.jimmerSql)
    compileOnly(libs.spring.boot)
    annotationProcessor(libs.spring.boot.configurationProcessor)

    testAnnotationProcessor(projects.jimmerApt)
    kspTest(projects.jimmerKsp)
    testImplementation(projects.jimmerSqlKotlin)
    testImplementation(libs.jupiter.api)
    testRuntimeOnly(libs.jupiter.engine)
    testImplementation(libs.spring.web)
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Ajimmer.source.includes=org.babyfish.jimmer.client.java.")
    options.compilerArgs.add("-Ajimmer.client.checkedException=true")
}

ksp {
    arg("jimmer.source.includes", "org.babyfish.jimmer.client.kotlin.")
    arg("jimmer.dto.testDirs", "src/test/dto2")
}

kotlin {
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}
