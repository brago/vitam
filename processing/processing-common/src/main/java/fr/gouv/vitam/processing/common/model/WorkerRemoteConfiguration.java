/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Worker remote configuration : contains the properties used to create a worker client.
 */
public class WorkerRemoteConfiguration {

    @JsonProperty("serverHost")
    private String serverHost;
    @JsonProperty("serverPort")
    private int serverPort;
    @JsonProperty("serverContextPath")
    private String serverContextPath;
    @JsonProperty("useSSL")
    private boolean useSSL;


    /**
     * @param serverHost : the worker server host
     * @param serverPort : the worker server port
     * @param serverContextPath : the worker server servlet context path
     * @param useSSL : the worker server use SSL
     */
    @JsonCreator
    public WorkerRemoteConfiguration(@JsonProperty("serverHost") String serverHost,
        @JsonProperty("serverPort") int serverPort,
        @JsonProperty("serverContextPath") String serverContextPath, @JsonProperty("useSSL") boolean useSSL) {
        ParametersChecker.checkParameter("serverPort is a mandatory parameter", serverPort);
        ParametersChecker.checkParameter("serverHost is a mandatory parameter", serverHost);
        ParametersChecker.checkParameter("serverContextPath is a mandatory parameter", serverContextPath);
        ParametersChecker.checkParameter("useSSL is a mandatory parameter", useSSL);
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.serverContextPath = serverContextPath;
        this.useSSL = useSSL;
    }

    /**
     * @return the serverHost
     */
    public String getServerHost() {
        return serverHost;
    }

    /**
     * @param serverHost the serverHost to set
     *
     * @return this
     */
    public WorkerRemoteConfiguration setServerHost(String serverHost) {
        this.serverHost = serverHost;
        return this;
    }

    /**
     * @return the serverPort
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * @param serverPort the serverPort to set
     *
     * @return this
     */
    public WorkerRemoteConfiguration setServerPort(int serverPort) {
        this.serverPort = serverPort;
        return this;
    }

    /**
     * @return the serverContextPath
     */
    public String getServerContextPath() {
        return serverContextPath;
    }



    /**
     * @param serverContextPath the serverContextPath to set
     *
     * @return this
     */
    public WorkerRemoteConfiguration setServerContextPath(String serverContextPath) {
        this.serverContextPath = serverContextPath;
        return this;
    }


    /**
     * @return the useSSL
     */
    public boolean getUseSSL() {
        return useSSL;
    }


    /**
     * @param useSSL the useSSL to set
     *
     * @return this
     */
    public WorkerRemoteConfiguration setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
        return this;
    }


    /**
     * toString : get the worker remote configuration
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("serverHost=" + getServerHost() + "\n");
        sb.append("serverPort=" + getServerPort() + "\n");
        sb.append("serverContextPath=" + getServerContextPath() + "\n");
        sb.append("useSSL=" + getUseSSL() + "\n");
        return sb.toString();
    }

}
