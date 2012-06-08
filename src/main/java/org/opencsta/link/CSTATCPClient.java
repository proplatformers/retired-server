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

package org.opencsta.link;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCPClient is the client side network communications class that connects to
 * the CSTA Server.
 * 
 * @author chrismylonas
 */
public class CSTATCPClient implements Runnable {

	/**
	 * 
	 */
	protected Logger clientlog = LoggerFactory.getLogger(CSTATCPClient.class);

	/**
	 * 
	 */
	private char DLE = '\u0010';

	/**
	 * 
	 */
	private byte[] CCCbuffer = new byte[256];

	/**
	 * 
	 */
	private byte[] CCCnewBuf = new byte[256];

	/**
	 * 
	 */
	private boolean CSTAClientCommunications;

	/**
	 * 
	 */
	private StringBuffer CCCrxstr;

	/**
	 * 
	 */
	private boolean CCCcomplete;

	/**
	 * 
	 */
	private boolean CCClastWasDLE;

	/**
	 * 
	 */
	Properties theProps;

	/**
	 * 
	 */
	String APPNAME;

	/**
	 * 
	 */
	int PORT;

	/**
	 * 
	 */
	String SERVER_ADDRESS;

	/**
	 * 
	 */
	private DataOutputStream out;

	/**
	 * 
	 */
	private DataInputStream in;

	/**
	 * 
	 */
	private Socket socket;

	/**
	 * 
	 */
	private byte[] buf;

	/**
	 * 
	 */
	private CSTA_Link_Interface parent;

	/**
	 * 
	 */
	private boolean runFlag = false;

	/**
	 * 
	 */
	private int line;

	/**
	 * 
	 */
	private StringBuffer chris;

	/**
	 * @param _theProps
	 */
	public CSTATCPClient(Properties _theProps) {
		theProps = _theProps;
		Init();
	}

	/**
	 * @param parent
	 * @param _theProps
	 * @param _appname
	 */
	public CSTATCPClient(CSTA_Link_Interface parent, Properties _theProps,
			String _appname) {
		theProps = _theProps;
		this.parent = parent;
		this.APPNAME = _appname;
		Init();
	}

	/**
	 * 
	 */
	public void Init() {
		try {
			clientlog.info(this.getClass().getName() + APPNAME
					+ " is the appname");
			PORT = Integer.parseInt(theProps.getProperty(APPNAME
					+ "_SERVER_PORT"));
			SERVER_ADDRESS = theProps.getProperty(APPNAME + "_SERVER_ADDRESS");
			String logstr = "CSTAServer address: " + SERVER_ADDRESS;
			setSocket(new Socket(SERVER_ADDRESS, PORT));
			logstr += "|||Socket connected to: " + getSocket();
			clientlog.info(this.getClass().getName() + " " + logstr);
			in = new DataInputStream(getSocket().getInputStream());
			out = new DataOutputStream(getSocket().getOutputStream());

		} catch (UnknownHostException e) {
			clientlog.warn(this.getClass().getName()
					+ "Unknown host: kq6py.eng");
			System.exit(1);
		} catch (IOException e) {
			clientlog.warn(this.getClass().getName() + "No I/O");
			System.exit(1);
		} catch (NumberFormatException nfe) {
			clientlog.warn(this.getClass().getName()
					+ "Number Format Exception: " + PORT);
			nfe.printStackTrace();
			System.exit(1);
		}
		chris = new StringBuffer();
		buf = new byte[1024];
		CCCrxstr = new StringBuffer();
		CCCcomplete = false;
	}

	/**
	 * @return
	 */
	public String GetConnectionInfo() {
		String str = "TCP/IP Connection status:\n\tSocket: " + getSocket();
		return str;
	}

