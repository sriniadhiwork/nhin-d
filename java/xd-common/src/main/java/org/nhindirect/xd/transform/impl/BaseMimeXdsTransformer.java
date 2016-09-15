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
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nhindirect.xd.transform.util.Utils;
import org.nhindirect.xd.common.DirectDocument2;
import org.nhindirect.xd.common.DirectDocuments;
import org.nhindirect.xd.common.type.FormatCodeEnum;
import org.nhindirect.xd.transform.MimeXdsTransformer;
import org.nhindirect.xd.transform.exception.TransformationException;
import org.nhindirect.xd.transform.util.type.MimeType;

/**
 *
 * @author Yan Wang <ywang@max.md>
 * @date   Oct 08 2014
 */
public abstract class BaseMimeXdsTransformer implements MimeXdsTransformer {
    private static final Log LOGGER = LogFactory.getFactory().getInstance(BaseMimeXdsTransformer.class);
    
    @Override
    abstract public ProvideAndRegisterDocumentSetRequestType transform(MimeMessage mimeMessage) throws TransformationException;
    
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
    protected DirectDocuments.SubmissionSet getSubmissionSet(String subject, Date sentDate, String auth, Address[] recipients) throws Exception {
        DirectDocuments.SubmissionSet submissionSet = new DirectDocuments.SubmissionSet();

        submissionSet.setSubmissionTime(sentDate);
        
        for (Address address : recipients) {
            submissionSet.getIntendedRecipient().add("||^^Internet^" + address.toString());
        }
        
        /**
         * Updated by Yan Wang <ywang@max.md> at 2015-06-02 .
         * Avoid empty name in order to solve Siemens AIS issue.
         */
        if (subject == null || subject.isEmpty()){
            subject = Utils.GetDefaultSubmissionName();
        }
        submissionSet.setName(subject);
        
        /**
         * ContenType Code  urn:uuid:aa543740-bdda-424e-8c96-df4873be8500
         */ 
        submissionSet.setContentTypeCode(Utils.GetMaxMDContentTypeCode(), true);
        
        // (R) Minimal Metadata Source
        if (auth.contains("<")){
            int p = auth.indexOf("<");
            auth = auth.substring(p+1);
            p = auth.indexOf(">");
            if (p > 0){
                auth = auth.substring(0, p);
            }
        }
        /**
         * Author Information urn:uuid:a7058bb9-b4e4-4307-ba5b-e3f0ab85e12d
         */
        submissionSet.setAuthorTelecommunication("^^Internet^" + auth); // TODO: format this correctly
        submissionSet.setUniqueId(UUID.randomUUID().toString());
        
        submissionSet.setSourceId(Utils.GetMaxMDSourceID()); // TODO: "UUID URN mapped by configuration to sending organization"
        submissionSet.setPatientId(Utils.GetMaxMDPatientID());
        
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
    protected DirectDocument2 getDocument(Date sentDate, String auth, String xdsMimeType, FormatCodeEnum xdsFormatCode, byte[] xdsDocument, BodyPart bodyPart) throws Exception {
        DirectDocument2 document = new DirectDocument2();
        DirectDocument2.Metadata metadata = document.getMetadata();
        
        metadata.setCreationTime(sentDate);
        
        // (R) Minimal Metadata Source
        metadata.setMimeType(xdsMimeType);
        metadata.setLanguageCode("en-us");
        metadata.setSourcePatient(Utils.GetMaxMDSimplePersion());
        
        /**
         * data, hash, size 
         */
        document.setData(xdsDocument);
        
        /**
         * DOC_CLASS_CODE("c102", "urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a", "classCode"),
         */
        metadata.setClassCode(Utils.GetMaxMDClassificationCode());
        
        /**
         * DOC_FORMAT_CODE("c104", "urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d", "Connect-a-thon confidentialityCodes"),        
         */
        if (xdsFormatCode != null) {
            metadata.setFormatCode(xdsFormatCode);
        } else{
            metadata.setFormatCode("NONE");
        }
        
        /**
         * DOC_CONFIDENTIALITY_CODE("c103", "urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f", "Connect-a-thon confidentialityCodes"),
         */
        metadata.setConfidentialityCode("NONE", true);
        
        /**
         * DOC_HEALTHCARE_FACILITY_TYPE_CODE("c105", "urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1", "Connect-a-thon healthcareFacilityTypeCodes"),         
         */
        metadata.setHealthcareFacilityTypeCode("NONE", true);
        
        /**
         * DOC_PRACTICE_SETTING_CODE("c106", "urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead", "Connect-a-thon practiceSettingCodes"),    
         */
        metadata.setPracticeSettingCode("NONE", true);
        
        /**
         * DOC_LOINC("c107", "urn:uuid:f0306f51-975f-434e-a61c-c59651d33983", "LOINC"),
         */
        metadata.setLoinc("NONE", true);
        
        /**
         * DOC_AUTHOR("c101", "urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d", null), 
         */
        metadata.setAuthorPerson("NONE");
        
        /**
         * DOC_UNIQUE_ID("eid02", "urn:uuid:2e82c1f6-a085-4c72-9da3-8640a32e42ab", "XDSDocumentEntry.uniqueId"),
         */
        metadata.setUniqueId(Utils.GetDocUniqueId());
        
        /**
         * DOC_PATIENT_ID("eid01", "urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427", "XDSDocumentEntry.patientId"), 
         */
        metadata.setPatientId(Utils.GetMaxMDPatientID());

        
        /**
         * Updated by Yan Wang <ywang@max.md> at 2015-06-02 .
         * Read Name and Description from BodyPart Header in order to solve Siemens AIS issue.
         */
        String documentName =  bodyPart.getFileName();
        if (documentName == null || documentName.isEmpty()){
            documentName = Utils.GetDefaultDocName();
        }
        metadata.setName(documentName);
        metadata.setDescription(bodyPart.getDescription());
        return document;
    }
    
    protected DirectDocument2 getEmailBodyDocument(Date sentDate, Part bodyPart) throws Exception {
        
        DirectDocument2 document = new DirectDocument2();
        DirectDocument2.Metadata metadata = document.getMetadata();
        
        LOGGER.debug("getEmailBodyDocument Content-Type: " + bodyPart.getContentType());
        Part emailBody = null;
        if (MimeType.TEXT_PLAIN.matches(bodyPart.getContentType())){
            LOGGER.debug("Text Email Body Found" );
            emailBody = bodyPart;
        } else if (MimeType.MULTIPART_ALTERNATIVE.matches(bodyPart.getContentType())){
            MimeMultipart multipart = (MimeMultipart) bodyPart.getContent();
            for (int i=0; i<multipart.getCount(); i++){
                if (MimeType.TEXT_PLAIN.matches(multipart.getBodyPart(i).getContentType())){
                    emailBody = multipart.getBodyPart(i);
                    LOGGER.debug("Text Email Body Found in multipart/alternative" );
                }
            }
        }
        
        if (emailBody == null){
            return null;
        }
        /**
         * DOC_CLASS_CODE("c102", "urn:uuid:41a5887f-8865-4c09-adf7-e362475b143a", "classCode"),
         */
        metadata.setClassCode(Utils.GetEmailBodyClassCode());
        metadata.setClassCode_localized(Utils.GetEmailBodyClassCodeLocalized());
        
        /**
         * DOC_LOINC("c107", "urn:uuid:f0306f51-975f-434e-a61c-c59651d33983", "LOINC"),
         */
        metadata.setLoinc(Utils.GetEmailBodyClassCode());
        metadata.setLoinc_localized(Utils.GetEmailBodyClassCodeLocalized());
        
        metadata.setMimeType(MimeType.TEXT_PLAIN.getType());
        metadata.setUniqueId(Utils.GetDocUniqueId());

        metadata.setCreationTime(sentDate);
        metadata.setLanguageCode("en-us");
        document.setData(read(emailBody));
        
        
        
        metadata.setSourcePatient(Utils.GetMaxMDSimplePersion());
        
        /**
         * Updated by Yan Wang <ywang@max.md> at 2015-06-02 .
         * Read Name and Description from BodyPart Header in order to solve Siemens AIS issue.
         */
        String documentName =  bodyPart.getFileName();
        if (documentName == null || documentName.isEmpty()){
            documentName = Utils.GetDefaultTextBodyName();
        }
        metadata.setName(documentName);
        metadata.setDescription(bodyPart.getDescription());
        
//        /**
//         * DOC_FORMAT_CODE("c104", "urn:uuid:a09d5840-386c-46f2-b5ad-9c3699a4309d", "Connect-a-thon confidentialityCodes"),        
//         */
//        metadata.setFormatCode("NONE");
//        
//        /**
//         * DOC_CONFIDENTIALITY_CODE("c103", "urn:uuid:f4f85eac-e6cb-4883-b524-f2705394840f", "Connect-a-thon confidentialityCodes"),
//         */
//        metadata.setConfidentialityCode("NONE", true);
//        
//        /**
//         * DOC_HEALTHCARE_FACILITY_TYPE_CODE("c105", "urn:uuid:f33fb8ac-18af-42cc-ae0e-ed0b0bdb91e1", "Connect-a-thon healthcareFacilityTypeCodes"),         
//         */
//        metadata.setHealthcareFacilityTypeCode("NONE", true);
//        
//        /**
//         * DOC_PRACTICE_SETTING_CODE("c106", "urn:uuid:cccf5598-8b07-4b77-a05e-ae952c785ead", "Connect-a-thon practiceSettingCodes"),    
//         */
//        metadata.setPracticeSettingCode("NONE", true);
//        
//        
//        /**
//         * DOC_AUTHOR("c101", "urn:uuid:93606bcf-9494-43ec-9b4e-a7748d1a838d", null), 
//         */
//        metadata.setAuthorPerson("NONE");
        
        /**
         * DOC_PATIENT_ID("eid01", "urn:uuid:58a6f841-87b3-4a3e-92fd-a8ffeff98427", "XDSDocumentEntry.patientId"), 
         */
        metadata.setPatientId(Utils.GetMaxMDPatientID());

        return document;
    }

    protected static byte[] read(BodyPart bodyPart) throws MessagingException, IOException {
        InputStream inputStream = bodyPart.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int data = 0;
        byte[] buffer = new byte[1024];
        while ((data = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, data);
        }

        return outputStream.toByteArray();
    }
    protected static byte[] read(Part bodyPart) throws MessagingException, IOException {
        InputStream inputStream = bodyPart.getInputStream();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int data = 0;
        byte[] buffer = new byte[1024];
        while ((data = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, data);
        }

        return outputStream.toByteArray();
    }
    
    protected static String getEncodedString(byte[] data){
        return Base64.encodeBase64String(data);
    }
}
