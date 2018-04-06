package com.study.gyl.guessegame;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;

import com.study.gyl.guessegame.model.Const;
import com.study.gyl.guessegame.utils.Util;


public class PassStageActivity extends Activity {
    public static final String SONG_NAME_STR = "song name";
    public static final String CURRENT_STAGE_STR = "current stage";
    public static final String REWARD_COINS_STR = "reward coins";

    private TextView mViewBitPercent;
    private TextView mViewPassStage;
    private TextView mViewPassSongName;
    private TextView mViewRewardCoins;

    private ImageButton mViewNextStage;
    private ImageButton mViewShareToWeiXin;

    private int mPassStage;
    private int mRewardCoins;
    private String mPassSongName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pass_stage);
        initView();
        initData();
        initEvent();
    }

    private void initEvent() {
        mViewNextStage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTheLastStage()) {
                    Util.getInstance().startActivityWithoutData(PassStageActivity.this, AllPassActivity.class);
                } else {
                    Intent intent = new Intent(PassStageActivity.this, MainActivity.class);
                    intent.putExtra(MainActivity.DELIVERED_STAGE, mPassStage);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    private boolean isTheLastStage() {
        return mPassStage == Const.SONGS_INFO.length;
    }

    private void initData() {
        Bundle bundle = getIntent().getExtras();
        mPassStage = bundle.getInt(CURRENT_STAGE_STR);
        mRewardCoins = bundle.getInt(REWARD_COINS_STR);
        mPassSongName = bundle.getString(SONG_NAME_STR);

        mViewPassStage.setText(mPassStage + "");
        mViewPassSongName.setText(mPassSongName);
        mViewRewardCoins.setText(mRewardCoins + "");
    }

    private void initView() {
        mViewBitPercent = findViewById(R.id.bit_percent);
        mViewPassStage = findViewById(R.id.pass_stage);
        mViewPassSongName = findViewById(R.id.pass_song_name);
        mViewRewardCoins = findViewById(R.id.reward_coins);
        mViewNextStage = findViewById(R.id.next_stage);
        mViewShareToWeiXin = findViewById(R.id.share_to_wx);
    }
}