	/**
	 * @param strBuf
	 * @param s_or_r
	 */
	public void WriteToLog(StringBuffer strBuf, char s_or_r) {
		String tmp = Character.toString(s_or_r) + ": ";
		for (int y = 0; y < strBuf.length(); y++)
			tmp += Integer.toHexString(strBuf.charAt(y)) + " ";

		clientlog.info(this.getClass().getName() + tmp);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		this.setRunFlag(true);
		if (isRunFlag()) {
			while (runFlag) {
				try {
					buf = new byte[1024];
					line = in.read(buf);
					clientlog.info(this.getClass().getName() + line
							+ " bytes received");
					buf2SBChris(line);
				} catch (IOException e) {
					clientlog.warn(this.getClass().getName() + "Read failed");
					System.exit(-1);
				}
			}
		}
	}

	/**
	 * 
	 */
	public void Disconnect() {
		try {
			clientlog.info(this.getClass().getName()
					+ "This end socket closing");
			getSocket().close();
		} catch (IOException e) {
		}
	}

	/**
	 * @param str
	 */
	public void Send(StringBuffer str) {
		String theString = str.toString();
		WriteToLog(str, 'S');
		try {
			out.writeBytes(theString);
		} catch (IOException e) {
			clientlog.warn(this.getClass().getName()
					+ "WARN IOE tcpclient.Send - " + e.toString());
		} catch (NullPointerException e2) {
			clientlog.warn(this.getClass().getName()
					+ "WARN NE tcpclient.Send- " + e2.toString());
		}
	}

	/**
	 * @return
	 */
	public boolean isRunFlag() {
		return runFlag;
	}

	/**
	 * @param runFlag
	 */
	public void setRunFlag(boolean runFlag) {
		this.runFlag = runFlag;
	}

	/**
	 * @param length
	 */
	private void buf2SBChris(int length) {
		for (int i = 0; i < length; i++) {

			if ((short) buf[i] < 0) {
				append2chris((int) buf[i] + 256);
			}

			else {
				Byte b = new Byte(buf[i]);
				append2chris((int) b.intValue());
			}
		}
		checkBuffer();
	}

	/**
	 * 
	 */
	private void checkBuffer() {
		if (chris.length() > 5) {
			// TestChris(chris) ;
			if (isBufferResetableAndEven(chris)) {
				clientlog.info(this.getClass().getName()
						+ "Incoming Buffer is even and being reset");
				parent.newMessageIn(new tmp_CSTATCPClient(parent,
						new StringBuffer(chris)));
				chris = new StringBuffer();
			} else if (isBufferStillReading(chris)) {
				clientlog.info(this.getClass().getName()
						+ "Buffer is still reading");
			} else if (isBufferHoldingMoreThanOneMessage(chris)) {
				clientlog.info(this.getClass().getName()
						+ "Buffer is holding more than one message");
				StringBuffer tmp = new StringBuffer(chris.substring(0,
						(((int) chris.charAt(2)) + 3)));
				parent.newMessageIn(new tmp_CSTATCPClient(parent,
						new StringBuffer(chris.substring(0,
								(((int) chris.charAt(2)) + 3)))));
				chris = new StringBuffer(
						chris.substring(((int) chris.charAt(2) + 3)));
				checkBuffer();
			}
		}
	}

	/**
	 * @param curOutStr
	 * @return
	 */
	private String ReplaceDLEwithDLEDLEandWrap(String curOutStr) {
		char DLEa = '\u0010';
		char ETX = '\u0003';
		StringBuffer curOutStr2 = new StringBuffer();
		char[] tmpDLE = { DLEa };
		String DLEasSTR = new String(tmpDLE);
		int length = curOutStr.length();
		for (int i = 0; i < length; i++) {
			if (curOutStr.charAt(i) == DLEa) {
				curOutStr2 = curOutStr2.append(DLEasSTR);
			} else
				;

			curOutStr2 = curOutStr2.append(curOutStr.charAt(i));
		}
		char[] dleetx = { DLEa, ETX };
		String DLEETX = new String(dleetx);
		curOutStr2 = curOutStr2.append(DLEETX);
		return curOutStr2.toString();
	}

	/**
	 * @param sb
	 * @return
	 */
	private boolean isBufferResetableAndEven(StringBuffer sb) {
		if (chris.length() == ((int) chris.charAt(2) + 3)) {
			return true;
		}
		return false;
	}

