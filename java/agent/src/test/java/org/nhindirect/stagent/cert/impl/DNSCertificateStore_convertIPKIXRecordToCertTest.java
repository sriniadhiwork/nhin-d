package org.nhindirect.stagent.cert.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.security.cert.Certificate;

import org.nhindirect.stagent.cert.CertCacheFactory;
import org.xbill.DNS.CERTRecord;

import junit.framework.TestCase;

public class DNSCertificateStore_convertIPKIXRecordToCertTest extends TestCase
{
	@Override
	public void setUp()
	{
		// flush the caches
		CertCacheFactory.getInstance().flushAll();
	}
	
	public void testConvertIPKIXRecordToCert_validCERTData_assertCertificate() throws Exception
	{
		File file = new File("./src/test/resources/certs/certCheckA.der");
		
		final String url = "file://" + file.getAbsolutePath();
		
		final CERTRecord rec = mock(CERTRecord.class);
		when(rec.getCert()).thenReturn(url.getBytes());
		
		final DNSCertificateStore store = new DNSCertificateStore();
		
		Certificate cert = store.convertIPKIXRecordToCert(rec);
		assertNotNull(cert);
	}
	
	public void testConvertIPKIXRecordToCert_invalidCERTData_assertNoCertificate() throws Exception
	{
		File file = new File("./src/test/resources/log4j.properties");
		
		final String url = "file://" + file.getAbsolutePath();
		
		final CERTRecord rec = mock(CERTRecord.class);
		when(rec.getCert()).thenReturn(url.getBytes());
		
		final DNSCertificateStore store = new DNSCertificateStore();
		
		Certificate cert = store.convertIPKIXRecordToCert(rec);
		assertNull(cert);
	}
	
	public void testConvertIPKIXRecordToCert_invalidURL_assertNoCertificate() throws Exception
	{
		
		final CERTRecord rec = mock(CERTRecord.class);
		when(rec.getCert()).thenReturn("http://localhost:9481/bogus".getBytes());
		
		final DNSCertificateStore store = new DNSCertificateStore();
		
		Certificate cert = store.convertIPKIXRecordToCert(rec);
		assertNull(cert);
	}
}
