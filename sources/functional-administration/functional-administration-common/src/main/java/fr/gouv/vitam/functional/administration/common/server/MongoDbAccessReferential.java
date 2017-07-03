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
package fr.gouv.vitam.functional.administration.common.server;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * MongoDb Access Referential interface
 */
public interface MongoDbAccessReferential {

    /**
     * insert documents
     *
     * @param arrayNode of documents
     * @param collection collection of Mongo for insert
     * @throws ReferentialException when error occurs
     */
    void insertDocuments(ArrayNode arrayNode, FunctionalAdminCollections collection) throws ReferentialException;

    /**
     * insert documents
     *
     * @param jsonNode of documents
     * @param collection collection of Mongo for insert
     * @throws ReferentialException when error occurs
     */
    void insertDocument(JsonNode jsonNode, FunctionalAdminCollections collection) throws ReferentialException;

    /**
     * Delete FileFormat collections
     *
     * @param collection collection of Mongo for delete
     * @throws DatabaseException thrown when error on delete
     * @throws ReferentialException when error occurs
     */
    void deleteCollection(FunctionalAdminCollections collection) throws DatabaseException, ReferentialException;

    /**
     * @param id of vitam document
     * @param collection collection of Mongo
     * @return vitam document
     * @throws ReferentialException when error occurs
     */
    VitamDocument<?> getDocumentById(String id, FunctionalAdminCollections collection)
        throws ReferentialException;

	/**
	 * @param id functional id value
	 * @param collection Mongo collection
	 * @param field unique field in collection as functional id
	 * @return
	 * @throws ReferentialException
	 */
	VitamDocument<?> getDocumentByUniqueId(String id,
			FunctionalAdminCollections collection, String field)
			throws ReferentialException;
    /**
     * @param map Map of key-value
     * @param object
     * @param collection collection of Mongo for update
     * @param action update actions
     * @throws ReferentialException when error occurs
     */
    void updateDocumentByMap(Map<String, Object> map, JsonNode object, FunctionalAdminCollections collection,
        UPDATEACTION action)
        throws ReferentialException;

    /**
     * Update with queryDsl
     * 
     * @param update JsonNode to update
     * @param collection collection of Mongo Type for update
     * @return map of modified data
     * @throws ReferentialException when error occurs;
     */
    Map<String, List<String>> updateData(JsonNode update, FunctionalAdminCollections collection)
        throws ReferentialException;

    /**
     * @param select filter
     * @param collection collection of Mongo for find
     * @return vitam document list
     * @throws ReferentialException when error occurs
     */
    MongoCursor<? extends VitamDocument<?>> findDocuments(JsonNode select, FunctionalAdminCollections collection)
        throws ReferentialException;

}
