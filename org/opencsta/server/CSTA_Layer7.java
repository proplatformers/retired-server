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

//import java.util.logging.* ;
import java.util.Properties;
import org.apache.log4j.*;
//import csta.common.objects.EventInfo ;
import org.opencsta.servicedescription.callcontrol.events.CallEvent_Base;
import org.opencsta.servicedescription.common.helpers.CSTA_Layer_7_Common;
import org.opencsta.servicedescription.common.helpers.CallEventHandler;
import org.opencsta.servicedescription.common.helpers.LogicalDeviceFeatureEventHandler;
import org.opencsta.servicedescription.ioservices.IOServices;
import org.opencsta.servicedescription.logicaldevicefeatures.events.AgentEvent_Base;

public class CSTA_Layer7 extends CSTA_Layer_7_Common {
    
    protected static Logger alog = Logger.getLogger(CSTAServer.class) ;
    private static CSTA_Layer5 layer5 ;
    private static Properties theProps ;
    int invoke_a = 0;
    int invoke_b = 0;
    int invoke_c = 0;
    int invoke_d = 0 ;
    //int ioCrossRef_a, ioCrossRef_b, ioCrossRef_c, ioCrossRef_d = 0 ;
    private static CSTAServer server ;
    private static IOServices ioservices ;
    private String xRef ;
    String invoke_id_ref ;
    StringBuffer wholeReceivedLayer7String ;
    private static TDSServer TDSserver ;
    private boolean CSTAUp ;
    
    public CSTA_Layer7(Properties _props) throws NoCSTAImplementationException{
        this.theProps = _props; 
        alog.info(this.getClass().getName() + " initialising ... creating Layer5") ;
        layer5 = null ;
        try{
            layer5 = new CSTA_Layer5(this,getTheProps()) ;
        }catch(NoCSTAImplementationException e){
            throw e ;
        }
        Init() ;
        if(layer5==null)
            alog.warn("layer5==null") ;
        else
        	;
    }
    
    private void Init(){
        ioservices = new IOServices(this) ;
        callEventHandler = new CallEventHandler() ;
        logicalDeviceFeatureEventHandler = new LogicalDeviceFeatureEventHandler() ;
    }
    
