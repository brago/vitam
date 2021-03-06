/*******************************************************************************
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
 *******************************************************************************/

package fr.gouv.vitam.worker.core.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UpdateWorkflowConstants;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({MetaDataClientFactory.class, WorkspaceClientFactory.class})
public class ListArchiveUnitsActionHandlerTest {

    ListArchiveUnitsActionHandler plugin = new ListArchiveUnitsActionHandler();
    private MetaDataClient metadataClient;
    private MetaDataClientFactory metadataClientFactory;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;

    private HandlerIOImpl action;
    private GUID guid = GUIDFactory.newGUID();
    private List<IOParameter> out;

    private static final String UPDATED_RULES_JSON = "ListArchiveUnitsActionPlugin/updatedRules.json";
    private static final String UPDATED_AU = "ListArchiveUnitsActionPlugin/archiveUnitsToBeUpdated.json";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("archiveUnit.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.UPDATE);

    public ListArchiveUnitsActionHandlerTest() {
        // do nothing
    }

    @Before
    public void setUp() throws Exception {
        File tempFolder = folder.newFolder();
        System.setProperty("vitam.tmp.folder", tempFolder.getAbsolutePath());
        SystemPropertyUtil.refresh();

        PowerMockito.mockStatic(MetaDataClientFactory.class);
        metadataClient = mock(MetaDataClient.class);
        metadataClientFactory = mock(MetaDataClientFactory.class);

        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);

        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(metadataClientFactory);
        PowerMockito.when(MetaDataClientFactory.getInstance().getClient())
            .thenReturn(metadataClient);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient())
            .thenReturn(workspaceClient);

        action = new HandlerIOImpl(guid.getId(), "workerId");
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.WORKSPACE,
            UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.AU_TO_BE_UPDATED_JSON)));
    }

    @After
    public void clean() {        
        action.partialClose();
    }


    @Test
    public void givenRunningProcessWhenExecuteThenReturnResponseOK() throws Exception {
        action.addOutIOParameters(out);

        final InputStream updatedRules =
            PropertiesUtils.getResourceAsStream(UPDATED_RULES_JSON);
        final JsonNode archiveUnitsToBeUpdated =
            JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream(UPDATED_AU));
        try {
            reset(workspaceClient);
            reset(metadataClient);
            when(workspaceClient.getObject(anyObject(), eq("PROCESSING/updatedRules.json")))
                .thenReturn(Response.status(Status.OK).entity(updatedRules).build());
            when(metadataClient.selectUnits(anyObject())).thenReturn(archiveUnitsToBeUpdated);

            saveWorkspacePutObject(
                UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.AU_TO_BE_UPDATED_JSON);            
            saveWorkspacePutObject(
                UpdateWorkflowConstants.UNITS_FOLDER + "/" + "aeaqaaaaaagds5zjaabmaak5mlsoesaaaaba.json");
            final ItemStatus response = plugin.execute(params, action);
            assertEquals(StatusCode.OK, response.getGlobalStatus());

            JsonNode auToBeUpdated = getSavedWorkspaceObject(
                UpdateWorkflowConstants.PROCESSING_FOLDER + "/" + UpdateWorkflowConstants.AU_TO_BE_UPDATED_JSON);
            int numberOfAu = 0;
            for (final JsonNode objNode : auToBeUpdated) {
                numberOfAu++;
            }
            assertEquals(3, numberOfAu);

            JsonNode aeaqaaaaaagds5zjaabmaak5mlsoesaaaaba = getSavedWorkspaceObject(
                UpdateWorkflowConstants.UNITS_FOLDER + "/" + "aeaqaaaaaagds5zjaabmaak5mlsoesaaaaba.json");
            assertNotNull(aeaqaaaaaagds5zjaabmaak5mlsoesaaaaba);
            int numberOfRulesInvolved = 0;
            for (final JsonNode objNode : aeaqaaaaaagds5zjaabmaak5mlsoesaaaaba) {
                numberOfRulesInvolved++;
            }
            assertEquals(3, numberOfRulesInvolved);
        } finally {
            updatedRules.close();
        }

    }

    private void saveWorkspacePutObject(String filename) throws ContentAddressableStorageServerException {
        doAnswer(invocation -> {
            InputStream inputStream = invocation.getArgumentAt(2, InputStream.class);
            java.nio.file.Path file =
                java.nio.file.Paths.get(System.getProperty("vitam.tmp.folder") + "/" + action.getContainerName() + "_" +
                    action.getWorkerId() + "/" + filename.replaceAll("/", "_"));
            java.nio.file.Files.copy(inputStream, file);
            return null;
        }).when(workspaceClient).putObject(org.mockito.Matchers.anyString(),
            org.mockito.Matchers.eq(filename), org.mockito.Matchers.any(InputStream.class));
    }

    private JsonNode getSavedWorkspaceObject(String filename) throws InvalidParseOperationException {
        File objectNameFile = new File(System.getProperty("vitam.tmp.folder") + "/" + action.getContainerName() + "_" +
            action.getWorkerId() + "/" + filename.replaceAll("/", "_"));
        return JsonHandler.getFromFile(objectNameFile);
    }

    @Test
    public void givenProcessErrorsWhenExecuteThenReturnResponseFatal() throws Exception {
        action.addOutIOParameters(out);

        reset(workspaceClient);
        when(workspaceClient.getObject(anyObject(), eq("PROCESSING/updatedRules.json")))
            .thenThrow(new ContentAddressableStorageNotFoundException("Storage not found"));
        final ItemStatus response = plugin.execute(params, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());


        reset(workspaceClient);
        when(workspaceClient.getObject(anyObject(), eq("PROCESSING/updatedRules.json")))
            .thenReturn(Response.status(Status.OK)
                .entity(IOUtils.toInputStream("<root><random>Random XML tags</random></root>", "UTF-8")).build());
        final ItemStatus response2 = plugin.execute(params, action);
        assertEquals(StatusCode.FATAL, response2.getGlobalStatus());


    }

    @Test
    public void givenProcessErrorMetadataWhenExecuteThenReturnResponseFatal() throws Exception {
        final InputStream updatedRules =
            PropertiesUtils.getResourceAsStream(UPDATED_RULES_JSON);
        try {
            action.addOutIOParameters(out);
            reset(workspaceClient);
            reset(metadataClient);

            when(workspaceClient.getObject(anyObject(), eq("PROCESSING/updatedRules.json")))
                .thenReturn(Response.status(Status.OK).entity(updatedRules).build());
            when(metadataClient.selectUnits(anyObject()))
                .thenThrow(new MetaDataExecutionException("Error while requesting Metadata"));
            assertThatThrownBy(() -> plugin.execute(params, action)).isExactlyInstanceOf(IllegalStateException.class);
        } finally {
            updatedRules.close();
        }

    }


}
