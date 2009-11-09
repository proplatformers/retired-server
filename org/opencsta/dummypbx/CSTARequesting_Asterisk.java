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
import java.util.Hashtable;
import java.util.List;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.event.ManagerEvent;

/**
 *
 * @author cm
 */
public class CSTARequesting_Asterisk implements AsteriskInterest,Runnable{
    Logger alog = Logger.getLogger(DummyPBX.class.getName()) ;
    private AsteriskController ast ;
    private List<CSTARequest> cstaRequests ;
    public CSTARequesting_Asterisk(String _host){
        ast = new AsteriskController(this, _host) ;
        try {
            ast.HelloManager();
        } catch (IOException ex) {
            alog.error(ex.toString()) ;
        }
        Init() ;
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

    }
    
    public void run(){
        while( ast.isLoggedIn() ){
            try {
                System.out.println("Waiting for RQs") ;
                    if( cstaRequests.size() > 0 ){

                    }
                    Thread.currentThread().sleep(2000) ;

                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
        }
    }

    public void addCSTARequest(CSTARequest creq){
        cstaRequests.add(creq);
    }

    public void AsteriskManagerEventReceived(ManagerEvent me) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void CallControlService(CSTARequest cstareq){
        //ast call generic function which passes arguments in List,Hashtable!

    }

}
