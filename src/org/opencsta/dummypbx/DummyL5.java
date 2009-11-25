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



//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.io.IOException;
//import java.net.ServerSocket;
//import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List ;
public class DummyL5 implements Runnable{
        private DummyL7 dl7 ;
        private boolean runFlag = false ;
        List<StringBuffer> workIN ;
        List<StringBuffer> workOUT ;
        Thread tcpThread ;
        TCPServer pbxSocket ;

        public DummyL5(DummyL7 _dl7){
                this.dl7 = _dl7 ;
                workIN = Collections.synchronizedList( new LinkedList<StringBuffer>() );
                workOUT = Collections.synchronizedList( new LinkedList<StringBuffer>() );
                pbxSocket = new TCPServer(this) ;
                tcpThread = new Thread(pbxSocket, "The PBX socket") ;
                tcpThread.start();
        }

        public void run(){
                setRunFlag(true) ;
                while( isRunFlag() ){
            try{
                synchronized(this){
                    wait(5000) ;
                }
            }catch(InterruptedException e){
                //System.out.println("Lower Layer Thread interrupted") ;
            }catch(NullPointerException e2){
                e2.printStackTrace();
            }

            while( this.sizeOfWorkOUT() > 0 ){
                System.out.println("DL5 Outbound worklist has WORK!") ;
                StringBuffer newStrBuf  = this.getWorkOUT() ;
                send(newStrBuf) ;
            }
        }
                dl7.setRunFlag(false) ;
        }

        public boolean isRunFlag() {
                return runFlag;
        }

        public void setRunFlag(boolean runFlag) {
                this.runFlag = runFlag;
        }



    public boolean addWorkIN(StringBuffer str){
        str = strip(str) ;
        return workIN.add(str) ;
    }

    public StringBuffer getWorkIN(){
        return (StringBuffer)workIN.remove(0) ;
    }

    public int sizeOfWorkIN(){
        return workIN.size() ;
    }

    public boolean addWorkOUT(StringBuffer str){
        return workOUT.add(str) ;
    }

    public StringBuffer getWorkOUT(){
        return (StringBuffer)workOUT.remove(0) ;
    }

    public int sizeOfWorkOUT(){
        return workOUT.size() ;
    }

    public StringBuffer strip(StringBuffer str){
        return str.deleteCharAt(0).deleteCharAt(0).deleteCharAt(0) ;
    }

    public void send(StringBuffer str){
        str = wrap(str) ;
        pbxSocket.send( str.toString() ) ;
        System.out.println("Sent string DL5") ;
    }

    public StringBuffer wrap(StringBuffer str){
        int length = str.length() ;
        str = str.insert(0, (char)length).insert(0, (char)0x80).insert(0, (char)0x26) ;
        return str ;
    }

}