	/**
	 * @param sb
	 * @return
	 */
	private boolean isBufferHoldingMoreThanOneMessage(StringBuffer sb) {
		if (chris.length() > ((int) chris.charAt(2) + 3)) {
			return true;
		}
		return false;
	}

	/**
	 * @param sb
	 * @return
	 */
	private boolean isBufferStillReading(StringBuffer sb) {
		String bufLength = Integer.toString(chris.length());
		String intendedLength = Integer.toHexString((int) chris.charAt(2));
		clientlog.debug(this.getClass().getName()
				+ "Buffer status -> Intended Length: " + intendedLength
				+ " | Current Length: " + bufLength);
		if (chris.length() < (((int) chris.charAt(2)) + 3)) {
			clientlog.debug(this.getClass().getName()
					+ "Buffer is still reading");
			return true;
		}
		clientlog.info(this.getClass().getName()
				+ "Buffer is complete, now ready for clearing");
		return false;
	}

	/**
	 * @param thisByte
	 */
	private void append2chris(int thisByte) {
		chris.append((char) thisByte);
	}

	/**
	 * @param cm
	 */
	public void TestChris(StringBuffer cm) {
		for (int i = 0; i < cm.length(); i++) {
			System.out.print(Integer.toHexString((char) cm.charAt(i)) + " ");
		}
	}

	/**
	 * @param curInStr
	 * @return
	 */
	private StringBuffer ReplaceDLEDLEwithDLEandStrip(StringBuffer curInStr) {
		boolean DLEflag = false;
		int length = curInStr.length();
		curInStr = curInStr.deleteCharAt((length - 1));
		length = length - 1;
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
	 * @return
	 */
	public boolean isCSTAClientCommunications() {
		return CSTAClientCommunications;
	}

	/**
	 * @param CSTAClientCommunications
	 */
	public void setCSTAClientCommunications(boolean CSTAClientCommunications) {
		this.CSTAClientCommunications = CSTAClientCommunications;
	}

	/**
	 * @param currentByte
	 */
	private void CCCAddToBuffer(int currentByte) {
		if (!CCCcomplete) {
			if (!CheckReceived(currentByte))
				CCCrxstr = CCCrxstr.append((char) currentByte);
			else
				CCCcomplete = true;// this is complete, just for the trailing
									// character not to be added.
		} else
			;
	}

	/**
	 * @param currentByte
	 * @return
	 */
	private boolean CheckReceived(int currentByte) {

		if (CCClastWasDLE == true) {
			if (currentByte == 0x10) {
				CCClastWasDLE = false;
				return false;
			} else if (currentByte == 0x03) {
				CCClastWasDLE = false;
				return true;
				/*
				 * BECAUSE WE RETURN TRUE, 0X03 DOESN'T GET ADDED ONTO THE
				 * STRINGBUFFER COS IT KNOWS WE HAVE REACHED THE END.....SO WHEN
				 * IT COMES TIME TO STRIP....DON'T STRIP AN EXTRA CHARACTER OFF
				 * THAT WASNT' PUT ON IN THE FIRST PLACE...
				 */
			} else {
				CCClastWasDLE = false;
				return false;
			}
		} else if (currentByte == 0x10 && CCClastWasDLE == false) {
			// System.out.println("Current byte is 0x10") ;
			CCClastWasDLE = true;
			return false;
		} else {
			return false;
		}

	}

	/**
	 * @return the socket
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * @param socket
	 *            the socket to set
	 */
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}

/**
 * @author chrismylonas
 * 
 */
class tmp_CSTATCPClient implements Runnable {

	/**
     *
     *
     */
	CSTA_Link_Interface parent;
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
	public tmp_CSTATCPClient(CSTA_Link_Interface parent, StringBuffer curInStr) {
		this.parent = parent;
		this.curInStr = curInStr;
	}

	/**
     *
     *
     */
	public void run() {
		if (parent != null) {
			parent.commandIn(curInStr);
		} else {
			throw new UnsupportedOperationException(
					"TMP CSTATCPCLIENT has a null parent");
		}
	}

}
