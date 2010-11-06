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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import org.opencsta.link.CSTA_Link_Siemens_Hipath3000_Serial;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import java.util.Properties;
import org.apache.log4j.Logger;
//import org.opencsta.config.PropertiesController;
import org.opencsta.link.CSTA_Link;
import org.opencsta.link.CSTA_Link_Siemens_Hipath3000_Network;
import org.opencsta.link.CSTA_Link_Siemens_Hipath3000_Serial;
import org.opencsta.servicedescription.common.helpers.CSTA_Layer_Interface;

/**
 * @author chrismylonas
 * 
 */
public class CSTA_Layer5 implements CSTA_Layer_Interface, Runnable {

	/**
	 * 
	 */
	protected static Logger alog = Logger.getLogger(CSTAServer.class);

	/**
	 * 
	 */
	private static Properties theProps;

	/**
	 * 
	 */
	private boolean loginACSERequestSent = false;

	/**
	 * 
	 */
	private boolean loginACSEResponseReceived = false;

	/**
	 * 
	 */
	private boolean loginFirstROSEInvokeReceived = false;

	/**
	 * 
	 */
	private boolean loginFirstROSEResponseSent = false;

	/**
     * 
     */
	private boolean loggedIn = false;

	/**
     * 
     */
	private static CSTA_Layer7 layer7;

	/**
     * 
     */
	StringBuffer notCompleteMsg;

	/**
     * 
     */
	private List<StringBuffer> workListUpward;

	/**
     * 
     */
	private List<StringBuffer> workListDownward;

	/**
     * 
     */
	private boolean alive = false;

	/**
     * 
     */
	private CSTA_Link link;

	// @todo fix up this link thread bizo - chuck it into csta link implm.
	// class.

	/**
     * 
     */
	private boolean CSTALinkUp;

	/**
	 * This main method is used for doing only layer 5 testing, usually during
	 * initial development on a new switch, to work out how to log onto it.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		FileInputStream is = null;
		Properties tp = null;
		try {
			is = new FileInputStream(
					(System.getProperty("user.dir") + "/cstaserver.conf"));
			tp = new Properties();
			tp.load(is);
		} catch (FileNotFoundException ex) {
			java.util.logging.Logger.getLogger(CSTAServer.class.getName()).log(
					java.util.logging.Level.SEVERE, null, ex);
		} catch (IOException ex) {
			java.util.logging.Logger.getLogger(CSTAServer.class.getName()).log(
					java.util.logging.Level.SEVERE, null, ex);
		}
		CSTA_Layer5 l5 = new CSTA_Layer5(tp);
		l5.Login();
		l5.run();
	}

	/**
	 * This constructor is used with the main method, and is to be used only for
	 * initial development/integration with a new switch.
	 * 
	 * @param _props
	 */
	public CSTA_Layer5(Properties _props) {
		alog.info(this.getClass().getName() + " initialisation");
		alog.info("################# This layer is the highest layer!!!");
		theProps = _props;
		this.setLoginACSERequestSent(false);
		this.setLoginACSEResponseReceived(false);
		this.setLoginFirstROSEInvokeReceived(false);
		this.setLoginFirstROSEResponseSent(false);
		notCompleteMsg = null;
		this.layer7 = null;
		alog.info("Layer 5 saved null layer 7, this must be a test");
		workListUpward = Collections
				.synchronizedList(new LinkedList<StringBuffer>());
		workListDownward = Collections
				.synchronizedList(new LinkedList<StringBuffer>());
		alog.info("Worklists --- starting CSTA_Link implementation");
		link = new CSTA_Link_Siemens_Hipath3000_Serial(this);
		// link = new CSTA_Link_Siemens_Hipath3000_Network(this) ;
		alog.info("CSTA Link Implementation initialised");
	}

