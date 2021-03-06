package com.example.zhan.datacollector;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {

    //代表MindWave Mobile设备的类
    TGDevice tgDevice;
    //输出算法的调试信息
    TextView tv;
    ScrollView sv;
    //控制按钮
    Button mConnect, mExportData, mClearData, mShowData, mRecordTime;
    //用来动态显示注意力和冥想度
    ProgressBar mAttentionProgressBar, mMeditationProgressBar;


    boolean buttonFlag = true;

    //蓝牙适配器
    BluetoothAdapter bluetoothAdapter;

    //数据库帮助类
    MyDateBaseHelp mMyDateBaseHelp = new MyDateBaseHelp(this, "database1.db3", null, 1);

    //相关数据
    Integer mMeditation, mAttention, mRawData;

    int subjectContactQuality_cnt;
    int subjectContactQuality_last;

    int m_rawData[] = new int[516];   //画图使用的原始信号
    int rawData[] = new int[516];     //算法处理的原始信号
    int raw_Quality = 200;
    int BaseNum = 250;
    int SpeedNum = 0;
    int Command = 0;          //控制命令
    int Count = 0;
    int m_Count = 0;
    int Flag = 0;

    //算法处理的数据
    double task_famil_baseline, task_famil_cur, task_famil_change;
    boolean task_famil_first;
    double task_diff_baseline, task_diff_cur, task_diff_change;
    boolean task_diff_first;

    private MyHandler mMyHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMyHandler = new MyHandler(this);
        
        tv = (TextView) findViewById(R.id.textView1);
        sv = (ScrollView) findViewById(R.id.scrollView1);


        //参数的具体含义我也不是很清楚
        subjectContactQuality_last = -1; /* start with impossible value */
        subjectContactQuality_cnt = 200; /* start over the limit, so it gets reported the 1st time */

        // 检查蓝牙是否可得到
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {

            Toast.makeText(this, "Bluetooth not available", Toast.LENGTH_LONG).show();
            //finish();
            return;

        } else {
            tgDevice = new TGDevice(bluetoothAdapter, mMyHandler);
        }

        //算法处理的数据
        task_famil_baseline = task_famil_cur = task_famil_change = 0.0;
        task_famil_first = true;
        task_diff_baseline = task_diff_cur = task_diff_change = 0.0;
        task_diff_first = true;

        //连接按钮
        mConnect = (Button) findViewById(R.id.connectButton);
        mConnect.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (buttonFlag) {
                    tgDevice.connect(true);
                    mConnect.setText("断开连接");
                    buttonFlag = false;
                } else {
                    tgDevice.close();
                    mAttentionProgressBar.setProgress(0);
                    mMeditationProgressBar.setProgress(0);
                    mConnect.setText("点击连接");
                    buttonFlag = true;
                }
            }
        });

        //导出数据按钮
        mExportData = (Button) findViewById(R.id.exportData);
        mExportData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ExportData exportData = new
                                ExportData(mMyDateBaseHelp.getReadableDatabase());
                        exportData.exportDatabaseToCSV();

                        Message message = new Message();
                        message.what = 246;
                        message.obj = exportData.getFilePath();
                        mMyHandler.sendMessage(message);

                    }
                }).start();
            }
        });

        //清空数据按钮
        mClearData = (Button) findViewById(R.id.clearData);
        mClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("mClearData");
                new AlertDialog.Builder(MainActivity.this).
                        setTitle("是否清空数据库表中的数据").setNegativeButton("取消", null)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mMyDateBaseHelp.getReadableDatabase()
                                        .execSQL("delete from table1");
                            }
                        }).show();
            }
        });

        //显示数据库中的数据
        mShowData = (Button) findViewById(R.id.showData);
        mShowData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Cursor cursor = mMyDateBaseHelp.getReadableDatabase().rawQuery(
                        "select * from table1", null
                );

                List<Map<String, Integer>> dataList = new ArrayList<>();
                while (cursor.moveToNext()) {
                    Map<String, Integer> map = new HashMap<>();
                    map.put("raw_data", cursor.getInt(1));
                    map.put("attention", cursor.getInt(2));
                    map.put("meditation", cursor.getInt(3));
                    dataList.add(map);
                }

                Intent intent = new Intent(MainActivity.this, ShowData.class);
                intent.putExtra("dataListIntentID", (Serializable) dataList);
                startActivity(intent);
            }
        });

        //记录时间的设置
        mRecordTime = (Button) findViewById(R.id.setRecordTime);
        mRecordTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater layoutInflater = getLayoutInflater();
                final View view = layoutInflater.inflate(
                        R.layout.set_record_time, (ViewGroup) findViewById(R.id.setRecordTimeLayout));

                new AlertDialog.Builder(MainActivity.this).setTitle("设置记录时间").setView(view).
                        setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                tgDevice.connect(true);

                                EditText setM = (EditText) view.findViewById(R.id.setMinute);
                                EditText setS = (EditText) view.findViewById(R.id.setSecond);

                                int sumSecond = 0;

                                if (!(setM.getText().toString().trim().equals(""))) {
                                    sumSecond += Integer.parseInt(setM.getText().toString().trim()) * 60;
                                }

                                if (!(setS.getText().toString().trim().equals(""))) {
                                    sumSecond += Integer.parseInt(setS.getText().toString());
                                }

                                if (sumSecond != 0) {

                                    tgDevice.connect(true);//时间设置成功，打开连接

                                    new Timer().schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            tgDevice.close();//到达指定时间关闭连接
                                        }
                                    }, sumSecond * 1000);
                                } else {
                                    Toast.makeText(MainActivity.this, "请输入！",
                                            Toast.LENGTH_LONG).show();
                                }

                            }

                        }).setNegativeButton("取消", null).show();

            }
        });

        //进度条
        mAttentionProgressBar = (ProgressBar) findViewById(R.id.attentionProgressBar);
        mMeditationProgressBar = (ProgressBar) findViewById(R.id.meditationProgressBar);


        //每隔一秒向数据库写入数据
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                // 发送将数据插入数据库的消息
                mMyHandler.sendEmptyMessage(135);
            }
        }, 0, 1000);


    }

    /**
     * 处理从TGDevice获取的数据
     */
    public class MyHandler extends Handler {

        WeakReference<MainActivity> mMainActivityWeakReference;

        public MyHandler(MainActivity mainActivity) {
            mMainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {

                case 135:
                    //处理注意力和冥想度的数据
                    if (mRawData != null && mAttention != null && mMeditation != null) {
//                        System.out.println("*" + mRawData + mAttention + " " + mMeditation);
                        //通过进度条的值反映注意力和冥想度
                        mAttentionProgressBar.setProgress(mAttention);
                        mMeditationProgressBar.setProgress(mMeditation);

                        mMyDateBaseHelp.getReadableDatabase().execSQL(
                                "insert into table1 values(null,?,?,?)",
                                new Integer[]{mRawData, mAttention, mMeditation}
                        );
                        mRawData = null;
                        mAttention = null;
                        mMeditation = null;
                    }
                    break;

                case 246:
                    new AlertDialog.Builder(MainActivity.this).setMessage("导出数据的目录是："
                            + msg.obj).setPositiveButton("确定",null).show();
                    break;

                case TGDevice.MSG_MODEL_IDENTIFIED:
                    /*
                     * now there is something connected,
            		 * time to set the configurations we need
            		 */
                    tv.append("Model Identified\n");
                    tgDevice.setBlinkDetectionEnabled(true);
                    tgDevice.setTaskDifficultyRunContinuous(true);
                    tgDevice.setTaskDifficultyEnable(true);
                    tgDevice.setTaskFamiliarityRunContinuous(true);
                    tgDevice.setTaskFamiliarityEnable(true);
                    tgDevice.setRespirationRateEnable(true); /// not allowed on EEG hardware, here to show the override message
                    break;

                case TGDevice.MSG_STATE_CHANGE:

                    switch (msg.arg1) {
                        case TGDevice.STATE_IDLE:
                            break;
                        case TGDevice.STATE_CONNECTING:
//    	                	tv.append( "Connecting...\n" );
                            break;
                        case TGDevice.STATE_CONNECTED:
                            tv.append("Connected.\n");
                            tgDevice.start();
                            break;
                        case TGDevice.STATE_NOT_FOUND:
                            tv.append("Could not connect to any of the paired BT devices.  Turn them on and try again.\n");
                            tv.append("Bluetooth devices must be paired 1st\n");
                            break;
                        case TGDevice.STATE_ERR_NO_DEVICE:
                            tv.append("No Bluetooth devices paired.  Pair your device and try again.\n");
                            break;
                        case TGDevice.STATE_ERR_BT_OFF:
                            tv.append("Bluetooth is off.  Turn on Bluetooth and try again.");
                            break;
                        case TGDevice.STATE_DISCONNECTED:
                            tv.append("Disconnected.\n");
                    } /* end switch on msg.arg1 */

                    break;

                case TGDevice.MSG_POOR_SIGNAL:
                    raw_Quality = msg.arg1;
                	/* display signal quality when there is a change of state, or every 30 reports (seconds) */
                	/*
                	if (subjectContactQuality_cnt >= 30 || msg.arg1 != subjectContactQuality_last) {
                		if (msg.arg1 == 0) tv.append( "SignalQuality: is Good: " + msg.arg1 + "\n" );
                		else tv.append( "SignalQuality: is POOR: " + msg.arg1 + "\n" );

                		subjectContactQuality_cnt = 0;
                		subjectContactQuality_last = msg.arg1;
                	}
                	else subjectContactQuality_cnt++;
                	*/
                    break;

                case TGDevice.MSG_RAW_DATA:

                    if (raw_Quality == 0) {
                        int rawdata = msg.arg1;
                        mRawData = msg.arg1;
                        m_rawData[m_Count] = rawdata;
                        m_Count++;
                        if (Flag == 0) {
                            if (rawdata >= 500) {
                                rawData[Count] = rawdata;
                                Count++;
                                Flag++;
                            }
                        } else {
                            rawData[Count] = rawdata;
                            Count++;
                        }
                        if (Count == 400) {
//                            Algorithm(rawData);
                            Count = 0;
                            Flag = 0;
                        }
                        if (m_Count == 512) {
//                            view.set_data(m_rawData);
                            m_Count = 0;
                        }
                    }

                    break;


                case TGDevice.MSG_ATTENTION:

                    mAttention = msg.arg1;

//                    attention = msg.arg1;
//                    view.set_attention(attention);
                    //tv.append( "Attention: " + msg.arg1 + "\n" );
                    break;

                case TGDevice.MSG_MEDITATION:

                    mMeditation = msg.arg1;

//                    meditation = msg.arg1;
//                    view.set_meditation(meditation);
                    //tv.append( "Meditation: " + msg.arg1 + "\n" );
                    break;

                case TGDevice.MSG_EEG_POWER:
                    TGEegPower e = (TGEegPower) msg.obj;
                    //tv.append("delta: " + e.delta + " theta: " + e.theta + " alpha1: " + e.lowAlpha + " alpha2: " + e.highAlpha + "\n");
                    break;

                case TGDevice.MSG_FAMILIARITY:
                    task_famil_cur = (Double) msg.obj;
                    if (task_famil_first) {
                        task_famil_first = false;
                    } else {
                		/*
                		 * calculate the percentage change from the previous sample
                		 */
                		/*
                		task_famil_change = calcPercentChange(task_famil_baseline,task_famil_cur);
                		if (task_famil_change > 500.0 || task_famil_change < -500.0 ) {
                			tv.append( "     Familiarity: excessive range\n" );
                			//Log.i( "familiarity: ", "excessive range" );
                		}
                		else {
                			tv.append( "     Familiarity: " + task_famil_change + " %\n" );
                			//Log.i( "familiarity: ", String.valueOf( task_famil_change ) + "%" );
                		}
                		*/
                    }
                    task_famil_baseline = task_famil_cur;
                    break;
                case TGDevice.MSG_DIFFICULTY:
                    task_diff_cur = (Double) msg.obj;
                    if (task_diff_first) {
                        task_diff_first = false;
                    } else {
                		/*
                		 * calculate the percentage change from the previous sample
                		 */
                		/*
                		task_diff_change = calcPercentChange(task_diff_baseline,task_diff_cur);
                		if (task_diff_change > 500.0 || task_diff_change < -500.0 ) {
                			tv.append( "     Difficulty: excessive range %\n" );
                			//Log.i("difficulty: ", "excessive range" );
                		}
                		else {
                			tv.append( "     Difficulty: " +  task_diff_change + " %\n" );
                			//Log.i( "difficulty: ", String.valueOf( task_diff_change ) + "%" );
                		}
                		*/
                    }
                    task_diff_baseline = task_diff_cur;
                    break;

                case TGDevice.MSG_ZONE:
                    switch (msg.arg1) {
                        case 3:
                            //   tv.append( "          Zone: Elite\n" );
                            break;
                        case 2:
                            //    tv.append( "          Zone: Intermediate\n" );
                            break;
                        case 1:
                            //    tv.append( "          Zone: Beginner\n" );
                            break;
                        default:
                        case 0:
                            //   tv.append( "          Zone: relax and try to focus\n" );
                            break;
                    }
                    break;

                case TGDevice.MSG_BLINK:
                    //tv.append( "Blink: " + msg.arg1 + "\n" );
                    break;

                case TGDevice.MSG_ERR_CFG_OVERRIDE:
                    switch (msg.arg1) {
                        case TGDevice.ERR_MSG_BLINK_DETECT:
                            //tv.append("Override: blinkDetect"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: blinkDetect", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKFAMILIARITY:
                            //tv.append("Override: Familiarity"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: Familiarity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKDIFFICULTY:
                            //tv.append("Override: Difficulty"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: Difficulty", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_POSITIVITY:
                            //tv.append("Override: Positivity"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: Positivity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_RESPIRATIONRATE:
                            //tv.append("Override: Resp Rate"+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: Resp Rate", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            //tv.append("Override: code: "+msg.arg1+"\n");
                            //Toast.makeText(getApplicationContext(), "Override: code: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case TGDevice.MSG_ERR_NOT_PROVISIONED:
                    switch (msg.arg1) {
                        case TGDevice.ERR_MSG_BLINK_DETECT:
                            //tv.append("No Support: blinkDetect"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: blinkDetect", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKFAMILIARITY:
                            //tv.append("No Support: Familiarity"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: Familiarity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_TASKDIFFICULTY:
                            //tv.append("No Support: Difficulty"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: Difficulty", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_POSITIVITY:
                            //tv.append("No Support: Positivity"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: Positivity", Toast.LENGTH_SHORT).show();
                            break;
                        case TGDevice.ERR_MSG_RESPIRATIONRATE:
                            //tv.append("No Support: Resp Rate"+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: Resp Rate", Toast.LENGTH_SHORT).show();
                            break;

                        default:
                            //tv.append("No Support: code: "+msg.arg1+"\n");
                            //Toast.makeText(getApplicationContext(), "No Support: code: "+msg.arg1+"", Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                default:
                    break;

            } /* end switch on msg.what */

            sv.fullScroll(View.FOCUS_DOWN);

        }
        
    }
    
    private double calcPercentChange(double baseline, double current) {
        double change;

        if (baseline == 0.0) baseline = 1.0; //don't allow divide by zero
		/*
		 * calculate the percentage change
		 */
        change = current - baseline;
        change = (change / baseline) * 1000.0 + 0.5;
        change = Math.floor(change) / 10.0;
        return (change);
    }

}
