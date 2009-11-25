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

package org.opencsta.testing.skeleton.link.network;

import org.opencsta.testing.skeleton.link.SkelCSTA_Link;
import org.opencsta.testing.skeleton.server.SkelCSTA_Layer5;

/**
 *
 * @author cm
 */
public class SkelCSTA_Link_NetworkTest extends SkelCSTA_Link{
    public SkelCSTA_Link_NetworkTest(SkelCSTA_Layer5 stack){
        super(stack) ;
    }
    
    public void run() {
        linklog.info("SkelCSTA_Link run loop") ;
    }

}
