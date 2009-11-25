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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.opencsta.server.CSTAServer;
import org.opencsta.server.CSTA_Layer5 ;

/**
 *
 * @author cm
 */
public abstract class CSTA_Link implements Runnable,CSTA_Link_Interface{
    protected static Logger alog = Logger.getLogger(CSTAServer.class) ;
    protected ExecutorService threadExecutor ;
    protected CSTA_Layer5 layer5 ;
    private boolean linkUp = false ;
    private boolean loginASN1ConnectorStartSent = false ;
    private boolean loginASN1ConnectorStartResponse = false ;
    private boolean loginACSEAbortSent = false ;
    private boolean loginACSEAbortResponseReceived = false ;
    private int currentStatus ;
    private boolean cleanStart = true ;
    private String[] status = { "Initialised" ,
                                "ASN1 Connector Down",
                                "ASN1 Connector Up",
                                "ACSE Abort Sent, Waiting Response",
                                "ACSE Abort Response Received, Link Up" ,
                                "Unknown"  } ;
    private Thread linkThread ;
    private static Properties theProps ;

    
    @SuppressWarnings("static-access")
    public CSTA_Link(CSTA_Layer5 layer5){
        this.layer5 = layer5 ;
        this.setCurrentStatus(0) ;
        threadExecutor = Executors.newFixedThreadPool( 11 );
        theProps = layer5.getTheProps() ;
        Init() ;
    }
    
    public void Init(){
        
    }

    public void threaden(){
        throw new UnsupportedOperationException("Not supported here, please override in subclass") ;
    }
    
