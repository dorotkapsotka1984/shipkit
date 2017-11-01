package org.shipkit.gradle.java;

import org.gradle.api.DefaultTask;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.bundling.Jar;
import org.shipkit.gradle.release.ReleaseNeededTask;
import org.shipkit.internal.gradle.java.tasks.ComparePublications;

import java.io.File;

/**
 * Compares sources jars and {@link ComparePublications#DEPENDENCY_INFO_FILEPATH} files produced by the build with analogical artifacts
 * from last published build. If it determines that there were no changes it advises the user to
 * skip publication of the new version artifacts (e.g. skip the release).
 * <p>
 * The outputs of this task are used by {@link ReleaseNeededTask}.
 * The {@link #getComparisonResult()} should be added to {@link ReleaseNeededTask#getComparisonResults()}.
 */
public class ComparePublicationsTask extends DefaultTask {

    @OutputFile private File comparisonResult;

    @InputFiles private Jar sourcesJar;

    //Not using @InputFile annotation on purpose below. @InputFile makes Gradle fail early
    // when the file path specified but file does not exist (@Optional does not help).
    // Using @Input is enough for this use case
    @Input @Optional private File previousSourcesJar;

    /**
     * File that stores text result of the comparison.
     * If the file is empty it means the publications are the same.
     * If the file does not exist it means that the task did not run.
     * In this case you can assume that there are differences and the release should be triggered.
     */
    public File getComparisonResult() {
        return comparisonResult;
    }

    /**
     * See {@link #getComparisonResult()}
     */
    public void setComparisonResult(File comparisonResult) {
        this.comparisonResult = comparisonResult;
    }

    @TaskAction public void comparePublications() {
        new ComparePublications().comparePublications(this);
    }

    /**
     * Sets the sourcesJar for comparision with {@link #getPreviousSourcesJar()}.
     * Task dependency will be automatically added from this task to sourcesJar task supplied as parameter.
     * During comparison, the algorithm will read jar's output file using {@link Jar#getArchivePath()}.
     */
    public void compareSourcesJar(Jar sourcesJar) {
        //when we compare, we can get the sources jar file via sourcesJar.archivePath
        this.sourcesJar = sourcesJar;

        //so that when we compare jars, the local sources jar is already built.
        this.dependsOn(sourcesJar);
    }

    /**
     * Previously released sources jar used for comparison with currently built sources jar.
     */
    public File getPreviousSourcesJar() {
        return previousSourcesJar;
    }

    /**
     * See {@link #getPreviousSourcesJar()}
     */
    public void setPreviousSourcesJar(File previousSourcesJar) {
        this.previousSourcesJar = previousSourcesJar;
    }

    /**
     * Currently built sources jar file used for comparison.
     */
    public Jar getSourcesJar() {
        return sourcesJar;
    }
}
