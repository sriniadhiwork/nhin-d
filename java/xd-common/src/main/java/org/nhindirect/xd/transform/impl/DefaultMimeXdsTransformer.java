/* 
Copyright (c) 2010, NHIN Direct Project
All rights reserved.

Authors:
   Vincent Lewis     vincent.lewis@gsihealth.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.  Neither the name of the The NHIN Direct Project (nhindirect.org). 
nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.nhindirect.xd.transform.impl;

import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.xd.common.DirectDocument2;
import org.nhindirect.xd.common.DirectDocuments;
import org.nhindirect.xd.common.XdmPackage;
import org.nhindirect.xd.common.type.DirectDocumentType;
import org.nhindirect.xd.common.type.FormatCodeEnum;
import org.nhindirect.xd.transform.MimeXdsTransformer;
import org.nhindirect.xd.transform.exception.TransformationException;
import org.nhindirect.xd.transform.util.Utils;
import org.nhindirect.xd.transform.util.type.MimeType;


/**
 * Transform a MimeMessage into a XDS request.
 * 
 * @author vlewis
 */
public class DefaultMimeXdsTransformer implements MimeXdsTransformer {

    private static final Log LOGGER = LogFactory.getFactory().getInstance(DefaultMimeXdsTransformer.class);

    public DefaultMimeXdsTransformer() {
        super();
    }

