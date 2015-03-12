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
package se.skl.tp.vp.vagvalagent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.AN_HOUR_AGO;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.IN_ONE_HOUR;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.IN_TEN_YEARS;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.TWO_HOURS_AGO;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.createRouting;
import static se.skl.tp.vp.util.VagvalSchemasTestUtil.getRelativeDate;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Before;
import org.junit.Test;

import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.hsa.cache.HsaCacheImpl;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalRequest;
import se.skltp.tak.vagval.wsdl.v2.VisaVagvalResponse;
import se.skltp.tak.vagvalsinfo.wsdl.v2.VirtualiseringsInfoType;

public class VagvalHandlerTest {

	HsaCache hsaCache;

	@Before
	public void beforeTest() throws Exception {

		URL url = getClass().getClassLoader().getResource("hsacache.xml");
		hsaCache = new HsaCacheImpl().init(url.getFile());
	}

	@Test
	public void testMapCreation() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("sender-1", "rivversion-1", "namnrymd-1", "receiver-1"));
		routing.add(createRouting("sender-1", "rivversion-1", "namnrymd-1", "receiver-2"));
		routing.add(createRouting("sender-1", "rivversion-1", "namnrymd-1", "receiver-3", getRelativeDate(TWO_HOURS_AGO), getRelativeDate(AN_HOUR_AGO)));
		routing.add(createRouting("sender-1", "rivversion-1", "namnrymd-1", "receiver-3", getRelativeDate(IN_ONE_HOUR), getRelativeDate(IN_TEN_YEARS)));

		VagvalHandler bh = new VagvalHandler(hsaCache, routing);

		assertEquals(1, bh.lookupInVirtualiseringsInfoMap("receiver-1", "namnrymd-1").size());
		assertEquals(1, bh.lookupInVirtualiseringsInfoMap("receiver-2", "namnrymd-1").size());
		assertEquals(2, bh.lookupInVirtualiseringsInfoMap("receiver-3", "namnrymd-1").size());
		assertNull(bh.lookupInVirtualiseringsInfoMap("receiver-4", "namnrymd-1"));
	}
	
	
	@Test
	public void testNearDates() throws Exception {

		ArrayList<VirtualiseringsInfoType> routing = new ArrayList<VirtualiseringsInfoType>();
		routing.add(createRouting("sender-1", "rivversion-1", "namnrymd-1", "receiver-2", getCalendarDate("2015-01-21T00:00:00.300"), getCalendarDate("2015-01-22T00:00:00.000")));
		routing.add(createRouting("sender-1", "rivversion-1", "namnrymd-1", "receiver-2", getCalendarDate("2015-01-23T00:00:00.300"), getCalendarDate("2015-01-24T00:00:00.000")));

		VagvalHandler bh = new VagvalHandler(hsaCache, routing);

		VisaVagvalRequest request = new VisaVagvalRequest();
		request.setReceiverId("receiver-2");
		request.setSenderId("sender-1");
		request.setTjanstegranssnitt("namnrymd-1");
		request.setTidpunkt(getCalendarDate("2015-01-22T15:00:00.300"));
		
		List<String> receiverAddresses = new ArrayList<String>();
		receiverAddresses.add("receiver-2");
		
		VisaVagvalResponse resp = bh.getRoutingInformationFromLeaf(request, false, receiverAddresses);
		assertEquals(1, resp.getVirtualiseringsInfo().size());		
	}

	private XMLGregorianCalendar getCalendarDate(String dateAsString) throws Exception {
		return DatatypeFactory.newInstance().newXMLGregorianCalendar(dateAsString);
	}
}
