package obsidian.aureader;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayList<String> filePathList;
    ArrayList<String> fileNameList;

    String debugTag="true warrior debugs code";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        listView = (ListView) findViewById(R.id.allFiles);
        fileNameList=new ArrayList<String>();
        filePathList=new ArrayList<String>();

        Log.i(debugTag, "Loading wait");
        getFileList(Environment.getExternalStorageDirectory());
        Log.i(debugTag, "Loaded");

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileNameList);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.i(debugTag, filePathList.get(i));
                Intent startReaderActivity=new Intent(MainActivity.this, ReaderActivity.class);
                startReaderActivity.putExtra("filePath", filePathList.get(i));
                startActivity(startReaderActivity);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getFileList(File dir) {
        String pattern=".epub";
        File listFile[]=dir.listFiles();

        if(listFile!=null) {
            for(File file: listFile) {
                if(file.isDirectory())
                    getFileList(file);
                else if(file.getName().endsWith(pattern)) {
                    fileNameList.add(file.getName());
                    filePathList.add(file.toString());
                }
            }
        }
    }
}
