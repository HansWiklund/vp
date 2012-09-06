/**
 * Copyright 2009 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.vagvalagent;

import java.math.BigInteger;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoIdType;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v1.HamtaAllaAnropsBehorigheterResponseType;
import se.skl.tp.vagvalsinfo.wsdl.v1.HamtaAllaVirtualiseringarResponseType;
import se.skl.tp.vagvalsinfo.wsdl.v1.SokVagvalsInfoInterface;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoIdType;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.util.XmlGregorianCalendarUtil;
import se.skl.tp.vp.vagvalrouter.VagvalInfo;
import se.skl.tp.vp.vagvalrouter.VagvalInfo.Info;

/**
 * Denna klass används för att kunna simulera en tjänstekatalog med valfritt
 * innehåll. Används ihop med tp-vagval-agent-teststub-config så att det skapas
 * en enpoint som svarar när vägvalsagenten anropar tjänstekatalogen för att få
 * en lista på alla vägval tp-vitualisering-DEV använder sig av denna.
 */

@javax.jws.WebService(portName = "SokVagvalsSoap11LitDocPort", serviceName = "SokVagvalsServiceSoap11LitDocService", targetNamespace = "urn:skl:tp:vagvalsinfo:v1")
public class SokVagvalsInfoTestStub implements SokVagvalsInfoInterface {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private VagvalInfo _vagvalInfo;

	public void setVagvalInfo(VagvalInfo vagvalInfo) {
		this._vagvalInfo = vagvalInfo;
	}

	private VagvalInfo getVagvalInfo() {
		if (_vagvalInfo == null) {
			_vagvalInfo = initVagvalInfo();
		}
		return _vagvalInfo;
	}
	private VagvalInfo initVagvalInfo() {
		logger.info("TK-teststub initiates valvalInfo");
		VagvalInfo vi = new VagvalInfo();
		vi.addVagval("vp-test-producer", "tp", "RIVTABP20", "urn:skl:tjanst1:rivtabp20", "https://localhost:19000/vardgivare-b/tjanst1");
		logger.info("TK-teststub vagvalInfo now contains {} records", vi.getInfos().size());		
		return vi;
	}

	/**
	 * Hämta en lista av alla anropsbehörigheter.
	 * 
	 * @param parameters
	 *            - null, eftersom operationen inte har någon payload.
	 */
	public HamtaAllaAnropsBehorigheterResponseType hamtaAllaAnropsBehorigheter(Object parameters) {

		logger.info("TK-teststub start hamtaAllaAnropsBehorigheter()");
		HamtaAllaAnropsBehorigheterResponseType sampleResponse = new HamtaAllaAnropsBehorigheterResponseType();

		try {
			Duration tenYearsDuration = DatatypeFactory.newInstance().newDurationYearMonth(true,
					new BigInteger("10"), new BigInteger("2"));
			Duration anHourAgo = DatatypeFactory.newInstance().newDuration(false, 0, 0, 0, 1, 0, 0);

			XMLGregorianCalendar fromTidpunkt = XmlGregorianCalendarUtil
					.getNowAsXMLGregorianCalendar();
			fromTidpunkt.add(anHourAgo);

			XMLGregorianCalendar tomTidpunkt = XmlGregorianCalendarUtil
					.getNowAsXMLGregorianCalendar();
			tomTidpunkt.add(tenYearsDuration);

			int id = 1;
			for (VagvalInfo.Info vagval : getVagvalInfo().getInfos()) {
				AnropsBehorighetsInfoIdType aboId = new AnropsBehorighetsInfoIdType();
				aboId.setValue(String.valueOf(id++));
				AnropsBehorighetsInfoType abo = new AnropsBehorighetsInfoType();
				abo.setAnropsBehorighetsInfoId(aboId);
				abo.setFromTidpunkt(fromTidpunkt);
				abo.setTomTidpunkt(tomTidpunkt);
				abo.setReceiverId(vagval.receiver);
				abo.setSenderId(vagval.sender);
				abo.setTjansteKontrakt(vagval.tjansteKontrakt);
				sampleResponse.getAnropsBehorighetsInfo().add(abo);
			}

			logger.info("TK-teststub hamtaAllaAnropsBehorigheter() returns {} records", sampleResponse.getAnropsBehorighetsInfo().size());

		} catch (Exception e) {
			throw new RuntimeException("Technical failure: " + e.getMessage(), e);
		}

		return sampleResponse;
	}

	/**
	 * Hämta en lista av alla virtualiseringar.
	 * 
	 * @param parameters
	 *            - null, eftersom operationen inte har någon payload.
	 */
	public HamtaAllaVirtualiseringarResponseType hamtaAllaVirtualiseringar(Object parameters) {

		logger.info("TK-teststub start hamtaAllaVirtualiseringar()");

		HamtaAllaVirtualiseringarResponseType sampleResponse = new HamtaAllaVirtualiseringarResponseType();

		try {
			XMLGregorianCalendar fromTidpunkt = XmlGregorianCalendarUtil
					.getNowAsXMLGregorianCalendar();
			Duration anHourAgo = DatatypeFactory.newInstance().newDuration(false, 0, 0, 0, 1, 0, 0);
			fromTidpunkt.add(anHourAgo);

			XMLGregorianCalendar tomTidpunkt = XmlGregorianCalendarUtil
					.getNowAsXMLGregorianCalendar();
			Duration tenYearsDuration = DatatypeFactory.newInstance().newDurationYearMonth(true,
					new BigInteger("10"), new BigInteger("2"));
			tomTidpunkt.add(tenYearsDuration);

			int id = 1;
			for (VagvalInfo.Info vagval : getVagvalInfo().getInfos()) {
				VirtualiseringsInfoType vi = new VirtualiseringsInfoType();
				vi.setAdress(vagval.adress);
				vi.setFromTidpunkt(fromTidpunkt);
				vi.setTomTidpunkt(tomTidpunkt);
				vi.setReceiverId(vagval.receiver);
				vi.setRivProfil(vagval.rivVersion);
				VirtualiseringsInfoIdType viId = new VirtualiseringsInfoIdType();
				viId.setValue(String.valueOf(id++));
				vi.setVirtualiseringsInfoId(viId);
				vi.setTjansteKontrakt(vagval.tjansteKontrakt);
				sampleResponse.getVirtualiseringsInfo().add(vi);
			}

		} catch (Exception e) {
			throw new RuntimeException("Technical failure: " + e.getMessage(), e);
		}

		logger.info("TK-teststub hamtaAllaVirtualiseringar() returns {} records", sampleResponse.getVirtualiseringsInfo().size());

		return sampleResponse;
	}

}

