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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ContractStatus;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;

/**
 * Handler class used to check the ingest contract of SIP. </br>
 */
public class CheckIngestContractActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckIngestContractActionHandler.class);

    // IN RANK
    private static final int SEDA_PARAMETERS_RANK = 0;

    private static final String HANDLER_ID = "CHECK_CONTRACT_INGEST";

    private HandlerIO handlerIO;
    final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public CheckIngestContractActionHandler() {}

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO ioParam) {
        checkMandatoryParameters(params);
        handlerIO = ioParam;
        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);
        CheckIngestContractStatus status;
        try {
            ObjectNode infoNode = JsonHandler.createObjectNode();
            checkMandatoryIOParameter(ioParam);
            final Map<String, Object> mandatoryValueMap =
                (Map<String, Object>) handlerIO.getInput(SEDA_PARAMETERS_RANK);
            String contractIdentifier = null;

            if (null != mandatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT) &&
                !((String) mandatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT)).isEmpty()) {
                contractIdentifier = (String) mandatoryValueMap.get(SedaConstants.TAG_ARCHIVAL_AGREEMENT);
                infoNode.put(SedaConstants.TAG_ARCHIVAL_AGREEMENT, contractIdentifier);
            }

            status = checkIngestContract(contractIdentifier);

            switch (status) {
                case INACTIVE:
                    itemStatus.setGlobalOutcomeDetailSubcode(CheckIngestContractStatus.INACTIVE.toString());
                    itemStatus.increment(StatusCode.KO);
                    break;
                case UNKNOWN:
                    itemStatus.setGlobalOutcomeDetailSubcode(CheckIngestContractStatus.UNKNOWN.toString());
                    itemStatus.increment(StatusCode.KO);
                    break;
                case NOT_PRESENT_IN_MANIFEST:
                    infoNode.put(SedaConstants.EV_DET_TECH_DATA, "Error Ingest constract not found in the Manifest");
                    itemStatus.setEvDetailData(JsonHandler.unprettyPrint(infoNode));
                    itemStatus.increment(StatusCode.KO);
                    break;
                case KO:
                    itemStatus.increment(StatusCode.KO);
                    break;
                case OK:
                    itemStatus.increment(StatusCode.OK);
                    break;
            }

        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        }
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * @param contractIdentifier name contract
     * @return true if contract ok
     */
    private CheckIngestContractStatus checkIngestContract(String contractIdentifier) {

        // Case when no contract on the manifest
        if (!ParametersChecker.isNotEmpty(contractIdentifier)) {
            return CheckIngestContractStatus.NOT_PRESENT_IN_MANIFEST;
        }

        try (final AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            RequestResponse<IngestContractModel> referenceContracts =
                client.findIngestContractsByID(contractIdentifier);
            if (referenceContracts.isOk()) {
                List<IngestContractModel> results = ((RequestResponseOK) referenceContracts).getResults();
                if (!results.isEmpty()) {
                    for (IngestContractModel result : results) {
                        ContractStatus status = result.getStatus();
                        if (ContractStatus.ACTIVE.equals(status) && result.getIdentifier().equals(contractIdentifier)) {
                            return CheckIngestContractStatus.OK;
                        } else {
                            return CheckIngestContractStatus.INACTIVE;
                        }
                    }
                }
            }
        } catch (AdminManagementClientServerException | InvalidParseOperationException e) {
            LOGGER.error(e);
        } catch (ReferentialNotFoundException e) {
            // Case when the manifest's contract is not in the database of contracts
            LOGGER.error("Contract not found :", e);
            return CheckIngestContractStatus.UNKNOWN;
        }

        return CheckIngestContractStatus.KO;
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO Auto-generated method stub
    }

    /**
     * Check ingest contract status values
     */
    public enum CheckIngestContractStatus {

        /**
         * Ingest constract not present in the Manifest.
         */
        NOT_PRESENT_IN_MANIFEST,
        /**
         * Missing contract: not exists in the database
         */
        UNKNOWN,
        /**
         * Existing but inactive contract
         */
        INACTIVE,
        /**
         * OK contract
         */
        OK,
        /**
         * Other error situation
         */
        KO
    }
}
