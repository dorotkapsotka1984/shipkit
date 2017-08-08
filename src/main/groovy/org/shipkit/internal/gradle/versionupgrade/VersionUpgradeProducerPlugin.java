package org.shipkit.internal.gradle.versionupgrade;

import org.gradle.api.*;
import org.gradle.api.tasks.Exec;
import org.shipkit.gradle.configuration.ShipkitConfiguration;
import org.shipkit.internal.gradle.configuration.DeferredConfiguration;
import org.shipkit.internal.gradle.configuration.ShipkitConfigurationPlugin;
import org.shipkit.internal.gradle.git.CloneGitRepositoryTask;
import org.shipkit.internal.gradle.util.TaskMaker;
import org.shipkit.internal.util.ExposedForTesting;
import org.shipkit.version.VersionInfo;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.shipkit.internal.gradle.util.StringUtil.capitalize;
import static org.shipkit.internal.util.ArgumentValidation.notNull;

/**
 * BEWARE! This plugin is in incubating state, so its API may change in the future!
 * The plugin applies following plugins:
 *
 * <ul>
 *     <li>{@link ShipkitConfigurationPlugin}</li>
 * </ul>
 *
 * and adds following tasks:
 *
 * <ul>
 *     <li>cloneConsumerRepo{consumerRepository} - clones consumer repository into temporary directory</li>
 *     <li>produceVersionUpgrade{consumerRepository} - runs task performVersionUpgrade on consumerRepository</li>
 *     <li>produceVersionUpgrade - task aggregating all of the produceVersionUpgrade{consumerRepository} tasks</li>
 * </ul>
 *
 * Plugin performs a version upgrade of the project that it's applied in, for all consumer repositories defined.
 * Example of plugin usage:
 *
 * Configure your 'shipkit.gradle' file like here:
 *
 *      apply plugin: 'org.shipkit.version-upgrade-producer'
 *
 *      versionUpgradeProducer{
 *          consumersRepositoriesNames = ['wwilk/shipkit', 'wwilk/mockito']
 *      }
 *
 * and then call:
 *
 * ./gradlew produceVersionUpgrade
 *
 */
public class VersionUpgradeProducerPlugin implements Plugin<Project> {

    private VersionUpgradeProducerExtension versionUpgrade;

    @Override
    public void apply(final Project project) {
        final ShipkitConfiguration conf = project.getPlugins().apply(ShipkitConfigurationPlugin.class).getConfiguration();

        versionUpgrade = project.getExtensions().create("versionUpgradeProducer", VersionUpgradeProducerExtension.class);

        final Task performAllUpdates = TaskMaker.task(project, "produceVersionUpgrade", new Action<Task>() {
            @Override
            public void execute(final Task task) {
                task.setDescription("Performs dependency upgrade in all consumer repositories.");
            }
        });

        DeferredConfiguration.deferredConfiguration(project, new Runnable() {
            @Override
            public void run() {
                notNull(versionUpgrade.getConsumersRepositoriesNames(),
                    "'versionUpgradeProducer.consumersRepositoriesName'");
                for(String consumerRepositoryName : versionUpgrade.getConsumersRepositoriesNames()){
                    Task cloneTask = createConsumerCloneTask(project, conf, consumerRepositoryName);
                    Task performUpdate = createProduceUpgradeTask(project, consumerRepositoryName);
                    performUpdate.dependsOn(cloneTask);
                    performAllUpdates.dependsOn(performUpdate);
                }
            }
        });
    }

    private Task createConsumerCloneTask(final Project project, final ShipkitConfiguration conf, final String consumerRepository){
        return TaskMaker.task(project,
            "cloneConsumerRepo" + capitalize(toCamelCase(consumerRepository)),
            CloneGitRepositoryTask.class,
            new Action<CloneGitRepositoryTask>() {
                @Override
                public void execute(final CloneGitRepositoryTask task) {
                    task.setDescription("Clones consumer repo " + consumerRepository + " into a temporary directory.");
                    String gitHubUrl = conf.getGitHub().getUrl();
                    task.setRepositoryUrl(gitHubUrl + "/" + consumerRepository);
                    task.setTargetDir(getConsumerRepoTempDir(project, consumerRepository));
                }
        });
    }

    private Task createProduceUpgradeTask(final Project project, final String consumerRepository){
        return TaskMaker.execTask(project, "produceVersionUpgrade" + capitalize(toCamelCase(consumerRepository)), new Action<Exec>() {
            @Override
            public void execute(final Exec task) {
                task.setDescription("Performs dependency upgrade in " + consumerRepository);
                task.setWorkingDir(getConsumerRepoTempDir(project, consumerRepository));
                task.commandLine("./gradlew", "performVersionUpgrade", getDependencyProperty(project));
            }
        });
    }

    private File getConsumerRepoTempDir(Project project, String consumerRepository) {
        return new File(project.getBuildDir().getAbsolutePath() + "/" + toCamelCase(consumerRepository));
    }

    private String getDependencyProperty(Project project){
        VersionInfo info = project.getExtensions().getByType(VersionInfo.class);
        return String.format("-Pdependency=%s:%s:%s", project.getGroup().toString(), project.getName(), info.getPreviousVersion());
    }

    private String toCamelCase(String repository){
        Matcher matcher = Pattern.compile("[/_-]([a-z])").matcher(repository);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, matcher.group(1).toUpperCase());
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @ExposedForTesting
    protected VersionUpgradeProducerExtension getVersionUpgrade(){
        return versionUpgrade;
    }
}
