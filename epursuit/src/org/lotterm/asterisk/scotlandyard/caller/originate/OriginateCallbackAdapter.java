package org.lotterm.asterisk.scotlandyard.caller.originate;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.LiveException;
import org.asteriskjava.live.OriginateCallback;

public class OriginateCallbackAdapter implements OriginateCallback {

	@Override
	public void onDialing(AsteriskChannel channel) {

	}

	@Override
	public void onSuccess(AsteriskChannel channel) {

	}

	@Override
	public void onNoAnswer(AsteriskChannel channel) {

	}

	@Override
	public void onBusy(AsteriskChannel channel) {

	}

	@Override
	public void onFailure(LiveException cause) {

	}

}
