package com.example.zhan.datacollector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.bmob.v3.listener.SaveListener;

public class Register extends Activity {

    private EditText mUsername,mPassword;
    private Button mRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mRegister = (Button) findViewById(R.id.registerButton);

        //暂时没有考虑程序的鲁棒性
        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyUser bu = new MyUser();
                bu.setUsername(mUsername.getText().toString());
                bu.setPassword(mPassword.getText().toString());

                //注意：不能用save方法进行注册
                bu.signUp(Register.this, new SaveListener() {
                    @Override
                    public void onSuccess() {
                        // TODO Auto-generated method stub
                        Toast.makeText(Register.this, "注册成功！", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Register.this,MainActivity.class);
                        startActivity(intent);
                        finish();//注册成功后就不需要存在了
                    }

                    @Override
                    public void onFailure(int code, String msg) {
                        // TODO Auto-generated method stub
                        Toast.makeText(Register.this,"注册失败！",Toast.LENGTH_SHORT).show();
//                        System.out.println(code + ":" + msg);
                    }
                });

            }
        });

    }

}
