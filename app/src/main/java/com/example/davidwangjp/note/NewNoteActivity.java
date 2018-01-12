package com.example.davidwangjp.note;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.parseInt;

public class NewNoteActivity extends AppCompatActivity {
    private ImageButton confirm;
    private ImageButton selectImage;
    private ImageButton takePhoto;
    private ImageButton record_button;
    private EditText note_title;
    private EditText note_content;
    private PerformEdit mPerformEdit;
    private TextView notebook;
    private Button markdown;
    private Button preview;
    private Button open;
    private Button redo, undo, bold;
    private String note_book;
    private int mImgViewWidth;
    private Uri imageUri;
    private float mInsertedImgWidth;
    private ArrayList<String> filepathes = new ArrayList<>();
    private String[] note_book_list;
    private boolean isNew;
    private int note_id;

    private final int PICK_PIC = 1;
    private final int TAKE_PIC = 2;
    private final int REC = 3;

    static MainActivity.DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_note);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        Intent intent = getIntent();
        note_id = intent.getIntExtra("noteId",-1);
        Log.e("NewNoteActivity","noteID:"+note_id);
        isNew = note_id < 0;
        note_book = intent.getStringExtra("notebook");

        dbHelper = new MainActivity.DatabaseHelper(NewNoteActivity.this);

        confirm = (ImageButton)findViewById(R.id.image_button_confirm);
        selectImage = (ImageButton)findViewById(R.id.select_image);
        takePhoto = (ImageButton)findViewById(R.id.take_photo);
        record_button = (ImageButton)findViewById(R.id.record_button);
        note_title = (EditText)findViewById(R.id.note_title);
        note_content = (EditText)findViewById(R.id.note_content);
        notebook = (TextView) findViewById(R.id.notebook);
        markdown = (Button)findViewById(R.id.markdown);
        preview = (Button)findViewById(R.id.preview);
        open = (Button)findViewById(R.id.open_note);
        redo = (Button)findViewById(R.id.redo);
        undo = (Button)findViewById(R.id.undo);
        bold = (Button)findViewById(R.id.bold);


        notebook.setText(note_book);
        //note_content.setMovementMethod(ScrollingMovementMethod.getInstance());// 设置可滚动
        mPerformEdit = new PerformEdit(note_content);

        if(!isNew)
        {
            SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
            String[] projection = {};
            String selection = "id = ?";
            String[] selectionArgs = {""+note_id};
            Cursor c = db.query("note", projection, selection, selectionArgs, null, null, null);
            if (c.moveToNext())
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                long time = c.getLong(c.getColumnIndex("update_time"));
                String update_time = sdf.format(new Date(time));
                Log.e("NewNoteActivity",update_time);

                String content = c.getString(c.getColumnIndex("note_content"));

                note_content.setText(Html.fromHtml(content, imgGetter, null));
                Log.e("note_content",note_content.getText().toString());

                String title = c.getString(c.getColumnIndex("name"));
                note_title.setText(title);

                note_book = c.getString(c.getColumnIndex("note_book"));
                notebook.setText(note_book);
            }
            c.close();
            db.close();
        }

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String filename = note_title.getText().toString();
                String filecontent = note_content.getText().toString();
                if(filename.isEmpty())
                    filename = "未命名笔记";
                if(filecontent.isEmpty())
                    Toast.makeText(getApplicationContext(), "内容不能为空", Toast.LENGTH_SHORT).show();
                else {
                    String content = note_content.getText().toString();
                    SpannableString spanString = new SpannableString(content);
                    String html =   Html.toHtml(spanString);
                    html = html.replace("&lt;","<").replace("&gt;",">");
                    String html_string = parseUnicodeToStr(html);
                    Log.e("NewNoteActivity",html_string+content);
                    SaveNote(filename, html_string);
                }
            }
        });

        notebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(NewNoteActivity.this);
                builder.setTitle("选择笔记本");

                //    指定下拉列表的显示数据
                ArrayList<String> notebooks = new ArrayList<>();
                SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
                Cursor c = db.query("notebook", null, null, null, null, null, "name ASC");
                if (c.moveToFirst())
                {
                    do
                    {
                        notebooks.add(c.getString(c.getColumnIndex("name")));
                    }
                    while (c.moveToNext());
                };
                int size = notebooks.size();
                note_book_list = (String[]) notebooks.toArray(new String[size]);

                //    设置一个下拉的列表选择项
                builder.setItems(note_book_list, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        Toast.makeText(NewNoteActivity.this, "选择的笔记本为：" + note_book_list[which], Toast.LENGTH_SHORT).show();
                        note_book = note_book_list[which];
                        notebook.setText(note_book);
                    }
                });
                builder.show();
            }
        });

        redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPerformEdit.redo();
            }
        });

        undo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPerformEdit.undo();
            }
        });

        bold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SpannableStringBuilder ss = new SpannableStringBuilder(note_content.getText());
                ss.setSpan(new StyleSpan(android.graphics.Typeface.BOLD),note_content.getSelectionStart(),note_content.getSelectionEnd(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                note_content.setText(ss);
            }
        });


        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePhone();
            }
        });
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhone();
            }
        });
