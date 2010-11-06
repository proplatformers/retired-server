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

package au.com.mrvoip.serialport;

import java.util.Properties;
import org.apache.log4j.*;

/**
 * 
 * @author chrismylonas
 */
public class SerialConfigurator {

	/**
	 * 
	 */
	protected static Logger slog = Logger.getLogger(SerialConfigurator.class);

	/**
	 * 
	 */
	private String SPORT;

	/**
	 * 
	 */
	private int BAUD_RATE;

	/**
	 * 
	 */
	private String appName;

	/**
	 * 
	 */
	private Properties theProps;

	/**
	 * 
	 */
	private String APP_CONFIG_FILE;

	/**
	 * @param appName
	 * @param _theProps
	 */
	public SerialConfigurator(String appName, Properties _theProps) {
		this.appName = appName.toUpperCase();
		System.out.println("SerialConfigurator appName: " + this.appName);
		String lowappName = appName.toLowerCase();
		theProps = _theProps;
		Init();
	}

	/**
     * 
     */
	private void Init() {
		setAPP_CONFIG_FILE(theProps.getProperty("APP_CONFIG_FILE"));
		slog.info(this.getClass().getName() + " -> "
				+ "Getting property: SERIALPORT_" + appName);
		setSerialDeviceName(theProps.getProperty("SERIALPORT_" + appName));
		slog.info(this.getClass().getName() + " -> "
				+ "Getting property: SERIALPORT_" + appName + "_BAUDRATE");
		setBaudRate(theProps.getProperty("SERIALPORT_" + appName + "_BAUDRATE"));
		slog.info(this.getClass().getName() + " -> "
				+ "Setting serial port parameters  ---- OK.DONE ("
				+ this.getDeviceName() + "/" + this.getBaudRate() + ")");
	}

	/**
	 * @param baudrate
	 */
	private void setBaudRate(String baudrate) {
		try {
			BAUD_RATE = Integer.valueOf(baudrate).intValue();
			slog.info("Baud Rate set to: " + Integer.toString(BAUD_RATE));
		} catch (NumberFormatException e) {
			e.printStackTrace();
			BAUD_RATE = 9600;// default to this value
			slog.info("Baud Rate Defaulting to: " + Integer.toString(BAUD_RATE));
		}
	}

	/**
	 * @param name
	 */
	private void setSerialDeviceName(String name) {
		SPORT = name;
		slog.info("Serial Port set to: " + SPORT);
	}

	/**
	 * @return
	 */
	public int getBaudRate() {
		return BAUD_RATE;
	}

	/**
	 * @return
	 */
	public String getDeviceName() {
		return SPORT;
	}

	/**
	 * @return the APP_CONFIG_FILE
	 */
	public String getAPP_CONFIG_FILE() {
		return APP_CONFIG_FILE;
	}

	/**
	 * @param APP_CONFIG_FILE
	 *            the APP_CONFIG_FILE to set
	 */
	public void setAPP_CONFIG_FILE(String APP_CONFIG_FILE) {
		this.APP_CONFIG_FILE = APP_CONFIG_FILE;
	}
}
