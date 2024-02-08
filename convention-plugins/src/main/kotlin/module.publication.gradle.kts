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

        // Provide artifacts information required by Maven Central
        pom {
            name.set("SymSpell Kt")
            description.set("A Kotlin Multiplatform implementation of the SymSpell Spell Checking algorithm.")
            url.set("https://github.com/Wavesonics/SymSpellKt")

            artifactId = "symspellkt"

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    name.set("Adam Brown")
                    id.set("Wavesonics")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/Wavesonics/SymSpellKt.git")
                developerConnection.set("scm:git:ssh://github.com/Wavesonics/SymSpellKt.git")
                url.set("https://github.com/Wavesonics/SymSpellKt")
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