    public void CloseCSTAStack(){
    	try{
    		layer5.CloseCSTAStack();
	        layer5 = null ;
	        alog.warn("******** CSTA Stack is Closed Down *********\n\tR E S T A R T I N G ......") ;
	        if( server == null )
	            alog.warn("P A N I C!  -  Server is null, probably restarting") ;
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    public void ServerIntro(CSTAServer server){
        this.server = server ;
    }
    
    public void LoginResponse(){
    	System.out.println("About to send the login response to the first rose invoke") ;
        char[] roseResponsetoLogin = {0xa2, 0x0b, 0x02, 0x01, 0x01,
                + 0x30, 0x06, 0x02, 0x02, 0x00, 0xd3, 0x05, 0x00} ;
                StringBuffer rr2login = new StringBuffer( new String( roseResponsetoLogin ) );
                layer5.FromAbove( rr2login ) ;
    }
    
    public boolean Login(){
    	layer5.Login();
    	//Wait for a successful log on, say 10 seconds
    	for( int  i = 0 ; i < 100 ; i++){
            alog.info("Loop to check if csta is up") ;
    		try {
    			Thread.sleep(200) ;
    		} catch (InterruptedException e) {
    			e.printStackTrace();
    		}
            if( this.isCSTAUp() ){
                return true ;
            }
//    		if( this.getJobCount() > 0 ){
//    			System.out.println("Logging in and there are " + Integer.toString( getJobCount() ) + "jobs waiting") ;
//    			StringBuffer spsb = layer5.getLayer5JobIn() ;
//    			layer5.PassedUp(spsb) ;
//    		}
    	}
        alog.warn(this.getClass().getName() + " is returning false for login") ;
        return false ;
//        if( layer5.isLoggedIn() )
//            return true ;
//        else{
//            return false ;
//        }
    }
    
    
    public boolean FromBelow( StringBuffer curInStr ){
        //KEEP A COPY OF THE STRINGBUFFER FOR THE CLIENT TO PROCESS
        wholeReceivedLayer7String = curInStr ;
        
        if(curInStr.charAt(0) == 0xA1){
            alog.info("Received a CSTA Event, Agent Event or TDS Data Event") ;
            //curInStr = curInStr.deleteCharAt(0).deleteCharAt(0) ;
            ;//if( curInStr.charAt(0) != 0x02 )
            //	curInStr = curInStr.deleteCharAt(0) ;
        }
        if( curInStr.charAt(0) == 0xA2 ||
                curInStr.charAt(0) == 0xA3 ||
                curInStr.charAt(0) == 0xA4 ){
            //RETRIEVE INVOKE_ID HERE RATHER THAN EARLIER, BECAUSE
            //IF IT IS AN EVENT, IT'S JUST A WASTE OF TIME
            curInStr = GrabInvokeID(curInStr) ;
            alog.info("MYLO - curInStr/wholeReceivedL7Str/invoke_id_ref|||"+curInStr.length()+"|"+wholeReceivedLayer7String.length()+"|"+invoke_id_ref.length()) ;
            //Send it to the client
            if( server == null){
                alog.warn("Server is null") ;
            }
            server.ToClient(wholeReceivedLayer7String, invoke_id_ref) ;
            //System.out.println("Layer7->server.ToClient()") ;
            //general_log.log(Level.INFO,"Layer 7 -> server.ToClient()") ;
        }
        return WorkString(curInStr) ;
    }
    
    private StringBuffer GrabInvokeID(StringBuffer curInStr){
        
        curInStr = CheckLengthAndStrip(curInStr, 2) ;
        
        //This method gets the invoke_id and puts it into the layer7
        //variable 'invoke_id_ref' so that it can be passed to whoever
        //needs it later.
        //Format for this method to work needs to be:
        //0x02 0x0X 0xvalue
        int length = (int)curInStr.charAt(1) ;
        //System.out.println("Getting INVOKE ID TO A STRING") ;
        String invoke_id = curInStr.substring(2,(2+length)) ;
        //2 is the start index,length = length & +1 is cos it is exclusive.
        invoke_id_ref = invoke_id ;
        //System.out.println("invoke_id_ref: " + invoke_id_ref) ;
        
        curInStr = DeleteChars(curInStr, (2 + length) ) ;
        return curInStr ;
    }
    
    private boolean WorkString(StringBuffer curInStr){
        //formerly PassedUp, then RoseResultPassedUp
        
        boolean untouched = true ;
        
        /*Enter the break-down-string loop*/
        while(curInStr.length() != 0){
            
            if( curInStr.charAt(0) == 0xA1 && untouched == true ){
                //USING THIS METHOD TO STRIP DOWN THE INVOKE_ID.  THE INV_ID
                //IS NOT USED FOR A ROSE_INVOKE - USUALLY AN EVENT.  IN THIS
                //CASE A CROSS_REF_ID IS USED>
                curInStr = GrabInvokeID(curInStr) ;
                
                untouched = false ;
            }
            
            else if(curInStr.charAt(0) == 0x02){//WHICH SERVICE ITIS
                //System.out.println("Service ID ") ;
                
                int length = (int)curInStr.charAt(1) ;
                
                if( curInStr.charAt(2) == 0x15 ){//CSTA EVENT
                    curInStr = DeleteChars(curInStr, 3) ;
                    CSTAEventReceived(curInStr) ;
                } else if( curInStr.charAt(2) == 0x6E )//START DATA PATH
                    //start data path
                    ioservices.StartDataPathReceived(curInStr, invoke_id_ref) ;
                else if( curInStr.charAt(2) == 0x70 )//SEND DATA
                    //Send Data
                    ioservices.TDSSendDataReceived(curInStr, invoke_id_ref) ;
                else if( curInStr.charAt(2) == 0x6F )//STOP DATA PATH
                    //Stop data path
                    ioservices.StopDataPathReceived(curInStr, invoke_id_ref) ;
                else
                    curInStr = DeleteChars(curInStr, (length+2)) ;
                
                //Anything else, we don't really care
                curInStr = new StringBuffer() ;
            }
            
            else if(curInStr.charAt(0) == 0x30){//SEQUENCE
                curInStr = CheckLengthAndStrip(curInStr, 2) ;
            }
            
            else if(curInStr.charAt(0) == 0x6B){//CALL ID
                curInStr = curInStr.deleteCharAt(0).deleteCharAt(0) ;
                if(curInStr.charAt(0) == 0x30)//SEQUENCE
                    curInStr = curInStr.deleteCharAt(0).deleteCharAt(0) ;
                if(curInStr.charAt(0) == 0x80){//CALL ID
                    int length = (int)curInStr.charAt(1) ;
                    for(int i = 0 ; i < length+2 ; i++)
                        curInStr = curInStr.deleteCharAt(0) ;
                }
            }
            
            else if(curInStr.charAt(0) == 0x05)//NO DATA NULL
                curInStr = DeleteChars(curInStr, 2) ;
            
            else if(curInStr.charAt(0) == 0xA1){//STATICID FOR THE DEVICE
                if( curInStr.charAt(2) == 0x30 )
                    curInStr = DeleteChars(curInStr, 2) ;
                else{
                    int length = (int)curInStr.charAt(3) ;
                    curInStr = DeleteChars(curInStr, (4+length)) ;
                }
            } else if(curInStr.charAt(0) == 0x55){
                //System.out.println("RECEIVED 0x55") ;
                curInStr = new StringBuffer() ;
            } else if(curInStr.charAt(0) == 0x80){
                int length = (int)curInStr.charAt(1) ;
                length += 2 ;
                curInStr = DeleteChars(curInStr, length) ;
            } else{
                //error in comms. probably
                StringContains(curInStr, "\n\n\nbad comms") ;
                alog.warn("Bad comms. Probably missing bytes - Trashing this string") ;
                curInStr = new StringBuffer() ;
            }
        }
        return true ;
    }
    
    /**
     *
     *
     *
     * @param curInStr
     */
    private void CSTAEventReceived(StringBuffer curInStr){
        
        //System.out.println("\n\nCSTAEventReceived!!!\n\n") ;
        
        //curInStr = DeleteChars(curInStr, 3) ;
        if( curInStr.charAt(0) == 0x30){
            curInStr = CheckLengthAndStrip(curInStr, 2) ;
        }
        
        //NOTE:: CLIENT RECEIVES STRING THAT LOOKS LIKE FROM THIS MOMENT
        
        //PUT IN WHICH CROSS REF ID IT WAS e.g. 00 00
        if ( curInStr.charAt(0) == 0x55 ){
            int length = (int)curInStr.charAt(1) ;
            xRef = curInStr.substring(2,(length+2)) ;
            server.EventToClient(curInStr, xRef) ;
            curInStr = DeleteChars(curInStr, 4) ;
        } else
            //System.out.println("PROBLEM WITH A SHOULD-BE 0x55") ;
            alog.warn("Problem with a should-be 0x55") ;
        
        //TEST.StringContains(curInStr, "EVENT::CharAt(0)==0xA0") ;
        if( curInStr.charAt(0) == 0xA0 ){//call event
            //RM13MARCH - re: LLFU
            //if( curInStr.charAt(1) == 0x82 )
            //      curInStr = DeleteChars(curInStr, 4) ;
            //else
            //      curInStr = DeleteChars(curInStr, 2) ;
            curInStr = CheckLengthAndStrip(curInStr, 2) ;
            //TEST.Success() ;
            //CallEvent currentEvent = callEventHandler.WorkEvent(curInStr, xRef) ;
            CallEvent_Base currentEvent = callEventHandler.WorkEvent(curInStr) ;
            try{
                System.out.println( currentEvent.toString() ) ;
            }catch(NullPointerException e){
                System.out.println("\n\n\n\tNULL POINTER EXCEPTION THROWN\n") ;
                e.printStackTrace() ;
            }
        } else if( curInStr.charAt(0) == 0xA3 ){//hookswitch event
            //System.out.println("First  char is A3, hookswitch event??") ;
            //TEST.Fail() ;
            return ;
        } else if( curInStr.charAt(0) == 0xA4 ){//logicalDeviceFeature Event
            curInStr = CheckLengthAndStrip(curInStr,2) ;
            AgentEvent_Base currentEvent = logicalDeviceFeatureEventHandler.WorkEvent(curInStr) ;
            try{
                System.out.println( currentEvent.toString() ) ;
            }catch(NullPointerException e){
                System.out.println("\n\n\n\tNULL POINTER EXCEPTION THROWN\n") ;
                e.printStackTrace() ;
            }
        } else
            return ;
        
        //server.EventToClient(curInStr, xRef) ;
        
        //		switch (curInStr.charAt(0)){
        //                    case 0xA2:
        //                        new ConferencedEvent("Conferenced Event", xRef, curInStr) ;
        //System.out.println("Conferenced Event") ;
        //                        break ;
        //                    case 0xA3:
        //                        new Event_Type_I("Connection Cleared", xRef, curInStr) ;
        //ConnectionCleared(curInStr, xRef) ;
        //                        break ;
        //                    case 0xA4:
        //                        new Event_Type_II("Delivered Event", xRef, curInStr) ;
        //DeliveredEvent(curInStr, xRef) ;
        //                        break ;
        //                    case 0xA6:
        //new Event_Type_II("Diverted Event", xRef, curInStr) ;
        //                        System.out.println("Diverted Event") ;
        //                        break ;
        //                    case 0xA7:
        //                        new Event_Type_II("Established Event", xRef, curInStr) ;
        //EstablishedEvent(curInStr, xRef) ;
        //                        break ;
        //                    case 0xA8:
        //                        System.out.println("Failed Event") ;
        //                        break ;
        //                    case 0xA9:
        //                        new Event_Type_I("Held", xRef, curInStr) ;
        //HeldEvent(curInStr, xRef) ;
        //                        break ;
        //                    case 0xAB:
        //                        new Event_Type_II("Network Reached", xRef, curInStr) ;
        //System.out.println("Network Reached") ;
        //                        break ;
        //                    case 0xAD:
        //                        System.out.println("Originated Event") ;
        //                        break ;
        //                    case 0xAE:
        //                        System.out.println("Queued Event") ;
        //                        break ;
        //                    case 0xAF:
        //                        new Event_Type_I("Retrieved", xRef, curInStr) ;
        //RetrievedEvent(curInStr, xRef) ;
        //                        break ;
        //                    case 0xB0:
        //                        new Event_Type_I("Service Initiated", xRef, curInStr) ;
        //ServiceInitiated(curInStr, xRef) ;
        //                        break ;
        //                    case 0xB1:
        //System.out.println("Transferred Event") ;
        //                        new TransferredEvent("Transferred Event", xRef, curInStr) ;
        //                        break ;
        //		}
        //System.out.println("Clearing String Layer7...NORMAL") ;
        //		curInStr = new StringBuffer() ;
    }
    
    

    
/*
        private void StartDataPathReceived(StringBuffer curInStr, String inv_id){
                System.out.println("Start Data Path") ;
 
                //Get rid of the service ID section
                curInStr = DeleteChars(curInStr, 3) ;
 
                //get the device this is data path is for
                int length = curInStr.charAt(5) ;
                String tmp_dev = curInStr.substring(6, (length+6)) ;
                System.out.println("DEVICE = " + tmp_dev) ;
                curInStr = DeleteChars( curInStr, (6+length) ) ;
 
                //September 15* Just get the last digit to find out which
                // TDS code was pressed
                length = curInStr.length() ;
                String TDScode = curInStr.substring( (length-1), length ) ;
                System.out.println("TDS Code pressed was: " + TDScode) ;
 
                //got to return a response to this request
                StartDataPathResponse(tmp_dev, TDScode, inv_id) ;
        }
 
        private void StopDataPathReceived(StringBuffer curInStr, String inv_id){
            System.out.println("Stop Data Path Received") ;
            curInStr = DeleteChars(curInStr, 5) ;
 
            String tmp_ioXref = null ;
 
            if( curInStr.charAt(0) == 0xA1 ){
                int length = (int)curInStr.charAt(1) ;
                length -= 2 ;
                tmp_ioXref = curInStr.substring(4, (4+length)) ;
            }
            //TEST.StringContains(tmp_ioXref, "Stop Data on ioCrossRef:") ;
        }
 
        private void StartDataPathResponse(String device, String TDScode, String inv_id){
            System.out.println("Start Data Path Response") ;
            StringBuffer strbuf = new StringBuffer() ;
            char[] id_0 = {INTEGER, 0x01, 0x6E} ;
            String id = new String(id_0) ;
 
            String ioXref = IOCrossRefGeneration() ;
            int length = ioXref.length() ;
            System.out.println("Length of the IOCROSSREF: " + Integer.toString(length)) ;
            strbuf = strbuf.insert(0, ioXref).insert(0,(char)length).insert(0,(char)0x04) ;
            length+=2 ;
            strbuf = strbuf.insert(0, (char)length).insert(0,(char)0xA1) ;
            strbuf = Sequence(strbuf) ;
            strbuf = strbuf.insert(0, id) ;
            strbuf = Sequence(strbuf) ;
 
            //char[] asdf = {0x30, 0x0a, 0x02, 0x01, 0x6e, 0x30, 0x05, 0xa1,
            //                + 0x03, 0x04, 0x01, 0x01} ;
            //String as = new String(asdf) ;
            //strbuf = strbuf.insert(0, as) ;
 
            //TEST.StringContains(inv_id, "TDS response invoke id" ) ;
            //TEST.StringContains(ioXref, "IO Cross Ref id") ;
            TDSserver.NewTDStransmission(ioXref, device, TDScode) ;
            length = inv_id.length() ;
            strbuf = strbuf.insert(0, inv_id).insert(0, (char)length).insert(0, (char)0x02) ;
            System.out.println("TDS: sending response to TDS request") ;
            Wrap2(strbuf) ;
        }
 
        private void TDSSendDataReceived(StringBuffer curInStr, String invoke_id_ref){
            System.out.println("TDS Send Data Received") ;
            String ioXref = null;
            String dataSent = null;
 
            curInStr = DeleteChars(curInStr, 5) ;
            //TEST.StringContains(curInStr, "TDS Just deleted 5 chars") ;
            if( curInStr.charAt(0) == 0xA1 ){
                //First is the ioCrossRef then the data, this is the
                 //getting of the ioCrossRef
                int length = (int)curInStr.charAt(3) ;
                //TEST.StringContains(curInStr, "Testing SendDataRecd") ;
                ioXref = curInStr.substring(4, (length+4)) ;
                curInStr = DeleteChars(curInStr, (length+4) ) ;
            }
            if( curInStr.charAt(0) == 0x04 ){
                //This is the data//
                int length = (int)curInStr.charAt(1) ;
                dataSent = curInStr.substring(2, (length+2) ) ;
                curInStr = DeleteChars(curInStr, (length+2) ) ;
            }
            if(curInStr.length() != 0)
                System.out.println("SendDataReceived, end of data - still more");
 
            //TEST.StringContains(ioXref,"IO Cross Ref ID") ;
            //TEST.StringContains(dataSent,"Data") ;
            TDSserver.SendData(ioXref, dataSent) ;
        }
 */
    /**
     * Upwards takes care of RoseInvoke part of response
     *
     *
     * @return the string with the rose invoke portion removed
     * @param sb the currently worked string
     */
    public StringBuffer Rose_Invoke(StringBuffer sb){
        String invoke_id = InvokeGeneration() ;
        sb = sb.insert(0,invoke_id).insert(0,(char)invoke_id.length()).insert(0,INTEGER) ;
        sb = sb.insert(0,(char)sb.length()).insert(0,ROSE_INVOKE) ;
        return sb ;
    }
/*
        private String IOCrossRefGeneration(){
            String ioCrossRef_id = new String() ;
            ioCrossRef_a++ ;
 
            if( ioCrossRef_a > 255 ){
                    ioCrossRef_a = 0 ;
                    ioCrossRef_b++ ;
                    if( ioCrossRef_b > 255 ){
                            ioCrossRef_b = 0 ;
                            ioCrossRef_c++ ;
                            if( ioCrossRef_c > 255 ){
                                    ioCrossRef_c = 0 ;
                                    ioCrossRef_d++ ;
                                    if( ioCrossRef_d > 255 ){
                                            ioCrossRef_d = 0 ;
                                            ioCrossRef_a = 1 ;
                                    }
                            }
                    }
            }
 
            if( ioCrossRef_d > 0 ){
                char[] ioID = { (char)ioCrossRef_d, (char)ioCrossRef_c, (char)ioCrossRef_b, (char)ioCrossRef_a } ;
                ioCrossRef_id = new String(ioID) ;
                return ioCrossRef_id ;
            }
            else if( ioCrossRef_c > 0 ){
                char[] ioID = { (char)ioCrossRef_c, (char)ioCrossRef_b, (char)ioCrossRef_a } ;
                ioCrossRef_id = new String(ioID) ;
                return ioCrossRef_id ;
            }
            else if( ioCrossRef_b > 0 ){
                char[] ioID = { (char)ioCrossRef_b, (char)ioCrossRef_a } ;
                ioCrossRef_id = new String(ioID) ;
                return ioCrossRef_id ;
            }
            else if( ioCrossRef_a > 0 ){
                char[] ioID = { (char)ioCrossRef_a } ;
                ioCrossRef_id = new String(ioID) ;
                return ioCrossRef_id ;
            }
 
            return ioCrossRef_id ;
        }
 */
    
    /**
     *
     *
     *
     * @return
     */
    private String InvokeGeneration(){
        String invoke_id = new String() ;
        invoke_a++ ;
        
        if( invoke_a > 255 ){
            invoke_a = 0 ;
            invoke_b++ ;
            if( invoke_b > 255 ){
                invoke_b = 0 ;
                invoke_c++ ;
                if( invoke_c > 255 ){
                    invoke_c = 0 ;
                    invoke_d++ ;
                    if( invoke_d > 255 ){
                        invoke_d = 0 ;
                        invoke_a = 1 ;
                    }
                }
            }
        }
        
        if( invoke_d > 0 ){
            char[] invID = { (char)invoke_d, (char)invoke_c, (char)invoke_b, (char)invoke_a } ;
            invoke_id = new String(invID) ;
            return invoke_id ;
        } else if( invoke_c > 0 ){
            char[] invID = { (char)invoke_c, (char)invoke_b, (char)invoke_a } ;
            invoke_id = new String(invID) ;
            return invoke_id ;
        } else if( invoke_b > 0 ){
            char[] invID = { (char)invoke_b, (char)invoke_a } ;
            invoke_id = new String(invID) ;
            return invoke_id ;
        } else if( invoke_a > 0 ){
            char[] invID = { (char)invoke_a } ;
            invoke_id = new String(invID) ;
            return invoke_id ;
        }
        
        return invoke_id ;
    }
    
    /**
     * Monitors a device
     *
     *
     * @return string to send to the pabx
     * @param device device to monitor
     */
    public StringBuffer MonitorStart2(String device){
        char[] id_0 = {0x02, 0x01, 0x47} ;
        String id = new String(id_0) ;
        StringBuffer str = new StringBuffer() ;
        
        char[] longString = {0x80, 0x04, 0x06, 0x00, 0x00, 0x00, 0x86, 0x02, 0x03, 0x40, 0x87, 0x02, 0x06, 0x00, 0x88, 0x03, 0x05, 0x54, 0x00,
                + 0x89, 0x03, 0x02, 0xFC, 0x18, 0x83, 0x02, 0x05, 0xC0, 0x85, 0x02, 0x01, 0xC0, 0x84, 0x02, 0x07, 0x00} ;
                
                /*char[] longString = {0x80, 0x04, 0x06, 0x40, 0x04, 0x00, 0x86, 0x02, 0x03, 0x40, 0x87, 0x02, 0x06, 0x00, 0x88, 0x03, 0x05, 0x54, 0x00,
                                + 0x89, 0x03, 0x02, 0xFC, 0x18, 0x83, 0x02, 0x05, 0xC0, 0x85, 0x02, 0x01, 0xC0, 0x84, 0x02, 0x07, 0x00} ;
                DOES NOT LISTEN TO TRANSFER OR CONFERENCE*/
                String guff = new String(longString) ;
                str = str.insert(0,guff) ;
                str = str.insert(0,(char)str.length()).insert(0,'\u00A0') ;
                str = Device(str, device) ;
                str = Sequence(str) ;
                str = str.insert(0,id) ;
                return str ;
    }
    
    /**
     * monitor that does not watch for conferenced or transferred events
     * maybe the description is wrong on this one actually...oops
     *
     * @return string to send to the pabx
     * @param device device to watch
     * @deprecated check this method with the other one that doesn't watch for transferred or conferenced events
     */
    public StringBuffer MonitorStart_LogicalDeviceFeatures(String device){
        char[] id_0 = {0x02, 0x01, 0x47} ;
        String id = new String(id_0) ;
        StringBuffer str = new StringBuffer() ;
        
        char[] longString = {0x80, 0x04, 0x06, 0x7F, 0xFC, 0x00, 0x86, 0x02, 0x03, 0x40, 0x87, 0x02, 0x06, 0x00, 0x88, 0x03, 0x05, 0x54, 0x00,
                + 0x89, 0x03, 0x02, 0x00, 0x00, 0x83, 0x02, 0x05, 0xC0, 0x85, 0x02, 0x01, 0xC0, 0x84, 0x02, 0x07, 0x00} ;
                
                /*char[] longString = {0x80, 0x04, 0x06, 0x40, 0x04, 0x00, 0x86, 0x02, 0x03, 0x40, 0x87, 0x02, 0x06, 0x00, 0x88, 0x03, 0x05, 0x54, 0x00,
                                + 0x89, 0x03, 0x02, 0xFC, 0x18, 0x83, 0x02, 0x05, 0xC0, 0x85, 0x02, 0x01, 0xC0, 0x84, 0x02, 0x07, 0x00} ;
                DOES NOT LISTEN TO TRANSFER OR CONFERENCE*/
                String guff = new String(longString) ;
                str = str.insert(0,guff) ;
                str = str.insert(0,(char)str.length()).insert(0,'\u00A0') ;
                str = Device(str, device) ;
                str = Sequence(str) ;
                str = str.insert(0,id) ;
                return str ;
    }
    
    /**
     * Monitor that does not watch conferenced or transferred events
     * @param device device to monitor
     * @return string to send to the pabx
     * @deprecated check this method with the other one that doesn't watch for transferred or conferenced events
     */
    public StringBuffer MonitorStart_LogicalDeviceFeaturesAndCalls(String device){
        char[] id_0 = {0x02, 0x01, 0x47} ;
        String id = new String(id_0) ;
        StringBuffer str = new StringBuffer() ;
        
        char[] longString = {0x80, 0x04, 0x06, 0x00, 0x00, 0x00, 0x86, 0x02, 0x03, 0x40, 0x87, 0x02, 0x06, 0x00, 0x88, 0x03, 0x05, 0x54, 0x00,
                + 0x89, 0x03, 0x02, 0x00, 0x00, 0x83, 0x02, 0x05, 0xC0, 0x85, 0x02, 0x01, 0xC0, 0x84, 0x02, 0x07, 0x00} ;
                
                /*char[] longString = {0x80, 0x04, 0x06, 0x40, 0x04, 0x00, 0x86, 0x02, 0x03, 0x40, 0x87, 0x02, 0x06, 0x00, 0x88, 0x03, 0x05, 0x54, 0x00,
                                + 0x89, 0x03, 0x02, 0xFC, 0x18, 0x83, 0x02, 0x05, 0xC0, 0x85, 0x02, 0x01, 0xC0, 0x84, 0x02, 0x07, 0x00} ;
                DOES NOT LISTEN TO TRANSFER OR CONFERENCE*/
                String guff = new String(longString) ;
                str = str.insert(0,guff) ;
                str = str.insert(0,(char)str.length()).insert(0,'\u00A0') ;
                str = Device(str, device) ;
                str = Sequence(str) ;
                str = str.insert(0,id) ;
                return str ;
    }
    
    /**
     * Stops monitoring the specified cross reference id
     *
     *
     * @return the string to send to the pabx
     * @param crossRef cross reference ID associated with the monitor to stop
     */
    public StringBuffer CallControl_Services_MonitorStop(String crossRef){
        char[] id_0 = {0x02, 0x01, 0x49} ;
        String id = new String(id_0) ;
        StringBuffer str = new StringBuffer() ;
        
        str = str.insert(0, crossRef) ;
        str = str.insert(0, (char)str.length()).insert(0, (char)0x55) ;
        str = Sequence(str) ;
        str = str.insert(0,id) ;
        return str ;
    }
    
    /**
     * Monitor start that only watches for Service Initiated and Connection Cleared
     *
     *
     * @return the string to send to the pabx
     * @param device the device to monitor
     */
    public StringBuffer MonitorStart_SI_CC_only(String device){
        char[] id_0 = {0x02, 0x01, 0x47} ;
        String id = new String(id_0) ;
        StringBuffer str = new StringBuffer() ;
        
        char[] longString = {0x80, 0x04, 0x06, 0x5F, 0xF4, 0x00, 0x86, 0x02, 0x03, 0x40, 0x87, 0x02, 0x06, 0x00, 0x88, 0x03, 0x05, 0x54, 0x00,
                + 0x89, 0x03, 0x02, 0xFC, 0x18, 0x83, 0x02, 0x05, 0xC0, 0x85, 0x02, 0x01, 0xC0, 0x84, 0x02, 0x07, 0x00} ;
                
                String guff = new String(longString) ;
                str = str.insert(0,guff) ;
                str = str.insert(0,(char)str.length()).insert(0,'\u00A0') ;
                str = Device(str, device) ;
                str = Sequence(str) ;
                str = str.insert(0,id) ;
                
                return str ;
                
    }
    
    
        /**
     * Allows this class to have knowledge of the CSTAServer - it's superior, and of the TDSServer - handy.
     *
     *
     * @param server the csta server
     * @param TDSserver the tds server
     */
    public void ServerIntro(CSTAServer server, TDSServer TDSserver){
        this.server = server ;
        this.TDSserver = TDSserver ;
    }
    
        /**
     * Not a very tested method
     *
     *
     * @param dev device
     * @param TDScode tds code
     * @deprecated maybe time to look at these old methods again
     */
    public void TDSEarlyTerminationData(String dev, String TDScode){
        //StringBuffer sb = new StringBuffer() ;
        //sb = Device(sb, "200") ;
        //int length = TDScode.length() ;
        //sb = sb.append(0x81).append(0x01).append(TDScode) ;
        TDSserver.TDSEarlyTerminationDataToClients(dev,TDScode) ;
    }
    
        /**
     * Sends the data portion of TDS communications
     *
     *
     * @param ioCrossRef io cross reference ID with the TDS
     * @param data data portion of TDS
     */
    public void DataToTDSServer(String ioCrossRef, String data){
        TDSserver.SendData(ioCrossRef,data) ;
    }
    
    /**
     * old method that converts a string to a stringbuffer and wraps it
     *
     *
     * @param asdf the string to convert
     * @deprecated time to sort through this old code
     */
    public void specialString(String asdf){
        StringBuffer tmp = new StringBuffer(asdf) ;
        Wrap(tmp) ;
    }
    
    /**
     * Wraps the layer 7 with a rose invoke
     *
     *
     * @param sb string to be wrapped
     */
    public void Wrap(StringBuffer sb){
        sb = sb.insert(0,(char)sb.length()).insert(0,ROSE_INVOKE) ;
        
        //WriteToLog(sb, 'S') ;
        //APRIL30LOG
//        if( layer5.FromAbove(sb) )
//            ;
//        else
//            //System.out.println("Sent from L7::Wrap FAILURE") ;
//            alog.warn("Sent from L7::Wrap FAILURE - another unlucky to get here message") ;
        layer5.FromAbove(sb) ;
    }
    
    /**
     * Wraps the layer 7 with a rose result
     *
     *
     * @param sb string to be wrapped
     */
    public void Wrap2(StringBuffer sb){
        sb = sb.insert(0,(char)sb.length()).insert(0,ROSE_RESULT) ;
        
        //WriteToLog(sb, 'S') ;
        //APRIL30LOG
//        if( layer5.FromAbove(sb) )
//            ;
//        else
            //System.out.println("Sent from L7::Wrap FAILURE") ;
//            alog.warn("Sent from L7::Wrap FAILURE  -  Unlucky to get here") ;
        layer5.FromAbove(sb) ;
    }
    
    /**
     * Generates the ROSE_INVOKE for the string that is ready to send to the pabx.
     *
     *
     * @return complete string after the rose invoke has been inserted
     * @param sb the string that will be sent after RoseInvoke is inserted
     */
    public String Get_Rose_Invoke(StringBuffer sb){
        String invoke_id = InvokeGeneration() ;
        return invoke_id ;
    }
    
    
    /**
     * Method generates the log off string
     *
     *
     * @return boolean in the line of communications
     */
    public boolean LogOff(){
        char[] logOffchars = {0x64, 0x06, 0x80, 0x01, 0x00, 0x81, 0x01, 0x01} ;
        String logOffStr = new String(logOffchars) ;
        StringBuffer logOffStr2 = new StringBuffer(logOffStr) ;
//        if( layer5.FromAbove(logOffStr2) ){
//            layer5.setLoggedIn(false) ;
//            return true ;
//        } else
//            return false ;
        layer5.FromAbove(logOffStr2) ;
        layer5.setLoggedIn(false) ;
        return true ;
        
        
    }
    
    public int getJobCount(){
    	return layer5.getSizeLayer5WorkIN() ;
    }
    
    public void doWork(){
    	StringBuffer sb = layer5.getLayer5JobIn() ;
    	layer5.PassedUp(sb) ;
    }
    /**
     * starts the telephone data service abilities
     *
     */
    public void startTDS(){
        alog.info("Starting TDS service ...") ;
        StringBuffer sb = ioservices.escapeService_TDSStart() ;
        String inv_id = Get_Rose_Invoke(sb) ;
        sb = sb.insert(0, inv_id).insert(0,(char)inv_id.length()).insert(0,INTEGER) ;
        Wrap(sb) ;
    }

    public boolean isCSTAUp() {
        return CSTAUp;
    }

    public void setCSTAUp(boolean CSTAUp) {
        this.CSTAUp = CSTAUp;
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