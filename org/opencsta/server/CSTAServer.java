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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.* ;
import org.apache.log4j.* ;
import org.opencsta.net.ServeOneClient ;
import org.opencsta.net.MVListeningThread ;
import org.opencsta.net.NetworkServer ;
import org.opencsta.servicedescription.objects.cstaparamtypes.deviceidentifiers.DeviceID;
import org.opencsta.servicedescription.objects.cstaparamtypes.statusreporting.MonitorCrossRefID;

public class CSTAServer implements NetworkServer,Runnable{
    protected static Logger alog = Logger.getLogger(CSTAServer.class) ;
    private static Properties theProps ;
    private static CSTA_Layer7 layer7 ;
    private static CSTAServer server ;
    private static TDSServer TDSserver ;
    private static Calendar timeStarted ;
    /**
     * OS-CSTA-DD1:invoke2client maps an invoke id to the client that 
     * requested the function.  this map is used all the time and when 
     * a start monitor is requested, the invoke id is used to get the 
     * cross ref id before sending it back (the result) to teh client
     *
     */
    Hashtable<String,ServeOneClient> invoke2client ;
    
    /**
     * OS-CSTA-DD1:deviceID2crossRefID maps an extension to a cross 
     * reference identifier. this map is used when a start monitor
     * is requested by a client and it is the first one for that particular 
     * extension - subsequent monitors then just look up this table and 
     * are added to the list of clients watching on this ext/xref
     *
     */
    Hashtable<DeviceID,MonitorCrossRefID> deviceID2crossRefID ;
    
    /**
     * check it out later
     *
     */
    Hashtable<MonitorCrossRefID,DeviceID> xref2ext ;
    
    /**
     * OS-CSTA-DD1: xref2list is a linked list that is held in this hashtable.
     * when an event occurs, the xref is looked up and the list is got.  
     * The client's that make up the list are then sent the event one 
     * at a time in a for loop.
     */
    Hashtable<MonitorCrossRefID,List<ServeOneClient>> xref2list ;
    
    /**
     * OS-CSTA-DD1:  invoke id to extension is used when a client asks 
     * for a start monitor.  the extension is kept in the map with the 
     * invoke id, which when the result is got, the extension is looked up
     * from this table.  it can then be entered into the 
     * deviceID2crossRefID table
     */
    Hashtable<String,DeviceID> invoke2ext ;
    
    /**
     * OS-CSTA-DD1:  client to extension list is a list of extensions a 
     * particular client is monitoring.  this is important for when a client
     * disconnects because it can tell us what extensions it was monitoring 
     * before it dropped out. this is good cos then we can look up the 
     * deviceID2crossRefID, use the xref to see the client list, and if 
     * that list is empty call a stop monitor.
     *
     */
    Hashtable<ServeOneClient,List<DeviceID>> client2extlist ;
    
    /**
     * OS-CSTA-DD1:  used in a stop monitor.  it gets the invoke from the 
     * result, which re-gets the xref that was given to the server.  we then
     * get then send this result to everyone in the xref2list table, 
     * remove the xref2list key in the same table, remove the 
     * deviceID2crossRefID key in the table, remove the ext in the client 
     * list of extension (client2extlist). and remove hte ext2devicesMonitored 
     * entry
     */
    Hashtable<String,MonitorCrossRefID> invoke2xref ;
    
    /**
     * OS-CSTA-DD1:  used in FromClient for simple looking up if a device 
     * is being monitored
     *
     */
    Hashtable<String,DeviceID> ext2devicesMonitored ;
    
    /**
     * OS-CSTA-DD1:  used when a start monitor response is got and there 
     * is a crossrefID issued.  This is a simple, string of the xref mapped 
     * to the crossRefID Object.  ALSO used in EventToClient()
     *
     */
    Hashtable<String,MonitorCrossRefID> xrefAsString2xrefObject ;
    
    /**
     * the list of clients connected
     *
     */
    List<ServeOneClient> listOfClients ;
    
    /**
     * indicates when the telephone data service has been activated
     *
     */
    private boolean tds_on ;
    
