package cn.dyzs.activity;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import cn.dyzs.view.linechartview.LineChartView;
import cn.dyzs.view.linechartview.R;

public class MainActivity extends AppCompatActivity {

    private static int i = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LineChartView lcv_yj = (LineChartView) findViewById(R.id.lcv_yj);
        lcv_yj.setData(lcv_yj.testLoadData(i));
        lcv_yj.setOnLineChartViewListener(new LineChartView.LineChartViewListener() {
            @Override
            public void onPointClick(int selection) {
                Toast.makeText(MainActivity.this, "sel:" + selection, Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.reload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lcv_yj.setData(lcv_yj.testLoadData(i));
                lcv_yj.playLineAnimation();
            }
        });
    }
}
