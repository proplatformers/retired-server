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

package org.opencsta.server;

/**
 * 
 * @author chrismylonas
 */
public class CSTAServerStatus {

	/**
     * 
     */
	private boolean CSTAServerUp;

	/**
     * 
     */
	private boolean CSTALinkUp;

	/**
     * 
     */
	private int clientConnections;

	/**
     * 
     */
	private int monitoredExtensions;

	/**
	 * @return the CSTAServerUp
	 */
	public boolean isCSTAServerUp() {
		return CSTAServerUp;
	}

	/**
	 * @param CSTAServerUp
	 *            the CSTAServerUp to set
	 */
	public void setCSTAServerUp(boolean CSTAServerUp) {
		this.CSTAServerUp = CSTAServerUp;
	}

	/**
	 * @return the CSTALinkUp
	 */
	public boolean isCSTALinkUp() {
		return CSTALinkUp;
	}

	/**
	 * @param CSTALinkUp
	 *            the CSTALinkUp to set
	 */
	public void setCSTALinkUp(boolean CSTALinkUp) {
		this.CSTALinkUp = CSTALinkUp;
	}

	/**
	 * @return the clientConnections
	 */
	public int getClientConnections() {
		return clientConnections;
	}

	/**
	 * @param clientConnections
	 *            the clientConnections to set
	 */
	public void setClientConnections(int clientConnections) {
		this.clientConnections = clientConnections;
	}

	/**
	 * @return the monitoredExtensions
	 */
	public int getMonitoredExtensions() {
		return monitoredExtensions;
	}

	/**
	 * @param monitoredExtensions
	 *            the monitoredExtensions to set
	 */
	public void setMonitoredExtensions(int monitoredExtensions) {
		this.monitoredExtensions = monitoredExtensions;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		String str = "";
		if (this.isCSTAServerUp())
			str += "CSTA Server is up and running\n";
		else
			str += "CSTA Server is not running correctly\n";
		if (this.isCSTALinkUp())
			str += "CSTA Link is Up\n";
		else
			str += "CSTA Link is NOT up\n";

		str += "Client connections is currently " + this.getClientConnections()
				+ "\n";
		str += "Devices monitored is currently "
				+ this.getMonitoredExtensions() + "\n";

		return str;
	}
}
