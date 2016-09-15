package org.nhindirect.xd.transform.util;

import java.util.Date;
import java.util.UUID;
import org.nhindirect.xd.transform.pojo.SimplePerson;

/**
 * Generate MaxMD XDR Meta parameters when missing in the original metadata
 * @date Sep 21 2014
 * @author Yan Wang <ywang@max.md>
 */
public class Utils {

    private static SimplePerson MAXMDSimplePerson = null;
    private static final String MaxMD_LOCAL_ID = "LocalOrg";
    private static final String MaxMD_LOCAL_ORG = "2.16.840.1.123.3.123";
    private static final String MaxMD_FIRSTNAME = "John";
    private static final String MaxMD_LASTNAME = "Smith";
    private static final String MaxMD_Street = "Street";
    private static final String MaxMD_CITY = "City";
    private static final String MaxMD_STATE = "NEW JERSEY";
    private static final String MaxMD_COUNTRY = "US";
    private static final String MaxMD_ZIPCODE = "07123";
    private static final String MaxMD_SOURCEID = "1";
    private static final String MaxMD_CONTENTTYPE_CODE = "XDR";
    private static final String MaxMD_CLASSIFICATION_CODE = "XDR";
    private static final String MaxMD_CONFIDENTIAL_CODE = "XDR";
    private static final String EMAIL_BODY_CLASS_CODE = "56444-3";
    private static final String EMAIL_BODY_CLASS_CODE_LOCALIZED = "Healthcare Communication";
    private static final String LOINC_OID = "2.16.840.1.123.3.123";
    private static final String XD_SMTP_HEADER = "X-XdOriginated";
    private static final String DEFAULT_DOCUMENT_NAME = "XDR Document";
    private static final String DEFAULT_MESSAGEBODY_NAME = "TextBody.txt";
    private static final String DEFAULT_SUBMISSION_NAME = "XDR Message";
    public static final String NONE = "NONE";
    private static final String PATIENT_ID = "PID-3|LocalOrg^^^&amp;2.16.840.1.123.3.123&amp;ISO";
    private static long docIndex = 0;

    public static SimplePerson GetMaxMDSimplePersion() {
        if (MAXMDSimplePerson == null) {
            MAXMDSimplePerson = new SimplePerson();
            MAXMDSimplePerson.setLocalId(MaxMD_LOCAL_ID);
            MAXMDSimplePerson.setLocalOrg(MaxMD_LOCAL_ORG);
            MAXMDSimplePerson.setFirstName(MaxMD_FIRSTNAME);
            MAXMDSimplePerson.setLastName(MaxMD_LASTNAME);
            MAXMDSimplePerson.setStreetAddress1(MaxMD_Street);
            MAXMDSimplePerson.setCity(MaxMD_CITY);
            MAXMDSimplePerson.setState(MaxMD_STATE);
            MAXMDSimplePerson.setCountry(MaxMD_COUNTRY);
            MAXMDSimplePerson.setZipCode(MaxMD_ZIPCODE);
            MAXMDSimplePerson.setGenderCode("F");
            MAXMDSimplePerson.setBirthDateTime("19850808");
        }
        return MAXMDSimplePerson;
    }
    
    public static String GetMaxMDOID(){
        return MaxMD_LOCAL_ORG;
    }
    
    public static String GetMaxMDSourceID(){
        return MaxMD_LOCAL_ORG + "." + MaxMD_SOURCEID;
    }
    public static String GetMaxMDPatientID(){
    //	 return GetMaxMDSimplePersion().getSourcePatientId();
    	return PATIENT_ID;
    }
    
    public static String GetMaxMDContentTypeCode(){
        return MaxMD_CONTENTTYPE_CODE;
    }
    
    public static String GetMaxMDClassificationCode(){
        return MaxMD_CLASSIFICATION_CODE;
    }
    public static String GetMaxMDConfidentialCode(){
        return MaxMD_CONFIDENTIAL_CODE;
    }
    
    public static String GetEmailBodyClassCode(){
        return EMAIL_BODY_CLASS_CODE;
    }
    
    public static String GetEmailBodyClassCodeLocalized(){
        return EMAIL_BODY_CLASS_CODE_LOCALIZED;
    }
    
    public static synchronized String GetDocUniqueId(){
        return MaxMD_LOCAL_ORG +"." + (new Date().getTime()) + "." + (++docIndex);
    }
    
    public static String GetXdSmtpHeader(){
        return XD_SMTP_HEADER;
    }
    
    public static String GetDefaultDocName(){
        return DEFAULT_DOCUMENT_NAME;
    }
    
    public static String GetDefaultSubmissionName(){
        return DEFAULT_SUBMISSION_NAME;
    }
    public static String GetDefaultTextBodyName(){
        return DEFAULT_MESSAGEBODY_NAME;
    }

}
