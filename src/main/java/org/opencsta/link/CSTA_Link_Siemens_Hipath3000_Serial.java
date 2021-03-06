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

//import au.com.mrvoip.csta.link.serialport.*;
//import org.opencsta.link.CSTA_Link;
import org.opencsta.server.CSTA_Layer5;
import au.com.mrvoip.csta.server.CSTA_Layer2;

/**
 * 
 * @author chrismylonas
 */
public class CSTA_Link_Siemens_Hipath3000_Serial extends CSTA_Link implements
		CSTA_Link_Serial {

	/**
	 * 
	 */
	private CSTA_Layer2 layer2;

	/**
	 * @param layer5
	 */
	public CSTA_Link_Siemens_Hipath3000_Serial(CSTA_Layer5 layer5) {
		super(layer5);
		// @todo prolly just add the link thread stuff here.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#Init()
	 */
	public void Init() {
		layer2 = new CSTA_Layer2(this, getTheProps());
		setLinkUp(false);
		setLoginASN1ConnectorStartSent(false);
		setLoginACSEAbortSent(false);
		setLoginASN1ConnectorStartResponse(false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#threaden()
	 */
	public void threaden() {
		alog.info(this.getClass().getName() + " getting CSTA Link Thread");
		threadExecutor.execute(layer2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#executeUpwards(java.lang.Runnable)
	 */
	@Override
	public void executeUpwards(Runnable t2) {
		alog.info(this.getClass().getName() + " tmp_Layer2 executing");
		threadExecutor.execute(t2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#run()
	 */
	public void run() {
		// while(isLinkUp()){
		// alog.info(this.getClass().getName() +
		// " is running - waiting 5 seconds for new jobs") ;
		// try{
		// synchronized(this){
		// //System.out.println("Layer2, wait") ;
		// wait(5000) ;
		// }
		// }catch(InterruptedException e){
		// //System.out.println("Lower Layer Thread interrupted") ;
		// }catch(NullPointerException e2){
		// e2.printStackTrace();
		// }
		//
		// while( getSizeLayer5WorkOUT() > 0 ){
		// //System.out.println("Getting layer5 work") ;
		// StringBuffer newStrBuf ;
		// newStrBuf = getLayer5OUTCommand() ;
		// if( sendCommand( newStrBuf ) )
		// continue ;
		// }
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#sendCommand(java.lang.StringBuffer)
	 */
	@Override
	public boolean sendCommand(StringBuffer cmdOut) {
		return layer2.sendCommand(cmdOut);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#provideASN1ConnectorString()
	 */
	@Override
	public String provideASN1ConnectorString() {
		alog.info(this.getClass().getName()
				+ "  providing blank layer 5 string");
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#provideACSEAbortString()
	 */
	@Override
	public String provideACSEAbortString() {
		alog.info(this.getClass().getName() + " providing ACSE Abort String");
		char[] c_ar = { 0x26, 0x80, 0x08, 0x64, 0x06, 0x80, 0x01, 0x00, 0x81,
				0x01, 0x01 };
		String str = new String(c_ar);
		return str;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opencsta.link.CSTA_Link_Serial#ResendString(java.lang.StringBuffer)
	 */
	public void ResendString(StringBuffer sb) {
		layer2.ReSendStringL2(sb);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opencsta.link.CSTA_Link_Serial#SendLastACKOnly(java.lang.StringBuffer
	 * )
	 */
	public void SendLastACKOnly(StringBuffer sb) {
		layer2.SendLastACKOnly(sb);
	}
}
