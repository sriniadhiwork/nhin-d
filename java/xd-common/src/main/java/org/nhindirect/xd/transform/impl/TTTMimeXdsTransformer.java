package org.nhindirect.xd.transform.impl;

import static org.nhindirect.xd.transform.impl.BaseMimeXdsTransformer.read;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.axis.utils.StringUtils;
import org.apache.commons.io.FileUtils;
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

import ihe.iti.xds_b._2007.ProvideAndRegisterDocumentSetRequestType;

/**
 * Transformer for Transport Testing Tool
 * @auth ywang <ywang@max.md>
 * @date Sep 30, 2015
 */
public class TTTMimeXdsTransformer extends BaseMimeXdsTransformer implements MimeXdsTransformer {
    private static final Log LOGGER = LogFactory.getFactory().getInstance(TTTMimeXdsTransformer.class);

    public TTTMimeXdsTransformer() {
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
            String messageId = mimeMessage.getMessageID();
            String from = mimeMessage.getFrom()[0].toString();
            Address[] recipients = mimeMessage.getAllRecipients();

            // Plain mail (no attachments)
            if (MimeType.TEXT_PLAIN.matches(mimeMessage.getContentType()) || MimeType.MULTIPART_ALTERNATIVE.matches(mimeMessage.getContentType())) {
                LOGGER.debug("Handling no attachments message - " + mimeMessage.getContentType());
                DirectDocument2 bodyDoc = getEmailBodyDocument(sentDate, mimeMessage);
                if (bodyDoc != null){
                    documents.getDocuments().add(bodyDoc);
                } else{
                    LOGGER.warn("No text/plain body found");
                }
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
                    boolean emailBodyFound = false;
                    
                    for (int i = 0; i < mimeMultipart.getCount(); i++) {
                        BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                        
                        /**
                         * Find first text/plain part or multipart/alternative
                         */
                        if (!emailBodyFound && (MimeType.TEXT_PLAIN.matches(bodyPart.getContentType()) || MimeType.MULTIPART_ALTERNATIVE.matches(bodyPart.getContentType()) ) ){
                            LOGGER.debug("\n\tEmail body found. File name: " + bodyPart.getFileName()+"\n\tContent type: " + bodyPart.getContentType());
                            emailBodyFound = true;
                            
                            // skip email body for TTT.
//                            documents.getDocuments().add(getEmailBodyDocument(sentDate, bodyPart));
                        } else{
                            if (bodyPart.getSize() <= 0) {
                                LOGGER.warn("Empty body, skipping");
                                continue;
                            }
                            
                            documentType = DirectDocumentType.lookup(bodyPart);
                            LOGGER.debug("\n\tFile name: " + bodyPart.getFileName()+"\n\tContent type: " + bodyPart.getContentType()+"\n\tDocumentType: " + documentType.toString());

                            
                            xdsFormatCode = documentType.getFormatCode();
                            xdsMimeType = documentType.getMimeType().getType();
                            // Best guess for UNKNOWN MIME type
                            if (DirectDocumentType.UNKNOWN.equals(documentType)) {
                                xdsMimeType = bodyPart.getContentType();
                            }
                            xdsDocument = read(bodyPart);
                            
                          /*  File tmpDir = new File("/usr/local/contrib/var/direct/10mintmp");
                            File attDump = File.createTempFile("attachment-" + (new Date()).toString() +"-" + bodyPart.getFileName(), ".data", tmpDir);
                            FileUtils.writeByteArrayToFile(attDump, xdsDocument);
                            LOGGER.debug("Saved BodyPart Data to: " + attDump.getAbsolutePath());*/
                            
                            documents.getDocuments().add(getDocument(sentDate, from, xdsMimeType, xdsFormatCode, xdsDocument, bodyPart));
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
        } catch (Throwable e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unexpected Exception occured while handling MimeMessage", e);
            }
            throw new TransformationException("Unable to complete transformation", e);
        }

        try {
            request = documents.toProvideAndRegisterDocumentSetRequestType();
        } catch (Throwable e) {
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
    @Override
    protected DirectDocuments.SubmissionSet getSubmissionSet(String subject, Date sentDate, String auth, Address[] recipients) throws Exception {
        
        DirectDocuments.SubmissionSet submissionSet = super.getSubmissionSet(subject, sentDate, auth, recipients);
        eliminateEmptyFields(submissionSet);
        return submissionSet;
    }
    
    @Override
    protected DirectDocument2 getEmailBodyDocument(Date sentDate, Part bodyPart) throws Exception {
        DirectDocument2 document = super.getEmailBodyDocument(sentDate, bodyPart);
        eliminateEmptyFields(document);
        return document;
    }
    
    @Override
    protected DirectDocument2 getDocument(Date sentDate, String auth, String xdsMimeType, FormatCodeEnum xdsFormatCode, byte[] xdsDocument, BodyPart bodyPart) throws Exception {
        DirectDocument2 document = super.getDocument(sentDate, auth, xdsMimeType, xdsFormatCode, xdsDocument, bodyPart);
        eliminateEmptyFields(document);
        return document;
    }
    
    protected void eliminateEmptyFields(DirectDocument2 document){
        DirectDocument2.Metadata metadata = document.getMetadata();
        if (StringUtils.isEmpty(metadata.getName())){
            metadata.setName(Utils.NONE);
        }
        if (StringUtils.isEmpty(metadata.getDescription())){
            metadata.setDescription(Utils.NONE);
        }
        
        metadata.setClassCode(Utils.GetMaxMDClassificationCode(), true);
        
        metadata.setSourcePatient(Utils.GetMaxMDSimplePersion());
        
    }
    protected void eliminateEmptyFields(DirectDocuments.SubmissionSet submissionSet){
        if (StringUtils.isEmpty(submissionSet.getDescription())){
            submissionSet.setDescription(Utils.NONE);
        }
        
        if (StringUtils.isEmpty(submissionSet.getName())){
            submissionSet.setName(Utils.NONE);
        }
        
    }

}