    /**
     * some temporary value used
     *
     */
    String xref_tmp ;
    char INTEGER = '\u0002' ;
    private boolean runFlag = false;
    private MVListeningThread clientsConnectHere ;
    private Thread MVlisteningThread ;
    /**
     * OS-CSTA-DD1:  This method looks up the Properties file and gets 
     * the various logger properties ranging from filename to sizelimit, 
     * creates the Logger objects and associates them with a filehandle
     * @param args command line parameters
     */
    public static void main(String[] args){
        System.out.println(System.getProperty("file.encoding")) ;
        System.setProperty("file.encoding", "ISO-8859-1") ;
        System.out.println(System.getProperty("file.encoding")) ;
        Properties someProps = loadPropertiesFromFile() ;
        server = new CSTAServer(someProps) ;
        server.run() ;
    }

    private static Properties loadPropertiesFromFile(){
        return loadPropertiesFromFile("opencstaserver.conf") ;
    }

    private static Properties loadPropertiesFromFile(String filename){
        FileInputStream is ;
        try {
            System.out.println("Trying to load properties from:  " + System.getProperty("user.dir") + "/" + filename) ;
            is = new FileInputStream( (System.getProperty("user.dir") + "/"+filename) );
            Properties props = new Properties() ;
            props.load(is) ;
            return props ;
        }catch (FileNotFoundException ex) {
            ex.printStackTrace() ;
        } catch (IOException ex) {
            ex.printStackTrace() ;
        }
        return null ;
    }

    private Properties loadProperties(InputStream propstream){
        Properties props ;
        try {
            props = new Properties();
            props.load(propstream);
            return props ;
        } catch (IOException ex) {
            ex.printStackTrace();
            props = null ;
        }
        return null ;
    }

    public void run(){
        setTimeStarted( Calendar.getInstance() ) ;
    	StartCSTAStack();
    	Init();
//        while( isRunFlag() ){
//            //System.out.println("THIS RUN() METHOD IS IMPLEMENTED") ;
//            try{
//                synchronized(this){
//                    wait(5000) ;
//                }
//            }catch(InterruptedException e){
//
//            }catch(NullPointerException e2){
//            	e2.printStackTrace();
//            }
//            while( layer7.getJobCount()  > 0 ){
//            	System.out.println("The job count waiting " +
//                        "in the IN tray of layer 5" +
//                        " is: " + Integer.toString(layer7.getJobCount() ) );
//            	layer7.doWork() ;
//            }
//        }
        System.out.println("CSTA SERVER LOOP IS DONE - SHOULD BE ALL OVER NOW") ;
    }
    /**
     * Constructor.
     *
     */
    public CSTAServer(){
        //this(PropertiesController.getInstance("CSTAServer") );
        FileInputStream is ;
        try {
            is = new FileInputStream( (System.getProperty("user.dir") + "/cstaserver.conf") );
            theProps = new Properties() ;
            theProps.load(is) ;
        }catch (FileNotFoundException ex) {
            ex.printStackTrace() ;
        } catch (IOException ex) {
            ex.printStackTrace() ;
        }
    }

    @SuppressWarnings("static-access")
    public CSTAServer(Properties props){
        this.theProps = props ;
    }
    
