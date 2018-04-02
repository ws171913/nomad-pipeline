package info.multani.jenkins.plugins.nomad;

import com.hashicorp.nomad.apimodel.Job;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Logger;

import info.multani.jenkins.plugins.nomad.model.TemplateEnvVar;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;


import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.DescriptorVisibilityFilter;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.tools.ToolLocationNodeProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;

/**
 * Nomad Job Template
 *
 */
public class NomadJobTemplate extends AbstractDescribableImpl<NomadJobTemplate> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String FALLBACK_ARGUMENTS = "${computer.jnlpmac} ${computer.name}";

    private static final Logger LOGGER = Logger.getLogger(NomadJobTemplate.class.getName());

    public static final int DEFAULT_SLAVE_JENKINS_CONNECTION_TIMEOUT = 100;

//    private String inheritFrom;

    private String name;

    private String namespace;
//
    private String image;
//
//    private boolean privileged;
//
//    private boolean alwaysPullImage;
//
    private String command;
//
    private String args;
//
//    private String remoteFs;
//
    private int instanceCap = Integer.MAX_VALUE;
//
    private int slaveConnectTimeout = DEFAULT_SLAVE_JENKINS_CONNECTION_TIMEOUT;
//
    private int idleMinutes;
//
//    private int activeDeadlineSeconds;
//
    private String label;
//
//
//    private String nodeSelector;
//
    private Node.Mode nodeUsageMode;

    private Integer resourcesCPU;

    private Integer resourcesMemory;

//    private boolean customWorkspaceVolumeEnabled;
////    private WorkspaceVolume workspaceVolume;
//
////    private final List<PodVolume> volumes = new ArrayList<PodVolume>();
//
    private List<TaskGroupTemplate> taskGroups = new ArrayList<TaskGroupTemplate>();
//
    private List<TemplateEnvVar> envVars = new ArrayList<>();
//
//    private List<PodAnnotation> annotations = new ArrayList<PodAnnotation>();
//
//    private List<PodImagePullSecret> imagePullSecrets = new ArrayList<PodImagePullSecret>();
//
    private transient List<ToolLocationNodeProperty> nodeProperties;

    @DataBoundConstructor
    public NomadJobTemplate() {
    }

    public NomadJobTemplate(NomadJobTemplate from) {
//        this.setAnnotations(from.getAnnotations());
        this.setContainers(from.getContainers());
//        this.setImagePullSecrets(from.getImagePullSecrets());
        this.setInstanceCap(from.getInstanceCap());
        this.setLabel(from.getLabel());
//        this.setName(from.getName());
        this.setNamespace(from.getNamespace());
//        this.setInheritFrom(from.getInheritFrom());
//        this.setNodeSelector(from.getNodeSelector());
        this.setNodeUsageMode(from.getNodeUsageMode());
        this.setSlaveConnectTimeout(from.getSlaveConnectTimeout());
//        this.setActiveDeadlineSeconds(from.getActiveDeadlineSeconds());
//        this.setVolumes(from.getVolumes());
//        this.setWorkspaceVolume(from.getWorkspaceVolume());
    }

//    @Deprecated
//    public PodTemplate(String image, List<? extends PodVolume> volumes) {
//        this(null, image, volumes);
//    }
//
//    @Deprecated
//    PodTemplate(String name, String image, List<? extends PodVolume> volumes) {
//        this(name, volumes, Collections.emptyList());
//        if (image != null) {
//            getContainers().add(new ContainerTemplate(name, image));
//        }
//    }
//
//    @Restricted(NoExternalUse.class) // testing only
//    PodTemplate(String name,
////            List<? extends PodVolume> volumes,
//            List<? extends ContainerTemplate> containers) {
//        this.name = name;
////        this.volumes.addAll(volumes);
//        this.containers.addAll(containers);
//    }

    private Optional<TaskGroupTemplate> getFirstContainer() {
        return Optional.ofNullable(getContainers().isEmpty() ? null : getContainers().get(0));
    }

