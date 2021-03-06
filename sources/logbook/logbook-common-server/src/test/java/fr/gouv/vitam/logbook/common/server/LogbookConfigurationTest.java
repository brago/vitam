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
package fr.gouv.vitam.logbook.common.server;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;
import fr.gouv.vitam.logbook.common.server.LogbookConfiguration;

public class LogbookConfigurationTest {

    private static final String HOST = "host";
    private static final int PORT = 1234;
    private static final String DB_NAME = "dbNameTest";
    private static final String JETTY_CONF = "jettyConfig";
    private static final String P2_FILE = "tsa.p12";
    private static final String P2_PASSWORD = "1234";
    private static final String WORKSPACE_URL = "http://localhost:8082";
    private static final String PROCESSING_URL = "http://localhost:8087";

    private final static String CLUSTER_NAME = "cluster-vitam";
    private final static String HOST_NAME = "localhost";
    private static int TCP_PORT = 9300;


    @Test
    public void testSetterGetter() {
        final List<MongoDbNode> mongo_nodes = new ArrayList<>();
        mongo_nodes.add(new MongoDbNode(HOST, PORT));
        final LogbookConfiguration config1 = new LogbookConfiguration();
        config1.setMongoDbNodes(mongo_nodes);
        assertEquals(config1.getMongoDbNodes().get(0).getDbHost(), HOST);
        assertEquals(config1.getMongoDbNodes().get(0).getDbPort(), PORT);
        assertEquals(config1.setDbName(DB_NAME).getDbName(), DB_NAME);
        assertEquals(JETTY_CONF, config1.setJettyConfig(JETTY_CONF).getJettyConfig());
        assertEquals(CLUSTER_NAME, config1.setClusterName(CLUSTER_NAME).getClusterName());
        config1.setP12LogbookFile(P2_FILE);
        assertEquals(P2_FILE, config1.getP12LogbookFile());
        config1.setP12LogbookPassword(P2_PASSWORD);
        assertEquals(P2_PASSWORD, config1.getP12LogbookPassword());
        config1.setWorkspaceUrl(WORKSPACE_URL);
        assertEquals(WORKSPACE_URL, config1.getWorkspaceUrl());
        config1.setProcessingUrl(PROCESSING_URL);
        assertEquals(PROCESSING_URL, config1.getProcessingUrl());
        final List<ElasticsearchNode> es_nodes = new ArrayList<>();
        es_nodes.add(new ElasticsearchNode(HOST_NAME, TCP_PORT));
        assertEquals(1, config1.setElasticsearchNodes(es_nodes).getElasticsearchNodes().size());

        final LogbookConfiguration config2 =
            new LogbookConfiguration(mongo_nodes, DB_NAME, CLUSTER_NAME, es_nodes);
        assertEquals(config2.getMongoDbNodes().get(0).getDbHost(), HOST);
        assertEquals(config2.getMongoDbNodes().get(0).getDbPort(), PORT);
        assertEquals(config2.getDbName(), DB_NAME);
        assertEquals(config2.getClusterName(), CLUSTER_NAME);
        assertEquals(config2.getElasticsearchNodes().size(), 1);
    }
}
