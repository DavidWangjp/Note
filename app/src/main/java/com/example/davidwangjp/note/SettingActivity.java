package com.example.davidwangjp.note;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

import static com.example.davidwangjp.note.MainActivity.defaultNotebook;
import static com.example.davidwangjp.note.MainActivity.globalNotebookCards;

public class SettingActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("设置");

        ListView settingList = (ListView) findViewById(R.id.setting_list);
        ArrayList<String> list = new ArrayList<>();
        list.add("默认笔记本");
        settingList.setAdapter(new ArrayAdapter<>(this, R.layout.setting_list_item, R.id.set_default_notebook, list));
        settingList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                switch (position)
                {
                    case 0:
                    {
                        AlertDialog.Builder builder = setSelectDefaultNotebookDialog();
                        builder.create().show();
                    }
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
        }
        return true;
    }

    AlertDialog.Builder setSelectDefaultNotebookDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
        final String[] notebookNames = new String[globalNotebookCards.size()];
        int defualtNotebookIndex = 0;
        for (int i = 0; i < globalNotebookCards.size(); i++)
        {
            notebookNames[i] = globalNotebookCards.get(i).name;
            if (notebookNames[i].equals(defaultNotebook))
                defualtNotebookIndex = i;

        }
        builder.setTitle("选择默认笔记本");
        builder.setSingleChoiceItems(notebookNames, defualtNotebookIndex, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                setDefaultNotebook(notebookNames[which]);
                MainActivity.defaultNotebook = getDefaultNotebook();
                dialog.dismiss();
            }
        });
        return builder;
    }

    String getDefaultNotebook()
    {
        SharedPreferences sharedPreferences = getSharedPreferences("settings", 0);
        String s = sharedPreferences.getString("default-notebook", null);
        return (s != null && !s.isEmpty()) ? s : null;
    }

    void setDefaultNotebook(String name)
    {
        SharedPreferences sharedPreferences = getSharedPreferences("settings", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("default-notebook", name);
        editor.apply();
    }
}