//    public String getInheritFrom() {
//        return inheritFrom;
//    }
//
//    @DataBoundSetter
//    public void setInheritFrom(String inheritFrom) {
//        this.inheritFrom = inheritFrom;
//    }
//
    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return namespace;
    }

    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Deprecated
    public String getImage() {
        return getFirstContainer().map(TaskGroupTemplate::getImage).orElse(null);
    }

    @Deprecated
    @DataBoundSetter
    public void setCommand(String command) {
        getFirstContainer().ifPresent((i) -> i.setCommand(command));
    }

    @Deprecated
    public String getCommand() {
        return getFirstContainer().map(TaskGroupTemplate::getCommand).orElse(null);
    }

    @Deprecated
    @DataBoundSetter
    public void setArgs(String args) {
        getFirstContainer().ifPresent((i) -> i.setArgs(args));
    }

    @Deprecated
    public String getArgs() {
        return getFirstContainer().map(TaskGroupTemplate::getArgs).orElse(null);
    }

    public String getDisplayName() {
        return "Nomad Job Template";
    }

//    @DataBoundSetter
//    @Deprecated
//    public void setRemoteFs(String remoteFs) {
//        getFirstContainer().ifPresent((i) -> i.setWorkingDir(remoteFs));
//    }

    @Deprecated
    public String getRemoteFs() {
        return ""; // TODO getFirstContainer().map(ContainerTemplate::getWorkingDir).orElse(null);
    }
//
    public void setInstanceCap(int instanceCap) {
        if (instanceCap < 0) {
            this.instanceCap = Integer.MAX_VALUE;
        } else {
            this.instanceCap = instanceCap;
        }
    }
//
    public int getInstanceCap() {
        return instanceCap;
    }

    public void setSlaveConnectTimeout(int slaveConnectTimeout) {
        if (slaveConnectTimeout <= 0) {
            LOGGER.log(Level.WARNING, "Agent -> Jenkins connection timeout " +
                    "cannot be <= 0. Falling back to the default value: " +
                    DEFAULT_SLAVE_JENKINS_CONNECTION_TIMEOUT);
            this.slaveConnectTimeout = DEFAULT_SLAVE_JENKINS_CONNECTION_TIMEOUT;
        } else {
            this.slaveConnectTimeout = slaveConnectTimeout;
        }
    }

    public int getSlaveConnectTimeout() {
        if (slaveConnectTimeout == 0)
            return DEFAULT_SLAVE_JENKINS_CONNECTION_TIMEOUT;
        return slaveConnectTimeout;
    }

    @DataBoundSetter
    public void setInstanceCapStr(String instanceCapStr) {
        if (StringUtils.isBlank(instanceCapStr)) {
            setInstanceCap(Integer.MAX_VALUE);
        } else {
            setInstanceCap(Integer.parseInt(instanceCapStr));
        }
    }

    public String getInstanceCapStr() {
        if (getInstanceCap() == Integer.MAX_VALUE) {
            return "";
        } else {
            return String.valueOf(instanceCap);
        }
    }

    @DataBoundSetter
    public void setSlaveConnectTimeoutStr(String slaveConnectTimeoutStr) {
        if (StringUtils.isBlank(slaveConnectTimeoutStr)) {
            setSlaveConnectTimeout(DEFAULT_SLAVE_JENKINS_CONNECTION_TIMEOUT);
        } else {
            setSlaveConnectTimeout(Integer.parseInt(slaveConnectTimeoutStr));
        }
    }

    public String getSlaveConnectTimeoutStr() {
        return String.valueOf(slaveConnectTimeout);
    }

    public void setIdleMinutes(int i) {
        this.idleMinutes = i;
    }

    public int getIdleMinutes() {
        return idleMinutes;
    }
//
//    public void setActiveDeadlineSeconds(int i) {
//        this.activeDeadlineSeconds = i;
//    }
//
//    public int getActiveDeadlineSeconds() {
//        return activeDeadlineSeconds;
//    }
//
    @DataBoundSetter
    public void setIdleMinutesStr(String idleMinutes) {
        if (StringUtils.isBlank(idleMinutes)) {
            setIdleMinutes(0);
        } else {
            setIdleMinutes(Integer.parseInt(idleMinutes));
        }
    }

    public String getIdleMinutesStr() {
        if (getIdleMinutes() == 0) {
            return "";
        } else {
            return String.valueOf(idleMinutes);
        }
    }
