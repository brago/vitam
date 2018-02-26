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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;

public class AccessionRegisterActionHandlerTest {
    private static final String ARCHIVE_ID_TO_GUID_MAP = "ARCHIVE_ID_TO_GUID_MAP_obj.json";
    private static final String OBJECT_GROUP_ID_TO_GUID_MAP = "OBJECT_GROUP_ID_TO_GUID_MAP_obj.json";
    private static final String BDO_TO_BDO_INFO_MAP = "BDO_TO_BDO_INFO_MAP_obj.json";
    private static final String ATR_GLOBAL_SEDA_PARAMETERS = "globalSEDAParameters.json";
    private static final String FAKE_URL = "http://localhost:8080";
    AccessionRegisterActionHandler accessionRegisterHandler;
    private static final String HANDLER_ID = "ACCESSION_REGISTRATION";
    private HandlerIOImpl handlerIO;
    private GUID guid;
    private WorkerParameters params;
    private static final Integer TENANT_ID = 0;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    MetaDataClient metaDataClient = mock(MetaDataClient.class);
    MetaDataClientFactory metaDataClientFactory = mock(MetaDataClientFactory.class);

    AdminManagementClient adminManagementClient = mock(AdminManagementClient.class);
    AdminManagementClientFactory adminManagementClientFactory = mock(AdminManagementClientFactory.class);

    @Before
    public void setUp() throws Exception {
        AdminManagementClientFactory.changeMode(null);
        guid = GUIDFactory.newGUID();
        params =
            WorkerParametersFactory.newWorkerParameters().setUrlWorkspace(FAKE_URL).setUrlMetadata(FAKE_URL)
                .setObjectNameList(Lists.newArrayList("objectName.json")).setObjectName("objectName.json")
                .setCurrentStep("currentStep").setContainerName(guid.getId());
        handlerIO = new HandlerIOImpl(guid.getId(), "workerId");

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(adminManagementClientFactory.getClient()).thenReturn(adminManagementClient);
    }

    @After
    public void end() {
        handlerIO.partialClose();
    }

    @Test
    @RunWithCustomExecutor
    public void testResponseOK() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        params.setContainerName(operationId.getId());

        List<UnitPerOriginatingAgency> originatingAgencies = new ArrayList<>();
        originatingAgencies.add(new UnitPerOriginatingAgency("sp1", 3));

        reset(metaDataClient);
        reset(adminManagementClient);
        when(metaDataClient.selectAccessionRegisterOnUnitByOperationId(operationId.toString()))
            .thenReturn(originatingAgencies);

        when(adminManagementClient.getAccessionRegisterDetailRaw(anyObject(), anyObject()))
            .thenReturn(
                VitamCodeHelper.toVitamError(VitamCode.REFERENTIAL_NOT_FOUND, "where no fund has gone before..."));

