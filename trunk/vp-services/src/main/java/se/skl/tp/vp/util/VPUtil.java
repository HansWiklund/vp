/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.util;

import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.helper.cert.CertificateExtractor;
import se.skl.tp.vp.util.helper.cert.CertificateExtractorFactory;
import se.skl.tp.vp.vagvalrouter.VagvalRouter;

/**
 * Utility class for the virtualization platform
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public final class VPUtil {
	
	private static final Logger log = LoggerFactory.getLogger(VPUtil.class);

	public static final String REMOTE_ADDR = MuleProperties.MULE_REMOTE_CLIENT_ADDRESS;
	
	public static final String CONSUMER_CONNECTOR_HTTPS_NAME = "VPConsumerConnector";
	public static final String CONSUMER_CONNECTOR_HTTPS_KEEPALIVE_NAME = "VPConsumerConnectorKeepAlive";
	public static final String CONSUMER_CONNECTOR_HTTP_NAME = "VPInsecureConnector";
	
	public static final String PEER_CERTIFICATES = "PEER_CERTIFICATES";
	public static final String REVERSE_PROXY_HEADER_NAME = "x-vp-auth-cert";
	
	public static final String SESSION_ERROR = "sessionStatus";
	public static final String SESSION_ERROR_DESCRIPTION = "sessionErrorDescription";
	public static final String SESSION_ERROR_TECHNICAL_DESCRIPTION = "sessionErrorTechnicalDescription";
	
	public static final String RECEIVER_ID = "receiverid";
	public static final String SENDER_ID = "senderid";
	public static final String RIV_VERSION = "rivversion";
	public static final String SERVICE_NAMESPACE = "cxf_service";
	public static final String ENDPOINT_URL = "endpoint_url";
	
	public static final String IS_HTTPS = "isHttps";
	
	public static final String CERT_SENDERID_PATTERN = "=([^,]+)";
	
	public static final String TIMER_TOTAL = "total";
	public static final String TIMER_ROUTE = "route";
	public static final String TIMER_ENDPOINT = "endpoint_time";
	
	/**
	 * Feature properties
	 */
	public static final String FEATURE_USE_KEEP_ALIVE = "featureUseKeepAlive";
	public static final String FEATURE_RESPONSE_TIMOEUT = "featureResponseTimeout";
		
	public static String extractNamespaceFromService(final QName qname) {
		return (qname == null) ? null : qname.getNamespaceURI();
	}
	
	public static String extractIpAddress(final String remoteAddress) {
		final String s = remoteAddress.split(":")[0];
		return s.substring(1, s.length());
	}
	
	//
	public static String nvl(String s) {
		return (s == null) ? "" : s;
	}

	public static boolean isWhitespace(final String s) {
		if (s == null) {
			return true;
		}
		
		return s.trim().length() == 0;
	}
	
	public static String getSenderId(MuleMessage message, String whiteList, Pattern certExtractPattern) {
		
		String senderId = null;

		// Start to look into the inbound property VagvalRouter.X_VP_CONSUMER_ID, will be filled in if an aggregating service use the VP.
		log.debug("Exists sender id as inbound property {}?", VagvalRouter.X_VP_CONSUMER_ID);
		senderId = message.getInboundProperty(VagvalRouter.X_VP_CONSUMER_ID, null);

		if (senderId != null) {
			log.debug("Sender id extracted from inbound property {}: {}", VagvalRouter.X_VP_CONSUMER_ID, senderId);

			// Check whitelist (throws an exception if not ok) if found in the inbound property VagvalRouter.X_VP_CONSUMER_ID.
			VPUtil.checkCallerOnWhiteList(message, whiteList, VagvalRouter.X_VP_CONSUMER_ID);

		} else {
			// Ok, no inbound property X_VP_CONSUMER_ID was found, look into the certificate instead
			try {
				log.debug("No, look into the senders certificate instead");
				CertificateExtractorFactory certificateExtractorFactory = new CertificateExtractorFactory(message, certExtractPattern, whiteList);
				CertificateExtractor certHelper = certificateExtractorFactory.creaetCertificateExtractor();
				senderId = certHelper.extractSenderIdFromCertificate();
				log.debug("Sender id extracted from certificate {}", senderId);
				
			} catch (final VpSemanticException e) {
				log.warn("Could not extract sender id from certificate. Reason: {} ", e.getMessage());
			} 	
		}
		
		// Cache the retrieved value in the session property SENDER_ID
		if (senderId != null) {
			message.setProperty(VPUtil.SENDER_ID, senderId, PropertyScope.SESSION);
		}
		return senderId;
	}	
	
	public static void checkCallerOnWhiteList(MuleMessage message, String whiteList, String httpHeader) throws VpSemanticException {

		String ip = VPUtil.extractIpAddress((String) message.getProperty(VPUtil.REMOTE_ADDR, PropertyScope.INBOUND));
		ip = ip.trim();
		
		log.debug("Check if caller {} is in white list berfore using HTTP header {}...", ip, httpHeader);

		if (VPUtil.isWhitespace(ip)) {
			throw new VpSemanticException(
				"Could not extract the IP address of the caller. Cannot check whether caller is on the white list. HTTP header that caused checking: " + httpHeader);
		}

		if (VPUtil.isWhitespace(whiteList)) {
			throw new VpSemanticException(
				"Could not check whether the caller is on the white list because the white list was empty. HTTP header that caused checking: " + httpHeader);
		}

		final String[] ips = whiteList.split(",");

		for (final String s : ips) {
			if (s.trim().equals(ip)) {
				log.debug("Caller found in white list");
				return;
			}
		}

		log.debug("Caller NOT found in white list");
		throw new VpSemanticException("Caller was not on the white list of accepted IP-addresses. IP-address: " + ip + ". HTTP header that caused checking: " + httpHeader);
	}
	
}
