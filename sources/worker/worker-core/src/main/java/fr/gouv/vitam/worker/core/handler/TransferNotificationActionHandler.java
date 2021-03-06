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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.culture.archivesdefrance.seda.v2.*;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.utils.LifecyclesSpliterator;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.*;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.database.collections.*;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameterName;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageClientException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.DataObjectDetail;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.ValidationXsdUtils;
import fr.gouv.vitam.worker.core.MarshallerObjectCache;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageException;
import org.bson.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.StreamSupport;

import static javax.xml.datatype.DatatypeFactory.newInstance;

/**
 * Transfer notification reply handler
 */
public class TransferNotificationActionHandler extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(TransferNotificationActionHandler.class);

    private static final int ATR_RESULT_OUT_RANK = 0;
    private static final int ARCHIVE_UNIT_MAP_RANK = 0;
    private static final int DATAOBJECT_MAP_RANK = 1;
    private static final int BDO_OG_STORED_MAP_RANK = 2;
    private static final int DATAOBJECT_ID_TO_DATAOBJECT_DETAIL_MAP_RANK = 3;
    private static final int SEDA_PARAMETERS_RANK = 4;
    private static final int OBJECT_GROUP_ID_TO_GUID_MAP_RANK = 5;
    static final int HANDLER_IO_PARAMETER_NUMBER = 6;


    private static final String XML = ".xml";
    private static final String HANDLER_ID = "ATR_NOTIFICATION";
    private static final String NAMESPACE_URI = "fr:gouv:culture:archivesdefrance:seda:v2.1";
    private static final String XLINK_URI = "http://www.w3.org/1999/xlink";
    private static final String PREMIS_URI = "info:lc/xmlns/premis-v2";
    private static final String XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";

    private HandlerIO handlerIO;
    private static final String DEFAULT_STRATEGY = "default";
    private static final String EVENT_ID_PROCESS = "evIdProc";

    private final List<Class<?>> handlerInitialIOList = new ArrayList<>();
    private final MarshallerObjectCache marshallerObjectCache = new MarshallerObjectCache();
    private StatusCode workflowStatus = StatusCode.UNKNOWN;
    private final ObjectFactory objectFactory = new ObjectFactory();

    private boolean isBlankTestWorkflow = false;
    private static final String TEST_STATUS_PREFIX = "Test ";
    private String statusPrefix = "";

    /**
     * Constructor TransferNotificationActionHandler
     */
    public TransferNotificationActionHandler() {
        for (int i = 0; i < HANDLER_IO_PARAMETER_NUMBER; i++) {
            handlerInitialIOList.add(File.class);
        }
    }

    /**
     * @return HANDLER_ID
     */
    public static String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        checkMandatoryParameters(params);
        final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();
        String eventDetailData;


        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        handlerIO = handler;
        try {
            workflowStatus =
                StatusCode.valueOf(params.getMapParameters().get(WorkerParameterName.workflowStatusKo));

            LogbookTypeProcess logbookTypeProcess = params.getLogbookTypeProcess();
            if (logbookTypeProcess != null && LogbookTypeProcess.INGEST_TEST.equals(logbookTypeProcess)) {
                isBlankTestWorkflow = true;
                statusPrefix = TEST_STATUS_PREFIX;
            }

            File atrFile;

            final LogbookOperation logbookOperation;
            try (LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient()) {
                Select select = new Select();
                select.setQuery(QueryHelper.eq(EVENT_ID_PROCESS, params.getContainerName()));
                final JsonNode node = client.selectOperationById(params.getContainerName(), select.getFinalSelect());
                final JsonNode elmt = node.get("$results").get(0);
                if (elmt == null) {
                    LOGGER.error("Error while loading logbook operation: no result");
                    throw new ProcessingException("Error while loading logbook operation: no result");
                }
                logbookOperation = new LogbookOperation(elmt);
            } catch (final LogbookClientException e) {
                LOGGER.error("Error while loading logbook operation", e);
                throw new ProcessingException(e);
            } catch (InvalidCreateOperationException e) {
                LOGGER.error("Error while creating DSL query", e);
                throw new ProcessingException(e);
            }

            //create ATR file in all cases
            atrFile = createATR(params, handlerIO, logbookOperation);

            // calculate digest by vitam alog
            final Digest vitamDigest = new Digest(VitamConfiguration.getDefaultDigestType());
            final String vitamDigestString = vitamDigest.update(atrFile).digestHex();

            LOGGER.debug(
                "DEBUG: \n\t" + vitamDigestString);
            // define eventDetailData
            eventDetailData =
                "{" +
                    "\"FileName\":\"" + "ATR_" + params.getContainerName() +
                    "\", \"MessageDigest\": \"" + vitamDigestString +
                    "\", \"Algorithm\": \"" + VitamConfiguration.getDefaultDigestType() + "\"}";

            itemStatus.setEvDetailData(eventDetailData);

            try {
                ValidationXsdUtils.checkWithXSD(new FileInputStream(atrFile), SedaUtils.SEDA_XSD_VERSION);
            } catch (SAXException e) {
                if (e.getCause() == null) {
                    LOGGER.error("ATR File is not valid with the XSD", e);
                }
                LOGGER.error("ATR File is not a correct xml file", e);
            } catch (XMLStreamException e) {
                LOGGER.error("ATR File is not a correct xml file", e);
            }

            handler.addOuputResult(ATR_RESULT_OUT_RANK, atrFile, true, false);
            // store data object
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(params.getContainerName());
            description.setWorkspaceObjectURI(handler.getOutput(ATR_RESULT_OUT_RANK).getPath());
            try (final StorageClient storageClient = storageClientFactory.getClient()) {
                storageClient.storeFileFromWorkspace(
                    DEFAULT_STRATEGY,
                    DataCategory.REPORT,
                    params.getContainerName() + XML, description);

                if (!workflowStatus.isGreaterOrEqualToKo()) {
                    description.setWorkspaceObjectURI(
                        IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE);
                    storageClient.storeFileFromWorkspace(
                        DEFAULT_STRATEGY,
                        DataCategory.MANIFEST,
                        params.getContainerName() + XML, description);
                }
            }

            itemStatus.increment(StatusCode.OK);
        } catch (ProcessingException | ContentAddressableStorageException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        } catch (URISyntaxException | InvalidParseOperationException |
            StorageClientException | IOException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * create ATR in all  processing cases (Ok or KO)
     *
     * @param params of type WorkerParameters
     * @param ioParam of type HandlerIO
     * @throws ProcessingException ProcessingException
     * @throws URISyntaxException URISyntaxException
     * @throws ContentAddressableStorageException ContentAddressableStorageException
     * @throws IOException IOException
     * @throws InvalidParseOperationException InvalidParseOperationException
     */
    private File createATR(WorkerParameters params, HandlerIO ioParam, LogbookOperation logbookOperation)
        throws ProcessingException, URISyntaxException, ContentAddressableStorageException, IOException,
        InvalidParseOperationException {

        ParameterHelper.checkNullOrEmptyParameters(params);

        final File atrFile = handlerIO.getNewLocalFile(handlerIO.getOutput(ATR_RESULT_OUT_RANK).getPath());

        // creation of ATR OK/KO report
        try {

            ArchiveTransferReplyType archiveTransferReply = objectFactory.createArchiveTransferReplyType();

            addFirstLevelBaseInformations(archiveTransferReply, params, logbookOperation);

            List<String> statusToBeChecked = new ArrayList();

            //ATR KO
            if (workflowStatus.isGreaterOrEqualToKo()){
                statusToBeChecked.add(StatusCode.FATAL.toString());
                statusToBeChecked.add(StatusCode.KO.toString());
            } else { //ATR OK
                // CHeck is only done in OK mode since all parameters are optional
                checkMandatoryIOParameter(ioParam);
                statusToBeChecked.add(StatusCode.WARNING.toString());
            }

            // if KO ATR KO or ATR Ok and status equals WARNING, we'll add informations here
            if (workflowStatus.isGreaterOrEqualToKo() || StatusCode.WARNING.name().equals(workflowStatus.name())) {
                addOperation(archiveTransferReply, logbookOperation, statusToBeChecked);
            }

            addDataObjectPackage(archiveTransferReply, params.getContainerName(), logbookOperation,
                statusToBeChecked);

            Marshaller archiveTransferReplyMarshaller = marshallerObjectCache.getMarshaller(ArchiveTransferReplyType.class);
            archiveTransferReplyMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, NAMESPACE_URI+" "+SedaUtils.SEDA_XSD_VERSION);
            archiveTransferReplyMarshaller.marshal(archiveTransferReply, atrFile);

        } catch (IOException | InvalidCreateOperationException e) {
            LOGGER.error("Error of response generation");
            throw new ProcessingException(e);
        } catch (JAXBException e) {
            String msgErr = "Error on marshalling object archiveTransferReply";
            LOGGER.error(msgErr, e);
            throw new ProcessingException(msgErr, e);
        }

        return atrFile;
    }


    private void addFirstLevelBaseInformations(ArchiveTransferReplyType archiveTransferReply, WorkerParameters params,
        LogbookOperation logbookOperation)
        throws InvalidParseOperationException {

        JsonNode infoATR = null;
        String messageIdentifier = "";

        if (handlerIO.getInput(SEDA_PARAMETERS_RANK) != null) {
            final JsonNode sedaParameters = JsonHandler.getFromFile((File) handlerIO.getInput(SEDA_PARAMETERS_RANK));
            infoATR =
                sedaParameters.get(SedaConstants.TAG_ARCHIVE_TRANSFER);
            if (infoATR != null && infoATR.get(SedaConstants.TAG_MESSAGE_IDENTIFIER) != null) {
                messageIdentifier = infoATR.get(SedaConstants.TAG_MESSAGE_IDENTIFIER).asText();
            }
        }

        archiveTransferReply.setDate(buildXMLGregorianCalendar());

        archiveTransferReply.setMessageIdentifier(buildIdentifierType(params.getContainerName()));
        //to attach to DataObjectPackage
        ManagementMetadataType mgmtMetadata = objectFactory.createManagementMetadataType();
        DescriptiveMetadataType descMetaData = objectFactory.createDescriptiveMetadataType();
        DataObjectPackageType dataObjectPackage = objectFactory.createDataObjectPackageType();

        dataObjectPackage.setDescriptiveMetadata(descMetaData);
        dataObjectPackage.setManagementMetadata(mgmtMetadata);
        archiveTransferReply.setDataObjectPackage(dataObjectPackage);


        if (logbookOperation.get(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
            final JsonNode evDetDataNode = JsonHandler.getFromString(
                logbookOperation.get(LogbookMongoDbName.eventDetailData.getDbname()).toString());
            if (evDetDataNode.get(SedaConstants.TAG_ARCHIVE_PROFILE) != null) {

                final String profilId = evDetDataNode.get(SedaConstants.TAG_ARCHIVE_PROFILE).asText();
                mgmtMetadata.setArchivalProfile(buildIdentifierType(profilId));
            }
        }

        if (infoATR != null) {
            archiveTransferReply.setArchivalAgreement(
                buildIdentifierType(
                    (infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT) != null)
                        ? infoATR.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT).textValue()
                        : ""
                )
            );
        }
        
        CodeListVersionsType codeListVersions = objectFactory.createCodeListVersionsType();
        archiveTransferReply.setCodeListVersions(codeListVersions);

        if (infoATR != null && infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS) != null && 
                infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION) != null) {
            codeListVersions.setReplyCodeListVersion(buildCodeType(
                    infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_REPLY_CODE_LIST_VERSION).textValue()));
        } else {
            codeListVersions.setReplyCodeListVersion(buildCodeType(""));
        }

        if (infoATR != null && infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS) != null && 
                infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION) != null) {
            codeListVersions.setMessageDigestAlgorithmCodeListVersion(buildCodeType(
                    infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_MESSAGE_DIGEST_ALGORITHM_CODE_LIST_VERSION).textValue()));
        } else {
            codeListVersions.setMessageDigestAlgorithmCodeListVersion(buildCodeType(""));
        }

        if (infoATR != null && infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS) != null && 
                infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION) != null) {
            codeListVersions.setFileFormatCodeListVersion(buildCodeType(
                    infoATR.get(SedaConstants.TAG_CODE_LIST_VERSIONS).get(SedaConstants.TAG_FILE_FORMAT_CODE_LIST_VERSION).textValue()));
        } else {
            codeListVersions.setFileFormatCodeListVersion(buildCodeType(""));
        }

        archiveTransferReply.setReplyCode(statusPrefix + workflowStatus.name());
        archiveTransferReply.setMessageRequestIdentifier(buildIdentifierType(messageIdentifier));

        if (!isBlankTestWorkflow) {
            archiveTransferReply.setGrantDate(buildXMLGregorianCalendar());
        }

        if (infoATR != null && infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY) != null) {
            archiveTransferReply.setArchivalAgency(
                buildOrganizationWithIdType(

                    (infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER) != null)
                        ? infoATR.get(SedaConstants.TAG_ARCHIVAL_AGENCY).get(SedaConstants.TAG_IDENTIFIER).textValue()
                        : ""
                )
            );
        }

        if (infoATR != null && infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY) != null) {
            archiveTransferReply.setTransferringAgency(
                buildOrganizationWithIdType(

                    (infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER) != null)
                        ? infoATR.get(SedaConstants.TAG_TRANSFERRING_AGENCY).get(SedaConstants.TAG_IDENTIFIER)
                        .textValue()
                        : ""
                )
            );
        }
    }

    private XMLGregorianCalendar buildXMLGregorianCalendar() {
        try {
            final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            return newInstance().newXMLGregorianCalendar(sdfDate.format(new Date()).toString());
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("The implementation of DatatypeFactory is not available or cannot be instantiated", e);
        }
        return null;
    }

    private IdentifierType buildIdentifierType(String value){
        IdentifierType identifierType = objectFactory.createIdentifierType();
        identifierType.setValue(value);

        return identifierType;
    }

    private CodeType buildCodeType(String value){
        CodeType codeType = objectFactory.createCodeType();
        codeType.setValue(value);

        return codeType;
    }

    private OrganizationWithIdType  buildOrganizationWithIdType(String value){
        OrganizationWithIdType organizationWithIdType  = objectFactory.createOrganizationWithIdType();
        organizationWithIdType.setIdentifier(buildIdentifierType(value));

        return organizationWithIdType;
    }



    private LifecyclesSpliterator<JsonNode> handlerLogbookLifeCycleUnit(String operationId,
        LogbookLifeCyclesClient client, LifeCycleStatusCode lifeCycleStatusCode)
        throws LogbookClientException {
        final Select select = new Select();
        LifecyclesSpliterator<JsonNode> scrollRequest = new LifecyclesSpliterator<>(select,
            query -> {
                RequestResponse response;
                try {
                    response = client.unitLifeCyclesByOperationIterator(operationId,
                        lifeCycleStatusCode, select.getFinalSelect());
                } catch (LogbookClientException | InvalidParseOperationException e) {
                    throw new IllegalStateException(e);
                }
                if (response.isOk()) {
                    return (RequestResponseOK) response;
                } else {
                    throw new IllegalStateException(
                        String.format("Error while loading logbook lifecycle Unit RequestResponse %d",
                            response.getHttpCode()));
                }
            }, VitamConfiguration.getDefaultOffset(), VitamConfiguration.getBatchSize());
        return scrollRequest;
    }

    /**
     * Add DataObjectPackage element to the ATR xml
     * @param archiveTransferReply the archiveTransferReplyType object to populate
     * @param logbookOperation
     * @param containerName the operation identifier
     * @param statusToBeChecked depends of ATR status (KO={FATAL,KO} or OK=Warning)
     * @throws ProcessingException thrown if a logbook could not be retrieved
     * @throws FileNotFoundException FileNotFoundException
     * @throws InvalidParseOperationException InvalidParseOperationException
     * @throws InvalidCreateOperationException InvalidCreateOperationException
     */
    private void addDataObjectPackage(ArchiveTransferReplyType archiveTransferReply, String containerName,
        LogbookOperation logbookOperation, List<String> statusToBeChecked)
        throws ProcessingException, FileNotFoundException, InvalidParseOperationException,
        InvalidCreateOperationException {

        try (LogbookLifeCyclesClient client = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            ////Build DescriptiveMetadata/List(ArchiveUnit)
            try {

                Map<String, Object> archiveUnitSystemGuid = null;
                InputStream archiveUnitMapTmpFile = null;
                final File file = (File) handlerIO.getInput(ARCHIVE_UNIT_MAP_RANK);
                if (file != null) {
                    archiveUnitMapTmpFile = new FileInputStream(file);
                }

                Map<String, String> systemGuidArchiveUnitId = new HashMap<>();

                if (archiveUnitMapTmpFile != null) {
                    archiveUnitSystemGuid = JsonHandler.getMapFromInputStream(archiveUnitMapTmpFile);
                    if (archiveUnitSystemGuid != null) {
                        for (final Map.Entry<String, Object> entry : archiveUnitSystemGuid.entrySet()) {
                            systemGuidArchiveUnitId.put(entry.getValue().toString(), entry.getKey());
                        }
                        //build archiveUnit List
                        List<ArchiveUnitType> auList = archiveTransferReply.getDataObjectPackage().getDescriptiveMetadata().getArchiveUnit();
                        //case KO or OK with warning
                        if (workflowStatus.isGreaterOrEqualToKo() || StatusCode.WARNING.name().equals(workflowStatus.name())) {

                            LifecyclesSpliterator<JsonNode> lifecyclesSpliterator =
                                handlerLogbookLifeCycleUnit(containerName, client, LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS);

                            // Iterate over all response in LifecyclesSpliterator
                            StreamSupport.stream(lifecyclesSpliterator, false)
                                .map(LogbookLifeCycleUnitInProcess::new)
                                .forEach(logbookLifeCycleUnit -> auList.add(
                                    buildArchiveUnit(statusToBeChecked,
                                        systemGuidArchiveUnitId, logbookLifeCycleUnit))
                                );
                        } else {
                            //set only archiveUnit(id,systemId) List
                            auList.addAll(buildListOfSimpleArchiveUnitWithoutEvents(archiveUnitSystemGuid));
                        }
                    }
                }

            } catch (final IllegalStateException | LogbookClientException e) {
                throw new ProcessingException("Exception when building ArchiveUnitList for ArchiveTransferReply KO", e);
            }

            //Build DataObjectGroup
            try {

                Map<String, Object> bdoObjectGroupSystemGuid = new HashMap<>();
                final Map<String, String> objectGroupGuid = new HashMap<>();
                final Map<String, List<String>> dataObjectsForOG = new HashMap<>();
                final File dataObjectMapTmpFile = (File) handlerIO.getInput(DATAOBJECT_MAP_RANK);
                final File bdoObjectGroupStoredMapTmpFile = (File) handlerIO.getInput(BDO_OG_STORED_MAP_RANK);
                final File objectGroupSystemGuidTmpFile = (File) handlerIO.getInput(OBJECT_GROUP_ID_TO_GUID_MAP_RANK);
                final File dataObjectToDetailDataObjectMapTmpFile = (File) handlerIO.getInput(DATAOBJECT_ID_TO_DATAOBJECT_DETAIL_MAP_RANK);

                Map<String, Object> dataObjectSystemGuid;
                if (dataObjectMapTmpFile != null && bdoObjectGroupStoredMapTmpFile != null) {
                    try (InputStream binaryDataObjectMapTmpFIS = new FileInputStream(dataObjectMapTmpFile);
                        InputStream bdoObjectGroupStoredMapTmpFIStream = new FileInputStream(bdoObjectGroupStoredMapTmpFile)){

                        dataObjectSystemGuid = JsonHandler.getMapFromInputStream(binaryDataObjectMapTmpFIS);
                        bdoObjectGroupSystemGuid = JsonHandler.getMapFromInputStream(bdoObjectGroupStoredMapTmpFIStream);
                    } catch (IOException e) {
                        throw new ProcessingException(e);
                    }
                } else {
                    dataObjectSystemGuid = new HashMap<>();
                }
                for (final Map.Entry<String, Object> entry : bdoObjectGroupSystemGuid.entrySet()) {
                    final String idOG = entry.getValue().toString();
                    final String idObj = entry.getKey();
                    if (!dataObjectsForOG.containsKey(idOG)) {
                        final List<String> listObj = new ArrayList<>();
                        listObj.add(idObj);
                        dataObjectsForOG.put(idOG, listObj);
                    } else {
                        dataObjectsForOG.get(idOG).add(idObj);
                    }
                }

                Map<String, Object> objectGroupSystemGuid = null;

                if (objectGroupSystemGuidTmpFile != null) {
                    final InputStream objectGroupGuidMapFIS = new FileInputStream(objectGroupSystemGuidTmpFile);
                    objectGroupSystemGuid =
                        JsonHandler.getMapFromInputStream(objectGroupGuidMapFIS);
                    if (objectGroupSystemGuid != null) {
                        for (final Map.Entry<String, Object> entry : objectGroupSystemGuid.entrySet()) {
                            final String guid = entry.getValue().toString();
                            objectGroupGuid.put(guid, entry.getKey());
                        }
                    }
                }

                Map<String, DataObjectDetail> dataObjectToDetailDataObject;

                if (dataObjectToDetailDataObjectMapTmpFile != null) {
                    final InputStream dataObjectToDetailDataObjectMapFIS = new FileInputStream(dataObjectToDetailDataObjectMapTmpFile);
                    dataObjectToDetailDataObject =
                        JsonHandler.getMapFromInputStream(dataObjectToDetailDataObjectMapFIS,
                            DataObjectDetail.class);
                } else {
                    dataObjectToDetailDataObject = new HashMap<>();
                }

                //build DataObjectGroup object List
                List<Object> dataObjectGroupList = archiveTransferReply.getDataObjectPackage().getDataObjectGroupOrBinaryDataObjectOrPhysicalDataObject();

                if (dataObjectSystemGuid != null) {
                    //case KO or OK with warning
                    if (workflowStatus.isGreaterOrEqualToKo() || StatusCode.WARNING.name().equals(workflowStatus.name())) {

                        LifecyclesSpliterator<JsonNode> lifecyclesSpliterator =
                            handleLogbookLifeCyclesObjectGroup(containerName, client,
                                LifeCycleStatusCode.LIFE_CYCLE_IN_PROCESS);

                        StreamSupport.stream(lifecyclesSpliterator, false)
                            .map(LogbookLifeCycleObjectGroupInProcess::new)
                            .forEach(logbookLifeCycleObjectGroup -> dataObjectGroupList.add(
                                buildDataObjectGroup(statusToBeChecked, objectGroupGuid, dataObjectsForOG, dataObjectSystemGuid,
                                    dataObjectToDetailDataObject, logbookLifeCycleObjectGroup))
                            );
                    } else {

                        dataObjectGroupList.addAll(
                            buildListOfSimpleDataObjectGroup(dataObjectsForOG, dataObjectSystemGuid,
                            dataObjectToDetailDataObject, objectGroupSystemGuid)
                        );
                    }
                }

            } catch (final LogbookClientException | IllegalStateException | InvalidParseOperationException |
                IllegalArgumentException e) {
                throw new ProcessingException("Exception when building DataObjectGroup for ArchiveTransferReply KO", e);
            }
        }
    }

    private List<ArchiveUnitType> buildListOfSimpleArchiveUnitWithoutEvents(Map<String, Object> archiveUnitSystemGuid) {

        List<ArchiveUnitType> ArchiveUnitTypeList = new ArrayList<>();

        for (final Map.Entry<String, Object> entry : archiveUnitSystemGuid.entrySet()) {
            ArchiveUnitType archiveUnit = objectFactory.createArchiveUnitType();
            DescriptiveMetadataContentType descContent = new DescriptiveMetadataContentType();

            archiveUnit.setId(entry.getKey());
            descContent.getSystemId().add(entry.getValue().toString());
            archiveUnit.setContent(descContent);
            ArchiveUnitTypeList.add(archiveUnit);
        }

        return ArchiveUnitTypeList;
    }

    private ArchiveUnitType buildArchiveUnit(List<String> statusToBeChecked,
        Map<String, String> systemGuidArchiveUnitId, LogbookLifeCycleUnitInProcess logbookLifeCycleUnit) {

        List<Document> logbookLifeCycleUnitEvents =
            (List<Document>) logbookLifeCycleUnit
                .get(LogbookDocument.EVENTS.toString());

        ArchiveUnitType archiveUnit = objectFactory.createArchiveUnitType();
        DescriptiveMetadataContentType descMetadataContent = objectFactory.createDescriptiveMetadataContentType();
        
        if (!systemGuidArchiveUnitId.isEmpty() &&
            logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID) != null &&
            systemGuidArchiveUnitId
                .get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString()) != null) {

            archiveUnit.setId(systemGuidArchiveUnitId
                .get(logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString())
            );

            descMetadataContent.getSystemId().add(
                logbookLifeCycleUnit.get(SedaConstants.PREFIX_ID).toString()
            );
        }
        
        archiveUnit.setContent(descMetadataContent);

        ArchiveUnitType.Management archiveUnitMgmt = objectFactory.createArchiveUnitTypeManagement();

        if(logbookLifeCycleUnitEvents != null && !logbookLifeCycleUnitEvents.isEmpty()){
            ManagementMetadataType.LogBook logbook = new ManagementMetadataType.LogBook();
            for (final Document document : logbookLifeCycleUnitEvents) {
                EventType eventObject = buildEventByContainerType(document, SedaConstants.TAG_ARCHIVE_UNIT, statusToBeChecked, null);
                if (eventObject != null) {
                    logbook.getEvent().add((ManagementMetadataType.LogBook.Event) eventObject);
                }
            }

            if (!logbook.getEvent().isEmpty()) {
                archiveUnitMgmt.setLogBook(logbook);
            }
        }

        archiveUnit.setManagement(archiveUnitMgmt);

        return archiveUnit;

    }

    private List<DataObjectGroupType> buildListOfSimpleDataObjectGroup(Map<String, List<String>> dataObjectsForOG,
            Map<String, Object> dataObjectSystemGuid, Map<String, DataObjectDetail> dataObjectToDetailDataObject,
            Map<String, Object> objectGroupSystemGuid) {

        final List<DataObjectGroupType> dataObjectGroupList = new ArrayList<>();

        final Set<String> usedDataObjectGroup = new HashSet<>();

        for(final Map.Entry<String, List<String>> dataObjectGroupEntry : dataObjectsForOG.entrySet()) {

            DataObjectGroupType dataObjectGroup = objectFactory.createDataObjectGroupType();

            String dataObjectGroupId = dataObjectGroupEntry.getKey();

            Object dataObjectGroupSystemId = objectGroupSystemGuid.get(dataObjectGroupId);

            dataObjectGroup.setId(dataObjectGroupId);

            for (final String dataObjectId : dataObjectGroupEntry.getValue()) {


                MinimalDataObjectType binaryOrPhysicalDataObject = null;

                final DataObjectDetail dataObjectDetail = dataObjectToDetailDataObject.get(dataObjectId);

                if (dataObjectDetail.isPhysical()) {
                    binaryOrPhysicalDataObject = objectFactory.createPhysicalDataObjectType();
                } else {
                    binaryOrPhysicalDataObject = objectFactory.createBinaryDataObjectType();
                }

                binaryOrPhysicalDataObject.setId(dataObjectId);

                binaryOrPhysicalDataObject.setDataObjectGroupSystemId(dataObjectGroupSystemId.toString());

                String dataObjectSystemGUID = dataObjectSystemGuid.get(dataObjectId).toString();
                if (dataObjectSystemGUID != null) {
                    binaryOrPhysicalDataObject.setDataObjectSystemId(dataObjectSystemGUID);
                }

                binaryOrPhysicalDataObject.setDataObjectVersion(dataObjectDetail.getVersion());

                //add  dataObject to dataObjectGroup
                dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject().add(binaryOrPhysicalDataObject);

            }
            //add ObjectGroup object to result list
            dataObjectGroupList.add(dataObjectGroup);
        }

        return dataObjectGroupList;
    }

    private DataObjectGroupType buildDataObjectGroup(
        List<String> statusToBeChecked,
        Map<String, String> objectGroupGuid, Map<String, List<String>> dataObjectsForOG,
        Map<String, Object> dataObjectSystemGuid, Map<String, DataObjectDetail> dataObjectToDetailDataObject,
        LogbookLifeCycleObjectGroupInProcess logbookLifeCycleObjectGroup) {

        Map<String, String>  dataObjectSystemGUIDToID = new TreeMap<>();

        final String eventIdentifier = null;
        DataObjectGroupType dataObjectGroup = objectFactory.createDataObjectGroupType();
        MinimalDataObjectType binaryOrPhysicalDataObject = null;

        final String ogGUID =
            logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname()) != null
                ? logbookLifeCycleObjectGroup.get(LogbookMongoDbName.objectIdentifier.getDbname())
                .toString()
                : "";

        String igId = "";
        if (objectGroupGuid.containsKey(ogGUID)) {
            igId = objectGroupGuid.get(ogGUID);
            dataObjectGroup.setId(igId);
        }
        if (dataObjectsForOG.get(igId) != null) {
            for (final String idObj : dataObjectsForOG.get(igId)) {
                if (dataObjectToDetailDataObject.get(idObj) != null &&
                    dataObjectToDetailDataObject.get(idObj).isPhysical()) {
                    binaryOrPhysicalDataObject = objectFactory.createPhysicalDataObjectType();
                } else {
                    binaryOrPhysicalDataObject = objectFactory.createBinaryDataObjectType();
                }

                binaryOrPhysicalDataObject.setId(idObj);
                String dataObjectSystemGUID = dataObjectSystemGuid.get(idObj).toString();
                if (dataObjectSystemGUID != null) {
                    binaryOrPhysicalDataObject.setDataObjectSystemId(dataObjectSystemGUID);
                    dataObjectSystemGUIDToID.put(dataObjectSystemGUID, idObj);
                }
                if (ogGUID != null && !ogGUID.isEmpty()) {
                    binaryOrPhysicalDataObject.setDataObjectGroupSystemId(ogGUID);
                }

                binaryOrPhysicalDataObject.setDataObjectVersion(
                    dataObjectToDetailDataObject.get(idObj) != null ? dataObjectToDetailDataObject.get(idObj).getVersion() : "");

                //add  dataObject to dataObjectGroup
                dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject().add(binaryOrPhysicalDataObject);
                binaryOrPhysicalDataObject = null;
            }

        }

        final List<Document> logbookLifeCycleObjectGroupEvents =
            (List<Document>) logbookLifeCycleObjectGroup.get(LogbookDocument.EVENTS.toString());
        if (logbookLifeCycleObjectGroupEvents != null) {
            dataObjectGroup.setLogBook(new DataObjectGroupType.LogBook());
            for (final Document eventDoc : logbookLifeCycleObjectGroupEvents) {
                String objectSystemId = eventDoc.get(LogbookMongoDbName.objectIdentifier.getDbname()).toString();
                String objectId = dataObjectSystemGUIDToID.get(objectSystemId);
                Object objectObject = findDataObjectById(dataObjectGroup, objectId);
                dataObjectGroup.getLogBook().getEvent().add(
                    (DataObjectGroupType.LogBook.Event) buildEventByContainerType(eventDoc,
                        SedaConstants.TAG_DATA_OBJECT_GROUP,
                        statusToBeChecked, objectObject)
                );
            }
        }

        return dataObjectGroup;

    }

    private Object findDataObjectById(DataObjectGroupType dataObjectGroup, String objectId) {
        MinimalDataObjectType dataObject = null;
        if(objectId != null) {

            for (MinimalDataObjectType object : dataObjectGroup.getBinaryDataObjectOrPhysicalDataObject()) {
                if (objectId.equals(object.getId())){
                    dataObject = object;
                    break;
                }
            }
        }

        return dataObject;
    }

    private void addOperation(ArchiveTransferReplyType archiveTransferReply, LogbookOperation logbookOperation,
        List<String> statusToBeChecked){

        final List<Document> logbookOperationEvents =
            (List<Document>) logbookOperation.get(LogbookDocument.EVENTS.toString());

        OperationType operation = objectFactory.createOperationType();
        List<EventType> eventList = new ArrayList<>();

        for (final Document event : logbookOperationEvents) {
            eventList.add(buildEventByContainerType(event, SedaConstants.TAG_OPERATION, statusToBeChecked, null));
        }

        operation.getEvent().addAll(eventList);
        archiveTransferReply.setOperation(operation);
    }

    private LifecyclesSpliterator<JsonNode> handleLogbookLifeCyclesObjectGroup(String containerName,
        LogbookLifeCyclesClient client, LifeCycleStatusCode statusCode)
        throws LogbookClientException, ProcessingException {
        Select select = new Select();
        LifecyclesSpliterator<JsonNode> scrollRequest = new LifecyclesSpliterator<>(select,
            query -> {
                RequestResponse response;
                try {
                    response = client.objectGroupLifeCyclesByOperationIterator(containerName,
                        statusCode, select.getFinalSelect());
                } catch (InvalidParseOperationException | LogbookClientException e) {
                    throw new IllegalStateException(e);
                }
                if (response.isOk()) {
                    return (RequestResponseOK) response;
                } else {
                    throw new IllegalStateException(String.format(
                        "Error while loading logbook lifecycle objectGroup Bad Response %d",
                        response.getHttpCode()));
                }
            }, VitamConfiguration.getDefaultOffset(), VitamConfiguration.getBatchSize());
        return scrollRequest;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        if (!handler.checkHandlerIO(1, handlerInitialIOList)) {
            throw new ProcessingException(HandlerIOImpl.NOT_CONFORM_PARAM);
        }
    }

    /**
     * Construct the event object for a given object type in managementMetadata item of ATR
     * Type can be : OperationType, ArchiveUnitType or DataObjectGroupType item
     * @param document mongo document holding event infos
     * @param dataObjectToReference DataObject to reference from the LoogBook/event object to create
     * @return
     *          EventType object for operationType and ArchiveUnitType
     *          DataObjectGroupType.Event for DataObjectGroupType
     *
     */
    private EventType buildEventByContainerType(Document document, String eventType,
        List<String> statusToBeChecked, Object dataObjectToReference)
    {
        EventType eventObject = null;

        if (document.get(LogbookMongoDbName.outcome.getDbname()) != null &&
            statusToBeChecked.contains(document.get(LogbookMongoDbName.outcome.getDbname()).toString())) {

            //case of DataObjectGroupType, must return an DataObjectGroupType.Event type object
            if (SedaConstants.TAG_DATA_OBJECT_GROUP.equals(eventType)) {
                eventObject = new DataObjectGroupType.LogBook.Event();

                if(dataObjectToReference != null) {
                    //String objRefId = document.get(LogbookMongoDbName.objectIdentifier.getDbname()).toString();
                    DataObjectGroupType.LogBook.Event.DataObjectReference dataObjectRef = objectFactory.createDataObjectGroupTypeLogBookEventDataObjectReference();
                    dataObjectRef.setDataObjectReferenceId(dataObjectToReference);

                    ((DataObjectGroupType.LogBook.Event) eventObject).setDataObjectReference(dataObjectRef);
                }

            } else if (SedaConstants.TAG_ARCHIVE_UNIT.equals(eventType)) {
                eventObject = new ManagementMetadataType.LogBook.Event();
            } else {
                eventObject = objectFactory.createEventType();
            }

            if (document.get(LogbookMongoDbName.eventType.getDbname()) != null) {
                eventObject.setEventTypeCode(document.get(LogbookMongoDbName.eventType.getDbname()).toString());

                if (SedaConstants.TAG_OPERATION.equals(eventType)) {
                    eventObject.setEventType(
                        VitamLogbookMessages
                        .getLabelOp(document.get(LogbookMongoDbName.eventType.getDbname()).toString())
                    );
                } else if (SedaConstants.TAG_ARCHIVE_UNIT.equals(eventType) || SedaConstants.TAG_DATA_OBJECT_GROUP
                    .equals(eventType)) {
                    eventObject.setEventType(VitamLogbookMessages
                        .getFromFullCodeKey(document.get(LogbookMongoDbName.eventType.getDbname()).toString())
                    );
                }
            }
            if (document.get(LogbookMongoDbName.eventDateTime.getDbname()) != null) {
                eventObject.setEventDateTime(
                    document.get(LogbookMongoDbName.eventDateTime.getDbname()).toString()
                );
            }
            if (document.get(LogbookMongoDbName.outcome.getDbname()) != null) {
                eventObject.setOutcome(
                    document.get(LogbookMongoDbName.outcome.getDbname()).toString()
                );
            }
            if (document.get(LogbookMongoDbName.outcomeDetail.getDbname()) != null) {
                eventObject.setOutcomeDetail(
                    document.get(LogbookMongoDbName.outcomeDetail.getDbname()).toString()
                );
            }
            if (document.get(LogbookMongoDbName.outcomeDetailMessage.getDbname()) != null) {
                eventObject.setOutcomeDetailMessage(
                    document.get(LogbookMongoDbName.outcomeDetailMessage.getDbname()).toString()
                );
            }
            if (document.get(LogbookMongoDbName.eventDetailData.getDbname()) != null) {
                final String detailData = document.get(LogbookMongoDbName.eventDetailData.getDbname()).toString();
                if (detailData.contains(SedaConstants.EV_DET_TECH_DATA)) {
                    eventObject.setEventDetailData(
                        document.get(LogbookMongoDbName.eventDetailData.getDbname()).toString()
                    );
                }
            }

        }
         return eventObject;
    }
}
