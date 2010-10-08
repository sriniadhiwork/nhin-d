/* 
 * Copyright (c) 2010, NHIN Direct Project
 * All rights reserved.
 *  
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in the 
 *    documentation and/or other materials provided with the distribution.  
 * 3. Neither the name of the the NHIN Direct Project (nhindirect.org)
 *    nor the names of its contributors may be used to endorse or promote products 
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY 
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.nhind.mail.service;

import ihe.iti.xds_b._2007.DocumentRepositoryPortType;
import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;

import java.util.UUID;

import oasis.names.tc.ebxml_regrep.xsd.rs._3.RegistryResponseType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhind.mail.util.DocumentRepositoryUtils;

/**
 * Document repository class for handling XDS webservice calls.
 * 
 * @author Vince
 */
public class DocumentRepository
{
    private String to = null;
    private String action = null;
    private String messageId = null;

    /**
     * Class logger.
     */
    private static final Log LOGGER = LogFactory.getFactory().getInstance(DocumentRepository.class);

    /**
     * Forward a given ProvideAndRegisterDocumentSetRequestType object to the
     * given XDR endpoint.
     * 
     * @param endpoint
     *            A URL representing an XDR endpoint.
     * @param prds
     *            The ProvideAndRegisterDocumentSetRequestType object.
     * @throws Exception
     */
    public String forwardRequest(String endpoint, ProvideAndRegisterDocumentSetRequestType prds) throws Exception
    {
        if (StringUtils.isBlank(endpoint))
            throw new IllegalArgumentException("Endpoint must not be blank");
        if (prds == null)
            throw new IllegalArgumentException("ProvideAndRegisterDocumentSetRequestType must not be null");

        LOGGER.info(" SENDING TO ENDPOINT " + endpoint);

        this.action = "urn:ihe:iti:2007:ProvideAndRegisterDocumentSet-bResponse";
        this.messageId = UUID.randomUUID().toString();
        this.to = endpoint;

        setHeaderData();

        DocumentRepositoryPortType port = null;

        try
        {
            port = DocumentRepositoryUtils.getDocumentRepositoryPortType(endpoint);
        }
        catch (Exception e)
        {
            LOGGER.error("Unable to create port", e);
            throw e;
        }

        // Inspect the message
        //
        // QName qname = new QName("urn:ihe:iti:xds-b:2007", "ProvideAndRegisterDocumentSet_bRequest");
        // String body = XMLUtils.marshal(qname, prds, ihe.iti.xds_b._2007.ObjectFactory.class);
        // LOGGER.info(body);

        RegistryResponseType rrt = port.documentRepositoryProvideAndRegisterDocumentSetB(prds);

        String response = rrt.getStatus();

        if (StringUtils.contains(response, "Failure"))
        {
            throw new Exception("Failure Returned from XDR forward");
        }

        LOGGER.info("Handling complete");

        return response;
    }

    /**
     * Set header data.
     * 
     * TODO: Investigate the usefulness of this method. It sets null known null
     * values.
     */
    protected void setHeaderData()
    {
        Long threadId = Long.valueOf(Thread.currentThread().getId());
        LOGGER.info("THREAD ID " + threadId);

        ThreadData threadData = new ThreadData(threadId);
        threadData.setTo(this.to);
        threadData.setMessageId(this.messageId);
        threadData.setAction(this.action);

        LOGGER.info(threadData.toString());
    }
}
