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

package au.com.mrvoip.csta.server;

import au.com.mrvoip.serialport.*;
//import java.util.Enumeration;
//import org.opencsta.config.PropertiesController ;
import java.util.Properties ;
import org.opencsta.communications.CommunicationsStream ;
//import java.util.logging.Level ;
//import java.util.logging.Logger ;
import org.apache.log4j.*;
import org.opencsta.link.CSTA_Link;

/**
 * OS-CSTA-DD1:  The physical layer in the CSTA protocol stack.  This represents either the
 * serial port or a network proxy (usually used for laptop testing on laptops that don't have serial
 * ports or for computers that do not have a connection to the PBX directly)
 *
 *
 * @author mylo
 */
public class CSTA_Layer1 implements SerialPortOwner, CommunicationsStream{
    
    /**
     *
     *
     */
    private CSTA_Layer2 parent ;
    
    /**
     *
     *
     */
    private Properties theProps ;
    
    /**
     *
     *
     */
    private boolean serialportlayer1 ;
    
    /**
     *
     *
     */
//    private boolean proxylayer1 ;
    
    
    /**
     *
     *
     */
    private static CommunicationsStream layer1 ;
    
    /**
     *
     *
     */
    private SerialConfigurator sc ;
    
    /**
     *
     *
     */
//    private Logger general_log ;
    protected static Logger alog = Logger.getLogger(CSTA_Link.class) ;
    
    /**
     *
     *
     */
//    private StringBuffer receivedBuffer ;
    
    /**
     * Creates a new instance of CSTA_Layer1
     *
     *
     * @param _parent
     */
    public CSTA_Layer1(CSTA_Layer2 _parent, Properties _theProps) {
        this.parent = _parent ;
        this.theProps = _theProps ;
        Init() ;
        OpenLayer() ;
    }
    
    /**
     *
     *
     */
    public void Init(){
        SetasSerial() ;
    }
    
    
    /**
     *
     *
     */
    private void SetasSerial(){
//        proxylayer1 = false ;
        serialportlayer1 = true ;
    }
    
    /**
     *
     *
     */
    private void OpenLayer(){
        if( serialportlayer1 ){
            sc = new SerialConfigurator("CSTAServer",theProps) ;
            StartSerialPort( sc.getDeviceName(), sc.getBaudRate() ) ;
        }
    }
    
    public void closeComms(){
    	layer1.closeComms();
    }
    
    public void openComms(){
    	
    }
    
    /**
     *
     *
     *
     * @param serial_device
     * @param baudrate
     */
    public void StartSerialPort(String serial_device, int baudrate){
    	alog.info(this.getClass().getName() + " creating TTYSx") ;
        layer1 = new TTYSx(serial_device, baudrate, this, "CSTAServer") ;
        alog.info("Serialport started at: " + serial_device + " +++ Baudrate: " + baudrate);
    }
    
    /**
     *
     *
     *
     * @return
     * @param sb
     */
    public boolean CheckReceived(StringBuffer sb){
        //        if( serialportlayer1 )
        //            return parent.CheckReceived(sb) ;
        //        else{
        //            if( parent.CheckReceived(sb) ){
        //                layer1.SendString( new StringBuffer("RETURN TRUE, RELEASE RXBUF IN TTYSX")) ;
        //                return true ;
        //            }
        //            else{
        //                layer1.SendString( new StringBuffer("RETURN FALSE, NEED TO KEEP RECEIVING") ) ;
        //                return false ;
        //            }
        //        }
        //        receivedBuffer = receivedBuffer.append(sb) ;
        if( parent.CheckReceived(sb) ){
            alog.debug("\t\t\t\t\t< < < < L1 return TRUE") ;
//            receivedBuffer = new StringBuffer() ;
            return true ;
        }
        
        return false ;
        
        //possibly change this whole part so that, CSTA_Layer1 has a buffer that is written to of
        //the current string.  when checkreceived returns true, the buffer is reset with a new StringBuffer()
        //call.  this way, the serial port acts much the same as the cstaproxy in that they just pass
        //information on, like the name CommunicationsStream suggests.  This way, all the smart thinking
        //or remembering of what the stringbuffer is so far is at the higher CSTA_Layer1 layer, keeping
        //the low level implementation free of this need to know.
        
    }
    
    /**
     *
     *
     *
     * @return
     * @param sb
     */
    public boolean SendString(StringBuffer sb){
        return layer1.SendString(sb) ;
    }
    
}
