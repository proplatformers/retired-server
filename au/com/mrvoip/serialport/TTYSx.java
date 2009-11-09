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

import gnu.io.* ;
import java.io.* ;
import java.util.* ;
import org.apache.log4j.*;
import org.opencsta.communications.CommunicationsStream;

public class TTYSx implements CommPortOwnershipListener, SerialPortEventListener, CommunicationsStream {
//    private static Logger general_log ;
	protected static Logger slog = Logger.getLogger(SerialConfigurator.class) ;
    CommPortIdentifier portID ;
    SerialPort sport = null ;
    OutputStream out ;
    InputStream in ;
    boolean open = false ;
    Thread rcvThread = null ;
    TTYSx_Receiver receiver ;
    boolean da = false ;
    private StringBuffer strBuf ;
//    private StringBuffer cur_out ;
    private SerialPortOwner parent ;
    private String sportOwnersName ;
    
    
    //SerialConnection01 connection ;
    public SerialParameters parameters ;
    
    public TTYSx(String whichPort, int baudRate, SerialPortOwner parent, String sportOwnersName) {
        //System.setProperty("file.encoding", "ISO-8859-1") ;
        //System.out.println(System.getProperty("file.encoding")) ;
//        System.out.println("mylosonline.communications.serialport.TTYSx being used") ;
//        general_log = Logger.getLogger("serialport.general") ;
    	slog.info("Serial port creation") ;
        this.parent = parent ;
        this.sportOwnersName = sportOwnersName ;
        parameters = new SerialParameters(whichPort, baudRate) ;
        //System.out.println("Serial Parameters set to:\n\t\tDevice:\t" + whichPort + "\n\t\tBaudRate:\t"+baudRate) ;
        try{
            portID = CommPortIdentifier.getPortIdentifier(whichPort) ;
            slog.info("Opening port " + portID.getName()) ;
        } catch(NoSuchPortException e){} ;
        
        try{
            openPort() ;
        } catch(PortInUseException e){
            //KILL THE APPLICATION GRACEFULLY AND TRY AGAIN
        }catch(NullPointerException e){
            //KILL THE APPLICATION GRACEFULLY AND TRY AGAIN
        }
        //	connection = new SerialConnection01(this, parameters) ;
        
        //	try{
        //		connection.openConnection() ;
        //	}
        //	catch(Exception e){
        //		System.out.println("openConnection did not work") ;
        //	}
        strBuf = new StringBuffer() ;
//        cur_out = new StringBuffer() ;
        //System.out.println("Nearly End SPORT construction:\n\t\tBaudRate:\t" + sport.getBaudRate() ) ;
        SetSerialPortParams(baudRate) ;
        //System.out.println("End SPORT construction:\n\t\tBaudRate:\t" + sport.getBaudRate() ) ;
    }
    
    private void SetSerialPortParams(int baudRate){
        try{
            sport.setSerialPortParams(baudRate, sport.getDataBits(),sport.getStopBits(),sport.getParity() ) ;
        }catch(UnsupportedCommOperationException e){
        }
    }
    
    public void openPort() throws PortInUseException, NullPointerException {
        String tmp_str_for_logger = "" ;
        
                /*
                 * Regiser an ownership listener so we can manage the port
                 */
        //this.portID.addPortOwnershipListener(this) ;
        
        //System.out.println("Serialport ownership listener added") ;
        
        
        try {
        	slog.info("TTYSx trying to open port") ;
            sport = (SerialPort)portID.open(sportOwnersName,5000) ;
            if( sport == null ){
                slog.warn("Error opening serial port") ;
                open = true ;
            } else {
                //System.out.println("Serialport is open") ;
                tmp_str_for_logger += ("\n\tSerialport open -> Owner:" + portID.getCurrentOwner()) ;
                open = true ;
            }
        }catch(PortInUseException e){
            e.printStackTrace() ;
            throw e ;
        }catch(NullPointerException e){
            e.printStackTrace() ;
            throw e ;
        }
        
            /*
             * Get the input stream
             */
        try {
            in = sport.getInputStream() ;
            if(in != null)
                tmp_str_for_logger += "\n\tInputStream in: ok!" ;
            else
                slog.warn("TTYSx - InputStream in: fail") ;
        }catch(IOException e){slog.warn("TTYSx - Cannot open inputstream") ; }
        
                /*
                 * Get the output stream
                 */
        try {
            out = sport.getOutputStream() ;
            if(out != null)
                tmp_str_for_logger += "\n\tOutputStream out: ok!" ;
            else
                slog.warn("TTYSx - OutputStream out: fail") ;
        }catch(IOException e){slog.warn("TTYSx - Cannot open outputstream") ;}
        
        
                /*
                 * Setup an event listener for the port
                 */
        
        try {
            sport.addEventListener(this) ;
            tmp_str_for_logger += "\n\tNow listening for events:" ;
        }catch(TooManyListenersException tmle) {
            tmle.printStackTrace() ;
            slog.warn("TTYSx - AddSerialEventListener: tmle, too many listeners exception") ;
        }
        
        //NOTIFY ON DATA_AVAILABLE
        sport.notifyOnDataAvailable(true);
        
                /*
                 * Create the receiver thread
                 */
        if( rcvThread == null ) {
            receiver = new TTYSx_Receiver(this) ;
            rcvThread = new Thread( this.receiver , "Rcv " + sport.getName() ) ;
            rcvThread.start() ;
            tmp_str_for_logger += "\n\tReady to receive data -> Thread started." ;
            //System.out.println("toString: " + rcvThread.toString() ) ;
            
        } else {
            rcvThread = null ;
            slog.warn("TTYSx - rcvThread: not started") ;
        }
        //System.out.println("Baud Rate: " + Integer.toString(sport.getBaudRate())) ;
        
        //try{
        //       sport.setSerialPortParams(9600, sport.getDataBits(),sport.getStopBits(),sport.getParity() ) ;
        //}
        //catch(UnsupportedCommOperationException e){
        //      System.out.println("Cannot set baud rate to 19200 for port " +sport.getName() + " -> CSTA WILL NOT WORK.") ;
        //}
        //System.out.println("Baud Rate: " + Integer.toString(sport.getBaudRate(
        //))) ;
        slog.info(tmp_str_for_logger) ;
    }
    
