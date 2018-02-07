package mu;

import java.util.TimerTask;

public class LicensingTask extends TimerTask
{
    @Override
    public void run()
    {
        CheckLicense();
    }

    private void CheckLicense()
    {
        Licensing temp = new Licensing();
        temp.start();
        try {
            temp.join();
        } catch (InterruptedException ignored) {}

        System.out.println("LICENSE CHECK DONE");
    }
}
