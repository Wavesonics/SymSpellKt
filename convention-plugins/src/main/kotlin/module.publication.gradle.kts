plugins {
    `maven-publish`
    signing
}

publishing {
    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        artifact(tasks.register("${name}JavadocJar", Jar::class) {
            archiveClassifier.set("javadoc")
            archiveAppendix.set(this@withType.name)
        })

        artifactId = artifactId.lowercase()

        // Provide artifacts information required by Maven Central
        pom {
            name.set("Symspell Kt")
            description.set("A Kotlin Multiplatform implementation of the SymSpell Spell Checking algorithm.")
            url.set("https://github.com/Wavesonics/SymSpellKt")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            issueManagement {
                system.set("Github")
                url.set("https://github.com/Wavesonics/SymSpellKt/issues")
            }
            scm {
                connection.set("scm:git:git://github.com/Wavesonics/SymSpellKt.git")
                developerConnection.set("scm:git:ssh://github.com/Wavesonics/SymSpellKt.git")
                url.set("https://github.com/Wavesonics/SymSpellKt")
            }
            developers {
                developer {
                    name.set("Adam Brown")
                    id.set("Wavesonics")
                    email.set("adamwbrown@gmail.com")
                }
            }
        }
    }
}


signing {
    val signingKey: String? = System.getenv("SIGNING_KEY")
    val signingPassword: String? = System.getenv("SIGNING_PASSWORD")
    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(null, signingKey, signingPassword)
        sign(publishing.publications)
    } else {
        println("No signing credentials provided. Skipping Signing.")
    }
}

// TODO: remove after https://youtrack.jetbrains.com/issue/KT-46466 is fixed
project.tasks.withType(AbstractPublishToMaven::class.java).configureEach {
    dependsOn(project.tasks.withType(Sign::class.java))
}