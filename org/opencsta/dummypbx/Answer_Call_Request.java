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

/**
 *
 * @author cm
 */
public class Answer_Call_Request extends CSTARequest implements Runnable{
    char service_id ;

    public Answer_Call_Request() {
    }

    public Answer_Call_Request(char service_id) {
        this.service_id = service_id;
    }

    public void run(){
        super.setService_id(service_id);
    }
}
