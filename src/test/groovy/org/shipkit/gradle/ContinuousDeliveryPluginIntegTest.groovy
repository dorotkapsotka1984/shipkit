package org.shipkit.gradle

import testutil.GradleSpecification

class ContinuousDeliveryPluginIntegTest extends GradleSpecification {

    def "all tasks in dry run"() {

        /**
         * TODO this test is just a starting point we will make it better and create more integration tests
         * Stuff that we should do:
         *  1. (Most important) Avoid writing too many integration tests. Most code should be covered by unit tests (see testing pyramid)
         *  2. Push out complexity to base class GradleSpecification
         *      so that what remains in the test is the essential part of a tested feature
         *  3. Add more specific assertions rather than just a list of tasks in dry run mode
         *  4. Use sensible defaults so that we don't need to specify all configuration in the test
         *  5. Move integration tests to a separate module
         *  6. Dependencies are hardcoded between GradleSpecification and build.gradle of release-tools project
         */

        projectDir.newFolder("gradle")
        projectDir.newFile("gradle/shipkit.gradle") << """
            releasing {
                gitHub.readOnlyAuthToken = "foo"
                gitHub.writeAuthToken = "secret"
                releasing.releaseNotes.file = "CHANGELOG.md"
                releasing.git.user = "shipkit"
                releasing.git.email = "shipkit@gmail.com"
                releasing.gitHub.repository = "repo"
            }
            
            allprojects {
                plugins.withId("org.shipkit.java-library") {
                    bintray {
                        user = "szczepiq"
                        key = "secret"
                    }
                }
            }
        """

        buildFile << """
            apply plugin: "org.shipkit.continuous-delivery"
        """

        settingsFile << "include 'api', 'impl'"
        projectDir.newFile("version.properties") << "version=1.0.0"
        projectDir.newFolder('api')
        projectDir.newFolder('impl')
        projectDir.newFile('api/build.gradle') << "apply plugin: 'org.shipkit.java-library'"
        projectDir.newFile('impl/build.gradle') << "apply plugin: 'org.shipkit.java-library'"

        expect:
        def result = pass("performRelease", "-m")
        //git push and bintray upload tasks should run as late as possible
        result.tasks.join("\n") == """:fetchAllContributors=SKIPPED
:api:generatePomFileForJavaLibraryPublication=SKIPPED
:api:compileJava=SKIPPED
:api:processResources=SKIPPED
:api:classes=SKIPPED
:api:jar=SKIPPED
:api:javadoc=SKIPPED
:api:javadocJar=SKIPPED
:api:sourcesJar=SKIPPED
:api:publishJavaLibraryPublicationToMavenLocal=SKIPPED
:impl:generatePomFileForJavaLibraryPublication=SKIPPED
:impl:compileJava=SKIPPED
:impl:processResources=SKIPPED
:impl:classes=SKIPPED
:impl:jar=SKIPPED
:impl:javadoc=SKIPPED
:impl:javadocJar=SKIPPED
:impl:sourcesJar=SKIPPED
:impl:publishJavaLibraryPublicationToMavenLocal=SKIPPED
:bumpVersionFile=SKIPPED
:fetchReleaseNotes=SKIPPED
:updateReleaseNotes=SKIPPED
:gitCommit=SKIPPED
:gitTag=SKIPPED
:gitPush=SKIPPED
:api:bintrayUpload=SKIPPED
:impl:bintrayUpload=SKIPPED
:bintrayUploadAll=SKIPPED
:performGitPush=SKIPPED
:performRelease=SKIPPED"""
    }
}
