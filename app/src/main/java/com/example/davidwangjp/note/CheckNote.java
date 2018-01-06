package com.example.davidwangjp.note;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CheckNote extends AppCompatActivity
{

    private String note_book;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_note);

        String html = "<html><head><title>TextView使用HTML</title></head><body><p><strong>强调</strong></p><p><em>斜体</em></p>"
                + "<p><a href=\"http://www.dreamdu.com/xhtml/\">超链接HTML入门</a>学习HTML!</p><p><font color=\"#aabb00\">颜色1"
                + "下面是网络图片</p><img src=\"/storage/emulated/0/photos/1514826936120.jpg\"/></body></html>";

        Intent intent = getIntent();
        String note_name = intent.getStringExtra("note");

        TextView name = findViewById(R.id.name);
        TextView notebook = findViewById(R.id.notebook);
        TextView content = findViewById(R.id.content);

        content.setMovementMethod(ScrollingMovementMethod.getInstance());// 设置可滚动
        content.setMovementMethod(LinkMovementMethod.getInstance());//设置超链接可以打开网页

        SQLiteDatabase db = NewNoteActivity.dbHelper.getReadableDatabase();
        String[] projection = {};
        String selection = "name = ?";
        String[] selectionArgs = {note_name};
        Cursor c = db.query("note", projection, selection, selectionArgs, null, null, null);
        if (c.moveToNext())
        {
            note_name = c.getString(c.getColumnIndex("name"));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            long time = c.getLong(c.getColumnIndex("create_time"));
            Log.e("CheckNote",""+time);
            String create_time = sdf.format(new Date(time));
            name.setText(create_time);


            String note_content = c.getString(c.getColumnIndex("note_content"));
            content.setText(Html.fromHtml(note_content, imgGetter, null));
            //note_book = c.getString(c.getColumnIndex("note_book"));
            notebook.setText(note_book);
        }
        c.close();
        db.close();
    }

    Html.ImageGetter imgGetter = new Html.ImageGetter()
    {
        public Drawable getDrawable(String source)
        {
            Log.e("CheckNote", source);
            Drawable drawable = null;
            drawable = Drawable.createFromPath(source);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            return drawable;
        }
    };
}
