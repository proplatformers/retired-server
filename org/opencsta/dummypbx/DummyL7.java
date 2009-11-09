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

package org.opencsta.dummypbx;

import org.opencsta.servicedescription.common.helpers.CSTA_Layer_7_Common;

public class DummyL7 extends CSTA_Layer_7_Common{
        private DummyPBX server ;
        private DummyL5 dl5 ;
        private boolean runFlag = true ;
        private static Thread dl5Thread ;
        private boolean expectAARQ = true ;
        private boolean expectROSERESPONSE = false ;
        StringBuffer wholeReceivedLayer7String ;

        public DummyL7(DummyPBX _server){
            this.server = _server ;
            dl5 = new DummyL5(this) ;
            dl5Thread = new Thread(dl5,"Dummy Layer 5 Thread") ;
            dl5Thread.start() ;
        }

        public void TestChris(StringBuffer cm){
                System.out.print("DL7 TESTING STRING") ;
                for( int i = 0 ; i < cm.length() ; i++ ){
                        System.out.print( Integer.toHexString((char)cm.charAt(i)) + " " ) ;
                }
                System.out.println("") ;
        }

        public void acceptLogins(){
                StringBuffer str ;
                while( !server.isLoggedIn() && isRunFlag() ){
                        System.out.println("Nobody logged in yet") ;
                        while( dl5.sizeOfWorkIN() > 0 ){
                                System.out.println("New messages in layer 5's IN worklist to be processed") ;
                                str = dl5.getWorkIN() ;
                                TestChris(str) ;
//                                str = dl5.strip(str) ;
//                                TestChris(str) ;
                                System.out.println("Is this an AARQ?") ;
                                if( isExpectAARQ() ){
                                        System.out.println("Checking first char") ;
                                        System.out.println( Integer.toHexString((int)str.charAt(0)) ) ;
                                        //TODO a better check for the AARQ is required
                                        if( str.charAt(0) == 0x60 ){
                                                setExpectAARQ(false) ;
                                                sendAARQRESPONSE() ;
                                                setExpectROSERESPONSE(true) ;
                                                sendROSEINVOKE() ;
                                        }
                                        else{
                                                System.out.println("It's not the expected character??") ;
                                        }
                                }
                                if( isExpectROSERESPONSE() ){
                                    //TODO capture the login information
                                    server.setLoggedIn(true) ;
                                }
                        }
            try{
                synchronized(this){
                    wait(2000) ;
                }
            }catch(InterruptedException e){
                //System.out.println("Lower Layer Thread interrupted") ;
            }catch(NullPointerException e2){
                e2.printStackTrace();
            }
                }

        }

        public boolean isExpectAARQ() {
                return expectAARQ;
        }

        public void setExpectAARQ(boolean expectAARQ) {
                System.out.println("Setting AARQ expectations to false, we've started a log in procedure") ;
                this.expectAARQ = expectAARQ;
        }

        public boolean isExpectROSERESPONSE() {
                return expectROSERESPONSE;
        }

        public void setExpectROSERESPONSE(boolean expectROSERESPONSE) {
                this.expectROSERESPONSE = expectROSERESPONSE;
        }

        private void sendAARQRESPONSE(){
                char[] tmp = {0x61, 0x48, 0xa1, 0x7, 0x6, 0x5, 0x2b, 0xc,
                                0x0, 0x81, 0x5a, 0xa2, 0x3, 0x2, 0x1, 0x0,
                                0xa3, 0x5, 0xa1, 0x3, 0x2, 0x1, 0x0, 0x88,
                                0x2, 0x6, 0x80, 0xaa, 0x21, 0xa2, 0x1f,
                                0xa0, 0x1d, 0xa1, 0x1b, 0x4, 0x10, 0x48,
                                0x45, 0x36, 0x34, 0x35, 0x50, 0x2e, 0x30,
                                0x30, 0x2e, 0x30, 0x32, 0x33, 0x3a, 0x36,
                                0x30, 0x4, 0x7, 0x30, 0x32, 0x33, 0x2e, 0x30,
                                0x30, 0x30, 0xbe, 0xa, 0x28, 0x8, 0xa0, 0x6,
                                0xa0, 0x4, 0x3, 0x2, 0x4, 0x10} ;
                String s = new String(tmp) ;
                dl5.addWorkOUT(new StringBuffer(s) ) ;
                synchronized( dl5 ){
                        dl5.notify() ;
                }
                System.out.println("Added the RQResponse to the outbound work list") ;
        }