//
//    @DataBoundSetter
//    public void setActiveDeadlineSecondsStr(String activeDeadlineSeconds) {
//        if (StringUtils.isBlank(activeDeadlineSeconds)) {
//            setActiveDeadlineSeconds(0);
//        } else {
//            setActiveDeadlineSeconds(Integer.parseInt(activeDeadlineSeconds));
//        }
//    }
//
//    public String getActiveDeadlineSecondsStr() {
//        if (getActiveDeadlineSeconds() == 0) {
//            return "";
//        } else {
//            return String.valueOf(activeDeadlineSeconds);
//        }
//    }
//
    public Set<LabelAtom> getLabelSet() {
        return Label.parse(label);
    }

    @DataBoundSetter
    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
//
//    @DataBoundSetter
//    public void setNodeSelector(String nodeSelector) {
//        this.nodeSelector = nodeSelector;
//    }
//
//    public String getNodeSelector() {
//        return nodeSelector;
//    }

    @DataBoundSetter
    public void setNodeUsageMode(Node.Mode nodeUsageMode) {
        this.nodeUsageMode = nodeUsageMode;
    }

    @DataBoundSetter
    public void setNodeUsageMode(String nodeUsageMode) {
        this.nodeUsageMode = Node.Mode.valueOf(nodeUsageMode);
    }

    public Node.Mode getNodeUsageMode() {
        return nodeUsageMode;
    }
//
//    @Deprecated
//    @DataBoundSetter
//    public void setPrivileged(boolean privileged) {
//        getFirstContainer().ifPresent((i) -> i.setPrivileged(privileged));
//    }
//
//    @Deprecated
//    public boolean isPrivileged() {
//        return getFirstContainer().map(ContainerTemplate::isPrivileged).orElse(false);
//    }
//
//    @Deprecated
//    @DataBoundSetter
//    public void setAlwaysPullImage(boolean alwaysPullImage) {
//        getFirstContainer().ifPresent((i) -> i.setAlwaysPullImage(alwaysPullImage));
//    }
//
//    @Deprecated
//    public boolean isAlwaysPullImage() {
//        return getFirstContainer().map(ContainerTemplate::isAlwaysPullImage).orElse(false);
//    }
//
    public List<TemplateEnvVar> getEnvVars() {
        if (envVars == null) {
            return Collections.emptyList();
        }
        return envVars;
    }

    public void addEnvVars(List<TemplateEnvVar> envVars) {
        if (envVars != null) {
            this.envVars.addAll(envVars);
        }
    }

    @DataBoundSetter
    public void setEnvVars(List<TemplateEnvVar> envVars) {
        if (envVars != null) {
            this.envVars.clear();
            this.addEnvVars(envVars);
        }
    }

//    public List<PodAnnotation> getAnnotations() {
//        if (annotations == null) {
//            return Collections.emptyList();
//        }
//        return annotations;
//    }
//
//    public void addAnnotations(List<PodAnnotation> annotations) {
//        this.annotations.addAll(annotations);
//    }
//
//
//    @DataBoundSetter
//    public void setAnnotations(List<PodAnnotation> annotations) {
//        if (annotations != null) {
//            this.annotations = new ArrayList<PodAnnotation>();
//            this.addAnnotations(annotations);
//        }
//    }
//
//    public List<PodImagePullSecret> getImagePullSecrets() {
//        return imagePullSecrets == null ? Collections.emptyList() : imagePullSecrets;
//    }
//
//    public void addImagePullSecrets(List<PodImagePullSecret> imagePullSecrets) {
//        this.imagePullSecrets.addAll(imagePullSecrets);
//    }
//
//    @DataBoundSetter
//    public void setImagePullSecrets(List<PodImagePullSecret> imagePullSecrets) {
//        if (imagePullSecrets != null) {
//            this.imagePullSecrets.clear();
//            this.addImagePullSecrets(imagePullSecrets);
//        }
//    }
//
    @DataBoundSetter
    public void setNodeProperties(List<ToolLocationNodeProperty> nodeProperties){
        this.nodeProperties = nodeProperties;
    }

    @Nonnull
    public List<ToolLocationNodeProperty> getNodeProperties(){
        if (nodeProperties == null) {
            return Collections.emptyList();
        }
        return nodeProperties;
    }

