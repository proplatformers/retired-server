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

package org.opencsta.server;

import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import org.opencsta.net.ServeOneClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author chrismylonas
 */
public class TDSServer {

	/**
     * 
     */
	protected static Logger alog = LoggerFactory.getLogger(CSTAServer.class);

	/**
     *
     *
     */
	private static CSTAServer server;

	/**
     *
     *
     */
	private static CSTA_Layer7 layer7;

	/**
     *
     *
     */
	private static Hashtable ioCrossRef2TDSObject;

	/**
     *
     *
     */
	private static List TDSclients;

	/**
	 * Creates a new instance of TDSServer
	 * 
	 * 
	 * @param layer7
	 * @param server
	 */
	public TDSServer(CSTA_Layer7 layer7, CSTAServer server) {
		this.server = server;
		this.layer7 = layer7;
		ioCrossRef2TDSObject = new Hashtable();
		TDSclients = Collections.synchronizedList(new LinkedList());
	}

	/**
     *
     *
     */
	public TDSServer() {
		ioCrossRef2TDSObject = new Hashtable();
		TDSclients = Collections.synchronizedList(new LinkedList());
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param client
	 */
	public boolean AddClient(ServeOneClient client) {
		return TDSclients.add(client);
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param client
	 */
	public boolean RemoveClient(ServeOneClient client) {
		return TDSclients.remove(client);
	}

	/**
	 * 
	 * 
	 * 
	 * @param ioXref
	 * @param device
	 * @param TDScode
	 */
	public void NewTDStransmission(String ioXref, String device, String TDScode) {
		alog.info("TDSServer creating a TDS Object for device: " + device
				+ " code: " + TDScode);
		TDSObject tdso = new TDSObject(device, TDScode);
		synchronized (ioCrossRef2TDSObject) {
			ioCrossRef2TDSObject.put(ioXref, tdso);
		}
	}

	/**
	 * 
	 * 
	 * 
	 * @param ioCrossRef
	 * @param data
	 */
	public void SendData(String ioCrossRef, String data) {
		// IN THIS METHOD 0x99 IS USED TO INDICATE THAT IT IS A TDS MESSAGE

		TDSObject tdso;

		synchronized (ioCrossRef2TDSObject) {
			tdso = (TDSObject) ioCrossRef2TDSObject.remove(ioCrossRef);
		}

		StringBuffer str = tdso.toString(data);
		int length = str.length();
		str = str.insert(0, (char) length).insert(0, (char) 0x99);
		server.TDSToClient(str, TDSclients);
	}

	/**
	 * this method is used for early termination when ioservices are used for
	 * basepage
	 * 
	 * 
	 * @param dev
	 * @param code
	 */
	public void TDSEarlyTerminationDataToClients(String dev, String code) {
		// IN THIS METHOD 0x98 IS USED TO INDICATE THAT IT IS AN EARLY TERM. TDS
		// MESSAGE
		TDSObject tdso = new TDSObject(dev, code);
		StringBuffer str = tdso.toString("");
		int length = str.length();
		str = str.insert(0, (char) length).insert(0, (char) 0x98);
		alog.info("TDS Early Termination is set, and sending this data to clients");
		server.TDSToClient(str, TDSclients);
	}

	/**
	 * 
	 * 
	 * 
	 * @param l7
	 */
	public void ReintroduceLayer7(CSTA_Layer7 l7) {
		this.layer7 = l7;
	}

}

/**
 * 
 * @author chrismylonas
 */
class TDSObject {

	/**
     *
     *
     */
	String device;

	/**
     *
     *
     */
	String TDScode;

	/**
	 * 
	 * 
	 * 
	 * @param dev
	 * @param code
	 */
	public TDSObject(String dev, String code) {
		this.device = dev;
		this.TDScode = code;

	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param data
	 */
	public StringBuffer toString(String data) {
		StringBuffer strbuf = new StringBuffer(device);
		int length = strbuf.length();
		strbuf = strbuf.insert(0, (char) length).insert(0, (char) 0x80);
		strbuf = strbuf.append((char) 0x81).append((char) 0x01).append(TDScode);
		length = data.length();
		strbuf = strbuf.append((char) 0x82).append((char) length).append(data);
		return strbuf;
	}
}
