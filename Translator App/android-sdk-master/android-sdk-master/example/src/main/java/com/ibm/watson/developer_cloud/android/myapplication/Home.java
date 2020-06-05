package com.ibm.watson.developer_cloud.android.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class Home extends AppCompatActivity {
    Button btntranslate, btnrecents,btndelete;
    DatabaseHelper mydb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btntranslate = (Button) findViewById(R.id.btn_translate);
        btnrecents = (Button) findViewById(R.id.btn_recents);
        btndelete=(Button) findViewById(R.id.btn_delete);
        mydb=new DatabaseHelper(this);


        btntranslate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Home.this, MainActivity.class);
                startActivity(intent);
            }
        });

        viewAll();
        delete();

    }

    public void viewAll(){
        btnrecents.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Cursor res=mydb.getAllData();
                        if(res.getCount()==0){
                            showMessage("Error","No data found");
                            return;
                        }
                        StringBuffer buffer = new StringBuffer();
                        while(res.moveToNext()){
                           // buffer.append("ID :"+res.getString(0)+"\n");
                            buffer.append("word :"+res.getString(1)+"\n");
                            buffer.append("meaning :"+res.getString(2)+"\n");
                            buffer.append("language :"+res.getString(3)+"\n\n");
                        }
                        showMessage("Data",buffer.toString());
                    }
                }
        );

    }

    public void showMessage(String title,String Message){
        AlertDialog.Builder builder=new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    public void delete(){
        btndelete.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mydb.deleteAll();
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.about:
                Intent intent = new Intent(Home.this, About.class);
                startActivity(intent);
                return true;
            case R.id.help:
                Intent intents = new Intent(Home.this, Help.class);
                startActivity(intents);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

}