//    @Deprecated
//    public String getResourceRequestMemory() {
//        return getFirstContainer().map(ContainerTemplate::getResourceRequestMemory).orElse(null);
//    }
//
//    @Deprecated
//    @DataBoundSetter
//    public void setResourceRequestMemory(String resourceRequestMemory) {
//        getFirstContainer().ifPresent((i) -> i.setResourceRequestMemory(resourceRequestMemory));
//    }
//
//    @Deprecated
//    public String getResourcesCpu() {
//        return getFirstContainer().map(ContainerTemplate::getResourcesCpu).orElse(null);
//    }
//
//    @Deprecated
//    @DataBoundSetter
//    public void setResourcesCpu(String resourcesCpu) {
//        getFirstContainer().ifPresent((i) -> i.setResourcestCpu(resourcesCpu));
//    }
//
//    @Deprecated
//    public String getResourceMemory() {
//        return getFirstContainer().map(ContainerTemplate::getResourceMemory).orElse(null);
//    }
//
//    @Deprecated
//    @DataBoundSetter
//    public void setResourceLimitMemory(String resourceLimitMemory) {
//        getFirstContainer().ifPresent((i) -> i.setResourceLimitMemory(resourceLimitMemory));
//    }
//
//    @Deprecated
//    public String getResourceRequestCpu() {
//        return getFirstContainer().map(ContainerTemplate::getResourceRequestCpu).orElse(null);
//    }
//
//    @Deprecated
//    @DataBoundSetter
//    public void setResourceRequestCpu(String resourceRequestCpu) {
//        getFirstContainer().ifPresent((i) -> i.setResourceRequestCpu(resourceRequestCpu));
//    }

//    @DataBoundSetter
//    public void setVolumes(@Nonnull List<PodVolume> items) {
//        synchronized (this.volumes) {
//            this.volumes.clear();
//            this.volumes.addAll(items);
//        }
//    }

//    @Nonnull
//    public List<PodVolume> getVolumes() {
//        if (volumes == null) {
//            return Collections.emptyList();
//        }
//        return volumes;
//    }

