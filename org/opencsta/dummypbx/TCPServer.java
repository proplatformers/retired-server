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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer implements Runnable{

        private ServerSocket clientSideConnectionSocket;
        private Socket client;
        private DataInputStream in;
        private DataOutputStream out ;
        private int line;
        private boolean runFlag = false ;
        private StringBuffer chris ;
        private byte[] buf;
        private DummyL5 parent ;

        public TCPServer(DummyL5 _parent){
                this.parent = _parent ;
                chris = new StringBuffer() ;
                buf = new byte[1024] ;
        }

        public void run() {
                setRunFlag(true) ;
                System.out.println("Starting DummyPBX") ;
                try{
                        clientSideConnectionSocket = new ServerSocket(7000);
                } catch (IOException e) {
                        System.out.println("Could not listen on port 7000");
                        System.exit(-1);
                }

                try{
                        client = clientSideConnectionSocket.accept();
                        System.out.println("Accepted a client connection") ;
                } catch (IOException e) {
                        System.out.println("Accept failed: 7000");
                        System.exit(-1);
                }

                try{
                        in = new DataInputStream(client.getInputStream());
                        out = new DataOutputStream( client.getOutputStream() );
                } catch (IOException e) {
                        System.out.println("Accept failed: 4444");
                        System.exit(-1);
                }

                while(runFlag){
                        try{
                                buf = new byte[1024] ;
                                line = in.read(buf);
                                System.out.println(line + " bytes received") ;
                                buf2SBChris(line) ;
                        } catch (IOException e) {
                                System.out.println("Read failed");
                                this.setRunFlag(false) ;
                                parent.setRunFlag(false) ;
                        }
                }
                System.out.println("Run Flag for the TCP Server has been set to false") ;
        }

        public boolean isRunFlag() {
                return runFlag;
        }

        public void setRunFlag(boolean runFlag) {
                this.runFlag = runFlag;
        }

        public void send(String str_rec){
                try {
                        out.writeBytes( str_rec ) ;
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        private void buf2SBChris(int length){
                for( int i = 0 ; i < length ; i++){

            if( (short)buf[i] < 0 ){
                append2chris( (int)buf[i] + 256 ) ;
            }

            else{
                Byte b = new Byte(buf[i]) ;
                append2chris( (int) b.intValue() ) ;
            }
                }
                if( chris.length() > 5 ){
                        if( isBufferResetableAndEven(chris) ){
                                parent.addWorkIN(new StringBuffer(chris)) ;
                                chris = new StringBuffer() ;
                        }
                        else if( isBufferStillReading(chris) ){
                                ;
                        }
                        else if( isBufferHoldingMoreThanOneMessage(chris) ){
                                parent.addWorkIN(new StringBuffer( chris.substring(0, ((int)chris.charAt(2) + 3 + 1)) )) ;
                                synchronized( parent ){
                                        parent.notify() ;
                                }
                                chris = new StringBuffer(chris.substring( ((int)chris.charAt(2) + 3)))  ;
                        }
                }
        }

        private boolean isBufferResetableAndEven(StringBuffer sb){
                if( chris.length() == ((int)chris.charAt(2) + 3) ){
                    //TODO make a better check for length implemented here
                    TestChris(sb,"TCPServer:118:isBufferResetableAndEven") ;
                    return true ;
                }
                return false ;
        }

        private boolean isBufferHoldingMoreThanOneMessage(StringBuffer sb){
                if( chris.length() > ((int)chris.charAt(2) + 3) ){
                        return true ;
                }
                return false ;
        }

        private boolean isBufferStillReading(StringBuffer sb){
                if( chris.length() < ((int)chris.charAt(2) + 3) ){
                        return true ;
                }
                return false ;
        }

        private void append2chris(int thisByte){
                chris.append((char)thisByte) ;
        }

        public void TestChris(StringBuffer cm,String msg){
            System.out.println("TEST: " + msg ) ;
            TestChris(cm) ;
        }

        public void TestChris(StringBuffer cm){
                System.out.print("CSTAServer ---> DummyPBX | R: ") ;
                for( int i = 0 ; i < cm.length() ; i++ ){
                        System.out.print( Integer.toHexString((char)cm.charAt(i)) + " " ) ;
                }
                System.out.println("") ;
        }

}