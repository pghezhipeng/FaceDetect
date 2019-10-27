package com.pg.facedetect;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class StartupActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        EditText idNumber = findViewById(R.id.et_idnumber);
        Button button = findViewById(R.id.btn_start);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = idNumber.getText().toString();
                if(id==null||id.isEmpty()){
                    Toast.makeText(StartupActivity.this,"请输入身份证",Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(StartupActivity.this,MainActivity.class);
                intent.putExtra("ID_NUM",id);
                startActivity(intent);
                //StartupActivity.this.finish();
            }
        });
    }
}
