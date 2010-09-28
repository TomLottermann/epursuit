package org.lotterm.asterisk.test;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.BaseAgiScript;

public class HelloAgiScript extends BaseAgiScript
{
    public void service(AgiRequest request, AgiChannel channel)
            throws AgiException
    {
        // Answer the channel...
        answer();
                
        // ...say hello...
        streamFile("welcome");
        //this.recordFile("/home/thomas/Desktop/test.wav", "wav", "10", 60);
        //exec("Record", "sounds/personalintro-%d.wav|10|60");
        //String myvar = getVariable("RECORDED_FILE");
        //exec("Playback", myvar);
        
        // ...and hangup.
        hangup();
    }
}
