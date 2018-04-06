package com.study.gyl.guessegame;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.study.gyl.guessegame.model.Const;
import com.study.gyl.guessegame.model.IDialogButtonListener;
import com.study.gyl.guessegame.model.IWordButtonClickListener;
import com.study.gyl.guessegame.model.Song;
import com.study.gyl.guessegame.model.WordButton;
import com.study.gyl.guessegame.utils.LogUtil;
import com.study.gyl.guessegame.utils.MusicPlayerUtil;
import com.study.gyl.guessegame.utils.Util;
import com.study.gyl.guessegame.view.WordGridView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity extends Activity implements IWordButtonClickListener {
    private static final String TAG = "MainActivity";
    public static final String DELIVERED_STAGE = "delivered stage";

    //正确答案
    private static final int STATUS_ANSWER_RIGHT = 1;
    //错误答案
    private static final int STATUS_ANSWER_WRONG = 2;
    //不完整
    private static final int STATUS_ANSWER_LACK  = 3;


    //声明动画相关的变量
    private Animation mPanAnim;
    private LinearInterpolator mPanLin;

    private Animation mBarInAnim;
    private LinearInterpolator mBarInLin;

    private Animation mBarOutAnim;
    private LinearInterpolator mBarOutLin;


    //播放音乐的按钮,盘片图片,控制杆图片
    private ImageButton mViewPlayStart;
    private ImageView mViewPan;
    private ImageView mViewPanBar;

    //现有金币的TextView
    private TextView mViewCurrentCoins;
    //当前关的TextView
    private TextView mViewCurrentStage;


    //文字框的文字容器
    private ArrayList<WordButton> mAllWords;
    private ArrayList<WordButton> mSelectedWords;
    private WordGridView mViewGridView;

    //已选择文字框的UI容器
    private LinearLayout mViewWordsLayout;

//    //过关界面布局
//    private LinearLayout mViewPassStageLayout;

    //当前歌曲
    private Song mCurrentSong;
    //当前关卡索引
    private int mCurrentStageIndex = 0;

    //处理闪烁文字的handler
    private Handler sparkWordsHandler;
    private static final int SPARK_TIMES = 6;
    private static final String COLOR_STR = "color";

    //目前的金币总数
    private int mCurrentCoins;

    //从PassStageActivity传递来的关索引
    private int mDeliveredStage;

    //对话框按钮监听器
    private IDialogButtonListener mClearOneWordDialogListener;
    private IDialogButtonListener mBuyRightAnswerDialogListener;
    private IDialogButtonListener mCoinNotEnoughDialogListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initEvent();
        initCurrentStageData();
    }

    /**
     * 获取当前关卡歌曲信息
     */
    private Song getStageSong(int stageIndex) {
        String[] songInfo = Const.SONGS_INFO[stageIndex];
        String songFIleName = songInfo[Const.INDEX_FILE_NAME];
        String songName = songInfo[Const.INDEX_SONG_NAME];
        Song song = new Song(songName,songFIleName);
        return song;
    }

    /**
     * 初始化当前关的文字框数据
     */
    private void initCurrentStageData() {
        //读取当前关的歌曲信息
        mCurrentSong = getStageSong(mCurrentStageIndex);

        //获得数据
        mAllWords = initAllWord();

        //更新数据
        mViewGridView.updateData(mAllWords);

        //初始化已选狂文字
        mSelectedWords = initSelectedWord();

        LayoutParams layoutParams = new LayoutParams(140,140);
        for (int i = 0;i < mSelectedWords.size(); i ++) {
            mViewWordsLayout.addView(mSelectedWords.get(i).getButton(),layoutParams);
        }

        //初始化唱片盘旋转动画需要持续的时间
        initPanAnimationDuration(mPanAnim);

        //进入本页面时自动播放音乐
        handlePlayButton();
    }

    /**
     * 初始化盘片旋转动画的持续时间
     * @param mPanAnim
     */
    private void initPanAnimationDuration(Animation mPanAnim) {
        LogUtil.d(TAG,"Pan duration",MusicPlayerUtil.getMusicDuration(MainActivity.this,mCurrentSong.getSongFileName()) + "");
        mPanAnim.setDuration(MusicPlayerUtil.getMusicDuration(MainActivity.this,mCurrentSong.getSongFileName()));
    }

    /**
     * 初始化待选文字框
     */
    private ArrayList<WordButton> initAllWord(){
        ArrayList<WordButton> data = new ArrayList<>();
        String[] allWords = generateWords();

        //获取所有的文字信息
        for (int i = 0; i < WordGridView.WORDS_COUNT; i++) {
            WordButton wordButton = new WordButton();
            wordButton.setIndex(i);
            wordButton.setWordString(allWords[i]);
            data.add(wordButton);
        }
        return data;
    }

    /**
     * 初始化已选文字框
     */
    private ArrayList<WordButton> initSelectedWord() {
        ArrayList<WordButton> data = new ArrayList<>();
        //获取所有的文字信息
        for (int i = 0;i < mCurrentSong.getSongNameLength();i++) {
            View view = Util.getInstance().getView(MainActivity.this, R.layout.gridview_item);

            final WordButton holder = new WordButton();
            holder.setButton((Button) view.findViewById(R.id.item_btn));

            holder.getButton().setTextColor(Color.WHITE);
            holder.getButton().setText("");
            holder.setVisible(false);
            holder.getButton().setBackgroundResource(R.mipmap.game_wordblank);
            holder.getButton().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //清除被点击的已选文字框的文字
                    if (!"".equals(holder.getWordString())){
                        clearSelectedWords(holder);
                    }
                }
            });
            data.add(holder);
        }
        return data;
    }

    /**
     * 生成所有的待选文字
     */
    private String[] generateWords() {
        String[] words = new String[WordGridView.WORDS_COUNT];
        //存入歌名
        for (int i = 0;i < mCurrentSong.getSongNameLength();i ++) {
            words[i] = mCurrentSong.getSongNameChars()[i] + "";
        }
        //存入随机汉字
        for (int i = mCurrentSong.getSongNameLength(); i < WordGridView.WORDS_COUNT; i ++) {
            words[i] = Util.getInstance().getRandomChineseChar() + "";
        }

        //打乱汉字顺序
        List<String> wordList = (List<String>) Arrays.asList(words);
        Collections.shuffle(wordList);
        for (int i = 0;i < words.length;i ++) {
            words[i] = wordList.get(i);
        }

        return words;
    }

    /**
     * 初始化事件
     */
    private void initEvent() {
        mBarInAnim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            //当控制杆移入动画结束时开始盘片旋转动画
            public void onAnimationEnd(Animation animation) {
                mViewPan.startAnimation(mPanAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mPanAnim.setAnimationListener(new AnimationListener() {
            @Override
            //当盘片动画开始时隐藏播放按钮
            public void onAnimationStart(Animation animation) {
                mViewPlayStart.setVisibility(View.GONE);
            }

            @Override
            //当盘片动画结束时开始控制杆移出动画
            public void onAnimationEnd(Animation animation) {
                mViewPanBar.startAnimation(mBarOutAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mBarOutAnim.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            //当控制杆移出动画结束时设置播放按钮为可见
            public void onAnimationEnd(Animation animation) {
                mViewPlayStart.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mViewPlayStart.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePlayButton();
            }
        });

        mViewGridView.setWordButtonClickListener(this);

        //删除一个非正确答案的汉字
        handleClearOneChar();
        //获取正确答案
        handleBuyRightAnswer();
        //分享至微信
        handleShareToWeiXin();
    }

    /**
     * 处理点击播放按钮的逻辑
     */
    private void handlePlayButton() {
        //开始控制杆移入的动画
        mViewPanBar.startAnimation(mBarInAnim);
        //播放音乐
        MusicPlayerUtil.playSong(MainActivity.this, mCurrentSong.getSongFileName(), new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mViewPan.clearAnimation();
            }
        });
    }

    /**
     * 初始化控件
     */
    private void initView() {
        mViewPlayStart =  findViewById(R.id.ibtn_play_start);
        mViewPan = findViewById(R.id.iv_game_disc);
        mViewPanBar =  findViewById(R.id.iv_index_pin);
        mViewCurrentCoins = findViewById(R.id.tv_bar_coins);
        mViewGridView =  findViewById(R.id.grid_view);
        mViewWordsLayout = findViewById(R.id.word_select_container);
        mViewCurrentStage = findViewById(R.id.current_stage);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        //初始化动画
        mPanAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        mPanLin = new LinearInterpolator();
        mPanAnim.setInterpolator(mPanLin);

        mBarInAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_45);
        mBarInLin = new LinearInterpolator();
        mBarInAnim.setFillAfter(true);
        mBarInAnim.setInterpolator(mBarInLin);

        mBarOutAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_d_45);
        mBarOutLin = new LinearInterpolator();
        mBarOutAnim.setFillAfter(true);
        mBarOutAnim.setInterpolator(mBarOutLin);

        //从文件中读取保存的金币数,默认为Const.TOTAL_COINS
        mCurrentCoins = Util.getInstance().readGameData(MainActivity.this)[Const.INDEX_LOAD_DATA_COINS];

        //设置现有金币的文本
        mViewCurrentCoins.setText(mCurrentCoins + "");

        sparkWordsHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.arg1 == 0x101) {
                    int color = msg.getData().getInt(COLOR_STR);
                    //间隔更改按钮文字颜色达到闪烁的效果
                    for (int i = 0; i < mSelectedWords.size(); i ++) {
                        mSelectedWords.get(i).getButton().setTextColor(color);
                    }
                }
            }
        };

        //如果此Activity是从PassStageActivity打开的,尝试读取请求的关索引
        mDeliveredStage = getIntent().getIntExtra(DELIVERED_STAGE, -1);
        if (mDeliveredStage != -1) {
            mCurrentStageIndex = mDeliveredStage;
        } else {
            mCurrentStageIndex = Util.getInstance().readGameData(MainActivity.this)[Const.INDEX_LOAD_DATA_STAGE];
            LogUtil.d(TAG,"mCurrentStageIndex from file",mCurrentStageIndex + "");
        }

        //设置当前关的索引TextView
        mViewCurrentStage.setText((mCurrentStageIndex + 1) + "");

        //设置清除一个错误答案对话框按钮点击事件
        mClearOneWordDialogListener = new IDialogButtonListener() {
            @Override
            public void onYesButtonClick() {
                clearOneChar();
                //金币的音效
                MusicPlayerUtil.playTone(MainActivity.this, MusicPlayerUtil.INDEX_COIN);
            }
        };

        //设置获取正确答案对话框按钮点击事件
        mBuyRightAnswerDialogListener = new IDialogButtonListener() {
            @Override
            public void onYesButtonClick() {
                tipRightAnswer();
                //金币的音效
                MusicPlayerUtil.playTone(MainActivity.this, MusicPlayerUtil.INDEX_COIN);
            }
        };

        //设置金币不够时对话框按钮点击事件
        mCoinNotEnoughDialogListener = new IDialogButtonListener() {
            @Override
            public void onYesButtonClick() {
                // TODO: 16/3/2 去商店购买金币
            }
        };
    }

    @Override
    protected void onPause() {
        //当Activity被中断时清除动画
        mViewPan.clearAnimation();
        //暂停音乐
        MusicPlayerUtil.stopSong();
        //保存游戏数据
        Util.getInstance().saveGameData(MainActivity.this,mCurrentStageIndex,mCurrentCoins);
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //按HOME键再重新回到游戏后需要自动播放音乐
        handlePlayButton();
    }


    /**
     * 文字按钮点击事件
     */
    @Override
    public void onWordButtonClick(WordButton wordButton) {
//        Toast.makeText(MainActivity.this, wordButton.getIndex() + "", Toast.LENGTH_SHORT).show();
        setSelectedWords(wordButton);

        //获得答案状态
        int answerResult = checkAnswer();
        LogUtil.d(TAG,"answerResult" , answerResult + "");
        //根据答案结果进行相应的处理
        switch (answerResult) {
            //过关并获得奖励
            case STATUS_ANSWER_RIGHT:
                handlePassStageEvent();
                break;

            //闪烁文字并进行提示
            case STATUS_ANSWER_WRONG:
                sparkWords();
                break;

            case STATUS_ANSWER_LACK:
                for (int i = 0; i < mSelectedWords.size(); i ++) {
                    mSelectedWords.get(i).getButton().setTextColor(Color.WHITE);
                }
                break;
        }
    }

    /**
     * 处理过关之后的相关逻辑
     */
    private void handlePassStageEvent() {
//        mViewPassStageLayout.setVisibility(View.VISIBLE);

        //停掉未播放完的音乐
        MusicPlayerUtil.stopSong();

        //金币音效
        MusicPlayerUtil.playTone(MainActivity.this, MusicPlayerUtil.INDEX_COIN);

        //停掉未完成的动画
        mViewPan.clearAnimation();

        int rewardCoins = (mCurrentStageIndex + 1) * 3;
        //奖励过关金币
        addOrReduceCoins(rewardCoins);
        //保存游戏数据
        Util.getInstance().saveGameData(MainActivity.this, mCurrentStageIndex, mCurrentCoins);

        Intent passStageIntent = new Intent(MainActivity.this, PassStageActivity.class);
        //将数据传递到PassStageActivity中
        Bundle bundle = new Bundle();
        //当前关歌曲名
        bundle.putString(PassStageActivity.SONG_NAME_STR,mCurrentSong.getSongName());
        //当前关索引
        bundle.putInt(PassStageActivity.CURRENT_STAGE_STR,mCurrentStageIndex + 1);
        //本关奖励金币数
        bundle.putInt(PassStageActivity.REWARD_COINS_STR,rewardCoins);
        passStageIntent.putExtras(bundle);
        startActivity(passStageIntent);
        //结束当前Activity,防止用户在PassStageActivity中通过点击返回键返回到本Activity
        finish();
    }

    /**
     *设置单个已选文字框
     */
    private void setSelectedWords(final WordButton wordButton) {
        for (int i = 0;i < mSelectedWords.size();i ++) {
            final WordButton selectedWord = mSelectedWords.get(i);
            //如果此已选框尚未被填充汉字
            if (selectedWord.getWordString().length() == 0) {
                setAnimationToSelectedWord(selectedWord, wordButton);

                //设置此已选框的文字及可见性等属性
                selectedWord.setWordString(wordButton.getWordString());
                selectedWord.getButton().setText(wordButton.getWordString());
                selectedWord.setVisible(true);
                //记录索引
                selectedWord.setIndex(wordButton.getIndex());
                LogUtil.d(TAG, "wordButton.getIndex()" , wordButton.getIndex() + "");

                //设置待选框的可见性
                setWordButtonVisibility(wordButton,View.INVISIBLE);
                break;
            }
        }
    }

    /**
     * 设置文字按钮的平移动画效果
     */
    private void setAnimationToSelectedWord(final WordButton selectedWord, final WordButton wordButton) {
        TranslateAnimation animation = (TranslateAnimation)
                getAnimationFromViewToAnother(wordButton.getButton(), selectedWord.getButton());
        wordButton.getButton().startAnimation(animation);
    }

    /**
     * 将控件从一个位置移动到另一个位置
     */
    private Animation getAnimationFromViewToAnother(View from, View to) {
        int[] fromPosition = getViewLocation(from);
        int[] toPosition = getViewLocation(to);
        LogUtil.d(TAG,"from x",fromPosition[0] + "");
        LogUtil.d(TAG,"from y",fromPosition[1] + "");
        LogUtil.d(TAG,"to x",toPosition[0] + "");
        LogUtil.d(TAG,"to y",toPosition[1] + "");

        TranslateAnimation animation = new TranslateAnimation(
                0, toPosition[0] - fromPosition[0], 0, toPosition[1] - fromPosition[1]);
        animation.setDuration(500);
        animation.setFillAfter(false);
        return animation;
    }

    /**
     * 获取一个控件在屏幕中的显示位置
     */
    private int[] getViewLocation(View view) {
        int[] position = new int[2];
        view.getLocationOnScreen(position);

        return position;
    }


    /**
     * 清除单个已选文字框
     */
    private void clearSelectedWords(WordButton wordButton) {
        //设置已选文字框的属性
        wordButton.getButton().setText("");
        wordButton.setVisible(false);
        wordButton.setWordString("");

        //设置待选文字框的属性
        setWordButtonVisibility(mAllWords.get(wordButton.getIndex()),View.VISIBLE);
    }

    /**
     * 设置WordButton的可见性
     */
    private void setWordButtonVisibility(WordButton wordButton,int visibility) {
        wordButton.getButton().setVisibility(visibility);
        wordButton.setVisible(visibility == View.VISIBLE ? true : false);
        LogUtil.d(TAG,"wordButton.getVisible()" , wordButton.getVisible() + "");
    }

    /**
     * 检查答案
     */
    private int checkAnswer() {
        for (int i = 0;i < mSelectedWords.size();i ++ ) {
            LogUtil.d(TAG,"mSelectedWords " + i,mSelectedWords.get(i).getWordString());
        }
        //先检查已选的汉字数与歌曲名汉字数是否匹配
        for (int i = 0;i < mSelectedWords.size();i ++ ) {
            if (mSelectedWords.get(i).getWordString().length() == 0) {
                return STATUS_ANSWER_LACK;
            }
        }

        //如果匹配,再检查正确与否
        StringBuffer sb = new StringBuffer();
        for (int i = 0;i < mSelectedWords.size(); i ++) {
            sb.append(mSelectedWords.get(i).getWordString());
        }

        return (sb.toString().equals(mCurrentSong.getSongName()))
                ? STATUS_ANSWER_RIGHT : STATUS_ANSWER_WRONG;
    }

    /**
     * 闪烁文字
     */
    private void sparkWords() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean change = false;
                for (int i = 0; i < SPARK_TIMES; i ++) {
                    Message message = sparkWordsHandler.obtainMessage();
                    message.arg1 = 0x101;
                    Bundle bundle = new Bundle();
                    bundle.putInt(COLOR_STR, change ? Color.RED : Color.WHITE);
                    message.setData(bundle);
                    sparkWordsHandler.sendMessage(message);
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    change = !change;
                }
            }
        }).start();
    }

    /**
     * 处理金币的增加或减少
     */
    private boolean addOrReduceCoins(int coins) {
        if (mCurrentCoins + coins >= 0) {
            mCurrentCoins += coins;
            mViewCurrentCoins.setText(mCurrentCoins + "");
            return true;
        }
        //减少失败
        return false;
    }

    /**
     * 处理分享至微信的逻辑
     */
    private void handleShareToWeiXin() {
        ImageButton button = (ImageButton) findViewById(R.id.btn_share_to_wx);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                WeiXinUtil.getInstance(MainActivity.this).sendRequest("我正在玩疯狂猜歌游戏,一起来玩吧!");
            }
        });
    }

    /**
     * 处理从待选框中清除一个汉字的逻辑
     */
    private void handleClearOneChar() {
        ImageButton button = (ImageButton) findViewById(R.id.clear_one_char);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LogUtil.d(TAG,"handleClearOneChar","handleClearOneChar");

                //显示提示对话框
                Util.getInstance().showDialog(
                        MainActivity.this,
                        false,
                        "确定使用" + getClearOneCharCoin() + "金币去掉一个错误答案?",
                        mClearOneWordDialogListener);
            }
        });
    }

    /**
     * 处理给出正确答案的逻辑
     */
    private void handleBuyRightAnswer() {
        ImageButton button = (ImageButton) findViewById(R.id.buy_right_answer);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                //显示提示对话框
                Util.getInstance().showDialog(
                        MainActivity.this,
                        false,
                        "确定使用" + getBuyRightAnswerCoin() + "金币获得一个正确答案?",
                        mBuyRightAnswerDialogListener);
            }
        });
    }

    /**
     * 从配置文件中读取清除一个汉字需要花费的金币数
     */
    private int getClearOneCharCoin() {
        return this.getResources().getInteger(R.integer.pay_delete_word);
    }

    /**
     * 从配置文件中读取获得正确答案需要花费的金币数
     */
    private int getBuyRightAnswerCoin() {
        return this.getResources().getInteger(R.integer.pay_tip_answer);
    }

    /**
     * 给出正确答案
     */
    private void tipRightAnswer() {
        boolean isFind = false;
        for (int i = 0;i < mSelectedWords.size();i ++) {
            if (mSelectedWords.get(i).getWordString().length() == 0) {
                WordButton wordButton = findRightWord(i);
                if (addOrReduceCoins(-getBuyRightAnswerCoin())) {
                    isFind = true;
                    onWordButtonClick(wordButton);
                } else {
                    //金币不够,弹出对话框
                    Util.getInstance().showDialog(
                            MainActivity.this,
                            false,
                            "金币不足,去商店购买?",
                            mCoinNotEnoughDialogListener);
                }
                break;
            }
        }
        if (!isFind) {
            sparkWords();
        }
    }

    /**
     * 根据已选框的索引获取一个正确答案的汉字
     */
    private WordButton findRightWord(int index) {
        for (int i = 0;i < mAllWords.size(); i ++) {
            if (mAllWords.get(i).getWordString().equals("" + mCurrentSong.getSongNameChars()[index])) {
                return mAllWords.get(i);
            }
        }
        return null;
    }

    /**
     * 删除一个非正确答案的文字
     */
    private void clearOneChar() {
        WordButton button = getOneRandomNotAnswerChar();
        if (button == null) {
            //待选框中剩余的汉字已经是正确答案,弹出对话框
            Util.getInstance().showDialog(MainActivity.this, true, "待选框中剩余的汉字已是正确答案", null);
            return;
        }
        if (!addOrReduceCoins(-getClearOneCharCoin())) {
            //金币不够,弹出对话框
            Util.getInstance().showDialog(
                    MainActivity.this,
                    false,
                    "金币不足,去商店购买?",
                    mCoinNotEnoughDialogListener);
            return;
        }
        LogUtil.d(TAG,"getOneRandomNotAnswerChar", button.getWordString());
        setWordButtonVisibility(button,View.INVISIBLE);
    }

    /**
     * 待选框中剩余的汉字是否为歌曲名
     */
    private boolean isRemainingWordsEqualsSongName() {
        List<Character> remainingWordsList = new ArrayList<>();
        //获取到待选框剩余汉字的列表
        for (int i = 0;i < mAllWords.size();i ++) {
            if (mAllWords.get(i).getVisible()) {
                remainingWordsList.add(mAllWords.get(i).getWordString().charAt(0));
            }
        }
        //如果待选框剩余汉字的个数与歌曲名的汉字个数不等,则不是歌曲名
        if (remainingWordsList.size() != mCurrentSong.getSongNameLength()) {
            return false;
        }
        //获取歌曲名的汉字列表
        List<Character> songNameWordsList = new ArrayList<>();
        for (int i = 0;i < mCurrentSong.getSongNameLength();i++) {
            songNameWordsList.add(mCurrentSong.getSongName().charAt(i));
        }
        //将待选框汉字列表和歌曲名汉字列表排序
        Collections.sort(remainingWordsList);
        Collections.sort(songNameWordsList);
        LogUtil.d(TAG,"remainingWordsList",remainingWordsList.toString());
        LogUtil.d(TAG,"songNameWordsList",songNameWordsList.toString());
        //如果待选框汉字列表和歌曲名汉字列表中的元素一致则为歌曲名,否则不是
        for (int i = 0;i < remainingWordsList.size();i++) {
            if (remainingWordsList.get(i) != songNameWordsList.get(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 随机获取一个非正确答案的汉字
     */
    private WordButton getOneRandomNotAnswerChar() {
        List<WordButton> notAnswerCharList = new ArrayList<>();
        for (int i = 0;i < mAllWords.size();i ++) {
            if (!isCharBelongSongName(mAllWords.get(i).getWordString().charAt(0)) && mAllWords.get(i).getVisible()) {
                notAnswerCharList.add(mAllWords.get(i));
            }
        }
        Collections.shuffle(notAnswerCharList);
        return notAnswerCharList.size() > 0 ? notAnswerCharList.get(0) : null;
    }

    /**
     * 判断某个汉字是否属于歌曲名
     */
    private boolean isCharBelongSongName(char ch) {
        return mCurrentSong.getSongName().indexOf(ch) >= 0;
    }
}
