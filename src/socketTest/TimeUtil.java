

import java.text.SimpleDateFormat;

public class TimeUtil {
	
	/**
	 * Class to generate timestamps with microsecond precision
	 * For example: MicroTimestamp.INSTANCE.get() = "2012-10-21 19:13:45.267128"
	 */
	public enum MicroTimestamp
	{  INSTANCE ;

	   private long              startDate ;
	   private long              startNanoseconds ;
	   private SimpleDateFormat  dateFormat ;

	   private MicroTimestamp()
	   {  this.startDate = System.currentTimeMillis() ;
	      this.startNanoseconds = System.nanoTime() ;
	      this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS") ;
	   }

	   public String get()
	   {  long microSeconds = (System.nanoTime() - this.startNanoseconds) / 1000 ;
	      long date = this.startDate + (microSeconds/1000) ;
	      return this.dateFormat.format(date) + String.format("%03d", microSeconds % 1000) ;
	   }
	}


}
