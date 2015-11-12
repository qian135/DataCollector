package com.example.zhan.datacollector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Map;

//显示存储在数据库中的数据
public class ShowData extends Activity {

    ListView mShowDataListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_data);

        Intent intent = getIntent();

        ArrayList<Map<String,Integer>> dataList1 =
                (ArrayList<Map<String,Integer>>)intent.getSerializableExtra("dataListIntentID");

        ArrayAdapter simpleAdapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1,dataList1);

        mShowDataListView = (ListView) findViewById(R.id.showData);
        mShowDataListView.setAdapter(simpleAdapter);

    }

}
