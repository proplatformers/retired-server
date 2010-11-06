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

//import org.opencsta.link.CSTA_Link ;
//import org.opencsta.net.TCPClient;
import java.net.Socket;
import org.opencsta.net.TCPClientOwnerInterface;
import org.opencsta.server.CSTA_Layer5;

/**
 * 
 * @author chrismylonas
 */
public class CSTA_Link_Siemens_Hipath3000_Network extends CSTA_Link implements
		TCPClientOwnerInterface {

	/**
     * 
     */
	private CSTATCPClient tcp;

	/**
	 * @param layer5
	 */
	public CSTA_Link_Siemens_Hipath3000_Network(CSTA_Layer5 layer5) {
		super(layer5);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#Init()
	 */
	@SuppressWarnings("static-access")
	@Override
	public void Init() {
		tcp = new CSTATCPClient(this, layer5.getTheProps(), "CSTALINKTCP");
		setLinkUp(true);
		setLoginASN1ConnectorStartSent(true);
		setLoginACSEAbortSent(false);
		setLoginASN1ConnectorStartResponse(false);
		setCurrentStatus(4);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#threaden()
	 */
	@Override
	public void threaden() {
		alog.info(this.getClass().getName() + " getting CSTA Link Thread");
		threadExecutor.execute(tcp);
		// Thread aThread = getLinkThread() ;
		// aThread = new Thread(tcp,"CSTA Network Link Thread");
		// alog.info(this.getClass().getName() + " starting CSTA Link Thread" )
		// ;
		// setLinkThread(aThread) ;
		// aThread.start() ;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#sendCommand(java.lang.StringBuffer)
	 */
	@Override
	public boolean sendCommand(StringBuffer line) {
		tcp.Send(line);
		return true;
	}

	/**
	 * @param curInStr
	 * @return
	 */
	public boolean PassedUp(StringBuffer curInStr) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * @param str
	 */
	public void addWorkIN(StringBuffer str) {
		layer5.addWorkIN(str);
		notify();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opencsta.link.CSTA_Link#run()
	 */
	@Override
	public void run() {
		// while(isLinkUp()){
		// //System.out.println("THIS RUN() METHOD IS IMPLEMENTED") ;
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

	/**
	 * @return
	 */
	public Socket getSocket() {
		return tcp.getSocket();
	}

	/**
     * 
     */
	public void cstaFail() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
