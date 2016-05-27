package com.falling.slidecutlistviewsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.falling.slidecutlistview.SlideCutListView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SlideCutListView slideCutListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        slideCutListView = (SlideCutListView) findViewById(R.id.list);
        slideCutListView.setRemoveListener(new SlideCutListView.RemoveListener() {
            @Override
            public void removeItem(int direction, int position) {
                Toast.makeText(MainActivity.this,"move to"+direction,Toast.LENGTH_SHORT).show();
            }
        });

        ArrayAdapter<String> mArrayAdapter = null;
        List<String> sourse = new ArrayList<>();
        sourse.add("1");
        sourse.add("2");
        sourse.add("3");
        sourse.add("4");
        sourse.add("5");
        mArrayAdapter= new ArrayAdapter<>(this, R.layout.item,R.id.list_item, sourse);
        slideCutListView.setAdapter(mArrayAdapter);

    }
}
