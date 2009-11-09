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

import java.io.* ;
import org.apache.log4j.*;

public class TTYSx_Receiver implements Runnable
{
	protected static Logger slog = Logger.getLogger(SerialConfigurator.class) ;
	private TTYSx owner ;
	private byte[] buffer ;
	int increment = 0 ;
	int[] intbuf ;
	int intbufIndex ;
	int recBytes ;

	public TTYSx_Receiver(TTYSx owner)
	{
		this.owner = owner ;
		//System.out.println("Thread INIT") ;
		buffer = new byte[512] ;
		intbuf = new int[512] ;
		intbufIndex = 0 ;
		recBytes = 0 ;
	}

	public void run()
	{
		slog.info("TTYSx_Receiver Receiving open:") ;
		while(this.owner.open)
		{
			try {
				synchronized(this) {
					//USED FOR TESTING01 part a
					//System.out.println("Receiver: waiting 1 sec - Iteration: " + ++increment) ;
					wait(3000) ;
				}
			}catch(InterruptedException e)
			{
				slog.info("Receiver INTERRUPTED!") ;
			}

			if( owner.da )
			{	//USED FOR TESTING01 part c
				//System.out.println("**********receiver thread: received some data...") ;
				this.readData() ;
			}
			else ;
				//USED FOR TESTING01 part b
				//System.out.print(" ...owner of this receiving thread returned a false da") ;

		}
	}




	public void readData()
	{
//		String	str;
		int	bytes;
//		long	endTime,
//			now;
		int totalBytes = 0 ;

		//System.out.println("") ;
		try
		{
		    	while (this.owner.open
			 && (this.owner.in.available() > 0 ))
			{
	    	    		bytes = this.owner.in.read(this.buffer);
				totalBytes+=bytes ;
				if (bytes > 0)
				{
					if (bytes > this.buffer.length)
					{
						    slog.warn(owner.sport.getName()
							 + ": Input buffer overflow!");
					}
	
					this.displayText(this.buffer, bytes);

				}
			} 


			owner.da = false;
			recBytes = 0 ;

		}

		catch (IOException ex)
		{
		    	slog.warn(owner.sport.getName()
					   + ": Cannot read input stream");
		}
	}

    private void displayText(byte[]	bytes,int byteCount){
        //System.out.println("2 may 2003, received bytes") ;
        Byte b ;
        for(int z = 0 ; z < byteCount ; z++){
            //System.out.println("Character received: " + Integer.toHexString(bytes[z])) ;
            if( (short)bytes[z] < 0 ){
                owner.toBuffer( (int)bytes[z] + 256 ) ;
                //System.out.println("Character received: " + Integer.toHexString(bytes[z]+256)) ;
            }

            else{
                b = new Byte(bytes[z]) ;
                owner.toBuffer( (int) b.intValue() ) ;
                //System.out.println("Character received: " + Integer.toHexString(bytes[z])) ;
            }

        }
    }

}	
