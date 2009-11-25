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

import com.managedservicesaustralia.asterisk.AsteriskController;
import com.managedservicesaustralia.asterisk.AsteriskInterest;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.event.ManagerEvent;



//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.ServerSocket;
//import java.net.Socket;

public class DummyPBX implements AsteriskInterest {
    Logger alog = Logger.getLogger(DummyPBX.class.getName()) ;
    private CSTARequesting_Asterisk crasterisk ;
//    private List<CSTARequest> cstaRequests ;
    private DummyL7 dl7 = null ;
    private boolean loggedIn = false ;
    //TODO database hook
    public static void main(String[] args){
            DummyPBX dummy = new DummyPBX() ;
    }
    public DummyPBX(){
        crasterisk = new CSTARequesting_Asterisk("127.0.0.1") ;
        Init() ;
        System.out.println("DummyPBX Initialising") ;
        dl7 = new DummyL7(this) ;
        run() ;
    }


    public DummyPBX(String _host){
        crasterisk = new CSTARequesting_Asterisk(_host) ;
        Init() ;
        System.out.println("DummyPBX Initialising") ;
        dl7 = new DummyL7(this) ;
        run() ;
    }

    private void Init(){
        publicPreInit() ;
        initialise() ;
        publicPostInit() ;
    }

    public void publicPreInit(){

    }

    public void publicPostInit(){
        
    }

    private void initialise(){
//        cstaRequests = Collections.synchronizedList( new LinkedList<CSTARequest>() );
    }

    public void run(){
            System.out.println("DummyPBX has started...") ;
            while( !isLoggedIn() ){
                    System.out.println("DummyPBX not logged in, accepting connections") ;
                    dl7.acceptLogins() ;
            }
            while( isLoggedIn() ){
                    try {
                        System.out.println("Waiting for RQs") ;
                        if( dl7.isWorkIN() ){
                            asn1convert( dl7.getNextWorkIN() ) ;
                        }
//                        if( cstaRequests.size() > 0 ){
//
//                        }
                        Thread.currentThread().sleep(2000) ;

                    } catch (InterruptedException e) {

                        e.printStackTrace();
                    }
            }
            dl7 = new DummyL7(this) ;
    }
    public boolean isLoggedIn() {
            return loggedIn;
    }
    public void setLoggedIn(boolean loggedIn) {
            this.loggedIn = loggedIn;
    }

    private void asn1convert(StringBuffer sb){
        System.out.println("DummyPBX is going to convert asn1 to csta") ;
        dl7.TestChris(sb);
        dl7.WorkString(sb);

    }

    public void addCSTARequest(CSTARequest creq){
        crasterisk.addCSTARequest(creq);
    }

    public void AsteriskManagerEventReceived(ManagerEvent me) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
