package speedata.com.uhfservice;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by 张明_ on 2017/5/26.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}
