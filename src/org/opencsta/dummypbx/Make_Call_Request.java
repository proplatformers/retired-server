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

import org.opencsta.servicetools.asterisk.AsteriskInterest;
import org.opencsta.servicedescription.objects.cstaparamtypes.deviceidentifiers.DeviceID;

/**
 *
 * @author cm
 */
public class Make_Call_Request extends CSTARequest implements Runnable{
    private char service_id ;
    private final String service_name = "Make Call";
    private DeviceID from ;
    private DeviceID to ;
    private StringBuffer str ;
    public Make_Call_Request(){
        service_id = 0x0a ;
    }

    public Make_Call_Request(char service_id) {
        this.service_id = service_id;
    }

    public Make_Call_Request(StringBuffer sb){
        this.setStr(str);
    }

    public void run(){
        convert() ;
    }

    private void convert(){
        
    }

    public StringBuffer toCSTAASN1(){
        StringBuffer str = new StringBuffer() ;
//        str = Device(str, dev2) ; //DEVICE TO
//        str = Device(str, dev1) ; //DEVICE FROM
//        str = Sequence(str) ;
//        str = str.insert(0,id) ; //SERVICE ID
        return str ;
    }

    public String toCSTAXML(){
        return "" ;
    }

    /**
     * @return the str
     */
    public StringBuffer getStr() {
        return str;
    }

    /**
     * @param str the str to set
     */
    public void setStr(StringBuffer str) {
        this.str = str;
    }
}
