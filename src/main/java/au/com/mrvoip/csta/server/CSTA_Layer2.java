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

package au.com.mrvoip.csta.server;

import java.io.IOException;
import java.util.Properties;

import org.opencsta.communications.CommunicationsStream;
import org.opencsta.link.CSTA_Link;
import org.opencsta.link.CSTA_Link_Serial;
import org.opencsta.servicedescription.common.helpers.CSTA_Layer_Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OS-CSTA-DD1: The data-link layer in the CSTA protocol stack
 * 
 * @author chrismylonas
 */
public class CSTA_Layer2 implements CSTA_Layer_Interface, Runnable {

	/**
	 * 
	 */
	private CSTA_Link link;

	/**
	 * 
	 */
	protected static Logger alog = LoggerFactory.getLogger(CSTA_Link.class);

	/**
	 * 
	 */
	protected static Logger cstalog = LoggerFactory.getLogger(CSTA_Layer2.class);

	/**
	 * 
	 */
	private static CommunicationsStream layer1; // serial port

	/**
	 * 
	 */
	private boolean ack0 = true;

	/**
	 * 
	 */
	private boolean lastWasDLE = false;

	/**
	 * 
	 */
	private boolean lastRecACK = false;

	/**
	 * 
	 */
	private static boolean successfullySent = false;

	/**
	 * 
	 */
	private boolean lastSentACK = true;

	/**
	 * 
	 */
	private boolean enqSent = false;

	// private boolean reSend = false ;

	/**
	 * 
	 */
	private StringBuffer lastSentString;

	/**
	 * 
	 */
	private StringBuffer newReceived;

	/**
	 * 
	 */
	private boolean alive = false;

	/**
	 * 
	 */
	private static boolean da = false;

	/**
	 * 
	 */
	private int DLEcount;

	/**
	 * 
	 */
	private boolean LRCcoming;

	// private static CSTA_Layer5 layer5 ;

	/**
	 * 
	 */
	private static int timer = 0;

	/**
	 * 
	 */
	private static Properties theProps;

	/**
	 * 
	 * 
	 * 
	 * @param newLayer5
	 */
	public CSTA_Layer2(CSTA_Link csta_link, Properties _theProps) {
		theProps = _theProps;
		this.link = csta_link;
		alog.info(this.getClass().getName() + " initialising");
		layer1 = new CSTA_Layer1(this, theProps);
		Init();
		if (layer1 == null)
			alog.warn("Layer2 has a null value for layer1");
		else
			;
	}

	/**
	 * @return
	 */
	public boolean isDA() {
		return da;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		alog.info(this.getClass().getName()
				+ " has been started by the CSTA Link Thread");
		while (link.isLinkUp()) {
			alog.info(this.getClass().getName()
					+ " is running, the csta link is up - waiting 5 seconds for new jobs");
			try {
				synchronized (this) {
					wait(5000);
				}
			} catch (InterruptedException e) {

			} catch (NullPointerException e2) {
				e2.printStackTrace();
			}

			while (link.getSizeLayer5WorkOUT() > 0) {
				StringBuffer newStrBuf;
				newStrBuf = link.getLayer5OUTCommand();
				if (sendCommand(newStrBuf))
					continue;
			}
		}
		layer1.closeComms();

	}

	/**
     *
     *
     */
	private void Init() {
		newReceived = new StringBuffer();
		alive = true;
		DLEcount = 0;
		LRCcoming = false;
	}

