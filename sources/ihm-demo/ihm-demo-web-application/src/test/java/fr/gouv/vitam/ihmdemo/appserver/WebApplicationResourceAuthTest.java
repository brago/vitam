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
package fr.gouv.vitam.ihmdemo.appserver;

import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.junit.AfterClass;
import org.junit.Assert;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.ihmdemo.common.api.IhmDataRest;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import org.mockito.Matchers;
import static org.mockito.Matchers.contains;

/**
 *
 */
public class WebApplicationResourceAuthTest {
    private static final String DEFAULT_WEB_APP_CONTEXT = "/ihm-demo";
    private static final String DEFAULT_STATIC_CONTENT = "webapp";
    private static final String DEFAULT_WEB_APP_CONTEXT_V2 = "/ihm-demo-v2";
    private static final String DEFAULT_STATIC_CONTENT_V2 = "webapp/v2";
    private static final String OPTIONS = "{\"name\": \"myName\"}";
    private static final String CREDENTIALS = "{\"token\": {\"principal\": \"admin\", \"credentials\": \"admin\"}}";
    private static final String CREDENTIALS_NO_VALID =
        "{\"token\": {\"principal\": \"myName\", \"credentials\": \"myName\"}}";
    private static final String OPTIONS_DOWNLOAD = "{\"usage\": \"Dissemination\", \"version\": 1}";
    private static final String UPDATE = "{\"title\": \"myarchive\"}";
    private static final String DEFAULT_HOST = "localhost";
    private static final String JETTY_CONFIG = "jetty-config-test.xml";
    private static final String ALL_PARENTS = "[\"P1\", \"P2\", \"P3\"]";
    private static final String FAKE_STRING_RETURN = "Fake String";
    private static final JsonNode FAKE_JSONNODE_RETURN = JsonHandler.createObjectNode();
    private static final String IHM_DEMO_CONF = "ihm-demo.conf";

    private static JunitHelper junitHelper;
    private static int port;
    private static String sessionId;
    private static IhmDemoMain application;
    private static final List<Integer> tenants = new ArrayList<>();
    private static String tokenCSRF;

    /**
     * @throws Exception
     */
    @BeforeClass
    public static void setup() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        tenants.add(0);
        tenants.add(1);
        // TODO P1 verifier la compatibilité avec les tests parallèles sur jenkins
        final WebApplicationConfig webApplicationConfig =
            (WebApplicationConfig) new WebApplicationConfig().setPort(port)
                .setServerHost(DEFAULT_HOST).setJettyConfig(JETTY_CONFIG)
                .setBaseUrl(DEFAULT_WEB_APP_CONTEXT).setAuthentication(true)
                .setStaticContent(DEFAULT_STATIC_CONTENT_V2).setBaseUri(DEFAULT_WEB_APP_CONTEXT_V2);
        VitamConfiguration.setTenants(tenants);
        webApplicationConfig.setSecureMode(Arrays.asList("File", "LDAP"));
        final File conf = PropertiesUtils.findFile(IHM_DEMO_CONF);
        final File newConf = File.createTempFile("test", IHM_DEMO_CONF, conf.getParentFile());
        PropertiesUtils.writeYaml(newConf, webApplicationConfig);
        application = new IhmDemoMain(newConf.getAbsolutePath());
        application.start();
        RestAssured.port = port;
        RestAssured.basePath = DEFAULT_WEB_APP_CONTEXT + "/v1/api";

        Response response = given()
            .contentType(ContentType.JSON)
            .body(CREDENTIALS)
            .post("/login");
        JsonNode body= JsonHandler.getFromString(response.body().asString());
        sessionId = response.getCookie("JSESSIONID");
        tokenCSRF = body.get("tokenCSRF").asText();

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        application.stop();
        junitHelper.releasePort(port);
    }

    @Test
    public void testSecureModeAPI() throws InvalidParseOperationException {
        String authMode = given()
            .get("/securemode").body().asString();
        assertTrue(authMode.contains("LDAP"));
    }

    @Test
    public void givenEmptyPayloadWhenSearchOperationsThenReturnBadRequest() {
        given().cookie("JSESSIONID", sessionId).header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON).body("{}").expect()
            .statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/logbook/operations");
    }

    @Test
    public void testSuccessGetLogbookResult()
        throws InvalidParseOperationException, LogbookClientException, InvalidCreateOperationException {

        given().cookie("JSESSIONID", sessionId).header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .contentType(ContentType.JSON).body(OPTIONS).expect()
            .statusCode(Status.OK.getStatusCode()).when()
            .post("/logbook/operations");
    }


    @Test
    public void testSuccessGetLogbookResultFromSession()
        throws InvalidParseOperationException, LogbookClientException, InvalidCreateOperationException {

        final String requestId = given().cookie("JSESSIONID", sessionId)
            .header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF).contentType(ContentType.JSON).body(OPTIONS)
            .expect().statusCode(Status.OK.getStatusCode()).when()
            .post("/logbook/operations").header(GlobalDataRest.X_REQUEST_ID);


        given().cookie("JSESSIONID", sessionId).header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(GlobalDataRest.X_REQUEST_ID, requestId)
            .contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.OK.getStatusCode()).when()
            .post("/logbook/operations").header(GlobalDataRest.X_REQUEST_ID);

    }

    @Test
    public void testErrorGetLogbookResultUsingPagination()
        throws InvalidParseOperationException, LogbookClientException, InvalidCreateOperationException {

        given().cookie("JSESSIONID", sessionId).header(GlobalDataRest.X_CSRF_TOKEN, tokenCSRF)
            .header(IhmDataRest.X_LIMIT, "1A")
            .contentType(ContentType.JSON).body(OPTIONS).expect().statusCode(Status.BAD_REQUEST.getStatusCode()).when()
            .post("/logbook/operations").header(GlobalDataRest.X_REQUEST_ID);

    }



}
