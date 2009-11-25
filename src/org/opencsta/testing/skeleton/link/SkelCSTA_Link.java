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

package org.opencsta.testing.skeleton.link;

import org.apache.log4j.Logger;
import org.opencsta.testing.skeleton.server.SkelCSTA_Layer5;

/**
 *
 * @author cm
 */
public abstract class SkelCSTA_Link implements Runnable {
    protected static Logger linklog = Logger.getLogger(SkelCSTA_Link.class) ;
    private SkelCSTA_Layer5 layer5 ;
    public SkelCSTA_Link(SkelCSTA_Layer5 cstastack){
        linklog.info("Creating SkelCSTA_Link") ;
        this.layer5 = cstastack ;
    }
}