	/**
     *
     *
     */
	public void CloseCSTAStack() {
		try {
			alog.info("Layer2 - Killing Serialport");
			alive = false;
			layer1.closeComms();
			layer1 = null;
			alog.info("Layer2 - Killing Serialport  ---- OK.DONE");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param curInStr
	 */
	public boolean FromBelow(StringBuffer curInStr) {
		WriteToLogger(curInStr, 'R');
		int thisLength = curInStr.length();
		// check validity of length - must be <= 250 bytes, DLE DLE count for 1
		// byte as per CSTA information provided by siemens
		if (thisLength > 250) {
			int doubleCheck = thisLength - DLEcount;
			if (doubleCheck > 250) {
				alog.warn("Layer 2 Received more than 250 characters");
				return false;
			} else
				;
		} else
			; // old DLE count reset used to be here

		int thisLRC = curInStr.charAt(thisLength - 1);
		if (thisLRC == ChkSum(curInStr)) {
			if (SendACK()) {

				// STRIP
				curInStr = Strip(curInStr);

				if (DLEcount != 0)
					// CHANGE DLE DLE to DLE
					curInStr = ReplaceDLEDLEwithDLE(curInStr);
				else
					;

				DLEcount = 0;// reset DLE count for next incoming string from
								// PABX
				alog.info(this.getClass().getName()
						+ "============TMP LAYER 2 PASSING UP SOON");
				new tmp_Layer2(link, new StringBuffer(curInStr));
				return true;
			} else
				alog.warn("L2::PassedUp - SendACK returned false");
		} else {
			alog.warn("Layer2 Sending NAK::LRC does not equal calculated value, please check bytes");
			SendNAK();
			return true;
		}
		alog.warn("Layer2 Passed up. check this method, returning true for a default value");
		return true;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param completeString
	 */
	@SuppressWarnings("static-access")
	private boolean SendStringL2(StringBuffer completeString) {
		successfullySent = false;
		WriteToLogger(completeString, 'S');
		lastSentString = completeString;
		layer1.SendString(completeString);/* Write to SerialPort */
		timer = 0;
		while (!successfullySent) {
			if ((timer % 10) == 0) {

				if (timer == 2400) {
					/*
					 * Approximately 12 seconds has elapsed and we haven't
					 * received an ACK. Send the first ENQuiry now and wait some
					 * more...then send another one. if we don'g get anything
					 * after some time after the second one, kill the the csta
					 * stack because layer2 has failed
					 */
					WriteToLogger(new StringBuffer(DLEENQ), 'S');
					alog.info("Layer2 Sending 1st DLE ENQ");
					enqSent = true;
					layer1.SendString(new StringBuffer(DLEENQ));
				} else if (timer == 3600) {
					WriteToLogger(new StringBuffer(DLEENQ), 'S');
					alog.info("Layer2 Sending 2nd DLE ENQ");
					layer1.SendString(new StringBuffer(DLEENQ));
				} else if (timer == 5000) {
					/* Kill the stack */
					alog.warn("Layer2 says, Kill csta stack.  Two DLE ENQs sent and no reply");
					enqSent = false;
					this.CloseCSTAStack();
				}

				try {
					Thread.currentThread().sleep(50);
				} catch (InterruptedException e) {
				}
			}
			if (timer > 5000) {
				return false;
			}
			timer++;
		}

		return true;
	}

	/**
	 * 
	 * 
	 * 
	 * @param completeString
	 */
	@SuppressWarnings("static-access")
	public void ReSendStringL2(StringBuffer completeString) {
		successfullySent = false;
		WriteToLogger(completeString, 'S');
		lastSentString = completeString;
		layer1.SendString(completeString);/* Write to SerialPort */
		// WOW - OLD COMMENTS! (NOV2010)
		// 09Jan03--STILL TO DO SOME WORK HERE...
		// This ends up looping forever by the looks of it! Shouldn't!
		while (!successfullySent) {
			try {
				Thread.currentThread().sleep(50);
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param curOutStr
	 */
	public boolean SendStringNoAckReqd(StringBuffer curOutStr)
			throws IOException {
		curOutStr = ReplaceDLEwithDLEDLE(curOutStr);
		curOutStr = Wrap(curOutStr);
		WriteToLogger(curOutStr, 'S');
		layer1.SendString(curOutStr);
		return true;
	}

	/**
	 * @param curOutStr
	 * @return
	 */
	public boolean sendCommand(StringBuffer curOutStr) {
		curOutStr = ReplaceDLEwithDLEDLE(curOutStr);
		curOutStr = Wrap(curOutStr);
		return SendStringL2(curOutStr);
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param curInStr
	 */
	private StringBuffer Strip(StringBuffer curInStr) {
		int length = curInStr.length();
		curInStr = curInStr.deleteCharAt(length - 1).deleteCharAt(length - 2)
				.deleteCharAt(length - 3).deleteCharAt(0).deleteCharAt(0);
		return curInStr;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param curOutStr
	 */
	private StringBuffer ReplaceDLEwithDLEDLE(StringBuffer curOutStr) {// used
																		// to
																		// return
																		// a
																		// string....
		StringBuffer tmp = curOutStr;
		char[] tmpDLE = { DLE };
		String DLEasSTR = new String(tmpDLE);
		int tmpDLEcount = 0;
		int length = tmp.length();
		for (int i = 0; i < length; i++) {
			if (tmp.charAt(i + tmpDLEcount) == DLE) {
				tmp = tmp.insert((i + tmpDLEcount), DLEasSTR);
				tmpDLEcount++;
			} else
				;
		}
		return tmp; // used to be curOutStr when tmp was toStringed....
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param curInStr
	 */
	private StringBuffer ReplaceDLEDLEwithDLE(StringBuffer curInStr) {
		boolean DLEflag = false;
		int length = curInStr.length();
		StringBuffer curInStr2 = new StringBuffer();
		for (int i = 0; i < length; i++) {
			if (curInStr.charAt(i) == DLE) {
				if (DLEflag == true)
					DLEflag = false;
				else {
					curInStr2 = curInStr2.append(curInStr.charAt(i));
					DLEflag = true;
				}
			} else
				curInStr2 = curInStr2.append(curInStr.charAt(i));
		}
		return curInStr2;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param curOutStr
	 */
	private StringBuffer Wrap(StringBuffer curOutStr) {
		curOutStr = curOutStr.insert(0, STX).insert(0, DLE).append(DLE)
				.append(ETX);
		curOutStr = curOutStr.append((char) ChkSumDown(curOutStr));

		return curOutStr;
	}

	/**
     *
     *
     */
	public void SendNAK() {
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	public boolean SendACK() {
		if (ack0) {
			ack0 = !ack0;
			layer1.SendString(new StringBuffer(ACK0));
			cstalog.info("S: 10 30");
		} else {
			ack0 = !ack0;
			layer1.SendString(new StringBuffer(ACK1));
			cstalog.info("S: 10 31");
		}
		return true;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param thisStr
	 */
	private int ChkSumDown(StringBuffer thisStr) {
		int checksum;
		int length = thisStr.length();
		int value = -18;
		for (int i = 0; i <= (length - 1); i++)
			// NOTE: THERE IS AN EQUAL SIGN ON THE WAY DOWN IN THE FOR
			// LOOP!!!!!!!!!!!!!!
			value += thisStr.charAt(i);
		checksum = value % 256;
		alog.debug("*\n**\n***\n****\n*****\n******\n*******\n********Calculated Checksum is: "
				+ Integer.toString(checksum));
		return checksum;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param thisStr
	 */
	private int ChkSum(StringBuffer thisStr) {
		int checksum;
		int length = thisStr.length();
		int value = -18;
		for (int i = 0; i < (length - 1); i++)
			value += thisStr.charAt(i);
		checksum = value % 256;
		return checksum;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param just_in
	 */
	private boolean MatchENQsAndACKs(boolean just_in) {
		if (just_in && lastRecACK) {
			// this ACK is the same as the last one we successfully received
			// so we must resend the last string we sent. This happens to be
			// the one that is missing the acknowledgement
			successfullySent = true;// get out of while loop for the old send
			enqSent = false;
			new tmp_Layer2_ENQ((CSTA_Link_Serial) link, lastSentString);
		} else if (!just_in && !lastRecACK) {
			// this ACK is the same as the last one we successfully received
			// so we must resend the lsat string we sent. This happens to be
			// the one htat is missing the acknowledgement
			successfullySent = true;// get out of while loop for the old send
			enqSent = false;
			new tmp_Layer2_ENQ((CSTA_Link_Serial) link, lastSentString);
		} else {
			// the ACK just received does not match the last one we noted we
			// received. This means the normal operation has continued, but
			// we didn't realise that it had happened.
			// do nothin
			successfullySent = true;
			enqSent = false;

		}
		return true;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param input
	 */
	public boolean CheckReceived(StringBuffer input) {
		int length = input.length();
		if (length == 1 && input.charAt(length - 1) == NAK) {
			alog.warn("***RECEIVED NAK***");
			cstalog.info("R: 15");
			new tmp_Layer2_NAK((CSTA_Link_Serial) link, lastSentString);
			return true;
		}

		else if (lastWasDLE) {
			if (input.charAt(length - 1) == 0x30) {
				if (enqSent) {
					lastWasDLE = false;
					return MatchENQsAndACKs(false);// false == 0x30
				} else {
					lastWasDLE = false;
					lastRecACK = false;
					if (!link.isLinkUp()) {
						alog.info("Layer 2, CSTA Link is not up yet - received an ACK 10 30, so it looks like a logon procedure");
						alog.info("<Layer 2> " + link.getStatus());
						if (!link.isLoginASN1ConnectorStartResponse()) {
							alog.info("Layer 2, looks like a CSTA Logon, first ACK to ASN1 Connector Initialisation");
							alog.info("<Layer 2> " + link.getStatus());
							link.setLoginASN1ConnectorStartResponse(true);
							link.increaseStatus();
							alog.info("<Layer 2>  " + link.getStatus());
							if (link.isLoginACSEAbortSent()) {

							}
						}
					}
					ReceivedACK(input);
				}
				return true;
			} else if (input.charAt(length - 1) == 0x31) {
				if (enqSent) {
					lastWasDLE = false;
					// lastRecACK = true ;
					return MatchENQsAndACKs(true);// true == 0x31
				} else {
					lastWasDLE = false;
					lastRecACK = true;
					if (!link.isLinkUp()) {
						alog.info("1031 - 1 - Layer 2, CSTA Link is not up yet - received an ACK 10 31, so it looks like a logon procedure");
						alog.info("<Layer 2> " + link.getStatus());
						if (!link.isLoginACSEAbortResponseReceived()) {
							alog.info("1031 - 3 - Layer 2, looks like a CSTA Logon, ACK to ACSE Abort String");
							alog.info("<Layer 2> " + link.getStatus());
							link.setLoginACSEAbortResponseReceived(true);
							link.setLinkUp(true);
							link.increaseStatus();
							alog.info("<Layer 2> " + link.getStatus());
						}
					}
					ReceivedACK(input);
				}
				return true;
			}

			else if (input.charAt(length - 1) == DLE) {
				lastWasDLE = false;
				DLEcount++;
				return false;
			}

			else if (input.charAt(length - 1) == STX) {
				lastWasDLE = false;
				return false;
			}

			else if (input.charAt(length - 1) == ETX) {
				lastWasDLE = false;
				LRCcoming = true;
				return false;
			}

			else if (input.charAt(length - 1) == ENQ) {
				cstalog.info("R: 10 05");
				alog.info("RECEIVED ENQ");
				return SendLastACK();
			} else {
				alog.warn("Check Layer2 CheckReceived - unlucky to get here");
				return false;
			}
		}

		else if ((input.charAt(length - 1) == DLE) && (LRCcoming == false)) {
			lastWasDLE = true;
			return false;
		}

		else if (LRCcoming == true) {
			lastWasDLE = false;
			LRCcoming = false;
			newReceived = new StringBuffer(input.toString());
			return (FromBelow(newReceived));
		} else {
			// OTHER LEGIT CHAR RECEIVED
			return false;
		}

	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param input
	 */
	private boolean ReceivedACK(StringBuffer input) {
		if ((input.charAt(input.length() - 1)) == 0x30)
			cstalog.info("R: 10 30");
		else
			cstalog.info("R: 10 31");
		successfullySent = true;
		return true;
	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 * @param lastACK
	 */
	public boolean Received_ENQ(boolean lastACK) {
		if (lastACK) {// ACK1
			new tmp_Layer2_ENQreceived((CSTA_Link_Serial) link, true);
			cstalog.info("S: 10 31");
		} else {// ACK0
			new tmp_Layer2_ENQreceived((CSTA_Link_Serial) link, false);
			cstalog.info("S: 10 30");
		}
		alog.info("Layer2 Sending Last ACK because an ENQ was received.");
		return true;

	}

	/**
	 * 
	 * 
	 * 
	 * @return
	 */
	private boolean SendLastACK() {// CHECK ALL THIS ACK CODE.
		// COS I DIDN"T MAKE A NOTE
		// OF THE lastSentAck/curRecAck/whateverelseACK booleans mean.
		if (lastSentACK) {// ACK0
			new tmp_Layer2_ENQreceived((CSTA_Link_Serial) link, true);
			cstalog.info("S: 10 31");
		} else {// ACK1
			new tmp_Layer2_ENQreceived((CSTA_Link_Serial) link, false);
			cstalog.info("S: 10 30");
		}
		alog.info("Layer2 Sending Last ACK because an ENQ was received.");
		return true;
	}

	public void SendLastACKOnly(StringBuffer sb) {
		layer1.SendString(sb);
	}

	/**
	 * 
	 * 
	 * 
	 * @param strBuf
	 * @param s_or_r
	 */
	private void WriteToLogger(StringBuffer strBuf, char s_or_r) {
		String str;
		str = Character.toString(s_or_r);
		str += ": ";
		for (int i = 0; i < strBuf.length(); i++)
			str += Integer.toHexString(strBuf.charAt(i)) + " ";
		cstalog.info(str);
	}
}

/**
 *
 *
 */
class tmp_Layer2_NAK implements Runnable {

	/**
     *
     *
     */
	StringBuffer resendString;

	/**
     *
     *
     */
	CSTA_Link_Serial link;

	/**
	 * 
	 * 
	 * 
	 * @param lyr2
	 * @param curOutStr
	 */
	public tmp_Layer2_NAK(CSTA_Link_Serial link, StringBuffer curOutStr) {
		this.link = link;
		this.resendString = curOutStr;
	}

	/**
     *
     *
     */
	public void run() {
		link.ResendString(resendString);
	}
}

/**
 *
 *
 */
class tmp_Layer2_ENQ implements Runnable {

	/**
     *
     *
     */
	StringBuffer resendString;

	/**
     *
     *
     */
	CSTA_Link_Serial link;

	/**
	 * 
	 * 
	 * 
	 * @param lyr2
	 * @param curOutStr
	 */
	public tmp_Layer2_ENQ(CSTA_Link_Serial link, StringBuffer curOutStr) {
		this.link = link;
		this.resendString = curOutStr;
	}

	/**
     *
     *
     */
	public void run() {
		link.ResendString(resendString);
	}
}

class tmp_Layer2_ENQreceived implements CSTA_Layer_Interface, Runnable {

	/**
     *
     *
     */
	boolean whichack;

	/**
     *
     *
     */
	CSTA_Link_Serial link;

	/**
	 * 
	 * 
	 * 
	 * @param lyr1
	 * @param whichack
	 */
	public tmp_Layer2_ENQreceived(CSTA_Link_Serial link, boolean whichack) {
		this.link = link;
		this.whichack = whichack;
	}

	/**
     *
     *
     */
	public void run() {
		if (whichack)
			link.SendLastACKOnly(new StringBuffer(ACK1));
		else
			link.SendLastACKOnly(new StringBuffer(ACK0));
	}
}

/**
 *
 *
 */
class tmp_Layer2 implements Runnable {

	/**
     *
     *
     */
	CSTA_Link link;

	/**
     *
     *
     */
	StringBuffer curInStr;

	/**
	 * 
	 * 
	 * 
	 * @param layer5
	 * @param curInStr
	 */
	public tmp_Layer2(CSTA_Link csta_link, StringBuffer curInStr) {
		this.link = csta_link;
		this.curInStr = curInStr;
		link.executeUpwards(this);
	}

	/**
     *
     *
     */
	public void run() {
		link.commandIn(curInStr);
	}
}
