package com.example.davidwangjp.note;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class SearchNoteActivity extends AppCompatActivity
{
    NoteListFragment noteListFragment = new NoteListFragment();
    static RecyclerView noteRecyclerView;
    MainActivity.NoteListAdapter noteAdapter;
    RecyclerView.LayoutManager noteLayoutManager;
    ArrayList<MainActivity.NoteCard> noteCards = MainActivity.noteAdapter.noteCards;
    MainActivity.NoteSortMode noteSortMode = MainActivity.NoteSortMode.LATEST_MODIFY;

    String notebookName;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_note);

        notebookName = getIntent().getStringExtra("notebook_name");

        Toolbar toolbar = findViewById(R.id.search_note_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        noteRecyclerView = LayoutInflater.from(SearchNoteActivity.this).inflate(R.layout.note_list, null).findViewById(R.id.note_list);
        noteLayoutManager = new LinearLayoutManager(this);
        noteRecyclerView.setLayoutManager(noteLayoutManager);
        noteAdapter = new MainActivity.NoteListAdapter(new ArrayList<MainActivity.NoteCard>());
        noteAdapter.setOnItemClickListener(new MainActivity.NoteListAdapter.OnItemClickListener()
        {
            @Override
            public void onItemClick(View view, int position)
            {
                if (noteAdapter.isMultiSelect)
                    noteAdapter.selectItem(position);
                else
                {
                    Intent intent = new Intent(SearchNoteActivity.this, NewNoteActivity.class);
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
                Toast.makeText(getApplicationContext(), "" + noteAdapter.isMultiSelect, Toast.LENGTH_SHORT).show();
            }
        });
        noteRecyclerView.setAdapter(noteAdapter);

        noteListFragment.setArguments(getIntent().getExtras());
        if (savedInstanceState == null)
        {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.replace(R.id.search_note_fragment_container, noteListFragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_note_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.search_note_view);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);
        searchView.setQueryHint("搜索笔记");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener()
        {
            @Override
            public boolean onQueryTextSubmit(String query)
            {
                Toast.makeText(getApplicationContext(), query, Toast.LENGTH_SHORT).show();
                noteAdapter.noteCards.clear();
                if (!notebookName.isEmpty())
                {
                    for (MainActivity.NoteCard noteCard : noteCards)
                        if (noteCard.title.contains(query) || noteCard.content.contains(query))
                            noteAdapter.noteCards.add(noteCard);
                }
                else
                {
                    for (MainActivity.NoteCard noteCard : noteCards)
                        if ((noteCard.title.contains(query) || noteCard.content.contains(query))
                                && noteCard.notebook.equals(notebookName))
                            noteAdapter.noteCards.add(noteCard);
                }
                noteAdapter.notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText)
            {
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        if (noteAdapter.isMultiSelect)
            menu.findItem(R.id.action_delete_note_search_note).setVisible(true);
        else
            menu.findItem(R.id.action_delete_note_search_note).setVisible(false);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        switch (id)
        {
            case R.id.set_sorting_way_search_note:
                AlertDialog.Builder builder = setChangeSortingWayDialog();
                builder.create().show();
                return true;
            case R.id.action_delete_note_search_note:
                SQLiteDatabase db = MainActivity.dbHelper.getReadableDatabase();
                for (int i = 0; i < noteAdapter.noteCards.size(); i++)
                {
                    if (noteAdapter.ifPositionSelected.get(i))
                    {
                        noteAdapter.noteCards.remove(i);
                        int noteId = noteAdapter.noteCards.get(i).id;
                        db.delete("note", "id = ?", new String[]{String.valueOf(noteId)});
                        for (MainActivity.NotebookCard notebookCard : MainActivity.notebookAdapter.notebookCards)
                        {
                            if (Objects.equals(notebookCard.name, noteAdapter.noteCards.get(i).notebook))
                            {
                                ContentValues contentValues = new ContentValues();
                                contentValues.put("name", notebookCard.name);
                                contentValues.put("note_num", notebookCard.noteNum - 1);
                                db.update("notebook", contentValues, "name = ?", new String[]{notebookCard.name});
                                break;
                            }
                        }

                    }
                }
                MainActivity.notebookAdapter.getNotebookData();
                MainActivity.noteAdapter.getNoteData();
                noteAdapter.clearAll();
                return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed()
    {
        if (noteAdapter.isMultiSelect)
        {
            noteAdapter.clearAll();
            noteAdapter.isMultiSelect = false;
            noteAdapter.notifyDataSetChanged();
            supportInvalidateOptionsMenu();
        }
        else
        {
            super.onBackPressed();
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

    AlertDialog.Builder setChangeSortingWayDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(SearchNoteActivity.this);
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
                        noteSortMode = MainActivity.NoteSortMode.LATEST_MODIFY;
                        Collections.sort(noteAdapter.noteCards, new Comparator<MainActivity.NoteCard>()
                        {
                            @Override
                            public int compare(MainActivity.NoteCard o1, MainActivity.NoteCard o2)
                            {
                                return o2.updateTime.compareTo(o1.updateTime);
                            }
                        });
                        noteAdapter.notifyDataSetChanged();
                        break;
                    case 1:
                        noteSortMode = MainActivity.NoteSortMode.TITLE;
                        Collections.sort(noteAdapter.noteCards, new Comparator<MainActivity.NoteCard>()
                        {
                            @Override
                            public int compare(MainActivity.NoteCard o1, MainActivity.NoteCard o2)
                            {
                                return o1.title.compareTo(o2.title);
                            }
                        });
                        noteAdapter.notifyDataSetChanged();
                        break;
                    case 2:
                        noteSortMode = MainActivity.NoteSortMode.LATEST_MODIFY;
                        Collections.sort(noteAdapter.noteCards, new Comparator<MainActivity.NoteCard>()
                        {
                            @Override
                            public int compare(MainActivity.NoteCard o1, MainActivity.NoteCard o2)
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
}
