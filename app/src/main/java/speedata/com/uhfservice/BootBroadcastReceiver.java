package speedata.com.uhfservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


//开机自启 <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
public class BootBroadcastReceiver extends BroadcastReceiver {
	static final String action_boot = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(action_boot)) {
			Intent ootStartIntent = new Intent(context, UHFService.class);
			context.startService(ootStartIntent);
		}

	}
}
