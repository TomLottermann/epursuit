package org.lotterm.asterisk.test;

import java.io.IOException;

import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.OriginateAction;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.response.ManagerResponse;

public class HelloManager
{
    private ManagerConnection managerConnection;

    public HelloManager() throws IOException
    {
        ManagerConnectionFactory factory = new ManagerConnectionFactory(
                "192.168.2.33", "callomat", "blubb");

        this.managerConnection = factory.createManagerConnection();
    }

	public void run() throws IOException, AuthenticationFailedException,
            TimeoutException
    {

		OriginateAction originateAction;
        ManagerResponse originateResponse;

        originateAction = new OriginateAction();
        originateAction.setChannel("SIP/2000"); //SIP/1083484/01703846797
        originateAction.setContext("phones");
        originateAction.setExten("mrx");
        originateAction.setPriority(new Integer(1));
        originateAction.setTimeout(new Long(30000));
        
        // connect to Asterisk and log in
        managerConnection.login();

        // send the originate action and wait for a maximum of 30 seconds for Asterisk
        // to send a reply
        originateResponse = managerConnection.sendAction(originateAction, 30000);
        System.out.println(originateAction.getChannel());

        // print out whether the originate succeeded or not
        System.out.println(originateResponse.getResponse());

        // and finally log off and disconnect
        managerConnection.logoff();
        

        
        System.out.println("BLUUUUUUUUUUUUUUB");
    }

    public static void main(String[] args) throws Exception
    {
        HelloManager helloManager;

        helloManager = new HelloManager();
        helloManager.run();
        
        System.out.println("BLAAAAAAAAAAAAA");
    }
}