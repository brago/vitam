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
package fr.gouv.vitam.storage.offers.workspace.driver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;

public class DriverImplTest extends VitamJerseyTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DriverImplTest.class);

    protected static final String HOSTNAME = "localhost";
    private static final String DRIVER_NAME = "WorkspaceDriver";
    private static JunitHelper junitHelper;
    private static StorageOffer offer = new StorageOffer();

    public DriverImplTest() {
        super(new TestVitamClientFactory(8080, "/offer/v1", mock(Client.class)));
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        junitHelper = JunitHelper.getInstance();
    }

    // Define the getApplication to return your Application using the correct
    // Configuration
    @Override
    public StartApplicationResponse<AbstractApplication> startVitamApplication(int reservedPort) {
        final TestVitamApplicationConfiguration configuration = new TestVitamApplicationConfiguration();
        configuration.setJettyConfig(DEFAULT_XML_CONFIGURATION_FILE);
        final AbstractApplication application = new AbstractApplication(configuration);
        try {
            application.start();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException("Cannot start the application", e);
        }
        return new StartApplicationResponse<AbstractApplication>().setServerPort(application.getVitamServer().getPort())
                .setApplication(application);
    }

    // Define your Application class if necessary
    public final class AbstractApplication
            extends AbstractVitamApplication<AbstractApplication, TestVitamApplicationConfiguration> {
        protected AbstractApplication(TestVitamApplicationConfiguration configuration) {
            super(TestVitamApplicationConfiguration.class, configuration);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.registerInstances(new MockResource(mock));
        }

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }

        @Override
        protected void configureVitamParameters() {
            // None
            VitamConfiguration.setSecret("vitamsecret");
            VitamConfiguration.setFilterActivation(false);
        }

    }

    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {
    }

    @Path("/offer/v1")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path("/status")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void givenNullUrlThenRaiseAnException() throws Exception {
        DriverImpl.getInstance().connect(null);
    }

    @Test(expected = StorageDriverException.class)
    public void givenCorrectUrlThenConnectResponseKO() throws Exception {
        try {
            offer.setBaseUrl("http://" + HOSTNAME + ":" + getServerPort());
            offer.setId("default");
            when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
            Driver driver = DriverImpl.getInstance();
            driver.addOffer(offer, null);
            Connection connection = driver.connect(offer.getId());
            connection.getStorageCapacity(0);
        } catch (Exception e) {
            LOGGER.error(e);
            throw e;
        }
    }

    @Test
    public void givenCorrectUrlThenConnectResponseNoContent() throws Exception {
        offer.setBaseUrl("http://" + HOSTNAME + ":" + getServerPort());
        offer.setId("default2");
        when(mock.get()).thenReturn(Response.status(Status.NO_CONTENT).build());
        Driver driver = DriverImpl.getInstance();
        driver.addOffer(offer, null);

        final Connection connection = driver.connect(offer.getId());

        assertNotNull(connection);
    }

    @Test()
    public void getNameOK() throws Exception {
        assertEquals(DRIVER_NAME, DriverImpl.getInstance().getName());
    }

    @Test()
    public void isStorageOfferAvailableOK() throws Exception {
        assertEquals(false, DriverImpl.getInstance().isStorageOfferAvailable(null));
    }

    @Test()
    public void getMajorVersionOK() throws Exception {
        assertEquals(0, DriverImpl.getInstance().getMajorVersion());
    }

    @Test()
    public void getMinorVersionOK() throws Exception {
        assertEquals(0, DriverImpl.getInstance().getMinorVersion());
    }

}
