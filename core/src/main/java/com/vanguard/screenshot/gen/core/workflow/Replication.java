/*
 ****************************************************************************
 *
 * Copyright (c)2024 The Vanguard Group of Investment Companies (VGI)
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS
 * IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 ****************************************************************************
 */
package com.vanguard.screenshot.gen.core.workflow;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;

import com.day.cq.replication.*;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


@Component(
        service=WorkflowProcess.class,
        property = {"process.label= Replication"}
)
public class Replication implements WorkflowProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ACTION = "action";

    private static final String AGENT = "agent";

    private static final String WORKFLOW_PROCESS_ARGUMENTS = "PROCESS_ARGS";

    @Reference
    private Replicator replicator;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap args) throws WorkflowException {

        final WorkflowData workflowData = workItem.getWorkflowData();

        if (args.containsKey(WORKFLOW_PROCESS_ARGUMENTS)){
            LOGGER.debug("workflow arguments : {}",args.get(WORKFLOW_PROCESS_ARGUMENTS,String.class));
            String argsMap = args.get(WORKFLOW_PROCESS_ARGUMENTS,String.class);

            Map<String, String> result = Arrays.stream(argsMap.split("\n"))
                    .map(s -> s.split("="))
                    .collect(Collectors.toMap(
                            a -> a[0],  //key
                            a -> a[1]   //value
                    ));

            // Get the path to the JCR resource from the payload
            final String payloadPath = workflowData.getPayload().toString();

            replicate(workflowSession.adaptTo(Session.class), payloadPath, result.get(AGENT));

        }else{
            // Terminate the workflow if there are no arguments in this Process Step
            LOGGER.error("Terminating the workflow as no arguments are passed in this Process Step");
            workflowSession.terminateWorkflow(workItem.getWorkflow());
        }
    }

    public void replicate(Session session, String payloadPath, String agent){
        if(session == null || StringUtils.isEmpty(payloadPath)|| StringUtils.isEmpty(agent)){
            LOGGER.error("Invalid arguments passed, Arguments : [Is passed Session null: {}, Replication Action Type : {}, Payload Path :{}, Agent Id Pattern : {}]",session == null, payloadPath, agent);
            return;
        }

        ReplicationOptions options = new ReplicationOptions();
        AgentIdFilter filter = new AgentIdFilter(agent);
        options.setFilter(filter);
        options.setSuppressStatusUpdate(true);
        try {
            replicator.replicate(session, ReplicationActionType.ACTIVATE, payloadPath, options);
        } catch (ReplicationException ex) {
            LOGGER.error("Error while replicating content, Arguments : [Replication Action Type : {}, Payload Path :{}, Agent Id Pattern : {}] \\n And the exception is {}", payloadPath, agent, ex);
        }
    }
}
