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
package fr.gouv.vitam.storage.engine.client;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.digest.DigestType;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.storage.driver.model.StorageMetadatasResult;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;

/**
 * Mock client implementation for storage
 */
class StorageClientMock extends AbstractMockClient implements StorageClient {
    static final String MOCK_INFOS_RESULT_ARRAY = "{\"capacities\": [{\"offerId\": \"offer1\",\"usableSpace\": " +
        "838860800, \"nbc\": 2}," + "{\"offerId\": " + "\"offer2\",\"usableSpace\": 838860800, \"nbc\": 2}]}";
    static final String MOCK_INFOS_EMPTY_RESULT_ARRAY = "{\"capacities\": []}";
    static final String MOCK_GET_FILE_CONTENT =
        "Vitam test of long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long " +
            "long long long long long long long long long long long long long long long long long long long long file";


    @Override
    public JsonNode getStorageInformation(String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException {
        Integer tenantId = 0;
        try {
            tenantId = ParameterHelper.getTenantParameter();
        } catch (Exception e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        try {
            if (tenantId == -1) {
                return null;
            } else if (tenantId == -2) {
                return JsonHandler.getFromString(MOCK_INFOS_EMPTY_RESULT_ARRAY);
            } else {
                return JsonHandler.getFromString(MOCK_INFOS_RESULT_ARRAY);
            }
        } catch (final InvalidParseOperationException e) {
            throw new StorageServerClientException(e);
        }
    }

    @Override public List<String> getOffers(String strategyId)
        throws StorageNotFoundClientException, StorageServerClientException {

        ArrayList<String> array = new ArrayList<>();
        array.add("id1");
        return array;
    }

    @Override
    public StoredInfoResult storeFileFromWorkspace(String strategyId, DataCategory type, String guid,
        ObjectDescription description)
        throws StorageAlreadyExistsClientException, StorageNotFoundClientException, StorageServerClientException {
        return generateStoredInfoResult(guid);
    }

    @Override
    public boolean deleteContainer(String strategyId) throws StorageServerClientException {
        return true;
    }

    @Override
    public boolean delete(String strategyId, DataCategory type, String guid, String digest,
        String digestAlgorithm)
        throws StorageServerClientException {
        return true;
    }

    @Override
    public boolean existsContainer(String strategyId) throws StorageServerClientException {
        return true;
    }

    @Override
    public boolean exists(String strategyId, DataCategory type, String guid, List<String> offerIds)
        throws StorageServerClientException {
        return true;
    }

    private StoredInfoResult generateStoredInfoResult(String guid) {
        final StoredInfoResult result = new StoredInfoResult();
        result.setId(guid);
        result.setInfo("Stockage de l'objet réalisé avec succès");
        result.setCreationTime(LocalDateUtil.getString(LocalDateTime.now()));
        result.setLastModifiedTime(LocalDateUtil.getString(LocalDateTime.now()));
        result.setNbCopy(1);
        result.setStrategy("default-fake");
        result.setOfferIds(Arrays.asList("fakeOfferId"));
        return result;
    }

    @Override
    public Response getContainerAsync(String strategyId, String guid, DataCategory type)
        throws StorageServerClientException, StorageNotFoundException {

        if (null != guid && guid.endsWith(".rng")) {
            try {
                return new FakeInboundResponse(Status.OK,
                    PropertiesUtils.getResourceAsStream("profile/profil_ok.rng"),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
            } catch (FileNotFoundException e) {
                throw new StorageNotFoundException(e);
            }
        } else {
            return new FakeInboundResponse(Status.OK,
                IOUtils.toInputStream(MOCK_GET_FILE_CONTENT, Charset.defaultCharset()),
                MediaType.APPLICATION_OCTET_STREAM_TYPE, null);
        }
    }

    @Override
    public VitamRequestIterator<JsonNode> listContainer(String strategyId, DataCategory type)
        throws StorageServerClientException {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(GlobalDataRest.X_CURSOR, true);
        return new VitamRequestIterator<>(this, HttpMethod.GET, type.getFolder(), JsonNode.class, headers, null);
    }

    @Override
    public RequestResponseOK storageLogBackup()
        throws StorageServerClientException, InvalidParseOperationException {
        return new RequestResponseOK<String>()
            .addResult(GUIDFactory.newGUID().toString());
    }

    @Override
    public RequestResponseOK storageLogTraceability()
        throws StorageServerClientException, InvalidParseOperationException {
        return new RequestResponseOK<String>()
            .addResult(GUIDFactory.newGUID().toString());
    }

    @Override
    public JsonNode getInformation(String strategyId, DataCategory type, String guid, List<String> offerIds)
        throws StorageServerClientException, StorageNotFoundClientException {
        try {
            ObjectNode offerIdToMetadata = JsonHandler.createObjectNode();
            StorageMetadatasResult metaData =
                new StorageMetadatasResult("aeaaaaaaaacu6xzeabinwak6t5ecmlaaaaaq", "object",
                    "c117854cbca3e51ea94c4bd2bcf4a6756209e6c65ddbf696313e1801b2235ff33d44b2bb272e714c335a44a3b4f92d399056b94dff4dfe6b7038fa56f23b438e",
                    6096, "Vitam_0", "Tue Aug 31 10:20:56 SGT 2016", "Tue Aug 31 10:20:56 SGT 2016");
            offerIdToMetadata.set("localhost", JsonHandler.toJsonNode(metaData));
            return offerIdToMetadata;
        } catch (InvalidParseOperationException e) {
            throw new StorageServerClientException(e);
        }
    }

    @Override
    public RequestResponse<OfferLog> getOfferLogs(String strategyId, DataCategory type, Long offset, int limit,
        Order order)
        throws StorageServerClientException {
        RequestResponseOK<OfferLog> requestResponseOK = new RequestResponseOK<>();
        OfferLog offerLog = new OfferLog();
        offerLog.setContainer(type.getFolder() + "_0");
        offerLog.setFileName("fileName_" + (offset + 1));
        offerLog.setSequence(offset + 1);
        offerLog.setTime(LocalDateTime.of(2017, 12, 13, 12, 0, 0, 0));
        requestResponseOK.addResult(offerLog);
        return requestResponseOK;
    }

}