    /**
     * OS-CSTA-DD1:  Starts the CSTA stack starting with the creation 
     * of layer 7.  After this is done, a TDSServer is created if one does 
     * not exist. (Note:  If for some reason this is not the first instatiation
     * of the CSTA stack, a cable kicked out and CSTA connection lost for 
     * example, then a TDSServer already exists and the new layer 7 that is 
     * recreated is sent to it).  A CSTA login is then attempted with a break
     * of 5 seconds between unsuccessful attempts.
     *
     */
    @SuppressWarnings("static-access")
    public void StartCSTAStack(){
        try {
            alog.info(this.getClass().getName() + " ####################Starting CSTA Stack - please wait ...");
            layer7 = null;
            alog.info(this.getClass().getName() + " creating Layer 7");
            layer7 = new CSTA_Layer7(theProps);
            alog.info(this.getClass().getName() + " ####################CSTA Stack Initialised");
            if (TDSserver == null) {
                alog.info(this.getClass().getName() + " Creating TDSServer");
                TDSserver = new TDSServer(layer7, this);
            } else {
                TDSserver.ReintroduceLayer7(layer7);
            }
            while (true) {
                alog.info(this.getClass().getName() + " trying to begin CSTA Login");
                if (layer7.Login()) {
                    alog.info(this.getClass().getName() + " ####################CSTA Link successfully started");
                    break;
                } else {
                    try {
                        layer7.CloseCSTAStack();
                        alog.info(this.getClass().getName() + " ####################Log in failure, trying again in 5 seconds");
                        layer7 = null;
                        Thread.currentThread().sleep(12000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e2) {
                        e2.printStackTrace();
                    }
                    layer7 = new CSTA_Layer7(theProps);
                }
            }
            alog.info(this.getClass().getName() + " CSTA Login Complete");
        } catch (NoCSTAImplementationException ex) {
            alog.error(ex.toString());
            alog.warn(this.getClass().getName() + " cannot start CSTAServer, quitting.") ;
        }
    }
    
    /**
     * OS-CSTA-DD1:  Sets up all the necessary tables and buffers.
     *
     */
    public void Init(){
        publicPreInit() ;
        initialise() ;
        publicPostInit() ;
        checkForWork() ;
    }
    
    private void initialise(){
        alog.info(this.getClass().getName() + " Initialising" ) ;
    	setRunFlag(true) ;
        invoke2client = new Hashtable<String,ServeOneClient>() ;
        deviceID2crossRefID = new Hashtable<DeviceID,MonitorCrossRefID>() ;
        xref2ext = new Hashtable<MonitorCrossRefID,DeviceID>() ;
        xref2list = new Hashtable<MonitorCrossRefID,List<ServeOneClient>>() ;
        invoke2ext = new Hashtable<String,DeviceID>() ;
        client2extlist = new Hashtable<ServeOneClient,List<DeviceID>>() ;
        invoke2xref = new Hashtable<String,MonitorCrossRefID>() ;
        ext2devicesMonitored = new Hashtable<String,DeviceID>() ;
        xrefAsString2xrefObject = new Hashtable<String,MonitorCrossRefID>() ;
        alog.info(this.getClass().getName() + " hashtables initialised") ;
        tds_on = false ;
        listOfClients = Collections.synchronizedList( new LinkedList<ServeOneClient>() );
        alog.info(this.getClass().getName() + " clients connected list initialised with none connected") ;
        xref_tmp = null ;
        layer7.ServerIntro(this, TDSserver);
        alog.info(this.getClass().getName() + " ready to start listening for client connections") ;
        int listenerPort = 0 ;
        try{
            listenerPort = Integer.parseInt(theProps.getProperty("CSTASERVER_CLIENTLISTENER_PORT")) ;
        }catch( NumberFormatException e ){
            alog.warn(e.toString() + " config file CSTASERVER_CLIENTLISTENER_PORT is not a number, using default port number which is hardcoded to 8996") ;
            listenerPort = 0 ;
        }
        if( listenerPort != 0 ){
            clientsConnectHere = new MVListeningThread(this,listenerPort) ;
        }
        else{
            clientsConnectHere = new MVListeningThread(this) ;
        }
        MVlisteningThread = new Thread(clientsConnectHere,"MVListening Thread") ;
        MVlisteningThread.start() ;
        alog.info(this.getClass().getName() + " is listening for client connections") ;
    }

    public void publicPreInit(){

    }

    public void publicPostInit(){

    }
    
    public void checkForWork(){
        int count = 0 ;
    	alog.info(this.getClass().getName() + " ##################CSTA Server is now running##################") ;
    	while( isRunFlag() ){
            if( count == 0){
        		alog.info(this.getClass().getName() + " waiting for work") ;
                count = 5 ;
            }
            try{
                synchronized(this){
                    wait(2000) ;
                }
            }catch(InterruptedException e){
            }catch(NullPointerException e2){
            	e2.printStackTrace();
            }
            count-- ;//not the best spot to put this, but it's not critical
        }
    }
    /**t
     * OS-CSTA-DD1:  Writes to log
     *
     *
     * @param strBuf OS-CSTA-DD1:  the string
     * @param s_or_r OS-CSTA-DD1:  s for sent, r for received
     */
    public void WriteToLog(StringBuffer strBuf, char s_or_r){
        //**LOG**CLIENTSRXTX FINE - <string>
        StringBuffer tmp = new StringBuffer() ;
        tmp = tmp.append(s_or_r).append(": ") ;
        for(int i = 0 ; i < strBuf.length() ; i++)
            tmp = tmp.append( Integer.toHexString(strBuf.charAt(i)) ).append(" ") ;
        
        alog.info(this.getClass().getName() +  " Sending to client:\t"  +  tmp.toString() ) ;
    }
    
    /**
     * OS-CSTA-DD1:  AddTo_listOfClients is used when a client connects and 
     * needs to be added to the listOfClients list. Usually in a monitoring 
     * situation.
     *
     *
     * @return returns true if successful operation
     * @param client OS-CSTA-DD1:  the current client connected to the server 
     * to add to the list
     */
    public boolean AddTo_listOfClients(ServeOneClient client){
        return listOfClients.add(client) ;
    }
    
    /**
     * OS-CSTA-DD1:  GetClientConnectionCount simply returns the number of 
     * clients that are connected to the CSTAServer.
     *
     *
     * @return the number of clients connected to server
     */
    public int GetClientConnectedCount(){
        return listOfClients.size() ;
    }
    
    /**
     * OS-CSTA-DD1:  This method is called by the ServeOneClient class when 
     * it has received a request from a connected client.  It checks to see
     * if the request is a start monitor request and takes the appropriate
     * steps to see if the requested device is already monitored or not.  
     * If it is then the client is sent a dummy response string and the client
     * is added to the list of clients already monitoring that extension.  
     * If it is not an already monitored device, then the server looks up if 
     * this client requesting the monitor on the new device is already 
     * monitoring other devices.  Steps are taken and in the end a start monitor
     * request is sent to the pbx.  If the client is notifying the server that
     * a telephone data service (TDS) is required, then the server starts the
     * TDS service.  If the received request is none of the above, then the
     * string is sent to the pbx via the stack and the server does the 
     * necessary administration to get the response (when it arrives)
     * to the correct client.
     *
     *
     * @param str OS-CSTA-DD1:  the string received from the client
     * @param client OS-CSTA-DD1:  the client which is sending the request
     */
    @SuppressWarnings("static-access")
    public void FromClient(StringBuffer str, ServeOneClient client){
        WriteToLog(str, 'R') ;
        char sortOfMonitor = str.charAt(0) ;
        if( str.charAt(0) == 0x47 || str.charAt(0) == 0x46 || str.charAt(0) == 0x45 ||str.charAt(0)==0x44){/*it is a start monitor request*/
/*Remove the first char because we have received a string like: 
  0x47 {extension}
  and get the extension and put it into a string for later use*/
            str = str.deleteCharAt(0) ;
            String ext = str.toString() ;
            DeviceID deviceID ;
            synchronized( ext2devicesMonitored ){
                deviceID = (DeviceID)ext2devicesMonitored.get(ext) ;
            }
            
            if( deviceID != null ){//this device is already monitored.
                MonitorCrossRefID xref ;
                synchronized( deviceID2crossRefID ){//find the xref for this devices watch
                    xref = (MonitorCrossRefID)deviceID2crossRefID.get(deviceID) ;
                }
                //Add to the list that are watching on that cross reference ID
                synchronized(xref2list){
                    List<ServeOneClient> list = (List<ServeOneClient>)xref2list.get(xref) ;
                    if( list.add(client) ){
                        alog.info(this.getClass().getName() + " Client added to list") ;
                                    /*Generate a string to give to the client.  It doesn't know that there is a monitor already.
                                     * This way the client thinks it has received a genuine response from the PABX*/
                        /*If there is a problem later with this generated string, we may have to put the real xref under here*/
                        char[] tmp = {0xa2, 0x2c, 0x02, 0x01, 0x01, 0x30, 0x27, 0x02, 0x01, 0x47, 0x30, 0x22, 0x55, 0x02, 0x00,
                                + 0x00, 0xa0, 0x1c, 0x80, 0x04, 0x06, 0x40, 0x04, 0x00, 0x86, 0x02, 0x03, 0x40, 0x88, 0x03, 0x05,
                                + 0x54, 0x00, 0x89, 0x03, 0x02, 0xfc, 0x18, 0x83, 0x02, 0x05, 0xc0, 0x85, 0x02, 0x01, 0xc0};
                                StringBuffer tmpStr = new StringBuffer( new String(tmp) ) ;
                                client.SendToClient(tmpStr) ;
                                /*NOW ADD THE EXTENSION TO THIS CLIENT'S LIST OF EXT's IT IS MONITORING*/
                                synchronized(client2extlist){
                                    if( client2extlist.containsKey(client) ){/*This client is already monitoring other extensions*/
                                        List<DeviceID> list1 = (List<DeviceID>)client2extlist.get(client) ;
                                        list1.add(deviceID) ;
                                    } else{/*This client is new to monitoring so make a new list for it so it can add further extension later if it wants*/
                                        List<DeviceID> list2 = Collections.synchronizedList(new LinkedList<DeviceID>()) ;
                                        list2.add(deviceID) ;
                                        client2extlist.put(client, list2) ;
                                    }
                                }
                                return ;
                    } else
                        /*This is a problem if it gets to here*/
                        //System.out.println("Client not added. Line 249") ;
                        alog.warn(this.getClass().getName() + " Client not added for some reason.  'Line 249'");
                }
            } else{//the ext is not monitored yet, request a monitor
                DeviceID newDeviceToMonitor = new DeviceID(ext) ;//make a new DeviceID object out of this extension
                synchronized(client2extlist){
                    if( client2extlist.containsKey(client) ){//look up whether this client is monitoring other devices already
                        List<DeviceID> list = (List<DeviceID>)client2extlist.get(client) ;//they are, so get the list of the devices
                        list.add(newDeviceToMonitor) ;//and add this device to that list
                    } else{//it's the first monitor for this client.
                        List<DeviceID> list = Collections.synchronizedList(new LinkedList<DeviceID>()) ;//make a new list of devicesMonitored
                        list.add(newDeviceToMonitor) ;//add this device to the list
                        client2extlist.put(client, list) ;//put the client into the hashtable of all clients monitoring
                    }
                }
                //now call a start monitor csta function and send it down the stack
                //str = layer7.CallControl_Services_MonitorStart2(ext) ;
                if( sortOfMonitor == 0x46 )
                    str = layer7.MonitorStart_SI_CC_only( newDeviceToMonitor.getValue() ) ;
                else if( sortOfMonitor == 0x47 )
                    str = layer7.MonitorStart2( newDeviceToMonitor.getValue() ) ;
                else if( sortOfMonitor == 0x45 )
                    str = layer7.MonitorStart_LogicalDeviceFeatures( newDeviceToMonitor.getValue() ) ;
                else if( sortOfMonitor == 0x44 )
                    str = layer7.MonitorStart_LogicalDeviceFeaturesAndCalls( newDeviceToMonitor.getValue() ) ;
                str = GetStrReady(str, client, newDeviceToMonitor) ;
                layer7.Wrap(str) ;
                return ;
            }
            
        }
        
        else if(str.charAt(0) == 0x99){
            str = null ;
            if( !isTDSstarted() )
                startTDS() ;
            else
                ;

            if( TDSserver.AddClient(client) ){
                alog.info(this.getClass().getName() + " Successfully added SMART-Client") ;
                return ;
            }
            else{
                alog.warn(this.getClass().getName() + " Failure to add SMART-Client") ;
            }
        }
        else if(str.charAt(0) == 0x01){//Administrative/Manager connection
            alog.info(this.getClass().getName() + " Non 'CSTA Command' Connection made") ;
            if( str.charAt(1) == 0x01 ){//
                //create a status object and return status
                CSTAServerStatus status = new CSTAServerStatus();
                status.setCSTAServerUp( this.isRunFlag() );
                status.setCSTALinkUp( this.layer7.isCSTAUp());
                status.setClientConnections( this.GetClientConnectedCount() );
                status.setMonitoredExtensions( this.deviceID2crossRefID.size() );
                alog.info( this.getClass().getName() + " " + status.toString() ) ;
            }
            else if( str.charAt(1) == 0x02 ){//
                alog.warn(this.getClass().getName() + " Received message from client to kill server") ;
                alog.info(this.getClass().getName() + " Killing server") ;
                KillServer() ;
            }
            else if( str.charAt(1) == 0x03 ){//

            }
            str = null ;
        }
        /*The request was not a start monitor, so just get the string ready 
         and wrap/send it down*/
        if( str != null){
            str = GetStrReady(str, client) ;
            layer7.Wrap(str) ;
        }
    }
    
    /**
     * OS-CSTA-DD1:  returns the value of the tds_on boolean
     *
     *
     * @return
     */
    private boolean isTDSstarted(){
        return tds_on ;
    }
    
    /**
     * OS-CSTA-DD1:  This method looks up the Properties file and gets the 
     * various logger properties ranging from filename to sizelimit, creates
     * the Logger objects and associates them with a filehandle
     *
     */
    private void startTDS(){
        layer7.startTDS() ;
        tds_on = true ;
    }
    
    /**
     * OS-CSTA-DD1:  CSTA event data is sent to the client via this method when
     * the cross reference is passed as a string.  The EventToClient method
     * that takes an xref as a MonitorCrossRefID object is called from within 
     * this method once the conversion has been made.
     *
     *
     * @param str OS-CSTA-DD1:  the string containing all the event information
     * @param xrefAsString OS-CSTA-DD1:  cross reference ID as a string
     */
    public void EventToClient(StringBuffer str, String xrefAsString){
        try{
            MonitorCrossRefID xref ;
            synchronized(xrefAsString2xrefObject){
                xref = (MonitorCrossRefID)xrefAsString2xrefObject.get(xrefAsString) ;
            }
            EventToClient(str, xref) ;
        }catch(NullPointerException e){
            e.printStackTrace() ;
        }
    }
    
    /**
     * OS-CSTA-DD1:  EventToClient is called when there is CSTA-Event data to 
     * be sent to the client.  Using the xref, the list of clients that are 
     * monitoring on this device (with this xref) is retrieved. Each client on 
     * that list is then sent the event data.
     *
     *
     * @param str OS-CSTA-DD1:  the event data from the pabx in an unprocessed
     * string
     * @param xref OS-CSTA-DD1:  cross reference ID for the monitor that 
     * this event is attributed to
     */
    public void EventToClient(StringBuffer str, MonitorCrossRefID xref){ 
        //throws NullPointerException
        ServeOneClient client ;
        List<ServeOneClient> list ;
        
        synchronized(xref2list){
            list = (List<ServeOneClient>)xref2list.get(xref) ;
        }
        for(int i = 0 ; i < list.size() ; i ++ ){
            try{
                client = (ServeOneClient)list.get(i) ;
                client.SendToClient(str) ;
            }catch(NullPointerException e){
                //client hasn't been picked up that it is disconnected so 
                //deal with it.
                //similar code to client disconnected code.
                //Feb 05 2003, do nothing for the moment.
            }
        }
    }
    
    /**
     * OS-CSTA-DD1:  TDSToClient is called when there is Telephone Data Service 
     * data to send to the clients.
     *
     *
     * @param str OS-CSTA-DD1:  unprocessed csta string holding the tds data
     * @param TDSclients OS-CSTA-DD1:  the list of clients that expect tds data
     */
    public void TDSToClient(StringBuffer str, List<ServeOneClient> TDSclients){
        alog.info(this.getClass().getName() + " is sending TDS data to a list of clients") ;
        ServeOneClient client ;
        for(int i = 0 ; i < TDSclients.size() ; i++){
            client = (ServeOneClient)TDSclients.get(i) ;
            client.SendToClient(str) ;
        }
    }
    
    /**
     * OS-CSTA-DD1:  ToClient is called when there is response data to send to 
     * the client.  First, the the invoke id is checked in the invoke2xref 
     * hashtable to remove an xref (if it exists) just in case it is a stop 
     * monitor response.  If it is, as in xref does not equal null, it is sent
     * to the client with the xref via EventToClient.  Everything is removed 
     * from wherever it needs to be when a stop monitor is called.
     * Otherwise, the invoke_id is used to get the correct client to 
     * send the response to.
     *
     *
     * @param str OS-CSTA-DD1:  unprocessed csta string
     * @param invoke_id OS-CSTA-DD1:  invoke ID which identifies which 
     * client made the request.
     */
    public void ToClient(StringBuffer str, String invoke_id){
        ServeOneClient client ;
        DeviceID extension ;
        MonitorCrossRefID xref ;
        
        synchronized(invoke2xref){
            xref = (MonitorCrossRefID)invoke2xref.remove(invoke_id) ;
        }
        
        if( xref != null ){
            //This means that the xref in the invoke2xref refers to a stop 
            //monitor.  Just sending the result of this stop monitor to the 
            //people watching this xref like it was an event, so i'm calling 
            //EventToClient and passing the two args
            
            EventToClient(str, xref) ;
            
            //Now the result has been sent to all clients, let's remove them from the table
            synchronized(xref2list){
                List<ServeOneClient> list_tmp = (List<ServeOneClient>)xref2list.remove(xref) ;
            }
            
            //USE xref2ext: find the extension that was being monitored with 
            //this xref by searching through deviceID2crossRefID
            synchronized(xref2ext){
                extension = (DeviceID)xref2ext.remove(xref) ;
                //and remove it
                MonitorCrossRefID xref_tmp2 = (MonitorCrossRefID)deviceID2crossRefID.remove(extension) ;
                DeviceID tmp = (DeviceID)ext2devicesMonitored.remove(extension.getValue()) ;
            }
            
            //Now remove extenstion in each clients' list of extensions it is monitoring
            //USE client2extlist
            synchronized(client2extlist){
                for(int i = 0 ; i < client2extlist.size() ; i++ ){
                    ServeOneClient client_tmp = (ServeOneClient)listOfClients.get(i) ;
                    List<DeviceID> list_tmp = (List<DeviceID>)client2extlist.get(client_tmp) ;
                    /*got the list, now go through it and find the extension*/
                    for( int j = 0 ; j < list_tmp.size() ; j++ ){//HE2R060605NullPointer
                        DeviceID ext_tmp = (DeviceID)list_tmp.get(j) ;
                        if( ext_tmp.getValue().equals(extension.getValue()) ){
                            DeviceID ext_tmp2 = (DeviceID)list_tmp.remove(j) ;
                            if( list_tmp.size() == 0 )/*remove this list then*/
                                client2extlist.remove(client_tmp) ;
                            else ;
                        } else ;
                    }
                }
            }
            
        }
        
        synchronized(invoke2client){
            client = (ServeOneClient)invoke2client.remove(invoke_id) ;
        }
        
        synchronized(invoke2ext){/*only for start monitor stuff*/
            extension = (DeviceID)invoke2ext.remove(invoke_id) ;
        }
        if( extension != null ){
            for(int i = 0 ; i < str.length() ; i++ ){
                if( str.charAt(i) == 0x55 ){
                    //The next line assumes a limitation of 256*256 monitors
                    xref_tmp = str.substring( (i+2), (i+4) ) ;
                    break ;
                } else ;
            }
            synchronized(ext2devicesMonitored){
                ext2devicesMonitored.put( extension.getValue(), extension ) ;
            }
            MonitorCrossRefID newXref = new MonitorCrossRefID(xref_tmp) ;
            xrefAsString2xrefObject.put(xref_tmp, newXref) ;
            synchronized(deviceID2crossRefID){
                deviceID2crossRefID.put( extension, newXref ) ;
            }
            
            synchronized(xref2ext){
                xref2ext.put( newXref, extension ) ;
            }
            synchronized( xref2list ){
                List<ServeOneClient> list = Collections.synchronizedList(new LinkedList<ServeOneClient>()) ;
                if( list.add(client) )
                    alog.info(this.getClass().getName() + " Client added to list and is teh first client monitoring this extension") ;
                else{
                    alog.warn(this.getClass().getName() + " Could not add client to the list") ;
                }
                xref2list.put(newXref, list) ;
            }
        } else ;
        
        if( client != null ){
            client.SendToClient(str) ;
        }
        else{
            alog.warn(this.getClass().getName() + " client == null") ;
        }
    }
    
    /**
     * OS-CSTA-DD1:  Firstly, the client is removed from the TDSserver if it 
     * exists in that instance.  Then, check whether the client was watching
     * any extensions and remove them from those monitor lists. Then finally
     * just remove the client from the list of clients connected to the server.
     *
     *
     * @param client OS-CSTA-DD1:  the client that is disconnected
     */
    public void ClientDisconnected(ServeOneClient client){
        List<DeviceID> list1 ;
        
        synchronized(client2extlist){
            if( client2extlist.containsKey(client) ){/*this means the client was watching extension(s)*/
                list1 = (List<DeviceID>)client2extlist.get(client) ;/*get the list*/
                for( int i = 0 ; i < list1.size() ; i++ ){/*for each extension in the list, get the xref, and remove the client from the list.*/
                    DeviceID ext = (DeviceID)list1.get(i) ;
                    MonitorCrossRefID xref ;
                    List<ServeOneClient> list2 ;
                    synchronized(deviceID2crossRefID){
                        xref = (MonitorCrossRefID)deviceID2crossRefID.get(ext) ;
                    }
                    synchronized(xref2list){
                        list2 = (List<ServeOneClient>)xref2list.get(xref) ;
                    }
                    if( list2.remove(client) ){
                        if( list2.size() == 0 ){/*after removing client if there are no more watching on the xref, then stop monitoring it*/
                            /*Call A stop monitor on this extension/xref, then remove this entry from the relevant maps*/
                            //System.out.println("Call a Stop Monitor") ;
                            StringBuffer str = layer7.CallControl_Services_MonitorStop( xref.getValue() ) ;
                            str = GetStrReady(str, xref) ;/*Send with xref, so we can put into invoke2xref.  Only used for stop monitor*/
                            layer7.Wrap(str) ;
                        } else
                            ;
                    } else{
                        alog.warn(this.getClass().getName() + " Unable to remove client from monitor list") ;
                    }
                }
                client2extlist.remove(client) ;
            } else ;
        }
        alog.info(this.getClass().getName() + " Number of clients connected is: " + Integer.toString( listOfClients.size() ) ) ;
        listOfClients.remove(client) ;
        alog.info(this.getClass().getName() + " ******** Client Disconnected") ;
        alog.info(this.getClass().getName() + " Number of clients connected is: " + Integer.toString( listOfClients.size() ) ) ;
    }
    
    
    /**
     * OS-CSTA-DD1:  This version of GetStrReady is only used for a stop monitor request.
     * It just gets a rose invoke from layer seven and wraps the string as it is needed to be for
     * this layer.
     *
     *
     * @return
     * @param str OS-CSTA-DD1:  The string to get ready to send
     * @param xref OS-CSTA-DD1:  The cross reference id
     */
    private StringBuffer GetStrReady(StringBuffer str, MonitorCrossRefID xref){/*This version of get string ready is only used for stop monitor*/
        String invoke_id = layer7.Get_Rose_Invoke(str) ;
        //System.out.println("Server getting an Invoke ID, it is:") ;
        //		StringContains(invoke_id) ;
        
        synchronized(invoke2xref){
            invoke2xref.put(invoke_id, xref) ;
        }
        
        str = str.insert(0, invoke_id).insert(0,(char)invoke_id.length()).insert(0,INTEGER) ;
        return str ;
    }
    
    /**
     * OS-CSTA-DD1:  This method gets an invoke from layer 7 and puts the client into the
     * invoke2client hashtable for later use when the response comes back.
     *
     *
     * @return
     * @param str OS-CSTA-DD1:  The string to get ready
     * @param client OS-CSTA-DD1:  The client which has made the request
     */
    private StringBuffer GetStrReady(StringBuffer str, ServeOneClient client){
        String invoke_id = layer7.Get_Rose_Invoke(str) ;
        //System.out.println("Server getting an Invoke ID, it is:") ;
        //		StringContains(invoke_id) ;
        
        if( client != null ){
            synchronized(invoke2client){
                invoke2client.put(invoke_id, client) ;
            }
        } else{
            alog.warn(this.getClass().getName() + " cannot 'put' into hashtable") ;
        }
        
        str = str.insert(0, invoke_id).insert(0,(char)invoke_id.length()).insert(0,INTEGER) ;
        return str ;
    }
    
    /**
     * OS-CSTA-DD1:  This method puts the client into the invoke2client 
     * hashtable for response time feedback, the extension into invoke2ext
     * hashtable for use somewhere ... A rose invoke is retrieved from layer 7.
     *
     *
     * @return
     * @param str
     * @param client OS-CSTA-DD1:  The client that is making the request
     * @param ext OS-CSTA-DD1:  The extension to put into the invoke2ext hashtable
     */
    private StringBuffer GetStrReady(StringBuffer str, ServeOneClient client, DeviceID ext){
        String invoke_id = layer7.Get_Rose_Invoke(str) ;
        //System.out.println("Server getting an Invoke ID, it is:") ;
        //		StringContains(invoke_id) ;
        if( client != null ){
            synchronized(invoke2client){
                invoke2client.put(invoke_id, client) ;
            }
            synchronized(invoke2ext){
                invoke2ext.put(invoke_id, ext) ;
            }
        } else{
            alog.warn(this.getClass().getName() + " cannot 'put' into hashtable") ;
        }
        
        str = str.insert(0, invoke_id).insert(0,(char)invoke_id.length()).insert(0,INTEGER) ;
        return str ;
    }
	private boolean isRunFlag() {
		return runFlag;
	}
	private void setRunFlag(boolean runFlag) {
		this.runFlag = runFlag;
	}

    public void KillServer(){
        layer7.CloseCSTAStack();
        clientsConnectHere.setListening(false);
        this.setRunFlag(false) ;
        clientsConnectHere = null ;
        layer7 = null ;
        //disconnect all the clients
        for( int i = 0 ; i < listOfClients.size() ; i++ ){
            ServeOneClient c = (ServeOneClient)listOfClients.remove(0) ;
            c.Disconnect();
            c = null ;
        }
        MVlisteningThread = null ;
        TDSserver = null;
        server = null ;
    }

    /**
     * @return the timeStarted
     */
    public static Calendar getTimeStarted() {
        return timeStarted;
    }

    /**
     * @param aTimeStarted the timeStarted to set
     */
    public static void setTimeStarted(Calendar aTimeStarted) {
        timeStarted = aTimeStarted;
    }
}
