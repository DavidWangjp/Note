package com.example.davidwangjp.note;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

public class LaunchActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
//        new Thread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                runOnUiThread(new Runnable()
//                {
//                    @Override
//                    public void run()
//                    {
//                        Intent intent = new Intent(LaunchActivity.this, MainActivity.class);
//                        startActivity(intent);
//                        LaunchActivity.this.finish();
//                    }
//                });
//            }
//        }).start();
        Handler handler = new Handler();
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                startActivity(new Intent(LaunchActivity.this, MainActivity.class));
                LaunchActivity.this.finish();
            }
        }, 2000);
    }
}
