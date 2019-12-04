package vchat.video.view;

import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.plugin.rawdata.MediaDataObserverPlugin;
import io.rong.push.RongPushClient;
import vchat.common.agora.VideoChatManager;
import vchat.common.entity.response.MatchResponse;
import vchat.common.event.HeadsetEvent;
import vchat.common.event.MatchEvent;
import vchat.common.floatwindow.FloatWindow;
import vchat.common.greendao.im.ImCallMessageBean;
import vchat.common.helper.PermissionHelper;
import vchat.common.im.ImMessageUtily;
import vchat.common.widget.dialog.BottomListDialog;
import vchat.video.R;
import vchat.video.R2;
import vchat.video.contract.MatchContract;
import vchat.video.faceunity.entity.EffectEnum;
import vchat.video.faceunity.renderer.CameraRenderer;
import vchat.video.presenter.MatchPresenter;
import vchat.video.utils.FUManager;
import vchat.video.utils.FaceVideoHelper;
import vchat.video.utils.HeadsetReceiver;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.android.innoshortvideo.core.InnoAVVender.faceUnity.entity.Effect;
import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.innotech.deercommon.basemvp.BasePresenter;
import com.innotech.deercommon.ui.FaceToolbar;
import com.kevin.core.app.KlCore;
import com.kevin.core.http.utils.ToastUtil;
import com.kevin.core.utils.LogUtil;

import static vchat.common.constant.ActivityPath.MATCH_ACTIVITY;

@Route(path = MATCH_ACTIVITY)
public class MatchActivity extends BaseTouchableActivity implements MatchContract.View, CameraRenderer.PhotoCallBack,MediaDataObserverPlugin.RemoteCaptureCallback{
    @BindView(R2.id.toolbar)
    FaceToolbar toolbar;
    @BindView(R2.id.big_video_view_container)
    FrameLayout bigVideoViewContainer;
    @BindView(R2.id.small_video_view_container)
    FrameLayout smallVideoViewContainer;

    @BindView(R2.id.click_layout)
    View clickLayout;

    @BindView(R2.id.layout_face_unity)
    ConstraintLayout layoutFaceUnity;
    @BindView(R2.id.cl_between_calling)
    ConstraintLayout clBetweenCalling;
    @BindView(R2.id.effect_container)
    LinearLayout effectContainer;
    @BindView(R2.id.beauty_container)
    LinearLayout beautyContainer;
    @BindView(R2.id.iv_face_effect)
    AppCompatImageView face_effect_button;
    @BindView(R2.id.iv_voice_effect)
    AppCompatImageView voice_effect_button;
    @BindView(R2.id.iv_remove_mosaic)
    AppCompatImageView remove_mosaic_button;
    @BindView(R2.id.iv_add_mosaic)
    AppCompatImageView add_mosaic_button;
    @BindView(R2.id.iv_like)
    AppCompatImageView like_button;
    @BindView(R2.id.next_button)
    Button next_button;
    @BindView(R2.id.status_text_view)
    TextView status_text_view;

    private SurfaceView mSurfaceViewRemote;
    private GLSurfaceView mSurfaceViewLocal;

    //滤镜相关
    private FUManager fuManager;
    protected BeautyPanel mBeautyPanel;
    protected EffectPanel mEffectPanel;
    /**
     * 视频聊天全局变量
     */
    private VideoChatManager videoChatManager;

    //Home键监听
    private FaceVideoHelper faceVideoHelper;

    private MatchPresenter mPresenter;

    private HeadsetReceiver myReceiver;

    private int remoteUid;

    private int smallWidth, smallHeight;

    private static final int VOICESTYPE_MALE = 0;
    private static final int VOICESTYPE_SHOTA = 1;
    private static final int VOICESTYPE_LOLI = 2;
    private static final int VOICESTYPE_HULK = 3;
    private static final int VOICESTYPE_NONE = 4;

    int mVoiceStyle = VOICESTYPE_NONE;

