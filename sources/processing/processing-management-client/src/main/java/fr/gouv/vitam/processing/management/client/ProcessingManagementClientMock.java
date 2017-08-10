/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.processing.management.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.SingletonUtils;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.WorkflowNotFoundException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ActionDefinition;
import fr.gouv.vitam.processing.common.model.Distribution;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.IOParameter;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.ProcessWorkflow;
import fr.gouv.vitam.processing.common.model.ProcessingUri;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkFlow;
import fr.gouv.vitam.processing.common.model.WorkerBean;

/**
 *
 */
public class ProcessingManagementClientMock extends AbstractMockClient implements ProcessingManagementClient {

    private static final String FAKE_EXECUTION_STATUS = "Fake";

    ProcessingManagementClientMock() {
        // Empty
    }

    @Override
    public boolean isOperationCompleted(String operationId) {
        return false;
    }

    @Override
    public ItemStatus getOperationProcessStatus(String id)
        throws VitamClientException, InternalServerException, BadRequestException {
        final List<Integer> status = new ArrayList<>();
        status.add(0);
        status.add(0);
        status.add(1);
        status.add(0);
        status.add(0);
        status.add(0);
        return new ItemStatus("FakeId", "FakeMessage", StatusCode.OK, status, SingletonUtils.singletonMap(), null,
            null, null);
    }


    // TODO FIXE ME query never user
    @Override
    public ItemStatus getOperationProcessExecutionDetails(String id, JsonNode query)
        throws VitamClientException, InternalServerException, BadRequestException {
        final List<Integer> status = new ArrayList<>();
        status.add(0);
        status.add(0);
        status.add(1);
        status.add(0);
        status.add(0);
        status.add(0);
        return new ItemStatus("FakeId", "FakeMessage", StatusCode.OK, status, SingletonUtils.singletonMap(), null,
            null, null);
    }



    @Override
    public ItemStatus cancelOperationProcessExecution(String id)
        throws InternalServerException, BadRequestException, VitamClientException {
        final List<Integer> status = new ArrayList<>();
        status.add(0);
        status.add(0);
        status.add(1);
        status.add(0);
        status.add(0);
        status.add(0);
        final ItemStatus itemStatus =
            new ItemStatus("FakeId", "FakeMessage", StatusCode.OK, status, SingletonUtils.singletonMap(), null,
                null, null);

        return itemStatus;
    }



    @Override
    public RequestResponse<ItemStatus> updateOperationActionProcess(String actionId, String operationId)
        throws InternalServerException, BadRequestException, VitamClientException {
        return new RequestResponseOK<>();
    }



    @Override
    public RequestResponse<JsonNode> executeOperationProcess(String operationId, String workflow, String contextId,
        String actionId)
        throws InternalServerException, BadRequestException, VitamClientException {
        return new RequestResponseOK<JsonNode>().addHeader(GlobalDataRest.X_GLOBAL_EXECUTION_STATE,
            FAKE_EXECUTION_STATUS);

    }



    @Override
    public void initWorkFlow(String contextId) throws VitamException {

    }



    @Override
    public ItemStatus updateVitamProcess(String contextId, String actionId, String container, String workflow)
        throws InternalServerException, BadRequestException, VitamClientException {
        final List<Integer> status = new ArrayList<>();
        status.add(0);
        status.add(0);
        status.add(1);
        status.add(0);
        status.add(0);
        status.add(0);
        return new ItemStatus("FakeId", "FakeMessage", StatusCode.OK, status, SingletonUtils.singletonMap(), null,
            null, null);
    }



    @Override
    public void registerWorker(String familyId, String workerId, WorkerBean workerDescription)
        throws ProcessingBadRequestException, WorkerAlreadyExistsException {
        // TODO Auto-generated method stub

    }



    @Override
    public void unregisterWorker(String familyId, String workerId) throws ProcessingBadRequestException {
        // TODO Auto-generated method stub

    }



    @Override
    public void initVitamProcess(String contextId, String container, String workflow)
        throws InternalServerException, VitamClientException, BadRequestException {}



    @Override
    public RequestResponse<JsonNode> listOperationsDetails(ProcessQuery query) throws VitamClientException {
        ProcessWorkflow pw = new ProcessWorkflow();
        pw.setOperationId(GUIDFactory.newOperationLogbookGUID(0).toString());
        pw.setState(ProcessState.RUNNING);
        pw.setStatus(StatusCode.STARTED);
        List<JsonNode> list = new ArrayList<JsonNode>();
        try {
            list.add(JsonHandler.toJsonNode(pw));
        } catch (InvalidParseOperationException e) {
            throw new VitamClientException(e.getMessage());
        }
        return new RequestResponseOK<JsonNode>().addAllResults(list);
    }

    @Override
    public Response executeCheckTraceabilityWorkFlow(String checkOperationId, JsonNode query, String workflow,
        String contextId, String actionId)
        throws InternalServerException, BadRequestException, WorkflowNotFoundException {
        // TODO Add headers to response
        return Response.ok().build();
    }

    @Override
    public RequestResponse<JsonNode> getWorkflowDefinitions() throws VitamClientException {
        Map<String, WorkFlow> workflowDefinitions = new HashMap<>();
        WorkFlow workflow = new WorkFlow();

        List<Action> actions = new ArrayList<>();
        actions.add(getAction("CHECK_DIGEST", ProcessBehavior.BLOCKING, new ArrayList<IOParameter>(
            Arrays.asList(new IOParameter().setName("algo").setUri(new ProcessingUri("VALUE", "SHA-512")))),
            null));
        actions.add(getAction("OG_OBJECTS_FORMAT_CHECK", ProcessBehavior.BLOCKING, null, null));

        List<Step> steps = new ArrayList<>();
        steps.add(new Step()
            .setWorkerGroupId("DefaultWorker")
            .setStepName("STP_OG_CHECK_AND_TRANSFORME")
            .setBehavior(ProcessBehavior.BLOCKING)
            .setDistribution(new Distribution().setKind(DistributionKind.LIST).setElement("ObjectGroup"))
            .setActions(actions));

        workflow.setId("DefaultIngestWorkflow");
        workflow.setIdentifier("PROCESS_SIP_UNITARY");
        workflow.setName("Default Ingest Workflow");
        workflow.setTypeProc("INGEST");
        workflow.setComment("DefaultIngestWorkflow comment");
        workflow.setSteps(steps);
        workflowDefinitions.put(workflow.getIdentifier(), workflow);

        try {
            return new RequestResponseOK().addResult(JsonHandler.toJsonNode(workflowDefinitions))
                .setHttpCode(Status.OK.getStatusCode());
        } catch (InvalidParseOperationException e) {
            throw new VitamClientException(e.getMessage());
        }
    }

    /**
     * Create a POJO action
     * 
     * @param actionKey action key
     * @param actionBehavior action behavior
     * @param in list of IO ins
     * @param out list of IO outs
     * @return Action object
     */
    private Action getAction(String actionKey, ProcessBehavior actionBehavior, List<IOParameter> in,
        List<IOParameter> out) {
        ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey(actionKey);
        actionDefinition.setBehavior(actionBehavior);
        actionDefinition.setIn(in);
        actionDefinition.setOut(out);
        return new Action().setActionDefinition(actionDefinition);

    }


}
