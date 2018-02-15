package mu;

import java.io.File;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import mu.ommlib.OMMDB;
import mu.utils.*;

public class OMServer {

	public OMServer()
	{
		String logDir = System.getProperty("user.home") + File.separator + "log" + File.separator;
		System.out.println("Log dir: " + logDir);
		Logger.setLogDir(logDir);
		Logger.setLogName("omserver");
	}

	private void startWorking()
	{
		OMMDB db = new OMMDB();
		if(db.checkDBtables()) {
			Logger.error("Database check failed. Cannot continue.");
			return;
		}

		//start licensing task
			long delay = 10 * 1000;//10 seconds
			long interval = 30 * 60 * 1000;//30 minutes
			LicensingTask task = new LicensingTask();

			//using Timer/TimerTask
			//Timer timer = new Timer();
			//timer.scheduleAtFixedRate(task, delay, interval);

			//using ScheduledExecutorService
			ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
			service.scheduleAtFixedRate(task, delay, interval, TimeUnit.MILLISECONDS);

		//start listener service
			//MUService.startWorking();

		while(true)
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}
			System.out.println("heart bit");
		}
	}

	public static void main(String args[])
	{
		OMServer omserver = new OMServer();
		omserver.startWorking();
		System.out.println("ALL DONE");
	}
}
