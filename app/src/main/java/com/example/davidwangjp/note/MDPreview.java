package com.example.davidwangjp.note;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import us.feras.mdv.MarkdownView;

public class MDPreview extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mdpreview);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("MarkDown预览");

        Intent intent=getIntent();
        String s=intent.getStringExtra("data");

        MarkdownView markdownView = (MarkdownView) findViewById(R.id.markdownView);
        markdownView.loadMarkdown(s);
    }

}