    public boolean sendCommand(StringBuffer line){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private StringBuffer Wrap(StringBuffer line){
        return line ;
    }

    public void NotifyLinkOutboundJobs(){
        linkThread.notify();
    }

    public void run(){
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    public int getSizeLayer5WorkOUT(){
        return layer5.getSizeLayer5WorkOUT();
    }
    
    public int getSizeLayer5WorkIN(){
        return layer5.getSizeLayer5WorkIN() ;
    }
    
    public StringBuffer getLayer5OUTCommand(){
        return layer5.getLayer5JobOut() ;
    }
    
    public void commandIn(StringBuffer cmdIn){
        layer5.PassedUp(cmdIn) ;
    }

    public void executeUpwards(Runnable t2){

    }

    /**
     * @return the linkUp
     */
    public boolean isLinkUp() {
        if( isCleanStart() ){
            alog.debug("<CSTALINK> " + getStatus() ) ;
            if( !isLoginASN1ConnectorStartSent() ){
                sendASN1ConnectorInit() ;
                alog.debug("<CSTALINK> " + getStatus() ) ;
            }
            else if( isLoginACSEAbortSent()){
                if( isLoginACSEAbortResponseReceived() ){
                    setLinkUp(true) ;
                    setCleanStart(false) ;
                    alog.debug("<CSTALINK> " + getStatus() ) ;
                }

            }
                else if( isLoginASN1ConnectorStartResponse() ){
                sendACSEAbort() ;
                alog.debug("<CSTALINK> " + getStatus() ) ;
            }
            else {
                alog.debug("<CSTALINK> Nothing to check anymore, wait for next iteration" ) ;
            }
        }
        else{
//@todo when an interrupted csta link thingy goes down, needs some work prolly
        }
        return linkUp;
    }

    /**
     * @param linkUp the linkUp to set
     */
    public void setLinkUp(boolean slinkUp) {
        this.linkUp = slinkUp;
    }

        /**
     * @return the loginASN1ConnectorStartSent
     */
    public boolean isLoginASN1ConnectorStartSent() {
        return loginASN1ConnectorStartSent;
    }

    /**
     * @param loginASN1ConnectorStartSent the loginASN1ConnectorStartSent to set
     */
    public void setLoginASN1ConnectorStartSent(boolean loginASN1ConnectorStartSent) {
        this.loginASN1ConnectorStartSent = loginASN1ConnectorStartSent;
    }

    /**
     * @return the loginASN1ConnectorStartResponse
     */
    public boolean isLoginASN1ConnectorStartResponse() {
        return loginASN1ConnectorStartResponse;
    }

    /**
     * @param loginASN1ConnectorStartResponse the loginASN1ConnectorStartResponse to set
     */
    public void setLoginASN1ConnectorStartResponse(boolean loginASN1ConnectorStartResponse) {
        this.loginASN1ConnectorStartResponse = loginASN1ConnectorStartResponse;
    }

    /**
     * @return the loginACSEAbortSent
     */
    public boolean isLoginACSEAbortSent() {
        return loginACSEAbortSent;
    }

    /**
     * @param loginACSEAbortSent the loginACSEAbortSent to set
     */
    public void setLoginACSEAbortSent(boolean loginACSEAbortSent) {
        this.loginACSEAbortSent = loginACSEAbortSent;
    }

    /**
     * @return the loginACSEAbortResponseReceived
     */
    public boolean isLoginACSEAbortResponseReceived() {
        return loginACSEAbortResponseReceived;
    }

    /**
     * @param loginACSEAbortResponseReceived the loginACSEAbortResponseReceived to set
     */
    public void setLoginACSEAbortResponseReceived(boolean loginACSEAbortResponseReceived) {
        this.loginACSEAbortResponseReceived = loginACSEAbortResponseReceived;
    }
    
    public void sendASN1ConnectorInit(){
        //send the string provided here, if not set, don't
        setLoginASN1ConnectorStartSent(true) ;
        //assuming sent
        this.increaseStatus();
        String str = provideASN1ConnectorString() ;
        sendCommand(new StringBuffer(str) ) ;
    }

    public void sendACSEAbort(){
        this.setLoginACSEAbortSent(true) ;
        this.increaseStatus();
        String str = provideACSEAbortString() ;
        sendCommand( new StringBuffer(str) ) ;
    }

    /**
     * @return the status
     */
    public int getCurrentStatus() {
        return this.currentStatus ;
    }

    /**
     * @param status 
     */
    public  String  getStatus() {
        String statusString = "CSTA Link Status: " ;
        statusString += status[getCurrentStatus()] ;
        return statusString ;
    }

    /**
     * @param currentStatus the currentStatus to set
     */
    public void setCurrentStatus(int currentStatus) {
        this.currentStatus = currentStatus;
    }

    public void increaseStatus(){
        alog.debug("Increasing CSTA Link Status") ;
        this.currentStatus++ ;
    }

    public void decreaseStatus(){
        alog.debug("Decreasing CSTA Link Status") ;
        this.currentStatus-- ;
    }

    public String provideASN1ConnectorString(){
        alog.info("CSTA Link, implmement this method in subclasses") ;
        return "BOO" ;
    }

    public String provideACSEAbortString(){
        alog.info("CSTA Link, implement this method in subclass") ;
        return "BOO" ;
    }

    public Thread getLinkThread() {
        return linkThread;
    }

    public void setLinkThread(Thread linkThread) {
        this.linkThread = linkThread;
    }

    public void killThread(){
        this.setLinkThread(null);
    }

    public void notifyThread(){
        linkThread.notify();
    }
    /**
     * @return the cleanStart
     */
    public boolean isCleanStart() {
        return cleanStart;
    }

    /**
     * @param cleanStart the cleanStart to set
     */
    public void setCleanStart(boolean cleanStart) {
        this.cleanStart = cleanStart;
    }
    
    public void newMessageIn(tmp_CSTATCPClient tcpclient){
        threadExecutor.execute(tcpclient);
    }
    
    public void killThreads(){
        alog.info(this.getClass().getName() + " CSTA LINK Thread Pool is shutting down") ;
        threadExecutor.shutdown();
        alog.info(this.getClass().getName() + " Done shutting down CSTA LINK thread pool") ;
    }

    /**
     * @return the theProps
     */
    public static Properties getTheProps() {
        return theProps;
    }

    /**
     * @param aTheProps the theProps to set
     */
    public static void setTheProps(Properties aTheProps) {
        theProps = aTheProps;
    }
}
