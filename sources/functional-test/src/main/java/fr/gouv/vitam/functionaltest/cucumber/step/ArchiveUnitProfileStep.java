/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functionaltest.cucumber.step;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;

import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ArchiveUnitProfileModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;

/**
 * Profile Step
 */
public class ArchiveUnitProfileStep {

    public ArchiveUnitProfileStep(World world) {
        this.world = world;
    }

    /**
     * ★★★ The world ★★★
     */
    private World world;
    /**
     * ★★★ The file name ★★★
     */
    private String fileName;


    /**
     * generic model result
     */
    private JsonNode model;

    /**
     * ★★★ define a sip ★★★
     *
     * @param fileName name of a sip
     */
    @Given("^un document type nommé (.*)$")
    public void an_archive_profile_named(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @throws InvalidParseOperationException
     * @throws IOException
     * @throws AccessExternalClientException
     */
    @When("je fais un import du document type")
    public void create_profile()
        throws InvalidParseOperationException, IOException, AccessExternalClientException {
        Path profil = Paths.get(world.getBaseDirectory(), fileName);
        final RequestResponse response =
            world.getAdminClient()
                .createArchiveUnitProfile(
                    new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                    Files.newInputStream(profil, StandardOpenOption.READ));
        assertThat(Response.Status.OK.getStatusCode() == response.getStatus());
        if (response.isOk()) {
            RequestResponseOK<ProfileModel> res = (RequestResponseOK) response;
            Object o = (res.getResults().stream().findFirst()).get();
            this.model = (JsonNode) o;
        } else {
            fail("Fail to import archive unit profile :" + response.toString());
        }
        final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);
    }

    @When("^je cherche un document type nommé (.*)")
    public void search_profiles(String name)
        throws AccessExternalClientException, InvalidParseOperationException, InvalidCreateOperationException,
        VitamClientException {
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        select.setQuery(match("Name", name));
        final JsonNode query = select.getFinalSelect();
        RequestResponse<ArchiveUnitProfileModel> requestResponse =
            world.getAdminClient().findArchiveUnitProfiles(
                new VitamContext(world.getTenantId()).setAccessContract(null)
                    .setApplicationSessionId(world.getApplicationSessionId()),
                query);
        if (requestResponse.isOk()) {
            this.model = ((RequestResponseOK<ArchiveUnitProfileModel>) requestResponse).getResultsAsJsonNodes().get(0);
        }
    }


    @Then("^le document type existe$")
    public void contract_found_are() {
        assertThat(this.model).isNotNull();
    }

    @Then("^les métadonnées du document type sont$")
    public void metadata_are(DataTable dataTable) throws Throwable {
        List<List<String>> raws = dataTable.raw();
        for (List<String> raw : raws) {
            String index = raw.get(0);
            String value = raw.get(1);
            assertThat(this.model.get(index).asText()).contains(value);
        }
    }

}
