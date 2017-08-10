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
package fr.gouv.vitam.access.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

import fr.gouv.vitam.access.internal.api.AccessInternalModule;
import fr.gouv.vitam.access.internal.api.DataCategory;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalExecutionException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalRuleExecutionException;
import fr.gouv.vitam.access.internal.common.model.AccessInternalConfiguration;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.Action;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserHelper;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.error.VitamCodeHelper;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.common.model.VitamConstants.AppraisalRuleFinalAction;
import fr.gouv.vitam.common.model.VitamConstants.StorageRuleFinalAction;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.QualifiersJson;
import fr.gouv.vitam.common.model.objectgroup.VersionsJson;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.AsyncInputStreamHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.StorageCollectionType;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;


/**
 * AccessModuleImpl implements AccessModule
 */
public class AccessInternalModuleImpl implements AccessInternalModule {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessInternalModuleImpl.class);

    private final LogbookLifeCyclesClient logbookLifeCycleClientMock;
    private final LogbookOperationsClient logbookOperationClientMock;
    private final StorageClient storageClientMock;
    private final WorkspaceClient workspaceClientMock;

    private static final String DEFAULT_STORAGE_STRATEGY = "default";
    private static final String ID_CHECK_FAILED = "the unit_id should be filled";
    private static final String STP_UPDATE_UNIT = "STP_UPDATE_UNIT";
    private static final String UNIT_METADATA_UPDATE = "UNIT_METADATA_UPDATE";
    private static final String UNIT_CHECK_RULES = "UNIT_METADATA_UPDATE_CHECK_RULES"; 
    private static final String UNIT_METADATA_STORAGE = "UNIT_METADATA_STORAGE";
    private static final String _DIFF = "$diff";
    private static final String _ID = "_id";
    private static final String RESULTS = "$results";
    private static final String MIME_TYPE = "MimeType";
    private static final String METADATA_INTERNAL_SERVER_ERROR = "Metadata internal server error";
    private static final String LOGBOOK_OPERATION_ALREADY_EXISTS = "logbook operation already exists";
    private static final String LOGBOOK_CLIENT_BAD_REQUEST_ERROR = "logbook client bad request error";
    private static final String LOGBOOK_CLIENT_NOT_FOUND_ERROR = "logbook client not found error";
    private static final String METADATA_EXECUTION_EXECUTION_ERROR = "metadata execution execution error";
    private static final String DOCUMENT_CLIENT_SERVER_ERROR = "document client server error";
    private static final String METADATA_DOCUMENT_SIZE_ERROR = "metadata document size error";
    private static final String ILLEGAL_ARGUMENT = "illegal argument";
    private static final String PARSING_ERROR = "parsing error";
    private static final String CLIENT_SERVER_ERROR = "client server error";
    private static final String CLIENT_NOT_FOUND = "client not found";
    private static final String BAD_REQUEST = "bad request";
    private static final String DIFF = "#diff";
    private static final String ID = "#id";
    private static final String DEFAULT_STRATEGY = "default";


    private static final String WORKSPACE_SERVER_EXCEPTION = "workspace server exception";
    private static final String STORAGE_SERVER_EXCEPTION = "Storage server exception";
    private static final String JSON = ".json";
    private static final String CANNOT_CREATE_A_FILE = "Cannot create a file: ";
    private static final String CANNOT_FOUND_OR_READ_SOURCE_FILE = "Cannot found or read source file: ";
    private static final String ARCHIVE_UNIT_NOT_FOUND = "Archive unit not found";
    private static final String ERROR_ADD_CONDITION = "Error during adding condition of Operations";
    private static final String ERROR_CHECK_RULES = "Error during checking updated rules";
    private static final String ERROR_UPDATE_RULE = "Can't Update Rule: ";
    private static final String ERROR_CREATE_RULE = "Can't Create Rule: ";
    private static final String ERROR_DELETE_RULE = "Can't Delete Rule: ";
    private static final String ERROR_PREVENT_INHERITANCE = " contains an inheritance prevention";
    private static final String MANAGEMENT_KEY = "#management";
    private static final String RULES_KEY = "Rules";
    private static final String INHERITANCE_KEY = "Inheritance";
    private static final String MANAGEMENT_PREFIX = MANAGEMENT_KEY + '.';
    private static final String RULES_PREFIX = '.' + RULES_KEY;

    /**
     * AccessModuleImpl constructor
     *
     * @param configuration of mongoDB access
     */
    // constructor
    public AccessInternalModuleImpl(AccessInternalConfiguration configuration) {
        ParametersChecker.checkParameter("Configuration cannot be null", configuration);
        storageClientMock = null;
        logbookLifeCycleClientMock = null;
        logbookOperationClientMock = null;
        workspaceClientMock = null;
    }

    /**
     * AccessModuleImpl constructor <br>
     * with metaDataClientFactory, configuration and logbook operation client and lifecycle
     *
     * @param storageClient a StorageClient instance
     * @param pLogbookOperationClient logbook operation client
     * @param pLogbookLifeCycleClient logbook lifecycle client
     */
    AccessInternalModuleImpl(StorageClient storageClient, LogbookOperationsClient pLogbookOperationClient,
        LogbookLifeCyclesClient pLogbookLifeCycleClient, WorkspaceClient workspaceClient) {
        storageClientMock = storageClient;
        logbookOperationClientMock = pLogbookOperationClient;
        logbookLifeCycleClientMock = pLogbookLifeCycleClient;
        workspaceClientMock = workspaceClient;
    }

    /**
     * select Unit
     *
     * @param jsonQuery as String { $query : query}
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     */
    @Override
    public JsonNode selectUnit(JsonNode jsonQuery)
        throws IllegalArgumentException, InvalidParseOperationException, AccessInternalExecutionException {

        JsonNode jsonNode = null;
        LOGGER.debug("DEBUG: start selectUnits {}", jsonQuery);

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            SanityChecker.checkJsonAll(jsonQuery);
            // Check correctness of request
            LOGGER.debug("{}", jsonNode);
            final RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
            parser.getRequest().reset();
            if (!(parser instanceof SelectParserMultiple)) {
                throw new InvalidParseOperationException("Not a Select operation");
            }
            jsonNode = metaDataClient.selectUnits(jsonQuery);
            LOGGER.debug("DEBUG {}", jsonNode);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error(PARSING_ERROR, e);
            throw e;
        } catch (final IllegalArgumentException e) {
            LOGGER.error(ILLEGAL_ARGUMENT, e);
            throw e;
        } catch (final Exception e) {
            LOGGER.error("exeption thrown", e);
            throw new AccessInternalExecutionException(e);
        }
        return jsonNode;
    }

    /**
     * select Unit by Id
     *
     * @param jsonQuery as String { $query : query}
     * @param idUnit as String
     * @throws IllegalArgumentException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     */


    @Override
    public JsonNode selectUnitbyId(JsonNode jsonQuery, String idUnit)
        throws IllegalArgumentException, InvalidParseOperationException, AccessInternalExecutionException {
        // Check correctness of request
        final RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
        parser.getRequest().reset();
        if (!(parser instanceof SelectParserMultiple)) {
            throw new InvalidParseOperationException("Not a Select operation");
        }
        return selectMetadataDocumentById(jsonQuery, idUnit, DataCategory.UNIT);
    }

    private JsonNode selectMetadataDocumentById(JsonNode jsonQuery, String idDocument, DataCategory dataCategory)
        throws InvalidParseOperationException, AccessInternalExecutionException {
        JsonNode jsonNode;
        ParametersChecker.checkParameter("Data category ", dataCategory);
        ParametersChecker.checkParameter("idDocument is empty", idDocument);

        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            switch (dataCategory) {
                case UNIT:
                    jsonNode = metaDataClient.selectUnitbyId(jsonQuery, idDocument);
                    break;
                case OBJECT_GROUP:
                    // FIXME P1: metadata should return NotFound if the objectGroup is not found
                    jsonNode = metaDataClient.selectObjectGrouptbyId(jsonQuery, idDocument);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported category " + dataCategory);
            }
            // TODO P1 : ProcessingException should probably be handled by clients ?
        } catch (MetadataInvalidSelectException | MetaDataDocumentSizeException | MetaDataExecutionException |
            ProcessingException | MetaDataClientServerException e) {
            throw new AccessInternalExecutionException(e);
        }
        return jsonNode;
    }

    @Override
    public JsonNode selectObjectGroupById(JsonNode jsonQuery, String idObjectGroup)
        throws InvalidParseOperationException, AccessInternalExecutionException {
        // Check correctness of request
        final RequestParserMultiple parser = RequestParserHelper.getParser(jsonQuery.deepCopy());
        parser.getRequest().reset();
        if (!(parser instanceof SelectParserMultiple)) {
            throw new InvalidParseOperationException("Not a Select operation");
        }
        return selectMetadataDocumentById(jsonQuery, idObjectGroup, DataCategory.OBJECT_GROUP);
    }

    @Override
    public void getOneObjectFromObjectGroup(AsyncResponse asyncResponse, String idObjectGroup,
        JsonNode queryJson, String qualifier, int version)
        throws MetaDataNotFoundException, StorageNotFoundException, AccessInternalExecutionException,
        InvalidParseOperationException {
        ParametersChecker.checkParameter("ObjectGroup id should be filled", idObjectGroup);
        ParametersChecker.checkParameter("You must specify a valid object qualifier", qualifier);
        Integer tenantId = ParameterHelper.getTenantParameter();

        ParametersChecker.checkParameter("You must specify a valid tenant", tenantId);
        ParametersChecker.checkValue("version", version, 0);

        final SelectParserMultiple selectRequest = new SelectParserMultiple();
        selectRequest.parse(queryJson);
        final SelectMultiQuery request = selectRequest.getRequest();
        request.reset().addRoots(idObjectGroup);
        // FIXME P1: we should find a better way to do that than use json, like a POJO.
        request.setProjectionSliceOnQualifier("FormatIdentification", "FileInfo");

        final JsonNode jsonResponse = selectObjectGroupById(request.getFinalSelect(), idObjectGroup);
        if (jsonResponse == null) {
            throw new AccessInternalExecutionException("Null json response node from metadata");
        }

        ObjectGroupResponse objectGroupResponse =
            JsonHandler.getFromJsonNode(jsonResponse.get(RESULTS), ObjectGroupResponse.class);        


        VersionsJson finalversionsResponse = null;
        // FIXME P1: do not use direct access but POJO
        // #2604 : Filter the given result for not having false positif in the request result
        // && objectGroupResponse.getQualifiers().size() > 1
        if (objectGroupResponse.getQualifiers() != null) {
            final String dataObjectVersion = qualifier + "_" + version;
            for (QualifiersJson qualifiersResponse : objectGroupResponse.getQualifiers()) {
                // FIXME very ugly fix, qualifier with underscore should be handled
                if (qualifiersResponse.getQualifier() != null && qualifiersResponse.getQualifier().contains("_")) {
                    qualifiersResponse.setQualifier(qualifiersResponse
                        .getQualifier().split("_")[0]);
                }
                if (qualifier.equals(qualifiersResponse.getQualifier())) {
                    for (VersionsJson versionResponse : qualifiersResponse.getVersions()) {
                        if (dataObjectVersion.equals(versionResponse.getDataObjectVersion())) {
                            finalversionsResponse = versionResponse;
                            break;
                        }
                    }
                }
            }
        }
        String mimetype = null;
        String filename = null;
        String objectId = null;
        if (finalversionsResponse != null) {
            if (finalversionsResponse.getFormatIdentification() != null &&
                !finalversionsResponse.getFormatIdentification().getMimeType().isEmpty()) {
                mimetype = finalversionsResponse.getFormatIdentification().getMimeType();

            }
            if (finalversionsResponse.getFileInfoResponse() != null &&
                !finalversionsResponse.getFileInfoResponse().getFilename().isEmpty()) {
                filename = finalversionsResponse.getFileInfoResponse().getFilename();
            }
            objectId = finalversionsResponse.getId();
        }

        if (Strings.isNullOrEmpty(mimetype)) {
            mimetype = MediaType.APPLICATION_OCTET_STREAM;
        }

        if (Strings.isNullOrEmpty(filename)) {
            filename = objectId;
        }

        final StorageClient storageClient =
            storageClientMock == null ? StorageClientFactory.getInstance().getClient() : storageClientMock;
        try {
            final Response response = storageClient.getContainerAsync(DEFAULT_STORAGE_STRATEGY, objectId,
                StorageCollectionType.OBJECTS);
            final AsyncInputStreamHelper helper = new AsyncInputStreamHelper(asyncResponse, response);
            final ResponseBuilder responseBuilder =
                Response.status(Status.OK).header(GlobalDataRest.X_QUALIFIER, qualifier)
                    .header(GlobalDataRest.X_VERSION, version)
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .type(mimetype);
            helper.writeResponse(responseBuilder);
        } catch (final StorageServerClientException e) {
            throw new AccessInternalExecutionException(e);
        } finally {
            if (storageClientMock == null && storageClient != null) {
                storageClient.close();
            }
        }
    }

    /**
     * update Unit by id
     *
     * @param queryJson json update query
     * @param idUnit as String
     * @param requestId GUID operation as String
     * @throws InvalidParseOperationException Throw if json format is not correct
     * @throws AccessInternalExecutionException Throw if error occurs when send Unit to database
     * @throws IllegalArgumentException Throw if error occurs when checking argument
     * @throws AccessInternalRuleExecutionException Throw When error occures on rules update check
     */
    @Override
    public JsonNode updateUnitbyId(JsonNode queryJson, String idUnit, String requestId)
        throws IllegalArgumentException, InvalidParseOperationException, AccessInternalExecutionException, AccessInternalRuleExecutionException {
        LogbookOperationParameters logbookOpParamStart, logbookOpParamEnd, logbookOpStpParamStart, logbookOpStpParamEnd;
        LogbookLifeCycleUnitParameters logbookLCParamStart, logbookLCParamEnd;
        ParametersChecker.checkParameter(ID_CHECK_FAILED, idUnit);
        JsonNode jsonNode = JsonHandler.createObjectNode();
        Integer tenant = ParameterHelper.getTenantParameter();
        boolean globalStep = true;
        boolean stepCheckRules = true;
        boolean stepMetadataUpdate = true;
        boolean stepStorageUpdate = true;
        final GUID idGUID;
        try {
            idGUID = GUIDReader.getGUID(idUnit);
            tenant = idGUID.getTenantId();
        } catch (final InvalidGuidOperationException e) {
            throw new IllegalArgumentException("idUnit is not a valid GUID", e);
        }
        // Check Request is really an Update
        final RequestParserMultiple parser = RequestParserHelper.getParser(queryJson);
        if (!(parser instanceof UpdateParserMultiple)) {
            parser.getRequest().reset();
            throw new IllegalArgumentException("Request is not an update operation");
        }

        // eventidentifierprocess for lifecycle
        final GUID updateOpGuidStart = GUIDFactory.newOperationLogbookGUID(tenant);
        LogbookOperationsClient logbookOperationClient = logbookOperationClientMock;
        LogbookLifeCyclesClient logbookLifeCycleClient = logbookLifeCycleClientMock;
        try (MetaDataClient metaDataClient = MetaDataClientFactory.getInstance().getClient()) {
            if (logbookOperationClient == null) {
                logbookOperationClient = LogbookOperationsClientFactory.getInstance().getClient();
            }
            if (logbookLifeCycleClient == null) {
                logbookLifeCycleClient = LogbookLifeCyclesClientFactory.getInstance().getClient();
            }
            // Create logbook operation STP
            logbookOpStpParamStart = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                StatusCode.STARTED, VitamLogbookMessages.getCodeOp(STP_UPDATE_UNIT, StatusCode.STARTED), idGUID,
                STP_UPDATE_UNIT);
            logbookOpStpParamStart.putParameterValue(LogbookParameterName.outcomeDetail, STP_UPDATE_UNIT + "." +
                StatusCode.STARTED);
            boolean updateLogbook = true;
            
            // Update LFC for Check Rules
            logbookLCParamStart = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.STARTED,
                idGUID, UNIT_CHECK_RULES);
            try {
                logbookLifeCycleClient.update(logbookLCParamStart);
            } catch (LogbookClientNotFoundException e) {
                // Ignore since could be during first insert step
                LOGGER.info("Should be in First Insert step so not alreday commited", e);
                updateLogbook = false;
            }

            /** Update: Check Rules task **/
            // Update Logbook for Check Rules
            if (updateLogbook) {
                logbookOperationClient.create(logbookOpStpParamStart);
                globalStep = false;

                // Update logbook operation TASK INDEXATION
                logbookOpParamStart =
                    getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                        StatusCode.STARTED, VitamLogbookMessages.getCodeOp(UNIT_CHECK_RULES, StatusCode.STARTED),
                        idGUID,
                        UNIT_CHECK_RULES);
                logbookOperationClient.update(logbookOpParamStart);
            }

            // Call method
            stepCheckRules = false;
            checkAndUpdateRuleQuery((UpdateParserMultiple)parser);
            // OK For both + Continue
            
            JsonNode newQuery = queryJson;
            try {
                newQuery = ((UpdateParserMultiple) parser).getRequest()
                    .addActions(UpdateActionHelper.push(VitamFieldsHelper.operations(), updateOpGuidStart.toString()))
                    .getFinalUpdate();
            } catch (final InvalidCreateOperationException e) {
                LOGGER.error(e);
                throw new AccessInternalExecutionException(ERROR_ADD_CONDITION, e);
            }
            
            // Update Logbook Check Rules End
            if (updateLogbook) {
                logbookOpParamEnd =
                    getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                        StatusCode.OK, VitamLogbookMessages.getCodeOp(UNIT_CHECK_RULES, StatusCode.OK), idGUID,
                        UNIT_CHECK_RULES);
                logbookOperationClient.update(logbookOpParamEnd);
            }
            stepCheckRules = true;

            if (updateLogbook) {
                // Update LFC Check Rules End
                logbookLCParamEnd = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.OK,
                    idGUID, UNIT_CHECK_RULES);
                logbookLifeCycleClient.update(logbookLCParamEnd);
            }
            
            /** Update: Indexation task **/
            // update logbook lifecycle TASK INDEXATION
            if (updateLogbook) {
                logbookLCParamStart = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.STARTED,
                    idGUID, UNIT_METADATA_UPDATE);
                logbookLifeCycleClient.update(logbookLCParamStart);
            }
            
            if (updateLogbook) {
                // Update logbook operation TASK INDEXATION
                logbookOpParamStart =
                    getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                        StatusCode.STARTED, VitamLogbookMessages.getCodeOp(UNIT_METADATA_UPDATE, StatusCode.STARTED),
                        idGUID,
                        UNIT_METADATA_UPDATE);
                logbookOperationClient.update(logbookOpParamStart);
            }
            stepMetadataUpdate = false;

            // call update
            jsonNode = metaDataClient.updateUnitbyId(newQuery, idUnit);

            if (updateLogbook) {
                // update logbook TASK INDEXATION
                logbookOpParamEnd =
                    getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                        StatusCode.OK, VitamLogbookMessages.getCodeOp(UNIT_METADATA_UPDATE, StatusCode.OK), idGUID,
                        UNIT_METADATA_UPDATE);
                logbookOperationClient.update(logbookOpParamEnd);
            }
            stepMetadataUpdate = true;

            if (updateLogbook) {
                // update global logbook lifecycle TASK INDEXATION
                logbookLCParamEnd = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.OK,
                    idGUID, UNIT_METADATA_UPDATE);
                logbookLCParamEnd.putParameterValue(LogbookParameterName.eventDetailData,
                    getDiffMessageFor(jsonNode, idUnit));
                logbookLifeCycleClient.update(logbookLCParamEnd);
            }

            /** Update: Storage task **/
            if (updateLogbook) {
                // update logbook operation TASK STORAGE
                logbookOpParamStart = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    StatusCode.STARTED,
                    VitamLogbookMessages.getCodeOp(UNIT_METADATA_STORAGE, StatusCode.STARTED),
                    idGUID, UNIT_METADATA_STORAGE);
                logbookOperationClient.update(logbookOpParamStart);

                // update logbook lifecycle TASK STORAGE
                logbookLCParamStart = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.STARTED,
                    idGUID, UNIT_METADATA_STORAGE);
                logbookLifeCycleClient.update(logbookLCParamStart);
            }
            stepStorageUpdate = false;

            // update stored Metadata
            replaceStoredUnitMetadata(idUnit, requestId);

            if (updateLogbook) {
                // update logbook operation TASK STORAGE
                logbookOpParamEnd = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    StatusCode.OK, VitamLogbookMessages.getCodeOp(UNIT_METADATA_STORAGE, StatusCode.OK), idGUID,
                    UNIT_METADATA_STORAGE);
                logbookOperationClient.update(logbookOpParamEnd);
            }
            stepStorageUpdate = true;

            if (updateLogbook) {
                // update logbook lifecycle TASK STORAGE
                logbookLCParamEnd = getLogbookLifeCycleUpdateUnitParameters(updateOpGuidStart, StatusCode.OK,
                    idGUID, UNIT_METADATA_STORAGE);
                logbookLCParamEnd.putParameterValue(LogbookParameterName.eventDetailData,
                    getDiffMessageFor(jsonNode, idUnit));
                logbookLifeCycleClient.update(logbookLCParamEnd);

                // update logbook operation STP
                logbookOpStpParamEnd = getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    StatusCode.OK, VitamLogbookMessages.getCodeOp(STP_UPDATE_UNIT, StatusCode.OK), idGUID,
                    STP_UPDATE_UNIT);
                logbookOpStpParamEnd.putParameterValue(LogbookParameterName.outcomeDetail, STP_UPDATE_UNIT + "." +
                    StatusCode.OK);
                logbookOperationClient.update(logbookOpStpParamEnd);
            }
            globalStep = true;

            if (updateLogbook) {
                /**
                 * Commit
                 */
                // Commit logbook lifeCycle action
                logbookLifeCycleClient.commitUnit(updateOpGuidStart.toString(), idUnit);
            }
        } catch (final InvalidParseOperationException ipoe) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, null);
            LOGGER.error(PARSING_ERROR, ipoe);
            throw ipoe;
        } catch (final IllegalArgumentException iae) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, null);
            LOGGER.error(ILLEGAL_ARGUMENT, iae);
            throw iae;
        } catch (final MetaDataDocumentSizeException mddse) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, null);
            LOGGER.error(METADATA_DOCUMENT_SIZE_ERROR, mddse);
            throw new AccessInternalExecutionException(mddse);
        } catch (final LogbookClientServerException lcse) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, null);
            LOGGER.error(DOCUMENT_CLIENT_SERVER_ERROR, lcse);
            throw new AccessInternalExecutionException(lcse);
        } catch (final MetaDataExecutionException mdee) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, null);
            LOGGER.error(METADATA_EXECUTION_EXECUTION_ERROR, mdee);
            throw new AccessInternalExecutionException(mdee);
        } catch (final LogbookClientNotFoundException lcnfe) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, null);
            LOGGER.error(LOGBOOK_CLIENT_NOT_FOUND_ERROR, lcnfe);
            throw new AccessInternalExecutionException(lcnfe);
        } catch (final LogbookClientBadRequestException lcbre) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, null);
            LOGGER.error(LOGBOOK_CLIENT_BAD_REQUEST_ERROR, lcbre);
            throw new AccessInternalExecutionException(lcbre);
        } catch (final LogbookClientAlreadyExistsException e) {
            LOGGER.error(LOGBOOK_OPERATION_ALREADY_EXISTS, e);
            throw new AccessInternalExecutionException(e);
        } catch (final MetaDataClientServerException e) {
            LOGGER.error(METADATA_INTERNAL_SERVER_ERROR, e);
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, null);
            throw new AccessInternalExecutionException(e);
        } catch (StorageClientException e) {
            // NO since metadata is already updated: rollBackLogbook(logbookLifeCycleClient, logbookOperationClient,
            // updateOpGuidStart, newQuery, idGUID);
            try {
                finalizeStepKoOperation(logbookOperationClient, updateOpGuidStart, globalStep, stepMetadataUpdate,
                    stepStorageUpdate, stepCheckRules, null);
            } catch (LogbookClientBadRequestException | LogbookClientNotFoundException |
                LogbookClientServerException e1) {
                LOGGER.error(STORAGE_SERVER_EXCEPTION, e1);
            }
            LOGGER.error(STORAGE_SERVER_EXCEPTION, e);
            throw new AccessInternalExecutionException(STORAGE_SERVER_EXCEPTION, e);
        } catch (ContentAddressableStorageException e) {
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, null);
            LOGGER.error(WORKSPACE_SERVER_EXCEPTION, e);
        } catch (AccessInternalRuleExecutionException e) {
        	ObjectNode evDetData = JsonHandler.createObjectNode();
        	evDetData.put("errorCode", e.getMessage());
            rollBackLogbook(logbookOperationClient, logbookLifeCycleClient, updateOpGuidStart, globalStep,
                stepMetadataUpdate, stepStorageUpdate, stepCheckRules, JsonHandler.unprettyPrint(evDetData));
            LOGGER.error(ERROR_CHECK_RULES, e);
            throw e;
        } catch (AccessInternalException e) {
            LOGGER.error(ARCHIVE_UNIT_NOT_FOUND, e);
            throw new AccessInternalExecutionException(e);
        } finally {
            if (logbookLifeCycleClientMock == null && logbookLifeCycleClient != null) {
                logbookLifeCycleClient.close();
            }
            if (logbookOperationClientMock == null && logbookOperationClient != null) {
                logbookOperationClient.close();
            }
        }
        return jsonNode;
    }

    /**
     * @param idUnit
     * @param requestId
     * @throws InvalidParseOperationException
     * @throws StorageClientException
     * @throws AccessInternalException
     * @throws ContentAddressableStorageServerException
     */
    private void replaceStoredUnitMetadata(String idUnit, String requestId)
        throws InvalidParseOperationException, ContentAddressableStorageException,
        StorageClientException, AccessInternalException {

        JsonNode jsonResponse = null;
        SelectMultiQuery query = new SelectMultiQuery();
        JsonNode constructQuery = query.getFinalSelect();
        final String fileName = idUnit + JSON;

        final WorkspaceClient workspaceClient =
            workspaceClientMock == null ? WorkspaceClientFactory.getInstance().getClient() : workspaceClientMock;
        try {
            jsonResponse = selectMetadataDocumentById(constructQuery, idUnit, DataCategory.UNIT);
            if (jsonResponse != null) {
                JsonNode unit = jsonResponse.get(RESULTS);
                workspaceClient.createContainer(requestId);
                File file = null;
                try {
                    file = File.createTempFile(idUnit, JSON);
                    JsonHandler.writeAsFile(unit, file);
                } catch (IOException e1) {
                    throw new AccessInternalExecutionException(CANNOT_CREATE_A_FILE + file, e1);
                }

                try (FileInputStream inputStream = new FileInputStream(file)) {
                    workspaceClient.putObject(requestId,
                        IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + fileName,
                        inputStream);
                } catch (final IOException e) {
                    throw new AccessInternalExecutionException(CANNOT_FOUND_OR_READ_SOURCE_FILE + file, e);
                } finally {
                    if (file != null) {
                        file.delete();
                    }
                }
                // updates (replaces) stored object
                storeMetaDataUnit(new ObjectDescription(StorageCollectionType.UNITS, requestId, fileName,
                    IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + File.separator + fileName));

            } else {
                LOGGER.error(ARCHIVE_UNIT_NOT_FOUND);
                throw new AccessInternalException(ARCHIVE_UNIT_NOT_FOUND);
            }
        } finally {
            cleanWorkspace(workspaceClient, requestId);
            if (workspaceClient != null && workspaceClientMock == null) {
                workspaceClient.close();
            }
        }
    }


    /**
     * The function is used for retrieving ObjectGroup in workspace and storing metaData in storage offer
     *
     * @param description
     * @throws StorageServerClientException
     * @throws StorageNotFoundClientException
     * @throws StorageAlreadyExistsClientException
     * @throws ProcessingException when error in execution
     */
    private void storeMetaDataUnit(ObjectDescription description) throws StorageClientException {
        final StorageClient storageClient =
            storageClientMock == null ? StorageClientFactory.getInstance().getClient() : storageClientMock;
        try {
            // store binary data object
            storageClient.storeFileFromWorkspace(DEFAULT_STRATEGY, description.getType(),
                description.getObjectName(),
                description);
        } finally {
            if (storageClient != null && storageClientMock == null) {
                storageClient.close();
            }
        }

    }

    private void finalizeStepKoOperation(LogbookOperationsClient logbookOperationClient, GUID updateOpGuidStart,
        boolean globalStep, boolean stepMetadataUpdate, boolean stepStorageUpdate, boolean stepCheckRules,
        String evDetData)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {
        LogbookOperationParameters logbookOpParamEnd;
        if (!stepCheckRules) {
        	// STEP UNIT_CHECK_RULES KO
            logbookOpParamEnd =
                getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    StatusCode.KO, VitamLogbookMessages.getCodeOp(UNIT_CHECK_RULES, StatusCode.KO),
                    updateOpGuidStart, UNIT_CHECK_RULES);
            logbookOpParamEnd.putParameterValue(LogbookParameterName.eventDetailData, evDetData);
            logbookOperationClient.update(logbookOpParamEnd);
        } else if (!stepMetadataUpdate) {
            // STEP UNIT_METADATA_UPDATE KO
            logbookOpParamEnd =
                getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    StatusCode.KO, VitamLogbookMessages.getCodeOp(UNIT_METADATA_UPDATE, StatusCode.KO),
                    updateOpGuidStart,
                    UNIT_METADATA_UPDATE);
            logbookOperationClient.update(logbookOpParamEnd);
        } else if (!stepStorageUpdate) {
            // STEP UNIT_METADATA_STORAGE KO
            logbookOpParamEnd =
                getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    StatusCode.KO, VitamLogbookMessages.getCodeOp(UNIT_METADATA_STORAGE, StatusCode.KO),
                    updateOpGuidStart,
                    UNIT_METADATA_STORAGE);
            logbookOperationClient.update(logbookOpParamEnd);
        }

        if (!globalStep) {
            // GLOBAL KO
            LogbookOperationParameters logbookOpStpParamEnd =
                getLogbookOperationUpdateUnitParameters(updateOpGuidStart, updateOpGuidStart,
                    StatusCode.KO, VitamLogbookMessages.getCodeOp(STP_UPDATE_UNIT, StatusCode.KO), updateOpGuidStart,
                    STP_UPDATE_UNIT);

            logbookOpStpParamEnd.putParameterValue(LogbookParameterName.outcomeDetail, STP_UPDATE_UNIT + "." +
                StatusCode.KO);
            logbookOperationClient.update(logbookOpStpParamEnd);
        }
    }

    private void rollBackLogbook(LogbookOperationsClient logbookOperationClient,
        LogbookLifeCyclesClient logbookLifeCycleClient, GUID updateOpGuidStart,
        boolean globalStep, boolean stepMetadataUpdate, boolean stepStorageUpdate,
        boolean stepCheckRules, String evDetData) {
        try {
            finalizeStepKoOperation(logbookOperationClient, updateOpGuidStart, globalStep, stepMetadataUpdate,
                stepStorageUpdate, stepCheckRules, evDetData);
            logbookLifeCycleClient.rollBackUnitsByOperation(updateOpGuidStart.toString());
        } catch (final LogbookClientBadRequestException lcbre) {
            LOGGER.error(BAD_REQUEST, lcbre);
        } catch (final LogbookClientNotFoundException lcbre) {
            LOGGER.error(CLIENT_NOT_FOUND, lcbre);
        } catch (final LogbookClientServerException lcse) {
            LOGGER.error(CLIENT_SERVER_ERROR, lcse);
        }
    }

    private LogbookLifeCycleUnitParameters getLogbookLifeCycleUpdateUnitParameters(GUID eventIdentifierProcess,
        StatusCode logbookOutcome, GUID objectIdentifier, String action) {
        final LogbookTypeProcess eventTypeProcess = LogbookTypeProcess.UPDATE;
        final GUID updateGuid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());

        LogbookLifeCycleUnitParameters logbookLifeCycleUnitParameters =
            LogbookParametersFactory.newLogbookLifeCycleUnitParameters(updateGuid,
                VitamLogbookMessages.getEventTypeLfc(action),
                eventIdentifierProcess,
                eventTypeProcess, logbookOutcome,
                VitamLogbookMessages.getOutcomeDetailLfc(action, logbookOutcome),
                VitamLogbookMessages.getCodeLfc(action, logbookOutcome) + objectIdentifier, objectIdentifier);
        return logbookLifeCycleUnitParameters;
    }

    private LogbookOperationParameters getLogbookOperationUpdateUnitParameters(GUID eventIdentifier,
        GUID eventIdentifierProcess, StatusCode logbookOutcome,
        String outcomeDetailMessage, GUID eventIdentifierRequest, String action) {
        final LogbookOperationParameters parameters =
            LogbookParametersFactory.newLogbookOperationParameters(eventIdentifier,
                action, eventIdentifierProcess, LogbookTypeProcess.UPDATE, logbookOutcome,
                outcomeDetailMessage,
                eventIdentifierRequest);
        return parameters;
    }

    private String getDiffMessageFor(JsonNode diff, String unitId) throws InvalidParseOperationException {
        if (diff == null) {
            return "";
        }
        final JsonNode arrayNode = diff.has(_DIFF) ? diff.get(_DIFF) : diff.get(RESULTS);
        if (arrayNode == null) {
            return "";
        }
        for (final JsonNode diffNode : arrayNode) {
            if (diffNode.get(ID) != null && unitId.equals(diffNode.get(ID).textValue())) {
                ObjectNode diffObject = JsonHandler.createObjectNode();
                diffObject.set("diff", diffNode.get(DIFF));
                return JsonHandler.writeAsString(diffObject);
            }
        }
        // TODO P1 : empty string or error because no diff for this id ?
        return "";
    }

    private void cleanWorkspace(final WorkspaceClient workspaceClient, final String containerName)
        throws ContentAddressableStorageServerException, ContentAddressableStorageNotFoundException {
        // call workspace
        if (workspaceClient.isExistingContainer(containerName)) {
            workspaceClient.deleteContainer(containerName, true);
        }
    }

    public void checkAndUpdateRuleQuery(UpdateParserMultiple updateParser) throws AccessInternalRuleExecutionException, AccessInternalExecutionException, InvalidParseOperationException {
    	UpdateMultiQuery request = updateParser.getRequest();
        List<String> deletedCategoryRules = new LinkedList<>();
        Map<String, JsonNode> updatedCategoryRules = new HashMap<>();

        checkActionsOnRules(request, deletedCategoryRules, updatedCategoryRules);
        if (deletedCategoryRules.isEmpty() && updatedCategoryRules.isEmpty()) {
            return;
        }

        String unitId = updateParser.getRequest().getRoots().toArray(new String[1])[0];
        JsonNode management = getUnitManagement(unitId);

        // Compare in DB unit _mgt with the request in order to check C(R)UD permissions
        Set<String> updatedCategories = updatedCategoryRules.keySet();
        for (String category: VitamConstants.getSupportedRules()) {
            ArrayNode rulesForCategory = null;
            JsonNode categoryNode = management.get(category);            
            if (categoryNode != null) {
                rulesForCategory = (ArrayNode) categoryNode.get(RULES_KEY);
            }

            if (deletedCategoryRules.contains(category)) {
                // Check all rules in the category for safe delete (rules only present in DB)
                checkDeletedCategories(categoryNode, category);
            }

            if (rulesForCategory != null) {
                if (updatedCategories.contains(category)) {
                    ArrayNode rulesForUpdatedCategory = (ArrayNode) updatedCategoryRules.get(category);

                    // Check for update (and delete) rules (rules at least present in DB)
                    ArrayNode updatedRules = checkUpdatedRules(category, rulesForCategory, rulesForUpdatedCategory);

                    // Check for new rules (rules only present in request)
                    ArrayNode createdRules = checkAddedRules(category, rulesForCategory, rulesForUpdatedCategory);

                    // Put newRules in a new action
                    Map<String, JsonNode> action = new HashMap<>();
                    updatedRules.addAll(createdRules);
                    action.put(MANAGEMENT_PREFIX + category + RULES_PREFIX, updatedRules);
                    try {
                        request.addActions(new SetAction(action));
                    } catch (InvalidCreateOperationException e) {
                        throw new AccessInternalExecutionException(ERROR_ADD_CONDITION, e);
                    }
                }
            } else if (updatedCategories.contains(category)) {
                // Check for new rules (rules only present in request)
                ArrayNode createdRules = checkAddedRules(category, null, (ArrayNode) updatedCategoryRules.get(category));

                Map<String, JsonNode> action = new HashMap<>();
                action.put(MANAGEMENT_PREFIX + category + RULES_PREFIX, createdRules);
                try {
                    request.addActions(new SetAction(action));
                } catch (InvalidCreateOperationException e) {
                    throw new AccessInternalExecutionException(ERROR_ADD_CONDITION, e);
                }
            }
        }
    }

    /**
     * Check if there is update actions on rules. If not no updates/checks on the query.
     * SetActions on rules are removed for the request because they will be computed for endDate and reinserted later
     * 
     * @param request The initial request
     * @param deletedCategoryRules The returned list of deleted Rules (Must be initialized)
     * @param updatedCategoryRules The returned list of updated Rules (Must be initialized)
     */
    private static void checkActionsOnRules(UpdateMultiQuery request, List<String> deletedCategoryRules, Map<String, JsonNode> updatedCategoryRules) {
        Iterator<Action> actionsIterator = request.getActions().iterator();
        while (actionsIterator.hasNext()) {
            Action action = actionsIterator.next();
            UPDATEACTION currentAction = action.getUPDATEACTION();
            if (UPDATEACTION.SET.equals(currentAction) || UPDATEACTION.UNSET.equals(currentAction)) {
                JsonNode object = action.getCurrentObject();

                if(UPDATEACTION.UNSET.equals(currentAction)) {
                    // Delete a field
                    ArrayNode values = (ArrayNode) object;
                    for(int i=0, len=values.size(); i<len; i++) {
                        String unsetField = values.get(i).asText();
                        if (unsetField.startsWith(MANAGEMENT_PREFIX)
                                && VitamConstants.getSupportedRules().contains(unsetField.substring(MANAGEMENT_PREFIX.length()))) {
                            // Delete a ruleCategory
                            deletedCategoryRules.add(unsetField.substring(MANAGEMENT_PREFIX.length()));
                        }
                    }
                } else {
                    // Set a field
                    Iterator<String> fields = object.fieldNames();
                    while (fields.hasNext()) {
                        String field = fields.next();
                        if (field.startsWith(MANAGEMENT_PREFIX)
                                && VitamConstants.getSupportedRules().contains(field.substring(MANAGEMENT_PREFIX.length()))) {
                            // Set a ruleCategory
                            updatedCategoryRules.put(field.substring(MANAGEMENT_PREFIX.length()), object.get(field));
                            actionsIterator.remove();
                        }
                    }
                }

            }
        }
    }

    private JsonNode getUnitManagement(String unitId) throws AccessInternalExecutionException {
        JsonNode jsonUnit = null;
            try {
                // FIXME Do it cleaner
                String emptyQuery = "{\"$queries\": [],\"$filter\": { },\"$projection\": {}}";
                JsonNode response = selectUnitbyId(JsonHandler.getFromString(emptyQuery), unitId);
                if (response == null || response.get("$results") == null) {
                    throw new AccessInternalExecutionException("Can't get unit by ID: " + unitId);
                }
                JsonNode results = response.get("$results");
                if (results.size() != 1) {
                    throw new AccessInternalExecutionException("Can't get unique unit by ID: " + unitId);
                }
                jsonUnit = results.get(0);
            } catch (AccessInternalExecutionException | IllegalArgumentException | InvalidParseOperationException e) {
                throw new AccessInternalExecutionException(e);
            }

        return jsonUnit.get(MANAGEMENT_KEY);
    }

    private void checkDeletedCategories(JsonNode categoryNode, String category) throws AccessInternalRuleExecutionException {
        if(checkInheritancePrevention(categoryNode)) {
        	LOGGER.error(ERROR_DELETE_RULE + category + ERROR_PREVENT_INHERITANCE);
            throw new AccessInternalRuleExecutionException(
                    VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_DELETE_CATEGORY_INHERITANCE.name());
        }
    }

    private ArrayNode checkUpdatedRules(String category, ArrayNode rulesForCategory, ArrayNode rulesForUpdatedCategory) throws AccessInternalRuleExecutionException, AccessInternalExecutionException {
        // Check for all rules in rulesForCategory and compare with rules in rulesForUpdatedCategory
        ArrayNode updatedRules = JsonHandler.createArrayNode();
        for(JsonNode unitRule: rulesForCategory) {
            boolean findIt = false;
            Iterator<JsonNode> updateRulesIterator = rulesForUpdatedCategory.iterator();
            // Try to find a matching rule in order to check update
            while (!findIt && updateRulesIterator.hasNext()) {
                JsonNode updateRule = updateRulesIterator.next();
                String updateRuleName = updateRule.get("Rule").asText();
                if (unitRule.get("Rule") != null && unitRule.get("Rule").asText().equals(updateRuleName)) {
                    findIt = true;

                    if (checkEndDateInRule(updateRule)) {
                        LOGGER.error(ERROR_UPDATE_RULE + updateRule + " contains an endDate");
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_END_DATE.name());
                    }
                    if (!checkRuleFinalAction(updateRule, category)) {
                        LOGGER.error(ERROR_UPDATE_RULE + updateRule + " contains wrong FinalAction");
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_FINAL_ACTION.name());
                    }
                    JsonNode ruleInReferential = checkExistingRule(updateRuleName);
                    if (ruleInReferential == null) {
                        LOGGER.error(ERROR_UPDATE_RULE + updateRule.get("Rule")  + " is not in referential");
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_EXIST.name());
                    }
                    if (!category.equals(ruleInReferential.get("RuleType").asText())) {
                        LOGGER.error(ERROR_UPDATE_RULE + updateRule.get("Rule")  + " is not a " + category);
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_CATEGORY.name());
                    }

                    try {
                        updateRule = computeEndDate((ObjectNode)updateRule, ruleInReferential);
                    } catch (AccessInternalRuleExecutionException e) {
                        throw new AccessInternalRuleExecutionException(
                            VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_UPDATE_RULE_START_DATE.name());
                    }
                    updatedRules.add(updateRule);
                }
            }
        }
        return updatedRules;
    }

    private ArrayNode checkAddedRules(String category, ArrayNode rulesForCategory, ArrayNode rulesForUpdatedCategory) throws AccessInternalRuleExecutionException, AccessInternalExecutionException {
        ArrayNode createdRules = JsonHandler.createArrayNode();
        for(JsonNode updateRule: rulesForUpdatedCategory) {
            if (updateRule.get("Rule") == null || updateRule.get("StartDate") == null) {
            	throw new AccessInternalRuleExecutionException(
                    VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CHECK_RULES.name());
            }
            boolean findIt = false;
            if (rulesForCategory != null) {
                for(JsonNode unitRule: rulesForCategory) {
                    if (unitRule.get("Rule") != null && unitRule.get("Rule").asText().equals(updateRule.get("Rule").asText())) {
                        // Stop loop over unitRule
                        findIt = true;
                    }
                }
            }
            if (!findIt) {
                // Created Rule
                if (checkEndDateInRule(updateRule)) {
                    LOGGER.error(ERROR_CREATE_RULE + updateRule + " contains an endDate");
                    throw new AccessInternalRuleExecutionException(
                        VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_END_DATE.name());
                }
                if (!checkRuleFinalAction(updateRule, category)) {
                	LOGGER.error(ERROR_CREATE_RULE + updateRule + " contains wrong FinalAction");
                    throw new AccessInternalRuleExecutionException(
                        VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_FINAL_ACTION.name());
                }
                JsonNode ruleInReferential = checkExistingRule(updateRule.get("Rule").asText());
                if (ruleInReferential == null) {
                	LOGGER.error(ERROR_CREATE_RULE + updateRule.get("Rule")  + " is not in referential");
                    throw new AccessInternalRuleExecutionException(
                        VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_EXIST.name());
                }
                if (!category.equals(ruleInReferential.get("RuleType").asText())) {
                	LOGGER.error(ERROR_CREATE_RULE + updateRule.get("Rule")  + " is not a " + category);
                    throw new AccessInternalRuleExecutionException(
                    		VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_CATEGORY.name());
                }

                try {
                    updateRule = computeEndDate((ObjectNode)updateRule, ruleInReferential);
                } catch (AccessInternalRuleExecutionException e) {
                    throw new AccessInternalRuleExecutionException(
                        VitamCodeHelper.getCode(VitamCode.ACCESS_INTERNAL_UPDATE_UNIT_CREATE_RULE_START_DATE));
                }
                createdRules.add(updateRule);
            }
        }
        return createdRules;
    }

    private boolean checkInheritancePrevention(JsonNode categoryNode) {
        JsonNode inheritance = categoryNode.get(INHERITANCE_KEY);
        return inheritance != null && (inheritance.get("PreventRulesId") != null  || inheritance.get("PreventInheritance") != null);
    }

    private boolean checkEndDateInRule(JsonNode rule) {
        return rule.get("EndDate") != null;
    }

    private boolean checkRuleFinalAction(JsonNode rule, String category) {
        JsonNode finalActionNode = rule.get("FinalAction");

        if (VitamConstants.TAG_RULE_APPRAISAL.equals(category)) {
            if (finalActionNode == null) {
                return false;
            }
            try {
                AppraisalRuleFinalAction.fromValue(finalActionNode.asText());
            } catch (IllegalArgumentException e) {
                LOGGER.info("While update rules, No Appraisal FinalAction match " + finalActionNode + e);
                return false;
            }
        } else if (VitamConstants.TAG_RULE_STORAGE.equals(category)) {
            if (finalActionNode == null) {
                return false;
            }
            try {
                StorageRuleFinalAction.fromValue(finalActionNode.asText());
            } catch (IllegalArgumentException e) {
                LOGGER.info("While update rules, No Storage FinalAction match " + finalActionNode + e);
                return false;
            }
        } else if (finalActionNode != null) {
            return false;
        }
        return true;
    }

    private JsonNode checkExistingRule(String ruleId) throws AccessInternalExecutionException {
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
        JsonNode response = null;
        try {
            response = client.getRuleByID(ruleId);
        } catch (FileRulesException e) {
            LOGGER.info("While update rules, No rules find for id " + ruleId + e);
            return null;
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            throw new AccessInternalExecutionException("Error during checking existing rules", e);
        }
        return response.get("$results").get(0);
    }

    private JsonNode computeEndDate(ObjectNode updatingRule, JsonNode ruleInReferential) throws AccessInternalRuleExecutionException {
        LocalDate endDate = null;

        // FIXME Start of duplicated method, need to add it in a common module
        final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String startDateString = updatingRule.get("StartDate").asText();
        String ruleId = updatingRule.get("Rule").asText();
        String currentRuleType = ruleInReferential.get("RuleType").asText();

        if (ParametersChecker.isNotEmpty(startDateString) && ParametersChecker.isNotEmpty(ruleId, currentRuleType)) {
            LocalDate startDate = LocalDate.parse(startDateString, timeFormatter);
            if (startDate.getYear() >= 9000) {
                throw new AccessInternalRuleExecutionException("Wrong Start Date");
            }

            final String duration = ruleInReferential.get(FileRules.RULEDURATION).asText();
            final String measurement = ruleInReferential.get(FileRules.RULEMEASUREMENT).asText();
            if (!"unlimited".equalsIgnoreCase(duration)) {
                final RuleMeasurementEnum ruleMeasurement = RuleMeasurementEnum.getEnumFromType(measurement);
                endDate = startDate.plus(Integer.parseInt(duration), ruleMeasurement.getTemporalUnit());
            }
        }
        // FIXME End of duplicated method
        if (endDate != null) {
            updatingRule.put("EndDate", endDate.format(timeFormatter));
        }

        return updatingRule;
    }
}