//    public boolean isCustomWorkspaceVolumeEnabled() {
//        return customWorkspaceVolumeEnabled;
//    }
//
//    @DataBoundSetter
//    public void setCustomWorkspaceVolumeEnabled(boolean customWorkspaceVolumeEnabled) {
//        this.customWorkspaceVolumeEnabled = customWorkspaceVolumeEnabled;
//    }
//
//    public WorkspaceVolume getWorkspaceVolume() {
//        return workspaceVolume;
//    }
//
//    @DataBoundSetter
//    public void setWorkspaceVolume(WorkspaceVolume workspaceVolume) {
//        this.workspaceVolume = workspaceVolume;
//    }

    @DataBoundSetter
    public void setContainers(@Nonnull List<TaskGroupTemplate> items) {
        synchronized (this.taskGroups) {
            this.taskGroups.clear();
            this.taskGroups.addAll(items);
        }
    }

    @Nonnull
    public List<TaskGroupTemplate> getContainers() {
        if (taskGroups == null) {
            return Collections.emptyList();
        }
        return taskGroups;
    }

    @SuppressWarnings("deprecation")
    protected Object readResolve() {
        if (taskGroups == null) {
//            // upgrading from 0.8
            taskGroups = new ArrayList<>();
            TaskGroupTemplate taskGroupTemplate = new TaskGroupTemplate(NomadCloud.JNLP_NAME, this.image);
            taskGroupTemplate.setCommand(command);
            taskGroupTemplate.setArgs(args);
//            taskGroupTemplate.setPrivileged(privileged);
//            taskGroupTemplate.setAlwaysPullImage(alwaysPullImage);
            taskGroupTemplate.setEnvVars(envVars);
            taskGroupTemplate.setResourcesCPU(resourcesCPU);
            taskGroupTemplate.setResourcesMemory(resourcesMemory);
//            taskGroupTemplate.setWorkingDir(remoteFs);
            taskGroups.add(taskGroupTemplate);
        }
//
//        if (annotations == null) {
//            annotations = new ArrayList<>();
//        }

        return this;
    }

    /**
     * Build a Pod object from a PodTemplate
     * 
     * @param slave
     */

    /**
     * Build a Pod object from a PodTemplate
     * @param slave
     * @return
     */
    public Job build(NomadSlave slave) {
        return new NomadJobTemplateBuilder(this).build(slave);
    }

    @Extension
    @Symbol("nomadJobTemplate")
    public static class DescriptorImpl extends Descriptor<NomadJobTemplate> {

        @Override
        public String getDisplayName() {
            return "Nomad Job";
        }

        @SuppressWarnings("unused") // Used by jelly
        @Restricted(DoNotUse.class) // Used by jelly
        public List<? extends Descriptor> getEnvVarsDescriptors() {
            return DescriptorVisibilityFilter.apply(null, Jenkins.getInstance().getDescriptorList(TemplateEnvVar.class));
        }
    }

    @Override
    public String toString() {
        return "NomadJobTemplate{" +
//                (inheritFrom == null ? "" : "inheritFrom='" + inheritFrom + '\'') +
                (name == null ? "" : ", name='" + name + '\'') +
                (namespace == null ? "" : ", namespace='" + namespace + '\'') +
                (image == null ? "" : ", image='" + image + '\'') +
//                (!privileged ? "" : ", privileged=" + privileged) +
//                (!alwaysPullImage ? "" : ", alwaysPullImage=" + alwaysPullImage) +
                (command == null ? "" : ", command='" + command + '\'') +
                (args == null ? "" : ", args='" + args + '\'') +
//                (remoteFs == null ? "" : ", remoteFs='" + remoteFs + '\'') +
                (instanceCap == Integer.MAX_VALUE ? "" : ", instanceCap=" + instanceCap) +
                (slaveConnectTimeout == DEFAULT_SLAVE_JENKINS_CONNECTION_TIMEOUT ? "" : ", slaveConnectTimeout=" + slaveConnectTimeout) +
                (idleMinutes == 0 ? "" : ", idleMinutes=" + idleMinutes) +
//                (activeDeadlineSeconds == 0 ? "" : ", activeDeadlineSeconds=" + activeDeadlineSeconds) +
                (label == null ? "" : ", label='" + label + '\'') +
//                (nodeSelector == null ? "" : ", nodeSelector='" + nodeSelector + '\'') +
                (nodeUsageMode == null ? "" : ", nodeUsageMode=" + nodeUsageMode) +
                (resourcesCPU == null ? "" : ", resourcesCpu='" + resourcesCPU + '\'') +
                (resourcesMemory == null ? "" : ", resourcesMemory='" + resourcesMemory + '\'') +
//                (resourceLimitCpu == null ? "" : ", resourceLimitCpu='" + resourceLimitCpu + '\'') +
//                (resourceLimitMemory == null ? "" : ", resourceLimitMemory='" + resourceLimitMemory + '\'') +
//                (!customWorkspaceVolumeEnabled ? "" : ", customWorkspaceVolumeEnabled=" + customWorkspaceVolumeEnabled) +
//                (workspaceVolume == null ? "" : ", workspaceVolume=" + workspaceVolume) +
                //(volumes == null || volumes.isEmpty() ? "" : ", volumes=" + volumes) +
//                (containers == null || containers.isEmpty() ? "" : ", containers=" + containers) +
                (envVars == null || envVars.isEmpty() ? "" : ", envVars=" + envVars) +
//                (annotations == null || annotations.isEmpty() ? "" : ", annotations=" + annotations) +
//                (imagePullSecrets == null || imagePullSecrets.isEmpty() ? "" : ", imagePullSecrets=" + imagePullSecrets) +
                (nodeProperties == null || nodeProperties.isEmpty() ? "" : ", nodeProperties=" + nodeProperties) +
                '}';
    }
}
