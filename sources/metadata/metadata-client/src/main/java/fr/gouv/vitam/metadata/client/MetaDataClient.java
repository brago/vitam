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
 **/

package fr.gouv.vitam.metadata.client;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.client.BasicClient;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parameter.SwitchIndexParameters;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.api.model.UnitPerOriginatingAgency;

/**
 * Metadata client interface
 */
public interface MetaDataClient extends BasicClient {

    /**
     * @param insertQuery as JsonNode <br>
     *        null is not allowed
     * @return the result as JsonNode
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataDocumentSizeException
     * @throws MetaDataClientServerException
     */
    JsonNode insertUnit(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataNotFoundException, MetaDataAlreadyExistException, MetaDataDocumentSizeException,
        MetaDataClientServerException;

    /**
     * Search units by select query (DSL)
     *
     * @param selectQuery : select query {@link fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery}
     *        as String <br>
     *        Null is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetaDataClientServerException
     */
    JsonNode selectUnits(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException,
        InvalidParseOperationException, MetaDataClientServerException, VitamDBException;

    /**
     * Search units by query (DSL) and path unit id
     *
     * @param selectQuery : select query {@link fr.gouv.vitam.common.database.builder.request.single.Select} as JsonNode
     *        <br>
     *        Null is not allowed
     * @param unitId : unit id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetaDataClientServerException
     */
    JsonNode selectUnitbyId(JsonNode selectQuery, String unitId)
        throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException;

    /**
     * Search Object Group by query (DSL) and path objectGroup id
     *
     * @param selectQuery : select query {@link fr.gouv.vitam.common.database.builder.request.single.Select} as JsonNode
     *        <br>
     *        Null is not allowed
     * @param objectGroupId : objectGroup id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException thrown when the Query is badly formatted or objectGroupId is empty
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetadataInvalidSelectException thrown when objectGroupId or selectQuery id is null or blank
     * @throws MetaDataClientServerException
     */
    JsonNode selectObjectGrouptbyId(JsonNode selectQuery, String objectGroupId)
        throws MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetadataInvalidSelectException,
        MetaDataClientServerException;

    /**
     * Update units by query (DSL) and path unit id
     *
     * @param updateQuery update query {@link fr.gouv.vitam.common.database.builder.request.single.Select} as JsonNode
     *        <br>
     *        Null is not allowed
     * @param unitId unit id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetaDataNotFoundException
     * @throws MetaDataClientServerException
     */
    JsonNode updateUnitbyId(JsonNode updateQuery, String unitId)
        throws MetaDataNotFoundException, MetaDataExecutionException,
        MetaDataDocumentSizeException, InvalidParseOperationException, MetaDataClientServerException;

    /**
     * @param insertQuery as String
     * @return response as JsonNode contains the request result
     * @throws InvalidParseOperationException
     * @throws MetaDataExecutionException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataDocumentSizeException
     * @throws MetaDataClientServerException
     */
    JsonNode insertObjectGroup(JsonNode insertQuery)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataNotFoundException, MetaDataAlreadyExistException, MetaDataDocumentSizeException,
        MetaDataClientServerException;

    /**
     * Update ObjectGroup
     * 
     * @param updateQuery
     * @param objectGroupId
     * @throws InvalidParseOperationException
     * @throws MetaDataNotFoundException
     * @throws MetaDataAlreadyExistException
     * @throws MetaDataDocumentSizeException
     * @throws MetaDataClientServerException
     * @throws MetaDataExecutionException
     */
    void updateObjectGroupById(JsonNode updateQuery, String objectGroupId)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataExecutionException;

    /**
     * 
     * @param operationId
     * @return the list of UnitsPerOriginatingAgency
     * @throws MetaDataClientServerException
     */
    List<UnitPerOriginatingAgency> selectAccessionRegisterOnUnitByOperationId(String operationId)
        throws MetaDataClientServerException;

    /**
     * 
     * @param operationId
     * @return the list of ObjectGroupPerOriginatingAgency
     * @throws MetaDataClientServerException
     */
    List<ObjectGroupPerOriginatingAgency> selectAccessionRegisterOnObjectByOperationId(String operationId)
        throws MetaDataClientServerException;

    /**
     * Search objectgroups by select query (DSL)
     *
     * @param selectQuery : select query {@link fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery}
     *        as String <br>
     *        Null is not allowed
     * @return Json object {$hint:{},$result:[{},{}]}
     * @throws MetaDataExecutionException thrown when internal Server Error (fatal technical exception thrown)
     * @throws InvalidParseOperationException
     * @throws MetaDataDocumentSizeException thrown when Query document Size is Too Large
     * @throws MetaDataClientServerException
     */
    JsonNode selectObjectGroups(JsonNode selectQuery)
        throws MetaDataExecutionException, MetaDataDocumentSizeException, InvalidParseOperationException,
        MetaDataClientServerException;

    /**
     * 
     * @return True if the Units index is flushed
     * @throws MetaDataClientServerException
     */
    boolean flushUnits() throws MetaDataClientServerException;

    /**
     * 
     * @return True if the ObjectGroups index is flushed
     * @throws MetaDataClientServerException
     */
    boolean flushObjectGroups() throws MetaDataClientServerException;

    /**
     * 
     * Reindex a collection with parameters
     * 
     * @param indexParam reindexation parameters
     * @return JsonObject containing information about the newly created index
     * @throws MetaDataClientServerException
     * @throws MetaDataNotFoundException in case the index does not exist
     * @throws InvalidParseOperationException
     */
    JsonNode reindex(IndexParameters indexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException;

    /**
     * 
     * Switch indexes
     * 
     * @param switchIndexParam switch index parameters
     * @return JsonObject containing information about the newly created index
     * @throws MetaDataClientServerException
     * @throws MetaDataNotFoundException in case the index does not exist
     * @throws InvalidParseOperationException
     */
    JsonNode switchIndexes(SwitchIndexParameters switchIndexParam)
        throws InvalidParseOperationException, MetaDataClientServerException, MetaDataNotFoundException;

    /**
     * Search units by path unit id
     *
     * @param unitId : unit id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{}]} or vitam error
     */
    RequestResponse<JsonNode> getUnitByIdRaw(String unitId) throws VitamClientException ;

    /**
     * Search object group by path unit id
     *
     * @param objectGroupId : objectGroup id <br>
     *        null and blank is not allowed
     * @return Json object {$hint:{},$result:[{}]} or vitam error
     */
    RequestResponse<JsonNode> getObjectGroupByIdRaw(String objectGroupId) throws VitamClientException ;
}
