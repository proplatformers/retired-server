/*
This file is part of Open CSTA.

    Open CSTA is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open CSTA is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with Open CSTA.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opencsta.servicedescription.ioservices;

import org.opencsta.server.CSTA_Layer7;
import org.opencsta.server.TDSServer;
import org.opencsta.servicedescription.common.helpers.CSTA_Layer_7_Common;
//import org.opencsta.config.PropertiesController ;
import java.util.Properties;

/**
 * 
 * @author chrismylonas
 */
public class IOServices extends CSTA_Layer_7_Common {

	/**
     *
     *
     */
	private static Properties theProps;

	/**
     *
     *
     */
	private static CSTA_Layer7 layer7;

	/**
     *
     *
     */
	private static int ioCrossRef_a = 0;

	/**
     *
     *
     */
	private static int ioCrossRef_b = 0;

	/**
     *
     *
     */
	private static int ioCrossRef_c = 0;

	/**
     *
     *
     */
	private static int ioCrossRef_d = 0;

	/**
     *
     *
     */
	private static TDSServer TDSserver;

	/**
     *
     *
     */
	private boolean earlyTermination;

	/**
	 * Creates a new instance of IOServices
	 * 
	 * 
	 * @param lyr7
	 */
	@SuppressWarnings("static-access")
	public IOServices(CSTA_Layer7 lyr7) {
		this.layer7 = lyr7;
		TDSserver = new TDSServer();
		theProps = layer7.getTheProps();
		Init();
	}

