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

package org.opencsta.blackbox;


import org.apache.log4j.Logger;
//import au.com.mrvoip.oscsta.server.CSTAServer;

public class NetworkBlackBox {
        private ClientSide clientSide ;
        private ServerSide serverSide ;
        private static Thread clientSideThread ;
        private static Thread serverSideThread ;
        protected static Logger bblog = Logger.getLogger(org.opencsta.blackbox.NetworkBlackBox.class) ;
        public static void main(String[] args) {

                NetworkBlackBox netbb = new NetworkBlackBox() ;
        }

        public NetworkBlackBox(){
                clientSide = new ClientSide(this) ;
                clientSideThread = new Thread(clientSide, "Client Side Thread") ;
                serverSide = new ServerSide(this) ;
                serverSideThread = new Thread(serverSide, "Server Side Thread") ;
                serverSideThread.start();
                clientSideThread.start();

        }

        public void clientSideToServerSide(String str_data){
                serverSide.sendData(str_data) ;
        }

        public void clientSideToServerSide(int char_data){
                serverSide.sendData(char_data) ;
        }

        public void serverSideToClientSide(String str_data){
                clientSide.receiveData(str_data) ;
        }

        public void serverSideToClientSide(int char_data){
                clientSide.receiveData(char_data) ;
        }
}