    @Override
    public ProvideAndRegisterDocumentSetRequestType transform(MimeMessage mimeMessage) throws TransformationException {
        ProvideAndRegisterDocumentSetRequestType request;
        DirectDocuments documents = new DirectDocuments();
        
        byte[] xdsDocument = null;
        String xdsMimeType = null;
        FormatCodeEnum xdsFormatCode = null;
        DirectDocumentType documentType = null;
        

        try {
            Date sentDate = mimeMessage.getSentDate();
            String subject = mimeMessage.getSubject();
            String from = mimeMessage.getFrom()[0].toString();
            Address[] recipients = mimeMessage.getAllRecipients();

            // Plain mail (no attachments)
            if (MimeType.TEXT_PLAIN.matches(mimeMessage.getContentType())) {
                LOGGER.debug("Handling plain mail (no attachments) - " + mimeMessage.getContentType());

                // Get the document type
                documentType = DirectDocumentType.lookup(mimeMessage);

                // Get the format code and MIME type
                xdsFormatCode = documentType.getFormatCode();
                xdsMimeType = documentType.getMimeType().getType();

                // Get the contents
                xdsDocument = ((String) mimeMessage.getContent()).getBytes();

                // Add document to the collection of documents
                
                /**
                 * If plain body, do not add as attachment
                 * @auth: Yan Wang <ywang@max.md>
                 * @date: Sep 21 2014
                 */
                documents.getDocuments().add(getDocument(sentDate, from, xdsMimeType, xdsFormatCode, xdsDocument));
                documents.setSubmissionSet(getSubmissionSet(subject, sentDate, from, recipients));
            } // Multipart/mixed (attachments)
            else if (MimeType.MULTIPART.matches(mimeMessage.getContentType())) {
                LOGGER.debug("Handling multipart/mixed - " + mimeMessage.getContentType());

                MimeMultipart mimeMultipart = (MimeMultipart) mimeMessage.getContent();
               BodyPart xdmBodyPart = null;
                
                for (int i = 0; i < mimeMultipart.getCount(); i++) {
                    //check for XDM
                     BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                     documentType = DirectDocumentType.lookup(bodyPart);
                    if (DirectDocumentType.XDM.equals(documentType)) {
                        LOGGER.debug("\n\tFile name: " + bodyPart.getFileName()+"\n\tContent type: " + bodyPart.getContentType()+"\n\tDocumentType: " + documentType.toString());
                        xdmBodyPart =  bodyPart;
                    }
                }
                
                /*
                 * Special handling for XDM attachments.
                 * 
                 * Spec says if XDM package is present, this will be the
                 * only attachment.
                 * 
                 * Overwrite all documents with XDM content and then break
                 */
                if (xdmBodyPart != null) {
                    XdmPackage xdmPackage = XdmPackage.fromXdmZipDataHandler(xdmBodyPart.getDataHandler());
                    // Spec says if XDM package is present, this will be the only attachment
                    // Overwrite all documents with XDM content and then break
                    LOGGER.info("XDM FILE FOUND");
                    documents = xdmPackage.getDocuments();
                } else{
                    boolean isCCD;
                    /**
                     * For each non-ccd part
                     */
                    LOGGER.trace("Looking for non-CCD attachments");
                    for (int i = 0; i < mimeMultipart.getCount(); i++) {
                        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                        // Skip empty BodyParts
                        if (bodyPart.getSize() <= 0) {
                            LOGGER.warn("Empty body, skipping");
                            continue;
                        }
                        documentType = DirectDocumentType.lookup(bodyPart);
                        isCCD = DirectDocumentType.CCD.equals(documentType) || DirectDocumentType.XML.equals(documentType);

                        if (!isCCD){
                            // Get the format code and MIME type
                            xdsFormatCode = documentType.getFormatCode();
                            xdsMimeType = documentType.getMimeType().getType();
                            // Best guess for UNKNOWN MIME type
                            if (DirectDocumentType.UNKNOWN.equals(documentType)) {
                                xdsMimeType = bodyPart.getContentType();
                            }
                            
                            LOGGER.debug("\n\tFile name: " + bodyPart.getFileName()+"\n\tContent type: " + bodyPart.getContentType()+"\n\tDocumentType: " + documentType.toString());
                            xdsDocument = read(bodyPart);
                            documents.getDocuments().add(getDocument(sentDate, from, xdsMimeType, xdsFormatCode, xdsDocument));
                        }
                    }
                    LOGGER.trace("Looking for CCD attachments");
                    for (int i = 0; i < mimeMultipart.getCount(); i++) {
                        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                        // Skip empty BodyParts
                        if (bodyPart.getSize() <= 0) {
                            LOGGER.warn("Empty body, skipping");
                            continue;
                        }

                        documentType = DirectDocumentType.lookup(bodyPart);
                        isCCD = DirectDocumentType.CCD.equals(documentType) || DirectDocumentType.XML.equals(documentType);
                        
                        if (isCCD){
                            // Get the format code and MIME type
                            xdsFormatCode = documentType.getFormatCode();
                            xdsMimeType = documentType.getMimeType().getType();

                            LOGGER.debug("\n\tFile name: " + bodyPart.getFileName()+"\n\tContent type: " + bodyPart.getContentType()+"\n\tDocumentType: " + documentType.toString());
                            xdsDocument = read(bodyPart);
                            documents.getDocuments().add(getDocument(sentDate, from, xdsMimeType, xdsFormatCode, xdsDocument));
                        }
                        
                    }
                    documents.setSubmissionSet(getSubmissionSet(subject, sentDate, from, recipients));
                }
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Message content type (" + mimeMessage.getContentType() + ") is not supported, skipping");
                }
            }
        } catch (MessagingException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unexpected MessagingException occured while handling MimeMessage", e);
            }
            throw new TransformationException("Unable to complete transformation.", e);
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unexpected IOException occured while handling MimeMessage", e);
            }
            throw new TransformationException("Unable to complete transformation.", e);
        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unexpected Exception occured while handling MimeMessage", e);
            }
            throw new TransformationException("Unable to complete transformation", e);
        }

        try {
            request = documents.toProvideAndRegisterDocumentSetRequestType();
        } catch (IOException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unexpected IOException occured while transforming to ProvideAndRegisterDocumentSetRequestType", e);
            }
            throw new TransformationException("Unable to complete transformation", e);
        }

        return request;
    }

    /*
     * Metadata Attribute           XDS     Minimal Metadata
     * -----------------------------------------------------
     * author                       R2      R
     * contentTypeCode              R       R2
     * entryUUID                    R       R
     * intendedRecipient            O       R
     * patientId                    R       R2
     * sourceId                     R       R
     * submissionTime               R       R
     * title                        O       O
     * uniqueId                     R       R
     */
    private DirectDocuments.SubmissionSet getSubmissionSet(String subject, Date sentDate, String auth, Address[] recipients) throws Exception {
        DirectDocuments.SubmissionSet submissionSet = new DirectDocuments.SubmissionSet();

        // (R) Minimal Metadata Source
        if (auth.contains("<")){
            int p = auth.indexOf("<");
            auth = auth.substring(p+1);
            p = auth.indexOf(">");
            if (p > 0){
                auth = auth.substring(0, p);
            }
        }
        submissionSet.setAuthorTelecommunication("^^Internet^" + auth); // TODO: format this correctly
//        submissionSet.setAuthorTelecommunication(auth); // TODO: format this correctly
        submissionSet.setSubmissionTime(sentDate);
        submissionSet.setUniqueId(UUID.randomUUID().toString());
        
        submissionSet.setSourceId(Utils.GetMaxMDSourceID()); // TODO: "UUID URN mapped by configuration to sending organization"
        submissionSet.setPatientId(Utils.GetMaxMDPatientID());
        submissionSet.setContentTypeCode(Utils.GetMaxMDContentTypeCode(), true);
        
        for (Address address : recipients) {
            submissionSet.getIntendedRecipient().add("||^^Internet^" + address.toString());
        }
        
        /*
         * Set XDR subject
         * Update by Yan Wang at Aug 21 2014
         */
        submissionSet.setName("Email subject: "+ subject);

        return submissionSet;
    }

    /*
     * Metadata Attribute           XDS     Minimal Metadata
     * -----------------------------------------------------
     * author                       R2      R2
     * classCode                    R       R2
     * confidentialityCode          R       R2
     * creationTime                 R       R2
     * entriUUID                    R       R
     * formatCode                   R       R
     * healthcareFacilityTypeCode   R       R2
     * languageCode                 R       R2
     * mimeType                     R       R
     * patientId                    R       R2
     * practiceSettingCode          R       R2
     * sourcePatientId              R       R2
     * typeCode                     R       R2
     * uniqueId                     R       R
     */
    private DirectDocument2 getDocument(Date sentDate, String auth, String xdsMimeType, FormatCodeEnum xdsFormatCode, byte[] xdsDocument) throws Exception {
        DirectDocument2 document = new DirectDocument2();
        DirectDocument2.Metadata metadata = document.getMetadata();

        // (R) Minimal Metadata Source
        metadata.setMimeType(xdsMimeType);
        metadata.setUniqueId(Utils.GetDocUniqueId());
//        metadata.setUniqueId(UUID.randomUUID().toString());
        
        // (R2) Minimal Metadata Source
        if (xdsFormatCode != null) {
            metadata.setFormatCode(xdsFormatCode);
        }

        /**
         * Added by Yan Wang <ywang@max.md> at Sep 21 2014
         */
        
        metadata.setCreationTime(sentDate);
        metadata.setLanguageCode("en-us");
        metadata.setPatientId(Utils.GetMaxMDPatientID());
        

        document.setData(xdsDocument);

        return document;
    }

    private static byte[] read(BodyPart bodyPart) throws MessagingException, IOException {
        InputStream inputStream = bodyPart.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int data = 0;
        byte[] buffer = new byte[1024];
        while ((data = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, data);
        }

        return outputStream.toByteArray();
    }
}
