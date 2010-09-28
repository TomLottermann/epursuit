package org.lotterm.asterisk.test;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.AsteriskQueueEntry;
import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.AsteriskServerListener;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.LiveException;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.MeetMeUser;
import org.asteriskjava.live.OriginateCallback;
import org.asteriskjava.live.internal.AsteriskAgentImpl;

public class HelloLiveEvents implements AsteriskServerListener
{
    private AsteriskServer asteriskServer;

    public HelloLiveEvents()
    {
        asteriskServer = new DefaultAsteriskServer("192.168.2.33", "callomat", "blubb");
        asteriskServer.originateToExtensionAsync("SIP/1000", "phones", "1301", 1, 10000, new OriginateCallback() {
			
			@Override
			public void onSuccess(AsteriskChannel channel) {
				System.out.println("success: "+channel);
			}
			
			@Override
			public void onNoAnswer(AsteriskChannel channel) {
				System.out.println("noanswer: "+channel);
			}
			
			@Override
			public void onFailure(LiveException cause) {
				System.out.println("exception: "+cause);
			}
			
			@Override
			public void onDialing(AsteriskChannel channel) {
				System.out.println("dialing: "+channel);
			}
			
			@Override
			public void onBusy(AsteriskChannel channel) {
				System.out.println("busy: "+channel);
			}
		});
    }

    public void run() throws ManagerCommunicationException
    {
        asteriskServer.addAsteriskServerListener(this);
        try {
			Thread.sleep(2000000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void onNewAsteriskChannel(AsteriskChannel channel)
    {

    }

    public void onNewMeetMeUser(MeetMeUser user)
    {

    }

    public static void main(String[] args) throws Exception
    {
        HelloLiveEvents helloLiveEvents = new HelloLiveEvents();
        helloLiveEvents.run();
    }

	@Override
	public void onNewAgent(AsteriskAgentImpl agent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onNewQueueEntry(AsteriskQueueEntry entry) {
		// TODO Auto-generated method stub
		
	}

}