	/**
     *
     *
     */
	private void Init() {
		try {
			Properties tmp_props = theProps;
			if (tmp_props.getProperty("BEHAVIOUR_APP_ET").toUpperCase()
					.equals("TRUE"))
				earlyTermination = true;
			else
				earlyTermination = false;
		} catch (Exception e) {
			earlyTermination = false;
		}
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	private String IOCrossRefGeneration() {
		String ioCrossRef_id = new String();
		ioCrossRef_a++;

		if (ioCrossRef_a > 255) {
			ioCrossRef_a = 0;
			ioCrossRef_b++;
			if (ioCrossRef_b > 255) {
				ioCrossRef_b = 0;
				ioCrossRef_c++;
				if (ioCrossRef_c > 255) {
					ioCrossRef_c = 0;
					ioCrossRef_d++;
					if (ioCrossRef_d > 255) {
						ioCrossRef_d = 0;
						ioCrossRef_a = 1;
					}
				}
			}
		}

		if (ioCrossRef_d > 0) {
			char[] ioID = { (char) ioCrossRef_d, (char) ioCrossRef_c,
					(char) ioCrossRef_b, (char) ioCrossRef_a };
			ioCrossRef_id = new String(ioID);
			return ioCrossRef_id;
		} else if (ioCrossRef_c > 0) {
			char[] ioID = { (char) ioCrossRef_c, (char) ioCrossRef_b,
					(char) ioCrossRef_a };
			ioCrossRef_id = new String(ioID);
			return ioCrossRef_id;
		} else if (ioCrossRef_b > 0) {
			char[] ioID = { (char) ioCrossRef_b, (char) ioCrossRef_a };
			ioCrossRef_id = new String(ioID);
			return ioCrossRef_id;
		} else if (ioCrossRef_a > 0) {
			char[] ioID = { (char) ioCrossRef_a };
			ioCrossRef_id = new String(ioID);
			return ioCrossRef_id;
		}

		return ioCrossRef_id;
	}

	/**
	 * 
	 * 
	 * 
	 * @param curInStr
	 * @param inv_id
	 */
	public void StartDataPathReceived(StringBuffer curInStr, String inv_id) {
		System.out.println("Start Data Path");

		/* Get rid of the service ID section */
		curInStr = DeleteChars(curInStr, 3);

		/* get the device this is data path is for */
		int length = curInStr.charAt(5);
		String tmp_dev = curInStr.substring(6, (length + 6));
		System.out.println("DEVICE = " + tmp_dev);
		curInStr = DeleteChars(curInStr, (6 + length));

		/*
		 * September 15* Just get the last digit to find out whichTDS code was
		 * pressed
		 */
		length = curInStr.length();
		String TDScode = curInStr.substring((length - 1), length);
		// SEND TO LAYER 7 THE DEV AND TDS CODE FOR QUICK SCROLLING IN BASPAGE
		// PNA NURSE APPLICATION - ONLY IF FLAG IS SET IN IOSERVICESBEHAVIOUR
		// FILE
		if (earlyTermination == true) {
			System.out.println("Early Termination == true ");
			// MUST SEND THE TDS CODE AND DEVICE TO THE LAYER 7 TO PASS ON TO
			// CLIENT VIA SERVER
			layer7.TDSEarlyTerminationData(tmp_dev, TDScode);
		} else
			;

		/* got to return a response to this request */
		StartDataPathResponse(tmp_dev, TDScode, inv_id);
	}

	/**
	 * 
	 * 
	 * 
	 * @param ioXref
	 */
	public void StopDataPath(String ioXref) {
		char[] id_0 = { INTEGER, 0x01, 0x6F };
		String id = new String(id_0);
		StringBuffer strbuf = new StringBuffer();
		strbuf = strbuf.insert(0, ioXref).insert(0, (char) 0x01)
				.insert(0, (char) 0x04);
		int length = strbuf.length();
		strbuf = strbuf.insert(0, (char) length).insert(0, (char) 0xA0);
		strbuf = Sequence(strbuf);
		strbuf = strbuf.insert(0, id);

		length = strbuf.length();
		System.out.println("\n\t\tTESTING STOP DATA PATH: ");
		for (int i = 0; i < length; i++)
			System.out.print(Integer.toHexString(strbuf.charAt(i)) + " ");

		layer7.Wrap2(strbuf);
	}

	/**
	 * 
	 * 
	 * 
	 * @param curInStr
	 * @param inv_id
	 */
	public void StopDataPathReceived(StringBuffer curInStr, String inv_id) {
		System.out.println("Stop Data Path Received");
		curInStr = DeleteChars(curInStr, 5);

		String tmp_ioXref = null;

		if (curInStr.charAt(0) == 0xA1) {
			int length = (int) curInStr.charAt(1);
			length -= 2;
			tmp_ioXref = curInStr.substring(4, (4 + length));
		}
		// TEST.StringContains(tmp_ioXref, "Stop Data on ioCrossRef:") ;
	}

	/**
	 * 
	 * 
	 * 
	 * @param device
	 * @param TDScode
	 * @param inv_id
	 */
	public void StartDataPathResponse(String device, String TDScode,
			String inv_id) {
		// System.out.println("Start Data Path Response") ;
		StringBuffer strbuf = new StringBuffer();
		char[] id_0 = { INTEGER, 0x01, 0x6E };
		String id = new String(id_0);

		String ioXref = IOCrossRefGeneration();
		int length = ioXref.length();
		// System.out.println("Length of the IOCROSSREF: " +
		// Integer.toString(length)) ;
		strbuf = strbuf.insert(0, ioXref).insert(0, (char) length)
				.insert(0, (char) 0x04);
		length += 2;
		strbuf = strbuf.insert(0, (char) length).insert(0, (char) 0xA1);
		strbuf = Sequence(strbuf);
		strbuf = strbuf.insert(0, id);
		strbuf = Sequence(strbuf);
		/*
		 * char[] asdf = {0x30, 0x0a, 0x02, 0x01, 0x6e, 0x30, 0x05, 0xa1, +
		 * 0x03, 0x04, 0x01, 0x01} ; String as = new String(asdf) ; strbuf =
		 * strbuf.insert(0, as) ;
		 */
		// TEST.StringContains(inv_id, "TDS response invoke id" ) ;
		// TEST.StringContains(ioXref, "IO Cross Ref id") ;
		TDSserver.NewTDStransmission(ioXref, device, TDScode);
		length = inv_id.length();
		strbuf = strbuf.insert(0, inv_id).insert(0, (char) length)
				.insert(0, (char) 0x02);
		// System.out.println("TDS: sending response to TDS request") ;
		layer7.Wrap2(strbuf);
		if (earlyTermination)
			;// StopDataPath(ioXref) ;
	}

	/**
	 * 
	 * 
	 * 
	 * @param curInStr
	 * @param invoke_id_ref
	 */
	public void TDSSendDataReceived(StringBuffer curInStr, String invoke_id_ref) {
		// System.out.println("TDS Send Data Received") ;
		String ioXref = null;
		String dataSent = null;

		curInStr = DeleteChars(curInStr, 5);
		StringContains(curInStr, "TDS Just deleted 5 chars");
		if (curInStr.charAt(0) == 0xA1) {
			/*
			 * First is the ioCrossRef then the data, this is thegetting of the
			 * ioCrossRef
			 */
			int length = (int) curInStr.charAt(3);
			StringContains(curInStr, "Testing SendDataRecd");
			ioXref = curInStr.substring(4, (length + 4));
			curInStr = DeleteChars(curInStr, (length + 4));
		}
		try {
			if (curInStr.charAt(0) == 0x04) {
				/* This is the data */
				int length = (int) curInStr.charAt(1);
				dataSent = curInStr.substring(2, (length + 2));
				curInStr = DeleteChars(curInStr, (length + 2));
			}
		} catch (IndexOutOfBoundsException e) {
			dataSent = "";
			e.printStackTrace();
		}
		if (curInStr.length() != 0)
			System.out.println("SendDataReceived, end of data - still more");

		// TEST.StringContains(ioXref,"IO Cross Ref ID") ;
		// TEST.StringContains(dataSent,"Data") ;
		// 09mayTDSserver.SendData(ioXref, dataSent) ;

		// MUST SEND THE DATA THAT WAS 'DATA-ED' TO THE CLIENT, SO THE
		// APPLICATION
		// THAT RECEIVE THIS 'DATA' AND USE IT!
		// NB: IF EARLY TERMINATION SET TO TRUE, DO NOT DO THIS STEP - FUTURE
		// IMPLEMENTATIONS MAY NEED TO CHANGE THIS STEP, BUT FOR PNA BASEPAGE,
		// THIS WILL SUFFICE
		if (earlyTermination == true)
			;
		else
			layer7.DataToTDSServer(ioXref, dataSent);

	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	public StringBuffer escapeService_TDSStart() {
		char[] id_0 = { INTEGER, 0x01, 0x33 };
		String id = new String(id_0);

		char[] tdsstartchararray = { 0xE1, 0x0E, 0x06, 0x06, 0x2B, 0x0C, 0x02,
				0x88, 0x53, 0x0F, 0x04, 0x04, 0x1A, 0x04, 0x01, 0xC0 };
		String tdsstartstr = new String(tdsstartchararray);

		StringBuffer strbuf = new StringBuffer();
		strbuf = strbuf.insert(0, tdsstartstr);
		strbuf = Sequence(strbuf);
		strbuf = strbuf.insert(0, id);
		return strbuf;
	}

}