    public void closeComms() {
    	slog.info("TTYSx - closing serial port") ;
        if( sport == null )
            slog.info("TTYSx - Port is null...nothing to close") ;
        else {
            sport.close() ;
            slog.info("TTYSx - Port is now closed") ;
            open = false ;
        }
        slog.info("TTYSx - closing serial port  ---- OK.DONE") ;
    }

    public void openComms(){
    }
    
    public void ownershipChange(int t) {
    }
    
    public void serialEvent(SerialPortEvent e) {
        if (this.sport == null) {
            slog.warn( (sport.getName()
            + "got serial event on a closed port") );
        } else ;
        
        switch( e.getEventType() ) {
            case SerialPortEvent.DATA_AVAILABLE:
            {
                da = true;
                
                if (rcvThread != null) {
                    synchronized (receiver) {
                        receiver.notify();
                    }
                }
                
                break;
            }
        }
    }
    
    
    public boolean SendString(StringBuffer sb){
        String str = sb.toString() ;
        int count = str.length() ;
        if( count > 0 ){
            try{
                out.write(str.getBytes() ) ;
                slog.debug( "written " + str.length() + "bytes.");
            }catch(IOException ex){
                if(open)
                    slog.warn( (sport.getName() + ": Cannot write to outputstream") );
                else ;
            }
        } else ;
        return true ;
        
    }
    
    
    public boolean SendString(String str){
        int count ;
        count = str.length() ;
        if( count > 0 ){
            try{
                out.write(str.getBytes()) ;
            } catch(IOException ex){
                if(open)
                    slog.warn( (sport.getName() + ": Cannot write to outputstream") );
                else ;
            }
        } else ;
        return true ;
    }
    
    //ADDED 18 MARCH 03 FOR QUICKNESS
    public void parametersSetBaudRate(int baudrate){
        parameters.setBaudRate(baudrate) ;
    }
    
    public void GetBaudRate(){
        int br = sport.getBaudRate() ;
        slog.info("Baud rate on serial port is currently: " + br) ;
    }
    
    public void KillReceiver(){
        
            /*open = false ;
            if (rcvThread != null){
                synchronized (receiver) {
                    receiver.notify();
                }
            }
            try{
                Thread.currentThread().sleep(1000) ;
            }catch(InterruptedException e){
            }*/
        //rcvThread = null ;
        //receiver = null ;
        this.closeComms() ;
    }
	public boolean CheckReceived(StringBuffer sb){
            return true ;
        }
    
    
    public void toBuffer(int currentByte){
        
        //System.out.print(" " + Integer.toHexString(currentByte) ) ;
        strBuf = strBuf.append( (char)currentByte ) ;
        //System.out.println("\n\t\t\t::Appended String to buffer, buffer size = " + Integer.toString( strBuf.length() ) ) ;
        
        //20MARCH03 - HERE SHOULD BE THE CODE FOR CHECKRECEIVED.
        //e.g.  if(CheckReceived(strBuf) then this code shoud be handled
        //in the extended serialportowner class which implements the CheckReceived
        //interface....then this is all that should be here....
        //going back.....if checkreceived and it's the right stuff
        //new stringbuffer, else do nothing....
        
        if( parent.CheckReceived(strBuf) ){
            //System.out.println("\n\t\t\t::::CHECK RECEIVED TRUE, CLEARING STRING::::") ;
            strBuf = new StringBuffer() ;
        } else
            ;//System.out.println("Check RECEIVED RETURNIGN FALSE");
        
    }
}
