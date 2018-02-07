package mu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;

public class MUService extends Thread
{
    protected static boolean serverContinue = true;
    protected Socket clientSocket;

    public static void startWorking()
    {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(10008);
            System.out.println ("Connection Socket Created");
            try {
                while (serverContinue)
                {
                    System.out.println ("Waiting for Connection");
                    new MUService (serverSocket.accept());
                }
            }
            catch (IOException e)
            {
                System.err.println("Accept failed.");
                System.exit(1);
            }
        }
        catch (IOException e)
        {
            System.err.println("Could not listen on port: 10008.");
            System.exit(1);
        }
        finally
        {
            try {
                serverSocket.close();
            }
            catch (IOException e)
            {
                System.err.println("Could not close port: 10008.");
                System.exit(1);
            }
        }
    }

    private MUService (Socket clientSoc)
    {
        clientSocket = clientSoc;
        start();
    }

    public void run()
    {
        System.out.println ("New Communication Thread Started");

        try {
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
            BufferedReader in = new BufferedReader(new InputStreamReader( clientSocket.getInputStream()));

            String inputLine;

            while ((inputLine = in.readLine()) != null)
            {
                System.out.println ("Server: " + inputLine);
                out.println(inputLine);

                if (inputLine.equals("Bye."))
                    break;

                if (inputLine.equals("End Server."))
                    serverContinue = false;
            }

            out.close();
            in.close();
            clientSocket.close();
        }
        catch (IOException e)
        {
            System.err.println("Problem with Communication Server");
            System.exit(1);
        }
    }
}
