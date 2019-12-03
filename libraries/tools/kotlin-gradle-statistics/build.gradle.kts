description = "kotlin-gradle-statistics"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlinStdlib())
    compileOnly(protobufLite()) //TODO is it required?
    compileOnly(kotlinStdlib()) //TODO is it required?

}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