//        record_button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {record();
//            }
//        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_note, menu);
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //unicode转String
    public String parseUnicodeToStr(String unicodeStr) {
        String regExp = "&#\\d*;";
        Matcher m = Pattern.compile(regExp).matcher(unicodeStr);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String s = m.group(0);
            s = s.replaceAll("(&#)|;", "");
            char c = (char) parseInt(s);
            m.appendReplacement(sb, Character.toString(c));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private void SaveNote(String name, String content) {

        int note_num;
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);
        contentValues.put("note_book", note_book);
        contentValues.put("note_content", content);
        contentValues.put("update_time", new Date().getTime());
        long newRowId;
        if(isNew) {
            contentValues.put("create_time", new Date().getTime());
            newRowId = db.insert("note", null, contentValues);
            String[] projection = {};
            String selection = "name = ?";
            String[] selectionArgs = {note_book};
            Cursor c = db.query("notebook", projection, selection, selectionArgs, null, null, null);
            if (c.moveToNext())
            {
                note_num = c.getInt(c.getColumnIndex("note_num"));
                ContentValues cv2 = new ContentValues();
                cv2.put("note_num",note_num + 1);
                Log.e("NewNoteActivity",""+note_num);
                db.update("notebook", cv2, "name = ?", new String[]{note_book});
            }
            c.close();
            isNew = false;
        }
        else
            newRowId = db.update("note", contentValues, "id = ?", new String[]{note_id+""});

        MainActivity.notebookAdapter.getNotebookData();
        MainActivity.noteAdapter.getNoteData();
        NotebookActivity.setNoteListAdapter();

        db.close();

        if (newRowId == -1)
        {
            Toast.makeText(getApplicationContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(getApplicationContext(), "保存成功", Toast.LENGTH_SHORT).show();
        }
    }

    public void choosePhone(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PICK_PIC);

        }else {
            choosePhoto();
        }

    }

    void choosePhoto(){
        /**
         * 打开选择图片的界面
         */
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");//相片类型
        startActivityForResult(intent, PICK_PIC);

    }

    public void takePhone(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    TAKE_PIC);

        }else {
            takePhoto();
        }

    }

    void takePhoto(){
        /**
         * 创建文件夹
         */
        File file=new File(Environment.getExternalStorageDirectory(),"photos");
        if(!file.exists()){
            file.mkdir();
        }
        /**
         * 将时间作为不同照片的名称
         */
        File output=new File(file,System.currentTimeMillis()+".jpg");

        /**
         * 如果该文件已经存在，则删除它，否则创建一个
         */
        try {
            if (output.exists()) {
                output.delete();
            }
            output.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**
         * 隐式打开拍照的Activity，并且传入TAKE_PIC常量作为拍照结束后回调的标志
         */

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
//        imageUri = Uri.fromFile(output);
//        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

// 系统版本大于N的统一用FileProvider处理
        if (currentapiVersion < 24) {
            // 从文件中创建uri
            Log.e("NewNoteActivity","1111");
            Uri uri = Uri.fromFile(output);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        } else {
            //兼容android7.0 使用共享文件的形式
            ContentValues contentValues = new ContentValues(1);
            contentValues.put(MediaStore.Images.Media.DATA, output.getAbsolutePath());
            Uri uri = this.getApplication().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            imageUri = uri;
            Log.e("NewNoteActivity","2222"+uri.toString()+" "+imageUri);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        }
        startActivityForResult(intent, TAKE_PIC);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_PIC) {
                if (data == null) {
                    Toast.makeText(this, "failed", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        Uri uri = data.getData();
                        if (uri != null)
                            filepathes.add(getRealPathFromURI(uri));
                        Log.e("NewNoteActivity","333"+uri.toString());
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                        SpannableString ss = getBitmapMime(bitmap, uri);
                        insertIntoEditText(ss);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this,"程序崩溃",Toast.LENGTH_SHORT).show();
                    }

                }
            }
            else if (requestCode == TAKE_PIC) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    SpannableString ss = getBitmapMime(bitmap, imageUri);
                    insertIntoEditText(ss);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this,"程序崩溃",Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    static Html.ImageGetter imgGetter = new Html.ImageGetter()
    {
        public Drawable getDrawable(String source)
        {
            Log.e("source", source);
            Drawable drawable = null;
            drawable = Drawable.createFromPath(source);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            return drawable;
        }
    };

    private void insertIntoEditText(SpannableString ss) {
        // 先获取Edittext中原有的内容
        Editable et = note_content.getText();
        int start = note_content.getSelectionStart();
        // 设置ss要添加的位置
        et.insert(start, ss);
        // 把et添加到Edittext中
        note_content.setText(et);
        // 设置Edittext光标在最后显示
        note_content.setSelection(start + ss.length());
    }


    /**
     * EditText中可以接收的图片(要转化为SpannableString)
     *
     * @param pic
     * @param uri
     * @return SpannableString
     */
    private SpannableString getBitmapMime(Bitmap pic, Uri uri) {
        int imgWidth = pic.getWidth();
        int imgHeight = pic.getHeight();
        mInsertedImgWidth = note_content.getWidth()*0.8f;
        //Log.e("NewNoteActivity",imgHeight+" "+imgWidth+" "+mInsertedImgWidth+ " "+note_content.getWidth());
        // 只对大尺寸图片进行下面的压缩，小尺寸图片使用原图
        if (imgWidth >= mInsertedImgWidth) {
            float scale = mInsertedImgWidth / (imgWidth*1.0f);
            Log.e("NewNoteActivity"," "+scale);
            Matrix mx = new Matrix();
            mx.setScale(scale, scale);
            pic = Bitmap.createBitmap(pic, 0, 0, imgWidth, imgHeight, mx, true);
        }
        String tempUrl = "<img src=\"" + getRealPathFromURI(uri) + "\" />";
        SpannableString ss = new SpannableString(tempUrl);
        // 依据Bitmap对象创建ImageSpan对象
        ImageSpan span = new ImageSpan(this, pic);
        // 用ImageSpan对象替换你指定的字符串
        ss.setSpan(span, 0, tempUrl.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case PICK_PIC: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    choosePhoto();

                } else {
                    // permission denied
                    Toast.makeText(NewNoteActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
            default:
                break;

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /** 从uri获取文件路径,uri以content开始 */
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getApplicationContext(),
                contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    /** 获取项目资源的URI */
    private Uri getDrawableURI(int resourcesid) {
        Resources r = getResources();
        Uri uri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + r.getResourcePackageName(resourcesid) + "/"
                + r.getResourceTypeName(resourcesid) + "/"
                + r.getResourceEntryName(resourcesid));
        return uri;
    }
}