	/**
	 * 
	 * Standard constructor when used with the whole CSTA application.
	 * 
	 * @param newLayer7
	 * @param _props
	 * @throws NoCSTAImplementationException
	 */
	public CSTA_Layer5(CSTA_Layer7 newLayer7, Properties _props)
			throws NoCSTAImplementationException {
		alog.info(this.getClass().getName() + " initialising");
		theProps = _props;
		this.setLoginACSERequestSent(false);
		this.setLoginACSEResponseReceived(false);
		this.setLoginFirstROSEInvokeReceived(false);
		this.setLoginFirstROSEResponseSent(false);
		notCompleteMsg = null;
		this.layer7 = newLayer7;
		workListUpward = Collections
				.synchronizedList(new LinkedList<StringBuffer>());
		workListDownward = Collections
				.synchronizedList(new LinkedList<StringBuffer>());
		alog.info("Worklists --- starting CSTA_Link implementation");
		if (theProps.getProperty("CSTASERVER_PBX_VENDOR").equalsIgnoreCase(
				"SIEMENS")) {
			alog.info("PBX Vendor: Siemens");
			if (theProps.getProperty("CSTASERVER_PBX_MODEL").equalsIgnoreCase(
					"HIPATH3000")) {
				alog.info("PBX Model: Hipath 3000");
				if (theProps.getProperty("CSTASERVER_PBX_IMPLEMENTATION")
						.equalsIgnoreCase("SERIAL")) {
					alog.info("PBX CSTA Implementation: Serial");
					link = new CSTA_Link_Siemens_Hipath3000_Serial(this);
				} else if (theProps
						.getProperty("CSTASERVER_PBX_IMPLEMENTATION")
						.equalsIgnoreCase("NETWORK")) {
					alog.info("PBX CSTA Implementation: Network");
					link = new CSTA_Link_Siemens_Hipath3000_Network(this);
				} else {
					alog.info("PBX CSTA Implementation: None - throw an exception - fix your config file -> CSTASERVER_PBX_IMPLEMENTATION to either serial or network");
					throw new NoCSTAImplementationException();
				}
			}
		} else {
			alog.info("NOT A SIEMENS");
		}
		alog.info("CSTA Link Implementation initialised");
	}

	/**
	 * 
	 */
	public void CloseCSTAStack() {
		if (link.isLinkUp()) {
			alog.info("CSTA Link is up");
		} else {
			alog.info("CSTA Link is down");
		}
		link.setLinkUp(false);
		if (link.isLinkUp()) {
			alog.info("CSTA Link is up");
		} else {
			alog.info("CSTA Link is down");
		}
		link.killThreads();
		try {
			synchronized (this) {
				wait(8000);
			}
		} catch (InterruptedException e) {

		} catch (NullPointerException e2) {
			e2.printStackTrace();
		}
		link = null;
		this.Layer5KeepAlive(false);
	}

