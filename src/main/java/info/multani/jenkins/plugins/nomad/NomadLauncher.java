/*
 * The MIT License
 *
 * Copyright (c) 2017, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package info.multani.jenkins.plugins.nomad;

import com.google.common.base.Throwables;
import com.hashicorp.nomad.apimodel.Job;
import com.hashicorp.nomad.javasdk.ErrorResponseException;
import com.hashicorp.nomad.javasdk.EvaluationResponse;
import com.hashicorp.nomad.javasdk.NomadApiClient;
import com.hashicorp.nomad.javasdk.ServerQueryResponse;
import hudson.AbortException;
import hudson.model.TaskListener;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import static java.util.logging.Level.*;
import java.util.logging.Logger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Launches on Nomad the specified {@link NomadComputer} instance.
 */
public class NomadLauncher extends JNLPLauncher {

    private static final Logger LOGGER = Logger.getLogger(NomadLauncher.class.getName());

    private boolean launched;

    @DataBoundConstructor
    public NomadLauncher(String tunnel, String vmargs) {
        super(tunnel, vmargs);
    }

    public NomadLauncher() {
        super();
    }

    @Override
    public boolean isLaunchSupported() {
        return !launched;
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) {
        PrintStream logger = listener.getLogger();

        if (!(computer instanceof NomadComputer)) {
            throw new IllegalArgumentException("This Launcher can be used only with KubernetesComputer");
        }
        NomadComputer kubernetesComputer = (NomadComputer) computer;
        computer.setAcceptingTasks(false);
        NomadSlave slave = kubernetesComputer.getNode();
        if (slave == null) {
            throw new IllegalStateException("Node has been removed, cannot launch " + computer.getName());
        }
        if (launched) {
            LOGGER.log(INFO, "Agent has already been launched, activating: {}", slave.getNodeName());
            computer.setAcceptingTasks(true);
            return;
        }

        NomadCloud cloud = slave.getNomadCloud();
        final NomadJobTemplate unwrappedTemplate = slave.getTemplate();
        try {
            NomadApiClient client = cloud.connect();
            Job job = getJobTemplate(slave, unwrappedTemplate);

            String jobID = job.getId();

            LOGGER.log(Level.FINE, "Creating Nomad job: {0}", jobID);

            EvaluationResponse evaluation;
            try {
                evaluation = client.getJobsApi().register(job);
            }
            catch (ErrorResponseException exc) {
                String msg = String.format("Unable to evaluate Nomad job '%s': %s", jobID, exc.getServerErrorMessage());
                LOGGER.log(Level.SEVERE, msg, exc);
                throw new AbortException(msg); // TODO: we should probably abort the build here, but AbortException doesn't do it.
            }

            String evaluationID = evaluation.getValue();
            LOGGER.log(INFO, "Registered Nomad job {0} with evaluation ID: {1}",
                    new Object[]{jobID, evaluationID});
            LOGGER.log(FINE, "Created Nomad job: {0}", jobID);
            
            logger.printf("[Nomad] Registered Nomad job %s with evaluation ID %s%n",
                    jobID, evaluationID);

            // We need the job to be running and connected before returning
            // otherwise this method keeps being called multiple times
//            List<String> validStates = ImmutableList.of("Running");

            int i = 0;
            int j = 100; // wait 600 seconds

            // TODO: wait for Job to be running
//            List<ContainerStatus> containerStatuses = null;
            // wait for Job to be running
            for (; i < j; i++) {
                LOGGER.log(INFO, "Waiting for job to be scheduled ({1}/{2}): {0}", new Object[]{jobID, i, j});
                logger.printf("Waiting for job to be scheduled (%2$s/%3$s): %1$s%n", jobID, i, j);

                Thread.sleep(6000);
                ServerQueryResponse<Job> response = client.getJobsApi().info(jobID);

                if (response == null) { // can exist?
                    throw new IllegalStateException("Job no longer exists: " + jobID);
                }

                Job jobFound = response.getValue();
                if (jobFound.getStatus().equals("running")) {
                    break;
                }

                LOGGER.log(INFO, "Container {0} is: {1}",
                        new Object[]{jobID, jobFound.getStatus()});
                logger.printf("Container %1$s is: %2$s%n",
                        jobID, jobFound.getStatus());

//                containerStatuses = job.getStatus().getContainerStatuses();
//                List<ContainerStatus> terminatedContainers = new ArrayList<>();
//                Boolean allContainersAreReady = true;
//                for (ContainerStatus info : containerStatuses) {
//                    if (info != null) {
//                        if (info.getState().getWaiting() != null) {
//                            // Job is waiting for some reason
//                            LOGGER.log(INFO, "Container is waiting {0} [{2}]: {1}",
//                                    new Object[]{jobID, info.getState().getWaiting(), info.getName()});
//                            logger.printf("Container is waiting %1$s [%3$s]: %2$s%n",
//                                    jobID, info.getState().getWaiting(), info.getName());
//                            // break;
//                        }
//                        if (info.getState().getTerminated() != null) {
//                            terminatedContainers.add(info);
//                        } else if (!info.getReady()) {
//                            allContainersAreReady = false;
//                        }
//                    }
//                }
//
//                if (!allContainersAreReady) {
//                    continue;
//                }
//
//                if (validStates.contains(job.getStatus().getPhase())) {
//                    break;
//                }
//            }
//            if (!validStates.contains(status)) {
//                throw new IllegalStateException("Container is not running after " + j + " attempts, status: " + status);
//            }
            }

            String status = job.getStatus();

            j = unwrappedTemplate.getSlaveConnectTimeout();

            // now wait for agent to be online
            for (; i < j; i++) {
                if (slave.getComputer() == null) {
                    throw new IllegalStateException("Node was deleted, computer is null");
                }
                if (slave.getComputer().isOnline()) {
                    break;
                }
                LOGGER.log(INFO, "Waiting for agent to connect ({1}/{2}): {0}", new Object[]{jobID, i, j});
                logger.printf("Waiting for agent to connect (%2$s/%3$s): %1$s%n", jobID, i, j);
                Thread.sleep(1000);
            }
            if (!slave.getComputer().isOnline()) {
                throw new IllegalStateException("Agent is not connected after " + j + " attempts, status: " + status);
            }
            computer.setAcceptingTasks(true);
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING, String.format("Error in provisioning; agent=%s, template=%s", slave, unwrappedTemplate), ex);
            LOGGER.log(Level.FINER, "Removing Jenkins node: {0}", slave.getNodeName());
            try {
                slave.terminate();
            } catch (IOException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Unable to remove Jenkins node", e);
            }
            throw Throwables.propagate(ex);
        }
        launched = true;
        try {
            // We need to persist the "launched" setting...
            slave.save();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not save() agent: " + e.getMessage(), e);
        } catch (Exception ex) {
            Logger.getLogger(NomadLauncher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Job getJobTemplate(NomadSlave slave, NomadJobTemplate template) {
        return template == null ? null : template.build(slave);
    }
}
