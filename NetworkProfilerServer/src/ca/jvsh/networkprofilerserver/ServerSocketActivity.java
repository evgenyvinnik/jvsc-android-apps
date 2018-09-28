package ca.jvsh.networkprofilerserver;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class ServerSocketActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_socket);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_server_socket, menu);
        return true;
    }
}
