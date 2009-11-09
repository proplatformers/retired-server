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

package org.opencsta.testing.skeleton.server;

import org.apache.log4j.Logger;
import org.opencsta.testing.skeleton.link.SkelCSTA_Link;
import org.opencsta.testing.skeleton.link.network.SkelCSTA_Link_NetworkTest;

/**
 *
 * @author cm
 */
public class SkelCSTA_Layer5 {
        protected static Logger skellog = Logger.getLogger(SkelCSTAServer.class) ;
        private SkelCSTA_Layer7 layer7 ;
        private SkelCSTA_Link cstalink ;
        private Thread linkThread ;

        
        public SkelCSTA_Layer5(SkelCSTA_Layer7 l7){
            skellog.info("Creating Layer 5") ;
            this.layer7 = l7 ;
            cstalink = new SkelCSTA_Link_NetworkTest(this) ;
            linkThread = new Thread(cstalink,"CSTA Link Thread") ;
            skellog.info("CSTA Link Thread created.  Starting ...") ;
            linkThread.start();
        }


}
