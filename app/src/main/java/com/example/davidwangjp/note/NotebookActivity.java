package com.example.davidwangjp.note;


import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.davidwangjp.note.MainActivity.NoteSortMode;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.example.davidwangjp.note.MainActivity.dbHelper;


public class NotebookActivity extends AppCompatActivity
{

    FloatingActionsMenu newNoteMenu;

    NoteListFragment noteListFragment = new NoteListFragment();
    static RecyclerView noteRecyclerView;
    static MainActivity.NoteListAdapter noteListAdapter = new MainActivity.NoteListAdapter(new ArrayList<MainActivity.NoteCard>());

    NoteSortMode noteSortMode = NoteSortMode.LATEST_MODIFY;
    static String notebookName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notebook);

        notebookName = getIntent().getStringExtra("notebook_name");
        noteListAdapter.isMultiSelect = false;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(notebookName);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        noteRecyclerView = LayoutInflater.from(NotebookActivity.this).inflate(R.layout.note_list, null).findViewById(R.id.note_list);
        noteRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        setNoteListAdapter();
        noteListAdapter.setOnItemClickListener(new MainActivity.NoteListAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(View view, int position)
            {
                if (noteListAdapter.isMultiSelect)
                    noteListAdapter.selectItem(position);
                else
                {
                    Intent intent = new Intent(NotebookActivity.this, NewNoteActivity.class);
                    intent.putExtra("noteId", noteListAdapter.noteCards.get(position).id);
                    intent.putExtra("notebook", noteListAdapter.noteCards.get(position).notebook);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position)
            {
                noteListAdapter.isMultiSelect = !noteListAdapter.isMultiSelect;
                if (!noteListAdapter.isMultiSelect)
                    noteListAdapter.clearAll();
                else
                    noteListAdapter.selectItem(position);
                noteListAdapter.notifyDataSetChanged();
                supportInvalidateOptionsMenu();
            }
        });
        noteRecyclerView.setAdapter(noteListAdapter);
        noteListFragment.setArguments(getIntent().getExtras());

        if (savedInstanceState == null)
        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.notebook_fragment_container, noteListFragment).commit();
        }

        newNoteMenu = findViewById(R.id.floating_action_menu_notebook);

        FloatingActionButton newNote = findViewById(R.id.new_note_notebook);
        newNote.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(NotebookActivity.this, NewNoteActivity.class);
                intent.putExtra("noteId", -1);
                intent.putExtra("notebook", notebookName);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), notebookName, Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    public void onBackPressed()
    {
        if (noteListAdapter.isMultiSelect)
        {
            noteListAdapter.clearAll();
            noteListAdapter.isMultiSelect = false;
            noteListAdapter.notifyDataSetChanged();
            supportInvalidateOptionsMenu();
        }
        if (newNoteMenu.isExpanded())
            newNoteMenu.collapse();
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        menu.findItem(R.id.notebook_delete_note).setVisible(noteListAdapter.isMultiSelect);
        menu.findItem(R.id.notebook_move_note).setVisible(noteListAdapter.isMultiSelect);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.notebook_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        switch (id)
        {
            case R.id.notebook_delete_note:
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(NotebookActivity.this);
                builder.setTitle("确认删除");
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        SQLiteDatabase db = dbHelper.getReadableDatabase();
                        Toast.makeText(getApplicationContext(), "" + 1, Toast.LENGTH_SHORT).show();
                        for (int i = 0; i < noteListAdapter.noteCards.size(); i++)
                        {
                            if (noteListAdapter.ifPositionSelected.get(i))
                            {
                                int noteId = noteListAdapter.noteCards.get(i).id;
                                db.delete("note", "id = ?", new String[]{String.valueOf(noteId)});
                                for (MainActivity.NotebookCard notebookCard : MainActivity.notebookAdapter.notebookCards)
                                {
                                    if (notebookCard.name.equals(noteListAdapter.noteCards.get(i).notebook))
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
                        MainActivity.notebookAdapter.getNotebookData();
                        MainActivity.noteAdapter.getNoteData();
                        MainActivity.noteAdapter.clearAll();
                        MainActivity.noteAdapter.notifyDataSetChanged();
                        noteListAdapter.noteCards.clear();
                        setNoteListAdapter();
                        sortNote(noteSortMode);
                        noteListAdapter.clearAll();
                        noteListAdapter.notifyDataSetChanged();
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
            case R.id.notebook_move_note:
            {
                final String[] moveToNotebook = new String[1];
                AlertDialog.Builder builder = new AlertDialog.Builder(NotebookActivity.this);
                final String[] notebookNames = new String[MainActivity.globalNotebookCards.size()];
                for (int i = 0; i < MainActivity.globalNotebookCards.size(); i++)
                    notebookNames[i] = MainActivity.globalNotebookCards.get(i).name;
                builder.setTitle("移动至");
                builder.setItems(notebookNames, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        moveToNotebook[0] = notebookNames[which];
                        SQLiteDatabase db = dbHelper.getReadableDatabase();
                        for (int i = 0; i < noteListAdapter.noteCards.size(); i++)
                        {
                            if (noteListAdapter.ifPositionSelected.get(i) && !noteListAdapter.noteCards.get(i).notebook.equals(moveToNotebook[0]))
                            {
                                int noteId = noteListAdapter.noteCards.get(i).id;
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("note_book", moveToNotebook[0]);
                                db.update("note", contentValues, "id = ?", new String[]{String.valueOf(noteId)});
                                for (MainActivity.NotebookCard notebookCard : MainActivity.notebookAdapter.notebookCards)
                                {
                                    if (notebookCard.name.equals(MainActivity.noteAdapter.noteCards.get(i).notebook))
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
                        MainActivity.notebookAdapter.getNotebookData();
                        MainActivity.noteAdapter.getNoteData();
                        MainActivity.noteAdapter.clearAll();
                        MainActivity.noteAdapter.notifyDataSetChanged();

                        setNoteListAdapter();
                        dialog.dismiss();
                    }
                });
                builder.create().show();
                return true;
            }
            case R.id.notebook_search_note:
                Intent intent = new Intent();
                intent.setClass(NotebookActivity.this, SearchNoteActivity.class);
                intent.putExtra("notebook_name", notebookName);
                startActivity(intent);
                return true;
            case R.id.notebook_note_sorting_way:
                AlertDialog.Builder builder = setChangeNoteSortingWayDialog();
                builder.create().show();
        }

        return super.onOptionsItemSelected(item);
    }

    static void setNoteListAdapter()
    {
        noteListAdapter.noteCards.clear();
        for (MainActivity.NoteCard noteCard : MainActivity.noteAdapter.noteCards)
        {
            if (noteCard.notebook.equals(notebookName))
            {
                noteListAdapter.noteCards.add(noteCard);
            }
        }
        noteListAdapter.notifyDataSetChanged();
    }

    AlertDialog.Builder setChangeNoteSortingWayDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(NotebookActivity.this);
        String[] sortingWays = {"最后修改时间", "标题", "创建日期"};
        builder.setTitle("排序方式");
        builder.setSingleChoiceItems(sortingWays, MainActivity.noteSortMode.ordinal(), new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                sortNote(NoteSortMode.values()[which]);
                dialog.dismiss();
            }
        });
        return builder;
    }

    void sortNote(NoteSortMode noteSortMode)
    {
        switch (noteSortMode)
        {

            case LATEST_MODIFY:
                this.noteSortMode = NoteSortMode.LATEST_MODIFY;
                Collections.sort(noteListAdapter.noteCards, new Comparator<MainActivity.NoteCard>()
                {
                    @Override
                    public int compare(MainActivity.NoteCard o1, MainActivity.NoteCard o2)
                    {
                        return o2.updateTime.compareTo(o1.updateTime);
                    }
                });
                noteListAdapter.notifyDataSetChanged();
                break;
            case TITLE:
                this.noteSortMode = NoteSortMode.TITLE;
                Collections.sort(noteListAdapter.noteCards, new Comparator<MainActivity.NoteCard>()
                {
                    @Override
                    public int compare(MainActivity.NoteCard o1, MainActivity.NoteCard o2)
                    {
                        return o1.title.compareTo(o2.title);
                    }
                });
                noteListAdapter.notifyDataSetChanged();
                break;
            case CREATE_DATE:
                this.noteSortMode = NoteSortMode.CREATE_DATE;
                Collections.sort(noteListAdapter.noteCards, new Comparator<MainActivity.NoteCard>()
                {
                    @Override
                    public int compare(MainActivity.NoteCard o1, MainActivity.NoteCard o2)
                    {
                        return o2.createDate.compareTo(o1.createDate);
                    }
                });
                noteListAdapter.notifyDataSetChanged();
                break;
        }

    }

    public static class NoteListFragment extends Fragment
    {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
        {
            return noteRecyclerView;
        }
    }
}
