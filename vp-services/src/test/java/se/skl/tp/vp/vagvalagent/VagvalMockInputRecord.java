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

import javax.xml.datatype.XMLGregorianCalendar;
import se.skl.tp.vp.vagvalrouter.VagvalInput;

public class VagvalMockInputRecord extends VagvalInput {

	public String adress;
	public boolean addVagval=true;
	public boolean addBehorighet=true;
	private XMLGregorianCalendar fromDate;
	private XMLGregorianCalendar toDate;

	public XMLGregorianCalendar getFromDate() {
		return fromDate;
	}

	public void setFromDate(XMLGregorianCalendar fromDate) {
		this.fromDate = fromDate;
	}

	public XMLGregorianCalendar getToDate() {
		return toDate;
	}

	public void setToDate(XMLGregorianCalendar toDate) {
		this.toDate = toDate;
	}

	@Override
	public String toString() {
		return super.toString() + " " + adress;
	}

}