        private void sendROSEINVOKE(){
                char[] tmp = {0xa1, 0xc, 0x2, 0x1, 0x1, 0x2, 0x2, 0x0, 0xd3, 0x30, 0x3, 0xa, 0x1, 0x2};
                String s = new String(tmp) ;
                dl5.addWorkOUT(new StringBuffer(s) ) ;
                synchronized( dl5 ){
                        dl5.notify() ;
                }
        }

        public boolean isRunFlag() {
                return runFlag;
        }

        public void setRunFlag(boolean runFlag) {
                this.runFlag = runFlag;
        }

        public boolean isWorkIN(){
            if( dl5.sizeOfWorkIN() > 0 )
                return true ;
            else
                return false ;
        }

        public StringBuffer getNextWorkIN(){
            StringBuffer str = dl5.getWorkIN() ;
            

            return str ;
        }

    protected boolean WorkString(StringBuffer curInStr){
        //formerly PassedUp, then RoseResultPassedUp
        wholeReceivedLayer7String = curInStr ;
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
            else if( curInStr.charAt(0) == 0xA2 && untouched == true ){
                //USING THIS METHOD TO STRIP DOWN THE INVOKE_ID.  THE INV_ID
                //IS NOT USED FOR A ROSE_INVOKE - USUALLY AN EVENT.  IN THIS
                //CASE A CROSS_REF_ID IS USED>
                //TODO need to put the invoke id into a meaningful variable
                curInStr = GrabInvokeID(curInStr) ;

                untouched = false ;
            }


            else if(curInStr.charAt(0) == 0x02){//WHICH SERVICE ITIS
                //System.out.println("Service ID ") ;

                int length = (int)curInStr.charAt(1) ;

                if( curInStr.charAt(2) == 0x15 ){//CSTA EVENT
                    curInStr = DeleteChars(curInStr, 3) ;
                    System.out.println("CSTA Event Received") ;
                    //CSTAEventReceived(curInStr) ;
                }
                else if( curInStr.charAt(2) == 0x33 ){
                    System.out.println("New SMART Client Connected to CSTA Server - needs extra capabilities") ;
                    //SEND response to this request

                }
                else if( curInStr.charAt(2) == 0x0a ){//make call
                    System.out.println("Make call") ;
                    server.addCSTARequest(new Make_Call_Request(wholeReceivedLayer7String)) ;
                    //create make call request, put into server's workpool, interrupt thread.log,report.done.
                }
                else if( curInStr.charAt(2) == 0x02 ){//answer call
                    System.out.println("Answer call") ;
                }
                else if( curInStr.charAt(2) == 0x09 ){//hold call
                    System.out.println("Hold call") ;
                }
                else if( curInStr.charAt(2) == 0x0e ){//retrieve call
                    System.out.println("retrieve call") ;
                }
                else if( curInStr.charAt(2) == 0x32 ){//single step transfer call
                    System.out.println("single step transfer call") ;
                }
                else if( curInStr.charAt(2) == 0xda ){//deflect call
                    System.out.println("deflect call") ;
                }
                else if( curInStr.charAt(2) == 0x05 ){//clear connection
                    System.out.println("Clear connection") ;
                }
                else if( curInStr.charAt(2) == 0x6E )//START DATA PATH
                    //start data path
                    System.out.println("Start Data path") ;
                    //ioservices.StartDataPathReceived(curInStr, invoke_id_ref) ;
                else if( curInStr.charAt(2) == 0x70 )//SEND DATA
                    //Send Data
                    System.out.println("Send Data") ;
                    //ioservices.TDSSendDataReceived(curInStr, invoke_id_ref) ;
                else if( curInStr.charAt(2) == 0x6F )//STOP DATA PATH
                    //Stop data path
                    System.out.println("Stop Data path") ;
                    //ioservices.StopDataPathReceived(curInStr, invoke_id_ref) ;
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
                System.out.println("Bad comms. Probably missing bytes - Trashing this string") ;
                curInStr = new StringBuffer() ;
            }
        }
        return true ;
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
        setInvoke_id_ref(invoke_id) ;  //sets the invoke_id_ref of layer 7 common
        //System.out.println("invoke_id_ref: " + invoke_id_ref) ;

        curInStr = DeleteChars(curInStr, (2 + length) ) ;
        return curInStr ;
    }


}