    @Nullable
    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_match;
    }


    @Override
    protected void init(Bundle savedInstanceState) {
        mPresenter = new MatchPresenter(this);
        EventBus.getDefault().register(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
        faceVideoHelper = new FaceVideoHelper(this);
        faceVideoHelper.solveFileUriExposedException();
        RongPushClient.clearAllNotifications(this);
        smallWidth = ScreenUtils.getScreenWidth() / 4;
        smallHeight = ScreenUtils.getScreenHeight() / 4;

        init();

        new PermissionHelper(this).addSuccessListener((permissionType) -> {
            fuManager.openCameraAgain();
            mPresenter.onMatch();
        }).addFailListener((permissionType, permissionStr, deny) -> {
            finish();
        }).requestPermission(PermissionConstants.CAMERA,
                        PermissionConstants.STORAGE,
                        PermissionConstants.MICROPHONE);

        faceVideoHelper.bringToFront(clickLayout, clBetweenCalling, effectContainer, beautyContainer, layoutFaceUnity);
        myReceiver = new HeadsetReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        super.registerReceiver(myReceiver, intentFilter);

        status_text_view.setText("Matching...");
    }

    private void init() {
        try{
            FloatWindow.get().hide();
        }catch (Exception e){}

        initRtcEngine();
        initFaceUnity();

        initView();
    }
    /**
     * 声网相关初始化
     */
    private void initRtcEngine(){
        videoChatManager = VideoChatManager.getInstance();
        videoChatManager.init(getBaseContext(), mRtcEventHandler);
        videoChatManager.muteLocalAudioStream(false);
        videoChatManager.muteLocalVideoStream(false);
        mSurfaceViewRemote = RtcEngine.CreateRendererView(getBaseContext());
        bigVideoViewContainer.addView(mSurfaceViewRemote);
    }
    /**
     * 相芯相关初始化
     */
    private void initFaceUnity() {
        fuManager = FUManager.getInstance();
        mSurfaceViewLocal = new GLSurfaceView(this);
        if ("V8.0.5.0.0.MBFCNDG".equals(Build.VERSION.INCREMENTAL) || "m1721.Flyme_7.0.1540265439".equals(Build.VERSION.INCREMENTAL) || "V10.1.1.0.NDDCNFI".equals(Build.VERSION.INCREMENTAL) || (Build.BRAND.equalsIgnoreCase("OPPO") && !"1524628693".equals(Build.VERSION.INCREMENTAL))) {
            //Xiaomi MI MAX 2 奇葩问题,解决surfaceview层叠被遮盖的问题，非最终解决方案
            //红米note 4 OPPO 等手机发现类似问题
        } else {
            mSurfaceViewLocal.setZOrderMediaOverlay(true);
        }
        smallVideoViewContainer.addView(mSurfaceViewLocal);
        mSurfaceViewLocal.setEGLContextClientVersion(2);
        fuManager.init(this, mSurfaceViewLocal, videoChatManager);
        mSurfaceViewLocal.setRenderer(fuManager.getmCameraRenderer());
        fuManager.getmCameraRenderer().setCallBack(this);
        mSurfaceViewLocal.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mBeautyPanel = new BeautyPanel(findViewById(R.id.beauty_container), fuManager.getmFURenderer(), null);
        mEffectPanel = new EffectPanel(findViewById(R.id.effect_container), fuManager.getmFURenderer(), null, new EffectPanel.IEffectPanel() {
            @Override
            public boolean canClose() {
                return false;
            }
        });
        loadMosaicEffect();

        fuManager.onCreate();
        fuManager.onResume();
    }
    /**
     * 加载马赛克特效
     */
    private void loadMosaicEffect(){
        Effect effect = EffectEnum.Effect_masaike_light.effect();
        fuManager.getmFURenderer().onEffectSelected(effect);
    }

    private void initView() {
        BarUtils.setStatusBarColor(this, Color.TRANSPARENT);
        BarUtils.setStatusBarLightMode(this, true);

        toolbar.leftClick(v -> finish());
        toolbar.setClickable(true);
        toolbar.setLeftImage(com.innotech.deercommon.R.mipmap.topbar_back_white);
    }

    @OnClick({R2.id.iv_face_effect, R2.id.iv_voice_effect, R2.id.iv_remove_mosaic,R2.id.iv_add_mosaic, R2.id.iv_like,R2.id.next_button,R2.id.small_video_view_container})
    public void onViewClicked(View view) {
        int i = view.getId();
        if(i == R.id.iv_face_effect){
            faceVideoHelper.bringToFront(clickLayout, clBetweenCalling, effectContainer, beautyContainer, layoutFaceUnity);
            effectContainer.setVisibility(effectContainer.isShown() ? View.INVISIBLE : View.VISIBLE);
        }else if(i == R.id.iv_voice_effect){
            changeVoice();
        }else if(i == R.id.iv_remove_mosaic){//移除马赛克效果
            Effect effect = EffectEnum.EffectNone.effect();
            fuManager.getmFURenderer().onEffectSelected(effect);
            add_mosaic_button.setVisibility(View.VISIBLE);
            remove_mosaic_button.setVisibility(View.GONE);
        }else if(i == R.id.iv_add_mosaic){
            Effect effect = EffectEnum.Effect_masaike_light.effect();
            fuManager.getmFURenderer().onEffectSelected(effect);
            remove_mosaic_button.setVisibility(View.VISIBLE);
            add_mosaic_button.setVisibility(View.GONE);
        }else if(i == R.id.next_button){
            mPresenter.onMatch();
            status_text_view.setText("Matching...");
        }else if(i == R.id.iv_like){
            //这里需要发送添加好友请求，对方收到通知后，会弹出一个同意与否的alert弹窗。
            //每个用户只能显示一次，点击后消失。匹配到新用户或者视频的时候显示。
            mPresenter.addFriend();
            like_button.setVisibility(View.GONE);
        }else if (i == R.id.small_video_view_container) {
            mLocalViewIsBig = !mLocalViewIsBig;
            swapLocalRemoteDisplay();
        }
    }

    private int getColorId(int style){
        if (mVoiceStyle == style){
            return KlCore.getApplicationContext().getResources().getColor(R.color.common_color_primary);
        }
        else{
            return KlCore.getApplicationContext().getResources().getColor(R.color.common_text_color_main);
        }
    }

    private void changeVoice(){
        //以前的道具改成变声
        BottomListDialog.BUILDER()
                .addItem(new BottomListDialog.Item(R.string.voice_male,
                        getColorId(VOICESTYPE_MALE),
                        dialog -> {
                            dialog.dismiss();
                            mVoiceStyle = VOICESTYPE_MALE;
                            videoChatManager.setMaleVoice();
                        }))
                .addItem(new BottomListDialog.Item(R.string.voice_shota,
                        getColorId(VOICESTYPE_SHOTA),
                        dialog -> {
                            dialog.dismiss();
                            mVoiceStyle = VOICESTYPE_SHOTA;
                            videoChatManager.setShotaVoice();
                        }))
                .addItem(new BottomListDialog.Item(R.string.voice_loli,
                        getColorId(VOICESTYPE_LOLI),
                        dialog -> {
                            dialog.dismiss();
                            mVoiceStyle = VOICESTYPE_LOLI;
                            videoChatManager.setLoliVoice();
                        }))
                .addItem(new BottomListDialog.Item(R.string.voice_hulk,
                        getColorId(VOICESTYPE_HULK),
                        dialog -> {
                            dialog.dismiss();
                            mVoiceStyle = VOICESTYPE_HULK;
                            videoChatManager.setHulkVoice();
                        }))
                .build(MatchActivity.this).show();
    }

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            LogUtil.d("匹配后成功加入频道");
            videoChatManager.openSpeaker();
        }

        @Override
        public void onConnectionLost() {
            super.onConnectionLost();
        }

        @Override
        public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
            super.onNetworkQuality(uid, txQuality, rxQuality);
        }

        @Override
        public void onFirstRemoteAudioFrame(int uid, int elapsed) {
            super.onFirstRemoteAudioFrame(uid, elapsed);
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            ToastUtil.showToast("有视频推流过来");
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                setupRemoteVideo(uid);

                faceVideoHelper.setViewInVisible(smallVideoViewContainer, mSurfaceViewLocal);
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            ToastUtil.showToast("有人加入");
            super.onUserJoined(uid, elapsed);
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (videoChatManager.getMediaDataObserverPlugin() != null) {
                    videoChatManager.getMediaDataObserverPlugin().addDecodeBuffer(uid);
                }
            });
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                if (videoChatManager.getMediaDataObserverPlugin() != null) {
                    videoChatManager.getMediaDataObserverPlugin().removeDecodeBuffer(uid);
                }
            });
        }

        @Override
        public void onUserMuteVideo(final int uid, final boolean mute) {

        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(HeadsetEvent headsetEvent) {

    }
    /**
     * 被动匹配逻辑
     * 正在播放视频，被匹配的逻辑
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMatchEvent(MatchEvent matchEvent) {
        //停止当前视频播放
        if(mFaceVideoView != null){
            mFaceVideoView.stop();
            mFaceVideoView.release();
            mFaceVideoView.setVisibility(View.GONE);
        }
        status_text_view.setText("Matching");
        mPresenter.imCallMessageBean = ImMessageUtily.getInstance().formatCallMessage(ImCallMessageBean.CallType.CALL_VIDEO, matchEvent.contactBean);

        faceVideoHelper.joinChannel(videoChatManager, mPresenter.imCallMessageBean.channelId);
        setupLocalVideo(R.id.small_video_view_container);
    }
    /**
     * 显示大小屏数据源
     */
    private void showBigRemote() {
        if (null != mPresenter.imCallMessageBean && mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VIDEO) {
            faceVideoHelper.setViewVisible(smallVideoViewContainer, bigVideoViewContainer, mSurfaceViewLocal, mSurfaceViewRemote);
            if (bigVideoViewContainer.getChildCount() > 0) {
                faceVideoHelper.setViewInVisible(mSurfaceViewLocal, smallVideoViewContainer);
                setupLocalVideo(R.id.small_video_view_container);
            }
        }
    }
    private Bitmap localBitmap;
    /**
     * 获取本地截图
     *
     * @param bitmap
     */
    @Override
    public void getPhoto(Bitmap bitmap) {
        localBitmap = bitmap;
        videoChatManager.getMediaDataObserverPlugin().saveRenderVideoSnapshot(this, remoteUid);
    }
    /**
     * 获取远端截图
     *
     * @param bitmap
     */
    @Override
    public void getRemoteBitmap(Bitmap bitmap) {
        if (isFinishing()) {
            return;
        }
//        faceVideoHelper.captureScreen(localBitmap, bitmap, layoutShare);
    }

    private void setupRemoteVideo(int uid) {
        remoteUid = uid;
        videoChatManager.setupRemoteVideo(mSurfaceViewRemote, uid);
    }

    MatchResponse matchResponse = null;
    /**
     * 主动匹配逻辑
     * 进入后请求match接口，直接匹配到人的逻辑
     */
    @Override
    public void onMatched(MatchResponse response){
        matchResponse = response;
        if(response.isPlayVideo()){
            startPrepareMatchedVideo();
        }else if(matchResponse.isMatchUser()){
            status_text_view.setText("Connecting...");
            mPresenter.imCallMessageBean = ImMessageUtily.getInstance().formatCallMessage(ImCallMessageBean.CallType.CALL_VIDEO, matchResponse.getMatchUser());
            faceVideoHelper.joinChannel(videoChatManager, matchResponse.get_channel_id());
            setupLocalVideo(R.id.small_video_view_container);
        }
    }

    FaceVideoView mFaceVideoView;
    /**
     * 开始播放匹配到的视频
     */
    private void startPrepareMatchedVideo(){
        if (mFaceVideoView == null) {
            mFaceVideoView = new FaceVideoView(MatchActivity.this);
            mFaceVideoView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            mFaceVideoView.setBackgroundColor(ContextCompat.getColor(this,R.color.black));
        }
        bigVideoViewContainer.removeAllViews();
        bigVideoViewContainer.addView(mFaceVideoView);

        bigVideoViewContainer.setForegroundGravity(Gravity.CENTER);
        faceVideoHelper.setViewVisible(smallVideoViewContainer);
        faceVideoHelper.setViewVisible(bigVideoViewContainer);

        faceVideoHelper.bringToFront(bigVideoViewContainer,smallVideoViewContainer,clickLayout, clBetweenCalling, effectContainer, beautyContainer, layoutFaceUnity);

        mFaceVideoView.setVideoListener(new FaceVideoView.IVideoListener() {
            @Override
            public void onStartPlay() {
                startPlay();
            }

            @Override
            public void onCompletion() {
//                leaveChannel();
            }

            @Override
            public void onBufferingUpdate(int i) {
                LogUtil.i("kevin_videoplayer", "buffering :"+i);
            }

            @Override
            public void onStop() {
                status_text_view.setText("Matching...");
            }
        });

//        mFaceVideoView.startPrepare(matchResponse.getPlay_url());
        mFaceVideoView.startPrepare("https://indiaim-prod.oss-ap-southeast-1.aliyuncs.com/video/v99.mp4");
    }

    private void startPlay(){
        setupLocalVideo(R.id.small_video_view_container);
        mFaceVideoView.startplay();
    }

    private void swapLocalRemoteDisplay() {
        faceVideoHelper.setViewVisible(toolbar,smallVideoViewContainer, bigVideoViewContainer, mSurfaceViewLocal, mSurfaceViewRemote);
        //大小布局
        if (!mLocalViewIsBig) {
            //小窗显示本地视频
            setBigViewParams(bigVideoViewContainer);
            removeThenAddView(bigVideoViewContainer, false);
            setSmallViewParams(smallVideoViewContainer);
        } else {
            //大窗显示本地视频
            setBigViewParams(smallVideoViewContainer);
            setSmallViewParams(bigVideoViewContainer);
            removeThenAddView(bigVideoViewContainer, true);
        }
        faceVideoHelper.bringToFront(toolbar,clickLayout, clBetweenCalling, effectContainer, beautyContainer, layoutFaceUnity);
    }

    private boolean mLocalViewIsBig = true;

    /**
     * 初始化本地视频聊天
     */
    private void setupLocalVideo(@IdRes int viewGroupId) {
        if (viewGroupId == R.id.big_video_view_container) {
            //设置大图为本地摄像头，小图为远端
            mLocalViewIsBig = true;
            swapLocalRemoteDisplay();
        } else if (viewGroupId == R.id.small_video_view_container) {
            //设置小图为本地摄像头，大图为远端
            mLocalViewIsBig = false;
            swapLocalRemoteDisplay();
        }
    }

    private void setBigViewParams(View view) {
        faceVideoHelper.setBigViewParams(view);
        layoutFaceUnity.bringToFront();
    }

    private void removeThenAddView(FrameLayout view, boolean isMediaOverlay) {
        if (view != null && view.getChildCount() > 0) {
            View v = view.getChildAt(0);
            view.removeAllViews();
            if (v instanceof SurfaceView) {
                ((SurfaceView) v).setZOrderMediaOverlay(isMediaOverlay);
            }
            view.addView(v);
        }
    }

    private void setSmallViewParams(View view) {
        faceVideoHelper.setSmallViewParams(view, smallWidth, smallHeight);
        faceVideoHelper.bringToFront(clickLayout, clBetweenCalling, effectContainer, beautyContainer, layoutFaceUnity, view);
    }

    @Override
    public void finish() {
        mPresenter.onLeave();
        super.finish();
    }

    @Override
    protected void onDestroy() {
        if (mFaceVideoView != null) {
            mFaceVideoView.release();
        }
        EventBus.getDefault().unregister(this);
        //当 activit结束时  需要把 注册的广播接收者 注销掉。
        super.unregisterReceiver(myReceiver);

        videoChatManager.stopPlayRing();
        if (videoChatManager.getMediaDataObserverPlugin() != null) {
            videoChatManager.removeVideoObserver();
            videoChatManager.getMediaDataObserverPlugin().removeAllBuffer();
        }
        RtcEngine.destroy();
        if (faceVideoHelper != null) {
            faceVideoHelper.onDestroy();
        }
        if (videoChatManager != null) {
            videoChatManager.onDestroy();
        }
        if (fuManager != null) {
            fuManager.onPause();
            fuManager.onDestroy();
        }
        super.onDestroy();

        if (videoChatManager != null) {
            videoChatManager.leaveChannel();
        }
    }

    private void leaveChannel() {
        videoChatManager.stopPlayRing();
        if (fuManager != null) {
            fuManager.onPause();
            fuManager.onDestroy();
        }
        if (videoChatManager != null) {
            videoChatManager.leaveChannel();
        }
        finish();
    }
    /**
     * 这个方法千万不能删除，IM消息有使用
     */
    @Keep
    public void closeActivity() {
        leaveChannel();
    }
}