        AdminManagementClientFactory.changeMode(null);
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/ARCHIVE_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/BDO_TO_BDO_INFO_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "ATR/globalSEDAParameters.json")));
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(2, PropertiesUtils.getResourceFile(BDO_TO_BDO_INFO_MAP), false);
        handlerIO.addOuputResult(3, PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS), false);
        handlerIO.reset();
        handlerIO.addInIOParameters(in);
        accessionRegisterHandler =
            new AccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(AccessionRegisterActionHandler.getId(), HANDLER_ID);

        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertNotNull(response.getEvDetailData());
        assertNotNull(JsonHandler.getFromString(response.getEvDetailData()).get("Volumetry"));
        assertEquals(1, JsonHandler.getFromString(response.getEvDetailData()).get("Volumetry").size());
        JsonNode register = ((ArrayNode) JsonHandler.getFromString(response.getEvDetailData()).get("Volumetry")).get(0);
        assertEquals("sp1", register.get("OriginatingAgency").asText());
        assertEquals("FRAN_NP_005061", register.get("SubmissionAgency").asText());
        assertEquals("STORED_AND_COMPLETED", register.get("Status").asText());
        assertEquals(true, register.get("Symbolic").asBoolean());
        assertEquals(0, register.get("TotalUnits").get("ingested").asInt());
        assertEquals(3, register.get("TotalUnits").get("attached").asInt());
        assertEquals(3, register.get("TotalUnits").get("symbolicRemained").asInt());
    }

    @Test
    @RunWithCustomExecutor
    public void testResponseKOCouldNotCreateRegister() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        params.setContainerName(operationId.getId());

        List<UnitPerOriginatingAgency> originatingAgencies = new ArrayList<>();
        originatingAgencies.add(new UnitPerOriginatingAgency("sp1", 3));

        reset(metaDataClient);
        reset(adminManagementClient);
        when(metaDataClient.selectAccessionRegisterOnUnitByOperationId(operationId.toString()))
            .thenReturn(originatingAgencies);

        when(adminManagementClient.getAccessionRegisterDetailRaw(anyObject(), anyObject()))
            .thenReturn(
                VitamCodeHelper.toVitamError(VitamCode.REFERENTIAL_REPOSITORY_DATABASE_ERROR, "database down..."));

        when(adminManagementClient.createorUpdateAccessionRegister(anyObject()))
            .thenThrow(new AdminManagementClientServerException("AdminManagementClientServerException"));

        AdminManagementClientFactory.changeMode(null);
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/ARCHIVE_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/BDO_TO_BDO_INFO_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "ATR/globalSEDAParameters.json")));
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(2, PropertiesUtils.getResourceFile(BDO_TO_BDO_INFO_MAP), false);
        handlerIO.addOuputResult(3, PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS), false);
        handlerIO.reset();
        handlerIO.addInIOParameters(in);
        accessionRegisterHandler =
            new AccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(AccessionRegisterActionHandler.getId(), HANDLER_ID);

        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.KO, response.getGlobalStatus());

    }

    @Test
    @RunWithCustomExecutor
    public void testResponseKOConflictRegister() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        params.setContainerName(operationId.getId());

        List<UnitPerOriginatingAgency> originatingAgencies = new ArrayList<>();
        originatingAgencies.add(new UnitPerOriginatingAgency("sp1", 3));

        reset(metaDataClient);
        reset(adminManagementClient);
        when(metaDataClient.selectAccessionRegisterOnUnitByOperationId(operationId.toString()))
            .thenReturn(originatingAgencies);

        when(adminManagementClient.getAccessionRegisterDetailRaw(anyObject(), anyObject()))
            .thenThrow(new VitamClientException("It happens..."));

        when(adminManagementClient.createorUpdateAccessionRegister(anyObject()))
            .thenThrow(new DatabaseConflictException("DatabaseConflictException"));

        AdminManagementClientFactory.changeMode(null);
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/ARCHIVE_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/BDO_TO_BDO_INFO_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "ATR/globalSEDAParameters.json")));
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(2, PropertiesUtils.getResourceFile(BDO_TO_BDO_INFO_MAP), false);
        handlerIO.addOuputResult(3, PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS), false);
        handlerIO.reset();
        handlerIO.addInIOParameters(in);
        accessionRegisterHandler =
            new AccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(AccessionRegisterActionHandler.getId(), HANDLER_ID);

        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.KO, response.getGlobalStatus());

    }


    @Test
    @RunWithCustomExecutor
    public void testResponseFatal() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);

        params.setContainerName(operationId.getId());

        List<UnitPerOriginatingAgency> originatingAgencies = new ArrayList<>();
        originatingAgencies.add(new UnitPerOriginatingAgency("sp1", 3));

        reset(metaDataClient);
        reset(adminManagementClient);
        when(metaDataClient.selectAccessionRegisterOnUnitByOperationId(operationId.toString()))
            .thenThrow(new MetaDataClientServerException("MetaDataClientServerException"));

        AdminManagementClientFactory.changeMode(null);
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/ARCHIVE_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/BDO_TO_BDO_INFO_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "ATR/globalSEDAParameters.json")));
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(2, PropertiesUtils.getResourceFile(BDO_TO_BDO_INFO_MAP), false);
        handlerIO.addOuputResult(3, PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS), false);
        handlerIO.reset();
        handlerIO.addInIOParameters(in);
        accessionRegisterHandler =
            new AccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(AccessionRegisterActionHandler.getId(), HANDLER_ID);
        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);
        // Then
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    @RunWithCustomExecutor
    public void testResponseOKWithNoAgency() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        GUID operationId = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setRequestId(operationId);
        reset(metaDataClient);
        reset(adminManagementClient);
        params.setContainerName(operationId.getId());
        AdminManagementClientFactory.changeMode(null);
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/ARCHIVE_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/OBJECT_GROUP_ID_TO_GUID_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "Maps/BDO_TO_BDO_INFO_MAP.json")));
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "ATR/globalSEDAParameters.json")));
        handlerIO.addOutIOParameters(in);
        handlerIO.addOuputResult(0, PropertiesUtils.getResourceFile(ARCHIVE_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(1, PropertiesUtils.getResourceFile(OBJECT_GROUP_ID_TO_GUID_MAP), false);
        handlerIO.addOuputResult(2, PropertiesUtils.getResourceFile(BDO_TO_BDO_INFO_MAP), false);
        handlerIO.addOuputResult(3, PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS), false);
        handlerIO.reset();
        handlerIO.addInIOParameters(in);
        accessionRegisterHandler =
            new AccessionRegisterActionHandler(metaDataClientFactory, adminManagementClientFactory);
        assertEquals(AccessionRegisterActionHandler.getId(), HANDLER_ID);

        // When
        final ItemStatus response = accessionRegisterHandler.execute(params, handlerIO);

        // Then
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

}
