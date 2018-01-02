package com.example.wjpywl.note;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import us.feras.mdv.MarkdownView;

public class MarkDown extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mark_down);

        Intent intent=getIntent();
        String s=intent.getStringExtra("data");

        MarkdownView markdownView = (MarkdownView) findViewById(R.id.markdownView);
        markdownView.loadMarkdown(s);
    }
}