	// Layer 5 has it's own thread for processing jobs/messages in both
	// direction
	// from the switch to the CSTA Server and from CSTA Server to the switch.
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		Layer5KeepAlive(true);
		StringBuffer sb_down;
		StringBuffer sb_up;
		while (alive) {
			try {
				synchronized (this) {
					wait(5000);
				}
			} catch (InterruptedException e) {

			} catch (NullPointerException e2) {
				e2.printStackTrace();
			}
			while (workListDownward.size() > 0) {
				sb_down = getLayer5JobOut();
				sb_down = Wrap(sb_down);
				link.sendCommand(sb_down);
			}
		}
	}

	/**
	 * Login to the switch
	 */
	public void Login() {
		if (startCSTALink()) {
			this.setCSTALinkUp(true);
			sendLoginACSERequest();
		} else
			;// login failed

		link.threaden();
	}

	/**
	 * First step in the login process.
	 */
	private void sendLoginACSERequest() {
		alog.info("CSTA Link is active - sending ACSE Request");
		String CSTA_username = layer7.getCSTA_username();
		String CSTA_password = layer7.getCSTA_password();

		// THIS WAS THE ORIGINAL HARDCODED STUFF FOR THE SIEMENS HIPATH 3000.
		// THE FOLLOWING CODE REPLACES IT FOR AN ASTERISK PORT
		// char[] aCSEaarq = {0x60, 0x31, 0xa1, 0x07, 0x06, 0x05, 0x2b, 0x0c,
		// 0x00, 0x81, 0x5a, 0x8a, 0x02, 0x06, 0x80, 0xac, 0x15, 0xa2,
		// + 0x13, 0xa0, 0x11, 0xa0, 0x0f, 0x04, 0x06, 0x41, 0x4d, 0x48, 0x4f,
		// 0x53, 0x54, 0x04, 0x05, 0x37, 0x37, 0x37, 0x37, 0x37,
		// + 0xbe, 0x0b, 0x28, 0x09, 0xa0, 0x07, 0xa0, 0x05, 0x03, 0x03, 0x00,
		// 0x10, 0x00} ;

		StringBuffer aCSEaarq = new StringBuffer();
		StringBuffer upsb = new StringBuffer();

		char[] lastparts = { 0xbe, 0x0b, 0x28, 0x09, 0xa0, 0x07, 0xa0, 0x05,
				0x03, 0x03, 0x00, 0x10, 0x00 };

		int ulength = CSTA_username.length();
		char[] uname = { 0x04, (char) ulength };
		upsb.append(new String(uname));
		for (int i = 0; i < CSTA_username.length(); i++) {
			upsb.append(CSTA_username.charAt(i));
		}
		int plength = CSTA_password.length();
		char[] pword = { 0x04, (char) plength };
		upsb.append(new String(pword));
		for (int i = 0; i < CSTA_password.length(); i++) {
			upsb.append(CSTA_password.charAt(i));
		}

		int uplength = upsb.length();
		char[] upcombo = { 0xa0, (char) uplength };
		upsb.insert(0, upcombo);

		int uplength2 = upsb.length();
		char[] upcombo2 = { 0xa0, (char) uplength2 };
		upsb.insert(0, upcombo2);

		int uplength3 = upsb.length();
		char[] upcombo3 = { 0xa2, (char) uplength3 };
		upsb.insert(0, upcombo3);

		int uplength4 = upsb.length();
		char[] upcombo4 = { 0xac, (char) uplength4 };
		upsb.insert(0, upcombo4);

		char[] firstparts = { 0xa1, 0x07, 0x06, 0x05, 0x2b, 0x0c, 0x00, 0x81,
				0x5a, 0x8a, 0x02, 0x06, 0x80 };
		aCSEaarq.append(new String(firstparts));
		aCSEaarq.append(upsb);
		aCSEaarq.append(new String(lastparts));

		int sblength = aCSEaarq.length();
		char[] header = { 0x60, (char) sblength };
		aCSEaarq.insert(0, header);

		String str_ACSE_req = new String(aCSEaarq);
		this.setLoginACSERequestSent(true);
		str_ACSE_req = Wrap(str_ACSE_req);
		// add commands to the work list and let the link determine the speed
		// of sending them through - for instance, on a serial connection, there
		// needs to be data-link layer checking first rather than just sending
		// all sorts of commands over the network (which is faster).
		// addWorkOUT(new StringBuffer(str_ACSE_req)) ;
		this.link.sendCommand(new StringBuffer(str_ACSE_req));
	}

	// Delete number of bytes from the beginning of the string.
	/**
	 * @param curInStr
	 * @param bytes
	 * @return
	 */
	private StringBuffer DeleteChars(StringBuffer curInStr, int bytes) {
		for (int i = 0; i < bytes; i++)
			curInStr = curInStr.deleteCharAt(0);
		return curInStr;
	}

	// Initial check of what we have received from TCP connection.
	/**
	 * @param curInStr
	 * @return
	 */
	public boolean PassedUp(StringBuffer curInStr) {
		String tmplogStr = "";
		for (int i = 0; i < curInStr.length(); i++) {
			tmplogStr += Integer.toHexString(curInStr.charAt(i)) + " ";
		}

		alog.info(this.getClass().getName() + " ---> " + " Entered Layer 5 R: "
				+ tmplogStr);

		if (curInStr.length() == 0)
			;
		if (curInStr.charAt(0) == 0x26 && curInStr.charAt(1) == 0x80)
			curInStr = Strip(curInStr);

		if (isLoggedIn()) {
			// strip 3 digits, pass up to layer 7
			// curInStr = Strip(curInStr) ;
			layer7.FromBelow(curInStr);
		} else {

			alog.info("\nQ1) We know we are not logged in, now what?");
			if (isLoginACSERequestSent()) {
				// sent the first login string, have we received a response
				alog.info("\nQ2) We know we have sent the ACSE Request, now what?");
				if (isLoginACSEResponseReceived()) {
					// we've received the response, have we received the follow
					// up string
					alog.info("\nQ3) We know we've received the ACSE Response, now what?");
					if (isLoginFirstROSEInvokeReceived()) {
						// we've received the first ROSE invoke, have we sent a
						// reply
						alog.info("\nQ4) We know we've received the first ROSE Invoke");
						if (isLoginFirstROSEResponseSent()) {
							// then we are logged in
							setLoggedIn(true);
							layer7.setCSTAUp(true);
							alog.info("CSTA connection established");
						} else {
							alog.warn("Hey, we're here!");
						}
					} else {
						// is this the First ROSE Invoke, check it, set
						// FirstROSEInvokeReceived to true
						if (curInStr.charAt(0) == 0xA1) {
							setLoginFirstROSEInvokeReceived(true);
							alog.debug("RESPONSE: We've just received the first ROSE Invoke");
							setLoginFirstROSEResponseSent(true);
							layer7.LoginResponse();
							this.setLoggedIn(true);
							layer7.setCSTAUp(true);
						}
						// send First ROSE Response

						// set true loggedIn with setLoggedIn(true)
					}
				} else {
					// is this the ACSE Response Received? check it, set
					// ACSERseponseReceived to true
					if (curInStr.charAt(0) == 0x61) {
						alog.debug("RESPONSE: We've just received the ACSE Response");
						setLoginACSEResponseReceived(true);
					}
				}
			}
		}
		return true;
	}

	// Layer 7 passing down to layer 5, wrap and send.
	// This will move into a linked list so the layer 5 thread can do something
	// to free up the server thread.
	/**
	 * @param curOutStr
	 */
	public void FromAbove(StringBuffer curOutStr) {
		curOutStr = Wrap(curOutStr);
		this.link.sendCommand(curOutStr);
	}

	// could possibly remove this method.
	/**
	 * @param curOutStr
	 * @return
	 */
	public boolean L7LoginFromAbove(StringBuffer curOutStr) {
		curOutStr = Wrap(curOutStr);
		this.link.sendCommand(curOutStr);
		return true;
	}

	// Strip layer 5 data.
	/**
	 * @param curInStr
	 * @return
	 */
	private StringBuffer Strip(StringBuffer curInStr) {
		/*
		 * Check to see the length if it is 0x80, 0x81 or 0x82 and take the
		 * required chars away
		 */
		if (curInStr.charAt(1) == 0x80 || curInStr.charAt(1) == 0x81)
			curInStr = curInStr.deleteCharAt(0).deleteCharAt(0).deleteCharAt(0);
		else if (curInStr.charAt(1) == 0x82)/*
											 * char for length must be 0x82 ; so
											 * take away the required numbers of
											 * bytes
											 */
			curInStr = curInStr.deleteCharAt(0).deleteCharAt(0).deleteCharAt(0)
					.deleteCharAt(0);
		else if (curInStr.charAt(1) == 0x83)/*
											 * does 0x83 exist even?? delete
											 * required number if it does anyway
											 */
			curInStr = curInStr.deleteCharAt(0).deleteCharAt(0).deleteCharAt(0)
					.deleteCharAt(0).deleteCharAt(0);
		else if (curInStr.charAt(1) == 0x84)/*
											 * delete 4 characters worth of
											 * length...
											 */
			curInStr = curInStr.deleteCharAt(0).deleteCharAt(0).deleteCharAt(0)
					.deleteCharAt(0).deleteCharAt(0).deleteCharAt(0);
		return curInStr;
	}

	// Wrap/encapsulate layer 5 data.
	/**
	 * @param curOutStr
	 * @return
	 */
	private StringBuffer Wrap(StringBuffer curOutStr) {
		curOutStr = curOutStr.insert(0, (char) curOutStr.length())
				.insert(0, '\u0080').insert(0, '\u0026');
		return curOutStr;
	}

	// Wrap/encapsulate layer 5 data.
	/**
	 * @param curOutStr
	 * @return
	 */
	private String Wrap(String curOutStr) {
		char length = (char) curOutStr.length();
		char[] temp = { 0x26, 0x80, length };
		String l5stuff = new String(temp);
		curOutStr = l5stuff + curOutStr;
		return curOutStr;
	}

	// flick a switch
	/**
	 * @return
	 */
	public boolean isLoginACSERequestSent() {
		return loginACSERequestSent;
	}

	// flick a switch
	/**
	 * @param loginACSERequestSent
	 */
	public void setLoginACSERequestSent(boolean loginACSERequestSent) {
		this.loginACSERequestSent = loginACSERequestSent;
	}

	// flick a switch
	/**
	 * @return
	 */
	public boolean isLoginACSEResponseReceived() {
		return loginACSEResponseReceived;
	}

	// flick a switch
	/**
	 * @param loginACSEResponseReceived
	 */
	public void setLoginACSEResponseReceived(boolean loginACSEResponseReceived) {
		this.loginACSEResponseReceived = loginACSEResponseReceived;
	}

	// flick a switch
	/**
	 * @return
	 */
	public boolean isLoginFirstROSEInvokeReceived() {
		return loginFirstROSEInvokeReceived;
	}

	// flick a switch
	/**
	 * @param loginROSEInvokeReceived
	 */
	public void setLoginFirstROSEInvokeReceived(boolean loginROSEInvokeReceived) {
		this.loginFirstROSEInvokeReceived = loginROSEInvokeReceived;
	}

	// flick a switch
	/**
	 * @return
	 */
	public boolean isLoginFirstROSEResponseSent() {
		return loginFirstROSEResponseSent;
	}

	// flick a switch
	/**
	 * @param loginROSEResponseSent
	 */
	public void setLoginFirstROSEResponseSent(boolean loginROSEResponseSent) {
		this.loginFirstROSEResponseSent = loginROSEResponseSent;
	}

	// flick a switch
	/**
	 * @return
	 */
	public boolean isLoggedIn() {
		return loggedIn;
	}

	// flick some switches
	/**
	 * @param loggedIn
	 */
	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
		if (this.loggedIn == false) {
			setLoginFirstROSEResponseSent(false);
			setLoginFirstROSEInvokeReceived(false);
			setLoginACSEResponseReceived(false);
			setLoginACSERequestSent(false);
		}
	}

	// flick a switch
	/**
	 * @param flip
	 */
	private void Layer5KeepAlive(boolean flip) {
		this.alive = flip;
	}

	// add a job to the layer 5 work list.
	/**
	 * @param job
	 */
	public synchronized void addWorkOUT(StringBuffer job) {
		workListDownward.add(job);
		alog.warn(this.getClass().getName()
				+ " added work out -- need to notify link thread");
	}

	// add a job to the layer 5 work list.
	/**
	 * @param job
	 */
	public synchronized void addWorkIN(StringBuffer job) {
		workListUpward.add(job);
	}

	/**
	 * @return
	 */
	public synchronized StringBuffer getLayer5JobOut() {
		return (StringBuffer) workListDownward.remove(0);
	}

	/**
	 * @return
	 */
	public synchronized StringBuffer getLayer5JobIn() {
		return (StringBuffer) workListUpward.remove(0);
	}

	/**
	 * @return
	 */
	public int getSizeLayer5WorkIN() {
		return workListUpward.size();
	}

	/**
	 * @return
	 */
	public int getSizeLayer5WorkOUT() {
		return workListDownward.size();
	}

	/**
	 * @return
	 */
	private boolean startCSTALink() {
		alog.info(this.getClass().getName()
				+ " .... CSTA Link first attempt to connect");
		int tmp_counter = 0;
		while (tmp_counter < 20) {

			alog.debug("Layer 5, Checking link if link is up");
			if (!link.isLinkUp()) {
				// send asn.1 connector init
				alog.debug("layer 5 reckons the link is down. The CSTA Link implmentation has probably started link init");
				alog.debug(link.getStatus());
			} else {
				alog.debug("Layer 5 says, Link is up");
				return true;
			}
			alog.debug("Waiting 1 second for CSTA Link to get organised");
			alog.debug("");
			alog.debug("");
			tmp_counter++;
			try {
				synchronized (this) {
					wait(1000);
				}
			} catch (InterruptedException e) {

			} catch (NullPointerException e2) {
				e2.printStackTrace();
			}
		}

		alog.warn("Staring CSTA Link failed after 20 seconds.");

		// create thread, and send the runnable 'link' to it so it can
		// now look after itself

		return false;
	}

	/**
	 * @param str
	 * @return
	 */
	public StringBuffer strip(StringBuffer str) {
		return str.deleteCharAt(0).deleteCharAt(0).deleteCharAt(0);
	}

	/**
	 * @return
	 */
	public boolean isCSTALinkUp() {
		return CSTALinkUp;
	}

	/**
	 * @param CSTALinkUp
	 */
	public void setCSTALinkUp(boolean CSTALinkUp) {
		this.CSTALinkUp = CSTALinkUp;
	}

	/**
	 * @return the theProps
	 */
	public static Properties getTheProps() {
		return theProps;
	}

	/**
	 * @param aTheProps
	 *            the theProps to set
	 */
	public static void setTheProps(Properties aTheProps) {
		theProps = aTheProps;
	}
}
