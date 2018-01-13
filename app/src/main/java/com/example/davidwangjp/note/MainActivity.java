package com.example.davidwangjp.note;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener
{
    enum NoteSortMode
    {
        LATEST_MODIFY, TITLE, CREATE_DATE
    }

    enum NotebookSortMode
    {
        NAME, NOTE_NUM
    }

    enum FragmentMode
    {
        ALL_NOTES, NOTEBOOKS
    }

    static final ArrayList<NoteCard> globalNoteCards = new ArrayList<>();
    static final ArrayList<NotebookCard> globalNotebookCards = new ArrayList<>();

    static DatabaseHelper dbHelper;

    static String defaultNotebook;

    static NoteSortMode noteSortMode = NoteSortMode.LATEST_MODIFY;
    static NotebookSortMode notebookSortMode = NotebookSortMode.NAME;
    FragmentMode fragmentMode = FragmentMode.ALL_NOTES;

    NoteListFragment noteListFragment = new NoteListFragment();
    NotebookListFragment notebookListFragment = new NotebookListFragment();
    static RecyclerView noteRecyclerView, notebookRecyclerView;
    static NoteListAdapter noteAdapter;
    static NotebookListAdapter notebookAdapter;

    Toolbar toolbar;
    FloatingActionsMenu newNoteMenu;

    private long exitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(MainActivity.this);

        defaultNotebook = getDefaultNotebook();


//        for (int i = 0; i < 50; i++)
//        {
//            Random random = new Random();
//            globalNoteCards.add(new NoteCard("Title" + random.nextInt(50), "1/1/" + random.nextInt(30), "Content" + i));
//        }

//        for (int i = 0; i < 10; i++)
//        {
//            Random random = new Random();
//            globalNotebookCards.add(new NotebookCard("Name" + random.nextInt(10), random.nextInt(100)));
//        }

        noteRecyclerView = LayoutInflater.from(MainActivity.this).inflate(R.layout.note_list, null).findViewById(R.id.note_list);
        noteRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteListAdapter();
        noteAdapter.setOnItemClickListener(new NoteListAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(View view, int position)
            {
                if (noteAdapter.isMultiSelect)
                    noteAdapter.selectItem(position);
                else
                {
                    Intent intent = new Intent(MainActivity.this, NewNoteActivity.class);
                    intent.putExtra("noteId", noteAdapter.noteCards.get(position).id);
                    intent.putExtra("notebook", noteAdapter.noteCards.get(position).notebook);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position)
            {
                noteAdapter.isMultiSelect = !noteAdapter.isMultiSelect;
                if (!noteAdapter.isMultiSelect)
                    noteAdapter.clearAll();
                else
                    noteAdapter.selectItem(position);
                noteAdapter.notifyDataSetChanged();

                supportInvalidateOptionsMenu();
            }
        });
        noteRecyclerView.setAdapter(noteAdapter);

        notebookRecyclerView = LayoutInflater.from(MainActivity.this).inflate(R.layout.notebook_list, null).findViewById(R.id.notebook_list);
        notebookRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notebookAdapter = new NotebookListAdapter();
        notebookAdapter.setOnItemClickListener(new NotebookListAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(View view, int position)
            {
                Intent intent = new Intent(MainActivity.this, NotebookActivity.class);
                intent.putExtra("notebook_name", notebookAdapter.notebookCards.get(position).name);
                startActivity(intent);
            }

            @Override
            public void onItemLongClick(View view, int position)
            {
                AlertDialog.Builder builder = setModifyNotebookDialog(position);
                builder.create().show();
            }
        });
        notebookRecyclerView.setAdapter(notebookAdapter);

        noteListFragment.setArguments(getIntent().getExtras());
        notebookListFragment.setArguments(getIntent().getExtras());


        if (savedInstanceState == null)
        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.add(R.id.fragment_container, noteListFragment).commit();
        }

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("所有笔记");
        setSupportActionBar(toolbar);

        newNoteMenu = findViewById(R.id.floating_action_menu);
        FloatingActionButton newNote = findViewById(R.id.new_note);


        newNote.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (defaultNotebook == null)
                {
                    if (globalNotebookCards.isEmpty())
                    {
                        Toast.makeText(getApplicationContext(), "没有笔记本，请新建", Toast.LENGTH_SHORT).show();
                        AlertDialog.Builder builder = setNewNotebookDialog();
                        final AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new AddNotebookPositiveButtonListenser(alertDialog, true));
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "没有默认笔记本，请选择", Toast.LENGTH_SHORT).show();
                        AlertDialog.Builder builder = setSelectDefaultNotebookDialog();
                        builder.create().show();
                    }
                }
                else
                {
                    Intent intent = new Intent(MainActivity.this, NewNoteActivity.class);
                    intent.putExtra("noteId", -1);
                    intent.putExtra("notebook", defaultNotebook);
                    startActivity(intent);
                    Toast.makeText(getApplicationContext(), defaultNotebook, Toast.LENGTH_SHORT).show();
                }

            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed()
    {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else if (noteAdapter.isMultiSelect)
        {
            noteAdapter.clearAll();
            noteAdapter.isMultiSelect = false;
            noteAdapter.notifyDataSetChanged();
            supportInvalidateOptionsMenu();
        }
        else if (newNoteMenu.isExpanded())
            newNoteMenu.collapse();
        else if ((System.currentTimeMillis() - exitTime) > 2000)
        {
            Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.findItem(R.id.action_delete_note).setVisible(noteAdapter.isMultiSelect);
        menu.findItem(R.id.move_note).setVisible(noteAdapter.isMultiSelect);


        boolean isAllNotes = (fragmentMode == FragmentMode.ALL_NOTES);
        menu.findItem(R.id.set_note_sorting_way).setVisible(isAllNotes);
        menu.findItem(R.id.search_note).setVisible(isAllNotes);
        menu.findItem(R.id.search_note).setVisible(isAllNotes);

        boolean isNotebooks = (fragmentMode == FragmentMode.NOTEBOOKS);
        menu.findItem(R.id.set_notebook_sorting_way).setVisible(isNotebooks);
        menu.findItem(R.id.search_notebook).setVisible(isNotebooks);
        menu.findItem(R.id.add_notebook).setVisible(isNotebooks);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem searchNoteItem = menu.findItem(R.id.search_notebook);
        SearchView searchNote = (SearchView) MenuItemCompat.getActionView(searchNoteItem);
        searchNote.setSubmitButtonEnabled(true);
        searchNote.setQueryHint("搜索笔记本");
        searchNote.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                notebookAdapter.notebookCards.clear();
                for (NotebookCard notebookCard : globalNotebookCards)
                {
                    if (notebookCard.name.contains(query))
                        notebookAdapter.notebookCards.add(notebookCard);
                }

                notebookAdapter.notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                return false;
            }

        });
        searchNote.setOnCloseListener(new SearchView.OnCloseListener()
        {
            @Override
            public boolean onClose()
            {
                notebookAdapter.getNotebookData();
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        switch (id)
        {
            case R.id.action_settings:
                return true;
            case R.id.action_delete_note:
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("确认删除");
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        SQLiteDatabase db = dbHelper.getReadableDatabase();
                        for (int i = 0; i < noteAdapter.noteCards.size(); i++)
                        {
                            if (noteAdapter.ifPositionSelected.get(i))
                            {
                                int noteId = noteAdapter.noteCards.get(i).id;
                                db.delete("note", "id = ?", new String[]{String.valueOf(noteId)});
                                for (NotebookCard notebookCard : notebookAdapter.notebookCards)
                                {
                                    if (Objects.equals(notebookCard.name, noteAdapter.noteCards.get(i).notebook))
                                    {
                                        ContentValues contentValues = new ContentValues();
                                        contentValues.put("name", notebookCard.name);
                                        notebookCard.noteNum -= 1;
                                        contentValues.put("note_num", notebookCard.noteNum);
                                        db.update("notebook", contentValues, "name = ?", new String[]{notebookCard.name});
                                        break;
                                    }
                                }

                            }
                        }
                        notebookAdapter.getNotebookData();
                        noteAdapter.getNoteData();
                        noteAdapter.clearAll();
                        noteAdapter.notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {

                    }
                });
                builder.create().show();
                return true;
            }
            case R.id.move_note:
            {
                final String[] moveToNotebook = new String[1];
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final String[] notebookNames = new String[globalNotebookCards.size()];
                for (int i = 0; i < globalNotebookCards.size(); i++)
                    notebookNames[i] = globalNotebookCards.get(i).name;
                builder.setTitle("移动至");
                builder.setItems(notebookNames, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        moveToNotebook[0] = notebookNames[which];
                        SQLiteDatabase db = dbHelper.getReadableDatabase();
                        for (int i = 0; i < noteAdapter.noteCards.size(); i++)
                        {
                            if (noteAdapter.ifPositionSelected.get(i) && !noteAdapter.noteCards.get(i).notebook.equals(moveToNotebook[0]))
                            {
                                int noteId = noteAdapter.noteCards.get(i).id;
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("note_book", moveToNotebook[0]);
                                db.update("note", contentValues, "id = ?", new String[]{String.valueOf(noteId)});
                                for (NotebookCard notebookCard : notebookAdapter.notebookCards)
                                {
                                    if (notebookCard.name.equals(noteAdapter.noteCards.get(i).notebook))
                                    {
                                        ContentValues contentValues1 = new ContentValues();
                                        notebookCard.noteNum -= 1;
                                        contentValues1.put("note_num", notebookCard.noteNum);
                                        db.update("notebook", contentValues1, "name = ?", new String[]{notebookCard.name});
                                    }
                                    else if (notebookCard.name.equals(moveToNotebook[0]))
                                    {
                                        ContentValues contentValues1 = new ContentValues();
                                        notebookCard.noteNum += 1;
                                        contentValues1.put("note_num", notebookCard.noteNum);
                                        db.update("notebook", contentValues1, "name = ?", new String[]{notebookCard.name});
                                    }
                                }
                            }
                        }
                        notebookAdapter.getNotebookData();
                        noteAdapter.getNoteData();
                        noteAdapter.clearAll();
                        noteAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), moveToNotebook[0], Toast.LENGTH_SHORT).show();
                    }
                });
                builder.create().show();
                return true;
            }
            case R.id.set_note_sorting_way:
            {
                AlertDialog.Builder builder = setChangeNoteSortingWayDialog();
                builder.create().show();
                return true;
            }
            case R.id.search_note:
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, SearchNoteActivity.class);
                intent.putExtra("notebook_name", "");
                startActivity(intent);
                return true;
            case R.id.set_notebook_sorting_way:
            {
                AlertDialog.Builder builder = setChangeNotebookSortingWayDialog();
                builder.create().show();
                return true;
            }
            case R.id.add_notebook:
            {
                AlertDialog.Builder builder = setNewNotebookDialog();
                final AlertDialog alertDialog = builder.create();
                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new AddNotebookPositiveButtonListenser(alertDialog, false));
                return true;
            }

        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        int id = item.getItemId();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (id)
        {
            case R.id.all_notes:
                toolbar.setTitle("所有笔记");
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                transaction.replace(R.id.fragment_container, noteListFragment).commit();
                fragmentMode = FragmentMode.ALL_NOTES;
                supportInvalidateOptionsMenu();
                break;
            case R.id.all_notebooks:
                toolbar.setTitle("笔记本");
                transaction = getSupportFragmentManager().beginTransaction();
                transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                transaction.replace(R.id.fragment_container, notebookListFragment).commit();
                if (noteAdapter.isMultiSelect)
                {
                    noteAdapter.clearAll();
                    noteAdapter.isMultiSelect = false;
                    noteAdapter.notifyDataSetChanged();
                }
                fragmentMode = FragmentMode.NOTEBOOKS;
                supportInvalidateOptionsMenu();
                break;
            case R.id.setting:
                startActivity(new Intent(MainActivity.this, SettingActivity.class));
        }
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public static class NoteListFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            return noteRecyclerView;
        }
    }

    public static class NotebookListFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            //return inflater.inflate(R.layout.notebook_card, container, false);
            return notebookRecyclerView;
        }
    }

    static class NoteListAdapter extends RecyclerView.Adapter<NoteListAdapter.ViewHolder>
    {
        ArrayList<NoteCard> noteCards = new ArrayList<>();
        Map<Integer, Boolean> ifPositionSelected = new HashMap<>();
        boolean isMultiSelect = false;

        class ViewHolder extends RecyclerView.ViewHolder
        {
            CardView noteCard;
            TextView title;
            TextView createDate;
            TextView updateDate;
            TextView content;
            LinearLayout linearLayout;
            CheckBox checkBox;

            ViewHolder(CardView v)
            {
                super(v);
                noteCard = v;
                title = v.findViewById(R.id.note_title);
                createDate = v.findViewById(R.id.note_date);
                updateDate = v.findViewById(R.id.note_update);
                content = v.findViewById(R.id.note_content);
                linearLayout = v.findViewById(R.id.note_card_background);
                checkBox = v.findViewById(R.id.note_card_checkbox);
            }
        }

        void getNoteData()
        {
            globalNoteCards.clear();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String sortOrder = null;
            switch (noteSortMode)
            {
                case LATEST_MODIFY:
                    sortOrder = "update_time DESC";
                    break;
                case TITLE:
                    sortOrder = "name ASC";
                    break;
                case CREATE_DATE:
                    sortOrder = "create_time DESC";
                    break;
            }
            Cursor c = db.query("note", null, null, null, null, null, sortOrder);
            if (c.moveToFirst())
            {
                do
                {
                    int id = c.getInt(c.getColumnIndex("id"));
                    String title = c.getString(c.getColumnIndex("name"));
                    String content = c.getString(c.getColumnIndex("note_content"));
                    long createTime = c.getLong(c.getColumnIndex("create_time"));
                    long updateTime = c.getLong(c.getColumnIndex("update_time"));
                    String notebook = c.getString(c.getColumnIndex("note_book"));
                    String createDate = new SimpleDateFormat("yyyy-MM-dd HH:mm")
                            .format(new Date(createTime));
                    String updateDate = new SimpleDateFormat("yyyy-MM-dd HH:mm")
                            .format(new Date(updateTime));
                    createDate = createDate.split(" ")[0];
                    updateDate = updateDate.split(" ")[0];
                    globalNoteCards.add(new NoteCard(id, title, createDate, updateDate, content, notebook));
                }
                while (c.moveToNext());
            }
            c.close();
            this.noteCards = (ArrayList<NoteCard>) globalNoteCards.clone();
            for (int i = 0; i < noteCards.size(); i++)
                ifPositionSelected.put(i, false);
            this.notifyDataSetChanged();
        }

        NoteListAdapter()
        {
            getNoteData();
        }

        NoteListAdapter(ArrayList<NoteCard> noteCards)
        {
            this.noteCards = noteCards;
            for (int i = 0; i < noteCards.size(); i++)
                ifPositionSelected.put(i, false);
        }

        @Override
        public NoteListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_card, parent, false);
            return new ViewHolder((CardView) v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position)
        {
            holder.title.setText(noteCards.get(position).title);
            holder.createDate.setText(noteCards.get(position).createDate);
            holder.updateDate.setText(noteCards.get(position).updateTime);
//            holder.content.setText(noteCards.get(position).content);
            holder.content.setText(Html.fromHtml(noteCards.get(position).content, NewNoteActivity.imgGetter, null));

            if (isMultiSelect)
                holder.checkBox.setVisibility(View.VISIBLE);
            else
                holder.checkBox.setVisibility(View.GONE);

            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
                {
                    ifPositionSelected.put(position, isChecked);
                }
            });

            if (ifPositionSelected.get(position) == null)
                ifPositionSelected.put(position, false);

            holder.checkBox.setChecked(ifPositionSelected.get(position));

            if (onItemClickListener != null)
            {
                holder.itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        onItemClickListener.onItemClick(v, position);
                    }
                });
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener()
                {
                    @Override
                    public boolean onLongClick(View v)
                    {
                        onItemClickListener.onItemLongClick(v, position);
                        return true;
                    }
                });
            }
        }

        @Override
        public int getItemCount()
        {
            return noteCards.size();
        }

        interface OnItemClickListener
        {
            void onItemClick(View view, int position);

            void onItemLongClick(View view, int position);
        }

        private OnItemClickListener onItemClickListener;

        void setOnItemClickListener(OnItemClickListener onItemClickListener)
        {
            this.onItemClickListener = onItemClickListener;
        }

        void selectItem(int position)
        {
            if (ifPositionSelected.get(position))
                ifPositionSelected.put(position, false);
            else
                ifPositionSelected.put(position, true);
            notifyDataSetChanged();
        }

        void clearAll()
        {
            for (int i = 0; i < noteCards.size(); i++)
                ifPositionSelected.put(i, false);
            notifyDataSetChanged();
        }
    }

    static class NotebookListAdapter extends RecyclerView.Adapter<NotebookListAdapter.ViewHolder>
    {
        ArrayList<NotebookCard> notebookCards = new ArrayList<>();

        class ViewHolder extends RecyclerView.ViewHolder
        {
            CardView notebookCard;
            TextView name;
            TextView noteNum;

            ViewHolder(CardView v)
            {
                super(v);
                notebookCard = v;
                name = v.findViewById(R.id.notebook_name);
                noteNum = v.findViewById(R.id.notebook_note_num);
            }
        }

        void getNotebookData()
        {
            globalNotebookCards.clear();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String sortOrder = null;
            switch (notebookSortMode)
            {
                case NAME:
                    sortOrder = "name ASC";
                    break;
                case NOTE_NUM:
                    sortOrder = "note_num ASC";
                    break;
            }
            Cursor c = db.query("notebook", null, null, null, null, null, sortOrder);
            if (c.moveToFirst())
            {
                do
                {
                    String name = c.getString(c.getColumnIndex("name"));
                    int note_num = c.getInt(c.getColumnIndex("note_num"));
                    globalNotebookCards.add(new NotebookCard(name, note_num));
                }
                while (c.moveToNext());
            }
            this.notebookCards = (ArrayList<NotebookCard>) globalNotebookCards.clone();
            this.notifyDataSetChanged();
        }

        NotebookListAdapter()
        {
            getNotebookData();
        }

        @Override
        public NotebookListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
        {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.notebook_card, parent, false);
            return new ViewHolder((CardView) v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position)
        {

            holder.name.setText(notebookCards.get(position).name);
            holder.noteNum.setText(notebookCards.get(position).noteNum + "条笔记");
            if (onItemClickListener != null)
            {
                holder.itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        onItemClickListener.onItemClick(v, position);
                    }
                });
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener()
                {
                    @Override
                    public boolean onLongClick(View v)
                    {
                        onItemClickListener.onItemLongClick(v, position);
                        return true;
                    }
                });
            }
        }

        @Override
        public int getItemCount()
        {
            return notebookCards.size();
        }

        interface OnItemClickListener
        {
            void onItemClick(View view, int position);

            void onItemLongClick(View view, int position);
        }

        private OnItemClickListener onItemClickListener;

        void setOnItemClickListener(OnItemClickListener onItemClickListener)
        {
            this.onItemClickListener = onItemClickListener;
        }


    }

    AlertDialog.Builder setChangeNoteSortingWayDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        String[] sortingWays = {"最后修改时间", "标题", "创建日期"};
        builder.setTitle("排序方式");
        builder.setSingleChoiceItems(sortingWays, noteSortMode.ordinal(), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch (which)
                {
                    case 0:
                        noteSortMode = NoteSortMode.LATEST_MODIFY;
                        Collections.sort(noteAdapter.noteCards, new Comparator<NoteCard>()
                        {
                            @Override
                            public int compare(NoteCard o1, NoteCard o2)
                            {
                                return o2.updateTime.compareTo(o1.updateTime);
                            }
                        });
                        noteAdapter.notifyDataSetChanged();
                        break;
                    case 1:
                        noteSortMode = NoteSortMode.TITLE;
                        Collections.sort(noteAdapter.noteCards, new Comparator<NoteCard>()
                        {
                            @Override
                            public int compare(NoteCard o1, NoteCard o2)
                            {
                                return o1.title.compareTo(o2.title);
                            }
                        });
                        noteAdapter.notifyDataSetChanged();
                        break;
                    case 2:
                        noteSortMode = NoteSortMode.LATEST_MODIFY;
                        Collections.sort(noteAdapter.noteCards, new Comparator<NoteCard>()
                        {
                            @Override
                            public int compare(NoteCard o1, NoteCard o2)
                            {
                                return o2.createDate.compareTo(o1.createDate);
                            }
                        });
                        noteAdapter.notifyDataSetChanged();
                        break;
                }
                dialog.dismiss();
            }
        });
        return builder;
    }

    AlertDialog.Builder setChangeNotebookSortingWayDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        String[] sortingWays = {"名称", "笔记数"};
        builder.setTitle("排序方式");
        builder.setSingleChoiceItems(sortingWays, notebookSortMode.ordinal(), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch (which)
                {
                    case 0:
                        notebookSortMode = NotebookSortMode.NAME;
                        Collections.sort(notebookAdapter.notebookCards, new Comparator<NotebookCard>()
                        {
                            @Override
                            public int compare(NotebookCard o1, NotebookCard o2)
                            {
                                return o1.name.compareTo(o2.name);
                            }
                        });
                        notebookAdapter.notifyDataSetChanged();
                        break;
                    case 1:
                        notebookSortMode = NotebookSortMode.NOTE_NUM;
                        Collections.sort(notebookAdapter.notebookCards, new Comparator<NotebookCard>()
                        {
                            @Override
                            public int compare(NotebookCard o1, NotebookCard o2)
                            {
                                return Integer.valueOf(o1.noteNum).compareTo(o2.noteNum);
                            }
                        });
                        notebookAdapter.notifyDataSetChanged();
                        break;
                }
                dialog.dismiss();
            }
        });
        return builder;
    }

    AlertDialog.Builder setModifyNotebookDialog(final int position)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        String[] modifyOptions = {"重命名", "删除"};
        builder.setTitle("笔记本选项");
        builder.setItems(modifyOptions, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                switch (which)
                {
                    case 0:
                    {
                        AlertDialog.Builder builder = setRenameNotebookDialog(position);
                        final AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                final EditText editText = alertDialog.findViewById(R.id.dialog_edit);
                                String name = editText.getText().toString();
                                if (name.isEmpty())
                                {
                                    Toast.makeText(getApplicationContext(), "请输入笔记本名称", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                SQLiteDatabase db = dbHelper.getReadableDatabase();
                                String[] projection = {};
                                String selection = "name = ?";
                                String[] selectionArgs = {name};
                                Cursor c = db.query("notebook", projection, selection, selectionArgs, null, null, null);
                                if (c.moveToFirst())
                                    Toast.makeText(getApplicationContext(), "已存在同名笔记本", Toast.LENGTH_SHORT).show();
                                else
                                {
                                    ContentValues contentValues = new ContentValues();
                                    contentValues.put("name", name);
                                    contentValues.put("note_num", notebookAdapter.notebookCards.get(position).noteNum);
                                    for (NoteCard noteCard : noteAdapter.noteCards)
                                    {
                                        if (noteCard.notebook.equals(notebookAdapter.notebookCards.get(position).name))
                                        {
                                            ContentValues contentValues1 = new ContentValues();
                                            contentValues1.put("note_book", name);
                                            db.update("note", contentValues1, "id = ?",
                                                    new String[]{String.valueOf(noteCard.id)});
                                        }
                                    }
                                    int count = db.update("notebook", contentValues, "name = ?",
                                            new String[]{notebookAdapter.notebookCards.get(position).name});
                                    if (count == -1)
                                    {
                                        Toast.makeText(getApplicationContext(), "重命名失败，请重试", Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        notebookAdapter.getNotebookData();
                                        alertDialog.dismiss();
                                    }
                                    noteAdapter.getNoteData();
                                    notebookAdapter.getNotebookData();
                                }
                            }
                        });
                        break;
                    }
                    case 1:
                    {
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("确认删除");
                        builder.setPositiveButton("确认", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                String name = notebookAdapter.notebookCards.get(position).name;
                                if (name.equals(defaultNotebook))
                                    setDefaultNotebook("");
                                defaultNotebook = getDefaultNotebook();
                                SQLiteDatabase db = dbHelper.getReadableDatabase();
                                db.delete("note", "note_book = ?", new String[]{name});
                                db.delete("notebook", "name = ?", new String[]{name});
                                noteAdapter.getNoteData();
                                notebookAdapter.getNotebookData();
                            }
                        });
                        builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {

                            }
                        });
                        builder.create().show();
                        break;
                    }
                }
                dialog.dismiss();
            }
        });

        return builder;
    }

    AlertDialog.Builder setRenameNotebookDialog(final int position)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_edittext, null);
        final EditText editText = view.findViewById(R.id.dialog_edit);
        editText.setText(notebookAdapter.notebookCards.get(position).name);
        builder.setView(view);
        builder.setTitle("重命名");
        builder.setPositiveButton("确定", null);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        return builder;
    }

    AlertDialog.Builder setNewNotebookDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_edittext, null);
        TextView textView = view.findViewById(R.id.dialog_edit);
        textView.setHint("笔记本名称");
        builder.setView(view);
        builder.setTitle("新建笔记本");
        builder.setPositiveButton("确定", null);
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.cancel();
            }
        });

        return builder;
    }

    AlertDialog.Builder setSelectDefaultNotebookDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final String[] notebookNames = new String[globalNotebookCards.size()];
        for (int i = 0; i < globalNotebookCards.size(); i++)
            notebookNames[i] = globalNotebookCards.get(i).name;

        builder.setTitle("选择默认笔记本");
        builder.setItems(notebookNames, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                setDefaultNotebook(notebookNames[which]);
                defaultNotebook = getDefaultNotebook();
                dialog.dismiss();
            }
        });
        return builder;
    }

    class AddNotebookPositiveButtonListenser implements View.OnClickListener
    {
        AlertDialog alertDialog;
        boolean isFirst;

        AddNotebookPositiveButtonListenser(AlertDialog alertDialog, boolean isFirst)
        {
            this.alertDialog = alertDialog;
            this.isFirst = isFirst;
        }

        @Override
        public void onClick(View v)
        {
            final EditText editText = alertDialog.findViewById(R.id.dialog_edit);
            String name = editText.getText().toString();
            if (name.isEmpty())
            {
                Toast.makeText(getApplicationContext(), "请输入笔记本名称", Toast.LENGTH_SHORT).show();
                return;
            }
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            String[] projection = {};
            String selection = "name = ?";
            String[] selectionArgs = {name};
            Cursor c = db.query("notebook", projection, selection, selectionArgs, null, null, null);
            if (c.moveToFirst())
                Toast.makeText(getApplicationContext(), "已存在同名笔记本", Toast.LENGTH_SHORT).show();
            else
            {
                ContentValues contentValues = new ContentValues();
                contentValues.put("name", name);
                contentValues.put("note_num", 0);
                long newRowId = db.insert("notebook", null, contentValues);
                if (newRowId == -1)
                {
                    Toast.makeText(getApplicationContext(), "新建失败，请重试", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (isFirst) setDefaultNotebook(name);
                    defaultNotebook = getDefaultNotebook();
                    notebookAdapter.getNotebookData();
                    alertDialog.dismiss();
                }
            }
        }
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

    static class NoteCard
    {
        int id;
        String title;
        String createDate;
        String updateTime;
        String content;
        String notebook;

        NoteCard(int id, String title, String createDate, String updateTime, String content, String notebook)
        {
            this.id = id;
            this.title = title;
            this.createDate = createDate;
            this.updateTime = updateTime;
            this.content = content;
            this.notebook = notebook;
        }

    }

    static class NotebookCard
    {
        String name;
        int noteNum;

        NotebookCard(String name, int noteNum)
        {
            this.name = name;
            this.noteNum = noteNum;
        }
    }

    static class DatabaseHelper extends SQLiteOpenHelper
    {
        static final int DATABASE_VERSION = 1;
        static final String DATABASE_NAME = "Note.db";

        DatabaseHelper(Context context)
        {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {
            String sql =
                    "CREATE TABLE notebook (" +
                            "name VARCHAR(20) PRIMARY KEY," +
                            "note_num INTEGER)";
            db.execSQL(sql);
            sql =
                    "CREATE TABLE note (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                            "name VARCHAR(20)," +
                            "note_book VARCHAR(20), " +
                            "note_content TEXT," +
                            "create_time INTEGER," +
                            "update_time INTEGER)";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
        {
            String sql =
                    "DROP TABLE IF EXISTS notebook";
            db.execSQL(sql);
            sql =
                    "DROP TABLE IF EXISTS note";
            db.execSQL(sql);
            onCreate(db);
        }
    }

}

