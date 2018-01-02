package com.example.wjpywl.note;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.net.URL;

public class CheckNote extends AppCompatActivity {

    private String note_name;
    private String note_content;
    private String note_book;
    private TextView name;
    private TextView notebook;
    private TextView content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_note);

        String html = "<html><head><title>TextView使用HTML</title></head><body><p><strong>强调</strong></p><p><em>斜体</em></p>"
                + "<p><a href=\"http://www.dreamdu.com/xhtml/\">超链接HTML入门</a>学习HTML!</p><p><font color=\"#aabb00\">颜色1"
                + "下面是网络图片</p><img src=\"/storage/emulated/0/photos/1514826936120.jpg\"/></body></html>";

        Intent intent=getIntent();
        note_name = intent.getStringExtra("note");

        name = (TextView)findViewById(R.id.name);
        notebook = (TextView)findViewById(R.id.notebook);
        content = (TextView)findViewById(R.id.content);

        content.setMovementMethod(ScrollingMovementMethod.getInstance());// 设置可滚动
        content.setMovementMethod(LinkMovementMethod.getInstance());//设置超链接可以打开网页

        SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
        String[] projection = {};
        String selection = "name = ?";
        String[] selectionArgs = { note_name };
        Cursor c = db.query("note", projection, selection, selectionArgs, null, null, null);
        if (c.moveToNext()) {
            note_name = c.getString(c.getColumnIndex("name"));
            name.setText(note_name);
            note_content = c.getString(c.getColumnIndex("note_content"));
            content.setText(Html.fromHtml(note_content, imgGetter, null));
                //note_book = c.getString(c.getColumnIndex("note_book"));
            notebook.setText(note_book);
        }
        c.close();
        db.close();
    }

    Html.ImageGetter imgGetter = new Html.ImageGetter() {
        public Drawable getDrawable(String source) {
            Log.e("CheckNote",source);
            Drawable drawable = null;
            drawable=Drawable.createFromPath(source);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            return drawable;
        }
    };
}
