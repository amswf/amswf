package vchat.video.view;

import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.android.innoshortvideo.core.InnoAVVender.faceUnity.entity.Effect;
import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.innotech.deercommon.basemvp.BasePresenter;
import com.kevin.core.app.KlCore;
import com.kevin.core.imageloader.FaceImageView;
import com.kevin.core.rxtools.RxTools;
import com.kevin.core.utils.LogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Random;

import butterknife.BindView;
import butterknife.OnClick;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.plugin.rawdata.MediaDataObserverPlugin;
import io.agora.rtc.plugin.rawdata.MediaPreProcessing;
import io.rong.push.RongPushClient;
import rx.Subscriber;
import vchat.common.agora.VideoChatManager;
import vchat.common.constant.ActivityPath;
import vchat.common.constant.Keys;
import vchat.common.entity.response.MatchUserResponse;
import vchat.common.event.HeadsetEvent;
import vchat.common.floatwindow.FloatWindow;
import vchat.common.greendao.im.ImCallMessageBean;
import vchat.common.greendao.user.UserBase;
import vchat.common.helper.PermissionHelper;
import vchat.common.im.ImMessageUtily;
import vchat.common.im.RongyunUtily;
import vchat.common.manager.UserManager;
import vchat.common.provider.ProviderFactory;
import vchat.common.provider.contacts.IRelationProvider;
import vchat.common.util.CharUtil;
import vchat.common.util.ImageLoaderUtil;
import vchat.common.util.MediaHelper;
import vchat.common.widget.CommonToast;
import vchat.common.widget.ImageDotView;
import vchat.common.widget.dialog.BottomListDialog;
import vchat.common.widget.textdrawable.TextDrawable;
import vchat.video.R;
import vchat.video.R2;
import vchat.video.ReportConstant;
import vchat.video.ShutVideoEvent;
import vchat.video.activity.AudioRecordActivity;
import vchat.video.contract.FaceVideoContract;
import vchat.video.faceunity.entity.EffectEnum;
import vchat.video.faceunity.renderer.CameraRenderer;
import vchat.video.presenter.FaceVideoPresenter;
import vchat.video.utils.FUManager;
import vchat.video.utils.FaceVideoHelper;
import vchat.video.utils.HeadsetReceiver;
import vchat.video.utils.OnDoubleClickListener;

import static io.agora.rtc.Constants.QUALITY_DOWN;
import static vchat.common.constant.ActivityPath.FACE_VIDEO;

/**
 * @author wang
 * @date 2019/1/11
 */

@Route(path = FACE_VIDEO)
public class FaceVideoActivity extends BaseTouchableActivity
        implements FaceVideoContract.View, CameraRenderer.PhotoCallBack, MediaDataObserverPlugin.RemoteCaptureCallback, FaceVideoHelper.TimerListener {

    @BindView(R2.id.big_video_view_container)
    FrameLayout remoteVideoViewContainer;
    @BindView(R2.id.small_video_view_container)
    FrameLayout localVideoViewContainer;
    @BindView(R2.id.iv_mute)
    ImageDotView ivMute;
    @BindView(R2.id.iv_switch_camera)
    AppCompatImageView ivSwitchCamera;
    @BindView(R2.id.iv_end_call)
    AppCompatImageView ivEndCall;
    @BindView(R2.id.iv_mute_local_video_stream)
    ImageDotView ivMuteLocalVideoStream;
    //    @BindView(R2.id.tv_switch_video_audio)
//    TextView tvSwitchVideoAudio;
    @BindView(R2.id.iv_avatar)
    FaceImageView ivAvatar;
    @BindView(R2.id.iv_change_layout)
    AppCompatImageView ivChangeLayout;
    @BindView(R2.id.iv_face_up)
    AppCompatImageView ivFaceUp;

    @BindView(R2.id.tv_switch_camera)
    TextView tvSwitchCamera;
    @BindView(R2.id.tv_face_up)
    TextView tvFaceUp;
    @BindView(R2.id.iv_sound)
    ImageDotView ivSound;
    @BindView(R2.id.tv_user_name)
    TextView tvUserName;
    @BindView(R2.id.tv_remote_name)
    TextView tvRemoteName;
    @BindView(R2.id.tv_tips)
    TextView tvTips;
    @BindView(R2.id.rl_call_before)
    RelativeLayout rlCallBefore;
    @BindView(R2.id.cl_refuse_accept)
    ConstraintLayout clRefuseAccept;
    @BindView(R2.id.cl_between_calling)
    ConstraintLayout clBetweenCalling;
    //    @BindView(R2.id.remote_video_view)
    SurfaceView mSurfaceViewRemote;
    @BindView(R2.id.layout_face_unity)
    ConstraintLayout layoutFaceUnity;

    @BindView(R2.id.beauty_container)
    LinearLayout beautyContainer;
    @BindView(R2.id.effect_container)
    LinearLayout effectContainer;
    @BindView(R2.id.rl_user_info)
    RelativeLayout rlUserInfo;
//    @BindView(R2.id.tv_sound)
//    TextView tvSound;
//    @BindView(R2.id.tv_mute)
//    TextView tvMute;

    @BindView(R2.id.tv_title)
    TextView tvTitle;
    @BindView(R2.id.chr_time)
    Chronometer chrTime;
    @BindView(R2.id.cl_hide_when_face_up)
    ConstraintLayout layoutHideWhenFaceUp;
    @BindView(R2.id.tv_audio_call)
    TextView audioCall;
    @BindView(R2.id.ll_video_call)
    LinearLayout videoCall;
    @BindView(R2.id.cl_hide_when_video)
    ConstraintLayout layoutHideWhenVideo;
    @BindView(R2.id.layout_share)
    FrameLayout layoutShare;
    @BindView(R2.id.shadow_layout)
    View layoutShadow;
    @BindView(R2.id.button_layout)
    TouchRelativeLayout buttonLayout;
    @BindView(R2.id.click_layout)
    View clickLayout;
    @BindView(R2.id.tv_to_message_layout)
    FrameLayout toSendMessage;

    private GLSurfaceView mSurfaceViewLocal;

    FaceVideoView mFaceVideoView;

    private static final String TO_SEND_MESSAGE = "00:20";
    private static final String NOT_BESIDE = "00:30";
    private static final String IS_BUSY = "01:00";


    public static boolean isCalling = false;
    /**
     * 视频聊天全局变量
     */
    private VideoChatManager videoChatManager;

    //滤镜相关
    private FUManager fuManager;
    protected BeautyPanel mBeautyPanel;
    protected EffectPanel mEffectPanel;

    //Home键监听
    private FaceVideoHelper faceVideoHelper;

    private int smallWidth, smallHeight;

    private int remoteUid;


    final private int ACTION_STATUS_MATCHING = 0;//正在匹配
    final private int ACTION_STATUS_WAITING = 1; //等待接听中
    final private int ACTION_STATUS_CALLING = 2; //正在打电话
    final private int ACTION_STATUS_ENDING = 3; //通话结束
    final private int ACTION_STATUS_PLAYINGVIDEO = 4; //正在拨打视频

    private int mActionStatus = ACTION_STATUS_MATCHING;

    Subscriber mCountDownScriber = null;

    MatchUserResponse mMatchUserResponse = null;

    boolean mCountdownOver = false;
    boolean mVideoPrepared = false;

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
        return R.layout.video_activity_video;
    }

    HeadsetReceiver myReceiver;

    @Override
    protected void init(Bundle savedInstanceState) {
        mPresenter = new FaceVideoPresenter(this);
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

        isCalling = true;

        getIntentAndSetStatus();

        initTouchView(buttonLayout);
        init();

        new PermissionHelper(this).addSuccessListener((permissionType) -> {
            fuManager.openCameraAgain();
            initAgoraEngineAndJoinChannel();
        }).addFailListener((permissionType, permissionStr, deny) -> {
            if (isFinishing() || chrTime == null) {
                return;
            }
            mPresenter.sendCloseCallMessage(ImCallMessageBean.HangUpType.USER_CLOSE, faceVideoHelper.getChronometerSeconds(chrTime));

            finish();
        })
                .requestPermission(PermissionConstants.CAMERA,
                        PermissionConstants.STORAGE,
                        PermissionConstants.MICROPHONE);
        faceVideoHelper.bringToFront(clickLayout, clBetweenCalling, effectContainer, beautyContainer, layoutFaceUnity);
        myReceiver = new HeadsetReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        super.registerReceiver(myReceiver, intentFilter);

        if (null != mPresenter.imCallMessageBean) {
            if (FaceVideoPresenter.isReceiver) {
                if (RongyunUtily.getInstance().getDumpList() != null && RongyunUtily.getInstance().getDumpList().size() > 0) {
                    for (ImCallMessageBean messageBean : RongyunUtily.getInstance().getDumpList()) {
                        if (mPresenter.imCallMessageBean.channelId.equals(messageBean.channelId)) {
                            RongyunUtily.getInstance().clearMessageList();
                            leaveChannel();
                            return;
                        }
                    }
                }
            }
        }
    }

    private void getIntentAndSetStatus() {
        mPresenter.imCallMessageBean = this.getIntent().getParcelableExtra(Keys.IM_MESSAGE_BEAN);
        if (mPresenter.imCallMessageBean != null) {
            mPresenter.imCallMessageBean.sendToContact = this.getIntent().getParcelableExtra(Keys.CONTACT_BEAN);
        }
        FaceVideoPresenter.isReceiver = this.getIntent().getBooleanExtra(Keys.IS_RECEIVER, true);
        if (FaceVideoPresenter.isReceiver) {
            mActionStatus = ACTION_STATUS_WAITING;
        } else {
            if (mPresenter.imCallMessageBean != null) {
                //列表里面打电话给某人，等待接听
                mActionStatus = ACTION_STATUS_WAITING;
            } else {
                //进来准备匹配人
                mActionStatus = ACTION_STATUS_MATCHING;
            }
        }
    }

    private void init() {
        try {
            FloatWindow.get().hide();
        } catch (Exception e) {

        }

        videoChatManager = VideoChatManager.getInstance();
        videoChatManager.init(getBaseContext(), mRtcEventHandler);
        mSurfaceViewRemote = RtcEngine.CreateRendererView(getBaseContext());
        remoteVideoViewContainer.addView(mSurfaceViewRemote);

        fuManager = FUManager.getInstance();
        clickLayout.setOnTouchListener(new OnDoubleClickListener(new OnDoubleClickListener.DoubleClickCallback() {
            @Override
            public void onDoubleClick() {
            }

            @Override
            public void onSingleClick() {
                if (!chrTime.isShown() || (!localVideoViewContainer.isShown() && !remoteVideoViewContainer.isShown())) {
                    buttonLayout.setVisibility(View.VISIBLE);
                    faceVideoHelper.startTimer();
                }
            }

            @Override
            public void onTouchDown() {
                layoutHideWhenFaceUp.setVisibility(View.VISIBLE);
                effectContainer.setVisibility(View.GONE);
                beautyContainer.setVisibility(View.GONE);
            }
        }));
        initFaceUnity();

        if (fuManager != null) {
            fuManager.onCreate();
            fuManager.onResume();
        }
        initView();
        initChronometer();
    }

    private void initChronometer() {
        chrTime.setOnChronometerTickListener(chronometer -> {
            if (mActionStatus == ACTION_STATUS_WAITING || mActionStatus == ACTION_STATUS_CALLING) {
                if (mPresenter.isConnected || FaceVideoPresenter.isReceiver) {
                    return;
                }
                if (NOT_BESIDE.contentEquals(chronometer.getText())) {
                    CommonToast.showShort(getString(R.string.video_mobile_is_left));
                } else if (IS_BUSY.contentEquals(chronometer.getText())) {
                    CommonToast.showShort(getString(R.string.video_he_is_busy));
                    mPresenter.sendCloseCallMessage(ImCallMessageBean.HangUpType.TIME_OUT_CLOSE, faceVideoHelper.getChronometerSeconds(chrTime));

                    mPresenter.reportCallStatus(ReportConstant.WAITCALL_FIRSTLEAVECHANNEL, ReportConstant.FIRSTJOINPESRSONUSERID, UserManager.getInstance().getUser().userId + "");
                    leaveChannel();
                }
            }
        });
        chrTime.start();
    }

    private void initFaceUnity() {
        mSurfaceViewLocal = new GLSurfaceView(this);
        if ("V8.0.5.0.0.MBFCNDG".equals(Build.VERSION.INCREMENTAL) || "m1721.Flyme_7.0.1540265439".equals(Build.VERSION.INCREMENTAL) || "V10.1.1.0.NDDCNFI".equals(Build.VERSION.INCREMENTAL) || (Build.BRAND.equalsIgnoreCase("OPPO") && !"1524628693".equals(Build.VERSION.INCREMENTAL))) {
            //Xiaomi MI MAX 2 奇葩问题,解决surfaceview层叠被遮盖的问题，非最终解决方案
            //红米note 4 OPPO 等手机发现类似问题
        } else {
            mSurfaceViewLocal.setZOrderMediaOverlay(true);
        }
        localVideoViewContainer.addView(mSurfaceViewLocal);
        mSurfaceViewLocal.setEGLContextClientVersion(2);
        fuManager.init(this, mSurfaceViewLocal, videoChatManager);
        mSurfaceViewLocal.setRenderer(fuManager.getmCameraRenderer());
        fuManager.getmCameraRenderer().setCallBack(this);
        mSurfaceViewLocal.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mBeautyPanel = new BeautyPanel(findViewById(R.id.beauty_container), fuManager.getmFURenderer(), null);
        mEffectPanel = new EffectPanel(findViewById(R.id.effect_container), fuManager.getmFURenderer(), null, new EffectPanel.IEffectPanel() {
            @Override
            public boolean canClose() {
                return isFriend();
            }
        });
//        loadDefaultEffect();
    }


    private boolean isFriend(){
        if (mPresenter.imCallMessageBean != null && mPresenter.imCallMessageBean.sendToContact != null){
            UserBase userBase = mPresenter.imCallMessageBean.sendToContact;
            if (userBase != null && userBase.isFriend()){
                return true;
            }
        }
        return false;
    }

    /**
     * 加载默认道具和变声
     */
    private void loadDefaultEffect(){

        if (isFriend()){
            return;
        }
        Random ra =new Random();
        int r = ra.nextInt(2);
        Effect effect = null;
        if (r == 0){
//            effect = EffectEnum.Effect_changjinglu.effect();
//            fuManager.getmFURenderer().onEffectSelected(effect);
            mVoiceStyle = VOICESTYPE_LOLI;
            videoChatManager.setLoliVoice();
        }
        else{
//            effect = EffectEnum.Effect_shayu.effect();
//            fuManager.getmFURenderer().onEffectSelected(effect);
            mVoiceStyle = VOICESTYPE_SHOTA;
            videoChatManager.setShotaVoice();
        }
        mEffectPanel.setDefaultEffect(effect);
    }

    private void setupRemoteVideo(int uid) {
        remoteUid = uid;
        videoChatManager.setupRemoteVideo(mSurfaceViewRemote, uid);
    }

    private static final long timeDuration = 5 * 60 * 1000;
    private long lastTime;
    /**
     * 远程视频事件处理
     */
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            if (isFinishing()) {
                return;
            }
            if (uid == (int) UserManager.getInstance().getUser().userId) {
                if (mActionStatus != ACTION_STATUS_MATCHING) {
                    videoChatManager.startPlayRing(FaceVideoPresenter.isReceiver ? "/assets/ring_in.mp3" : "/assets/ring_out.mp3");
                }
                if (FaceVideoPresenter.isReceiver) {
                    //接听方
                    if (!isHeadset) {
                        videoChatManager.openSpeaker();
                    }
                } else {
                    //拨打方
                    if (mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VIDEO && !isHeadset) {
                        videoChatManager.openSpeaker();
                    } else {
                        videoChatManager.closeSpeaker();
                    }
                    RongyunUtily.getInstance().call(mPresenter.imCallMessageBean, mPresenter.imCallMessageBean.sendToContact.getRyId(), null);
                }
            }
        }

        @Override
        public void onConnectionLost() {
            super.onConnectionLost();
            if (isFinishing()) {
                return;
            }
            CommonToast.showShort(getString(R.string.video_text_net_error));
            mPresenter.sendCloseCallMessage(ImCallMessageBean.HangUpType.USER_CLOSE, faceVideoHelper.getChronometerSeconds(chrTime));

            leaveChannel();
        }

        @Override
        public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
            super.onNetworkQuality(uid, txQuality, rxQuality);
            if (uid == 0) {
                if (mPresenter.isConnected && rxQuality >= QUALITY_DOWN) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastTime > timeDuration) {
                        CommonToast.showShort(getString(R.string.video_text_net_vbad));
                        lastTime = currentTime;
                    }
                }
            }
        }

        @Override
        public void onFirstRemoteAudioFrame(int uid, int elapsed) {
            super.onFirstRemoteAudioFrame(uid, elapsed);
            mActionStatus = ACTION_STATUS_CALLING;
            if (!FaceVideoPresenter.isReceiver) {
                runOnUiThread(() -> {
                    if (isFinishing()) {
                        return;
                    }
                    mPresenter.isConnected = true;
                    FaceVideoPresenter.currentChannelId = mPresenter.imCallMessageBean.channelId;
                    chrTime.setTextColor(Color.WHITE);
                    chrTime.setBase(SystemClock.elapsedRealtime());
                    chrTime.start();
                    rlCallBefore.setVisibility(View.INVISIBLE);
                    layoutHideWhenVideo.setVisibility(View.INVISIBLE);
                    ivMute.setImageView(R.drawable.video_icon_audio);
                    ivMute.setSelected(false);
//                    tvMute.setTextColor(getResources().getColor(R.color.common_white));
                    ivMuteLocalVideoStream.setImageView(R.drawable.video_icon_switch_audio);
                    ivMuteLocalVideoStream.setSelected(true);
//                    ivMuteLocalVideoStream.setColorFilter(getResources().getColor(R.color.common_color_primary_blue), PorterDuff.Mode.MULTIPLY);

//                    tvSwitchVideoAudio.setTextColor(getResources().getColor(R.color.common_color_primary_blue));
                    if (mPresenter.imCallMessageBean.callType != ImCallMessageBean.CallType.CALL_VIDEO) {
                        //音频
                        videoChatManager.muteLocalVideoStream(true);
                        videoChatManager.closeSpeaker();
                        faceVideoHelper.setViewVisible(clBetweenCalling, ivSound, ivMute, ivMuteLocalVideoStream, rlUserInfo);
//                        faceVideoHelper.changeImageView(ivMuteLocalVideoStream);
                        ivMuteLocalVideoStream.setSelected(false);
//                        ivMuteLocalVideoStream.clearColorFilter();
//                        tvSwitchVideoAudio.setTextColor(getResources().getColor(R.color.common_white));
                    } else {
                        videoChatManager.muteLocalVideoStream(false);
                        ivSound.setSelected(true);
//                        tvSound.setTextColor(getResources().getColor(R.color.common_color_primary_blue));
//                        ivSound.setColorFilter(getResources().getColor(R.color.common_color_primary_blue),
//                                PorterDuff.Mode.MULTIPLY);
                    }
                    if (isHeadset) {
                        ivSound.setImageView(R.drawable.video_icon_sound_disable);
                        ivSound.setEnabled(false);
//                        ivSound.clearColorFilter();
//                        ivSound.setColorFilter(getResources().getColor(R.color.common_bg_transparent_white_7a), PorterDuff.Mode.MULTIPLY);
//                        tvSound.setTextColor(getResources().getColor(R.color.common_bg_transparent_white_7a));
                    }
                    videoChatManager.stopPlayRing();
                    faceVideoHelper.startTimer();
                    faceVideoHelper.setViewVisible(tvTitle, chrTime);
                    toSendMessage.setVisibility(View.GONE);
                    tvTitle.setText(R.string.video_title_audio);
                    audioCall.setText(R.string.video_title_audio);
                    layoutShadow.setVisibility(View.GONE);
//                    ((TextView) findViewById(R.id.tv_cancel)).setText(getString(R.string.video_text_shutdown));
                    videoChatManager.muteLocalAudioStream(false);
//                    faceVideoHelper.sendChatEvent(CallAction.ACTION_CONNECTION, System.currentTimeMillis(), mPresenter.imCallMessageBean);

                });
            }
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                mActionStatus = ACTION_STATUS_CALLING;
                rlUserInfo.setVisibility(View.INVISIBLE);
                setupRemoteVideo(uid);
                isBigSmallLayout = true;
                if (!FaceVideoPresenter.isReceiver) {
                    //发起方
                    rlCallBefore.setVisibility(View.INVISIBLE);
                    faceVideoHelper.setViewVisible(ivMute, ivSound, ivMuteLocalVideoStream);
                    videoChatManager.muteLocalAudioStream(ivMute.isSelected());
                    setupLocalVideo(R.id.small_video_view_container);
                    if (mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VIDEO) {
                        videoChatManager.muteLocalVideoStream(mPresenter.isLocalVideoMuted);
                        if (!mPresenter.isLocalVideoMuted) {
                            faceVideoHelper.setViewVisible(localVideoViewContainer, mSurfaceViewLocal);
                        } else {
                            faceVideoHelper.setViewInVisible(localVideoViewContainer, mSurfaceViewLocal);
                        }
                    } else {
                        if (!mPresenter.isConnected || mPresenter.isLocalVideoMuted) {
                            videoChatManager.muteLocalVideoStream(true);
                            faceVideoHelper.setViewInVisible(localVideoViewContainer, mSurfaceViewLocal);
                        }
                    }
                } else {
                    //接收方
                    setupLocalVideo(R.id.small_video_view_container);
                    if (!mPresenter.isConnected || mPresenter.isLocalVideoMuted) {
                        if (mPresenter.isLocalVideoMuted) {
                            layoutShadow.setVisibility(View.VISIBLE);
                            layoutFaceUnity.setVisibility(View.INVISIBLE);
                        }
                        if (mPresenter.isConnected) {
                            layoutShadow.setVisibility(View.GONE);
                        }
                        faceVideoHelper.setViewInVisible(mSurfaceViewLocal, localVideoViewContainer);
                    }
                }
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            remoteUid = uid;
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
        public void onUserOffline(int uid, int reason) { // Tutorial Step 7
            runOnUiThread(() -> {

                if (isFinishing()) {
                    return;
                }
                if (videoChatManager.getMediaDataObserverPlugin() != null) {
                    videoChatManager.getMediaDataObserverPlugin().removeDecodeBuffer(uid);
                }
                onRemoteUserLeft();
            });
        }

        @Override
        public void onUserMuteVideo(final int uid, final boolean mute) { // Tutorial Step 10
            if (isFinishing()) {
                return;
            }
            runOnUiThread(() -> onRemoteUserVideoMuted(mute));
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
//            runOnUiThread(() -> finish());
        }
    };

    /**
     * 切换音视频布局
     *
     * @param isVideoLayout true:视频 false：音频
     */
    public void switchVideoAudioLayout(boolean isVideoLayout) {
        layoutFaceUnity.setVisibility(isVideoLayout ? View.VISIBLE : View.INVISIBLE);
        rlUserInfo.setVisibility(!isVideoLayout ? View.VISIBLE : View.INVISIBLE);
        if (isVideoLayout) {
            faceVideoHelper.setViewVisible(mSurfaceViewLocal, mSurfaceViewRemote, localVideoViewContainer, remoteVideoViewContainer);
        } else {
            buttonLayout.setVisibility(View.VISIBLE);
            faceVideoHelper.setViewInVisible(mSurfaceViewLocal, mSurfaceViewRemote, localVideoViewContainer, remoteVideoViewContainer);
        }
    }

    /**
     * 显示大小屏数据源
     */
    private void showBigRemote() {
        faceVideoHelper.setViewVisible(ivMute, ivMuteLocalVideoStream, ivSound);
        if (null != mPresenter.imCallMessageBean && mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VIDEO) {
            faceVideoHelper.setViewVisible(localVideoViewContainer, remoteVideoViewContainer, mSurfaceViewLocal, mSurfaceViewRemote);
            setupLocalVideo(R.id.small_video_view_container);
            if (ivMuteLocalVideoStream.isSelected()) {
                onMuteLocalVideoStream(mPresenter.isLocalVideoMuted);
            }
        }
    }

    /**
     * 在远程视频显示开关点击时
     *
     * @param muted true：该用户已暂停发送视频流
     *              false：该用户已恢复发送视频流
     */
    private void onRemoteUserVideoMuted(boolean muted) {
        isRemoteVideoMuted = muted;
        if (isBigSmallLayout) {
            //当前大小屏布局
            if (muted) {
                //对方关闭视频
                ivChangeLayout.setVisibility(View.INVISIBLE);
                if (mSurfaceViewLocal.isShown()) {
                    //本地视频打开状态
                    setupLocalVideo(R.id.big_video_view_container);
                    faceVideoHelper.setViewInVisible(mSurfaceViewRemote, remoteVideoViewContainer);
                } else {
                    //本地视频关闭状态（切换到音频聊天）
                    switchVideoAudioLayout(false);
                }
            } else {
                //对方打开视频
                if (mPresenter.isLocalVideoMuted) {
                    //自己没有打开视频
                    faceVideoHelper.startTimer();
                    ivChangeLayout.setVisibility(View.INVISIBLE);
                    rlUserInfo.setVisibility(View.INVISIBLE);
                    setupLocalVideo(R.id.small_video_view_container);
                    faceVideoHelper.setViewInVisible(mSurfaceViewLocal, localVideoViewContainer);
                } else {
                    ivChangeLayout.setVisibility(View.INVISIBLE);
                    ivChangeLayout.setImageResource(R.drawable.video_icon_left_right);
                    setupLocalVideo(R.id.small_video_view_container);
                    if (!mPresenter.isConnected) {
                        faceVideoHelper.setViewInVisible(mSurfaceViewLocal, localVideoViewContainer);
                    }
                }
            }
        } else {
            //当前左右屏布局
            if (muted) {
                ivChangeLayout.setVisibility(View.INVISIBLE);
                //对方关闭视频
                isBigSmallLayout = true;
                setupLocalVideo(R.id.big_video_view_container);
                faceVideoHelper.setViewInVisible(mSurfaceViewRemote, remoteVideoViewContainer);
            }
        }
    }

    /**
     * 在远程聊天断开时
     */
    private void onRemoteUserLeft() {
        CommonToast.showShort(getString(R.string.video_remote_shut_down));
        leaveChannel();
    }


//    public static boolean isReceiver;

    private void initView() {
        BarUtils.setStatusBarColor(this, Color.TRANSPARENT);
        BarUtils.setStatusBarLightMode(this, true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) audioCall.getLayoutParams();
            lp.topMargin = BarUtils.getStatusBarHeight() + SizeUtils.dp2px(12);
            audioCall.setLayoutParams(lp);
            ViewGroup.MarginLayoutParams lp1 = (ViewGroup.MarginLayoutParams) tvRemoteName.getLayoutParams();
            lp1.topMargin = BarUtils.getStatusBarHeight() + SizeUtils.dp2px(1);
            tvRemoteName.setLayoutParams(lp1);
            ViewGroup.MarginLayoutParams lp2 = (ViewGroup.MarginLayoutParams) ivSwitchCamera.getLayoutParams();
            lp2.topMargin = BarUtils.getStatusBarHeight();
            ivSwitchCamera.setLayoutParams(lp2);
        }
        faceVideoHelper.setTimerListener(this);
        MediaHelper.getInstance().init(this);

        if (null != mPresenter.imCallMessageBean) {
            if (FaceVideoPresenter.isReceiver) {
                //被叫
                //被动收到消息进入通话页面
                FaceVideoPresenter.currentChannelId = mPresenter.imCallMessageBean.channelId;
                layoutFaceUnity.setVisibility(View.INVISIBLE);
                clRefuseAccept.setVisibility(View.VISIBLE);
                if (mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VOICE) {
                    //音频
                    videoCall.setVisibility(View.GONE);
                    audioCall.setVisibility(View.VISIBLE);
                    audioCall.setText(getString(R.string.video_receive_audio_invite));
                    findViewById(R.id.iv_accept_with_audio).setVisibility(View.GONE);
                    ((AppCompatImageView) findViewById(R.id.iv_accept)).setImageResource(R.drawable.video_icon_audio_accept);
                    tvTitle.setText(getString(R.string.video_title_audio));
                    rlUserInfo.setVisibility(View.VISIBLE);
                    faceVideoHelper.setViewInVisible(layoutFaceUnity,
                            localVideoViewContainer,
                            remoteVideoViewContainer,
                            mSurfaceViewLocal,
                            mSurfaceViewRemote);
                } else {
                    //视频
                    layoutFaceUnity.setVisibility(View.VISIBLE);
                    tvTitle.setText(getString(R.string.video_title_video));
                    rlUserInfo.setVisibility(View.INVISIBLE);
                    layoutHideWhenVideo.setVisibility(View.INVISIBLE);
                }
                rlCallBefore.setVisibility(View.VISIBLE);
                clBetweenCalling.setVisibility(View.INVISIBLE);

                notifyView();
            } else {
                //主叫
                //自己拨打进入通话页面
                rlCallBefore.setVisibility(View.INVISIBLE);
                if (mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VIDEO) {
                    tvTitle.setText(getString(R.string.video_title_video));
                    setupLocalVideo(R.id.big_video_view_container);
                    mPresenter.isLocalVideoMuted = false;
                    faceVideoHelper.setViewInVisible(rlUserInfo, layoutHideWhenVideo, mSurfaceViewRemote, remoteVideoViewContainer);
                } else {
                    tvTitle.setText(getString(R.string.video_title_audio));
                    rlUserInfo.setVisibility(View.VISIBLE);
                    faceVideoHelper.setViewInVisible(layoutFaceUnity,
                            localVideoViewContainer,
                            remoteVideoViewContainer,
                            mSurfaceViewLocal,
                            mSurfaceViewRemote);
                }
                if (mPresenter.imCallMessageBean.sendToContact != null) {
                    if (!TextUtils.isEmpty(mPresenter.imCallMessageBean.sendToContact.getShowRemarkName())) {
                        TextDrawable ivHeadPlaceholder = TextDrawable.builder()
                                .buildRound(CharUtil.isChineseOrEnglish(mPresenter.imCallMessageBean.sendToContact
                                                .getShowRemarkName()
                                                .substring(0,
                                                        1)) ? mPresenter.imCallMessageBean.sendToContact
                                                .getShowRemarkName()
                                                .substring(
                                                        0,
                                                        1) : "#",
                                        ContextCompat.getColor(this,
                                                R.color.common_color_primary_blue));
                        ImageLoaderUtil.getInstance().loadImageWithCircle(mPresenter.imCallMessageBean.sendToContact.getThumbnailAvatar(), ivAvatar, ivHeadPlaceholder);
                    }
                }
            }
        } else {
            //正在匹配
            //主叫
            //自己拨打进入通话页面
            rlCallBefore.setVisibility(View.VISIBLE);
            audioCall.setVisibility(View.GONE);
            videoCall.setVisibility(View.VISIBLE);
            tvTips.setText(getString(R.string.video_matching));
            setupLocalVideo(R.id.big_video_view_container);
            mPresenter.isLocalVideoMuted = false;
            faceVideoHelper.setViewInVisible(rlUserInfo, layoutHideWhenVideo, mSurfaceViewRemote, remoteVideoViewContainer);
        }
    }

    /**
     * 刷新页面View
     */
    private void notifyView() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (null != mPresenter.imCallMessageBean.sendToContact) {
                    IRelationProvider contactsDBProvider = ProviderFactory.getInstance().getRelationProvider();
                    UserBase localContactBean = contactsDBProvider.queryBaseUserByRongYunId(mPresenter.imCallMessageBean.sendToContact.getRyId());
                    if (null != localContactBean) {
                        mPresenter.imCallMessageBean.sendToContact.setRemark(localContactBean.getRemark());
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //2019-02-14 14:51:46 修改为全局变量
                        String mName = mPresenter.imCallMessageBean.sendToContact == null ? getString(R.string.common_fei_you) : mPresenter.imCallMessageBean.sendToContact.getShowRemarkName();
                        tvRemoteName.setText(mName);
                        tvUserName.setText(mName);
                        if (mPresenter.imCallMessageBean.sendToContact != null) {
                            if (!TextUtils.isEmpty(mName)) {
                                TextDrawable ivHeadPlaceholder = TextDrawable.builder().buildRound(CharUtil.isChineseOrEnglish(mName.substring(0,
                                        1)) ? mName.substring(
                                        0,
                                        1) : "#", ContextCompat.getColor(FaceVideoActivity.this, R.color.common_color_primary_blue));
                                ImageLoaderUtil.getInstance().loadImageWithCircle(mPresenter.imCallMessageBean.sendToContact.getThumbnailAvatar(), ivAvatar, ivHeadPlaceholder);
                            }
                        }
                        if (FaceVideoPresenter.isReceiver) {
                            tvTips.setText(getString(mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VOICE ? R.string.video_receive_audio_invite : R.string.video_receive_video_invite));
                        } else {
                            if (mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VIDEO) {
                                tvTitle.setText(R.string.video_waiting_to_accept);
                            }
                        }
                    }
                });
            }
        }).start();
    }


    private void setViewWhileMatching() {
        rlCallBefore.setVisibility(View.VISIBLE);
        layoutHideWhenVideo.setVisibility(View.INVISIBLE);
        videoCall.setVisibility(View.VISIBLE);
        audioCall.setVisibility(View.GONE);
        rlUserInfo.setVisibility(View.INVISIBLE);
    }

    /**
     * 等待别人接听时的ui
     */
    private void setViewWhileWaiting() {
        rlCallBefore.setVisibility(View.VISIBLE);
        //layoutHideWhenVideo.setVisibility(mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VIDEO ? View.INVISIBLE : View.VISIBLE);
        videoCall.setVisibility(mPresenter.imCallMessageBean.callType != ImCallMessageBean.CallType.CALL_VIDEO ? View.GONE : View.VISIBLE);
        audioCall.setVisibility(mPresenter.imCallMessageBean.callType != ImCallMessageBean.CallType.CALL_VIDEO ? View.VISIBLE : View.GONE);
        tvTips.setText(getString(R.string.video_waiting_to_accept));
        if (mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VOICE) {
            rlUserInfo.setVisibility(View.VISIBLE);
        } else {
            rlUserInfo.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 初始化视频聊天
     */
    private void initAgoraEngineAndJoinChannel() {
        if (FaceVideoPresenter.isReceiver) {
            //被动收到消息进入通话页面
            videoChatManager.muteLocalVideoStream(true);
            videoChatManager.muteLocalAudioStream(true);
            faceVideoHelper.joinChannel(videoChatManager, mPresenter.imCallMessageBean.channelId);
        } else {
            if (mActionStatus == ACTION_STATUS_MATCHING) {
                //自己拨打进入通话页面
                setViewWhileMatching();
                videoChatManager.muteLocalVideoStream(true);
                videoChatManager.muteLocalAudioStream(true);
                mPresenter.matchUser();
                mMatchUserResponse = null;
                startCountdown();
            } else {
                //自己拨打进入通话页面
                setViewWhileWaiting();
                videoChatManager.muteLocalVideoStream(true);
                videoChatManager.muteLocalAudioStream(true);
                faceVideoHelper.joinChannel(videoChatManager, mPresenter.imCallMessageBean.channelId);
            }
        }
    }

    private FaceVideoPresenter mPresenter;

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
        } else {
            swapLocalRemoteDisplay();
        }
    }



    @OnClick({R2.id.tv_to_message_layout, R2.id.iv_face_up, R2.id.tv_face_up, R2.id.button_layout, R2.id.iv_accept_with_audio, R2.id.iv_face_effect, R2.id.tv_face_effect, R2.id.iv_refuse, R2.id.iv_accept, R2.id.iv_change_layout, R2.id.iv_sound, R2.id.iv_mute_local_video_stream, R2.id.big_video_view_container, R2.id.small_video_view_container, R2.id.iv_mute, R2.id.iv_switch_camera, R2.id.iv_end_call})
    public void onViewClicked(View view) {
        int i = view.getId();
        String status;
        if (mPresenter.isConnected) {
            status = ReportConstant.MEDIACHAT_NORMAL_STATUSLOG;
        } else {
            if (FaceVideoPresenter.isReceiver) {
                status = ReportConstant.ANSWER_BEFORE_STATUSLOGBTN;
            } else {
                status = ReportConstant.CALL_BEFORE_STATUS_LOG_BTN;
            }
        }
        if (i == R.id.small_video_view_container) {
            mPresenter.reportCall(ReportConstant.MEDIACHAT_NORMAL_SWITCH_SCREEN, "");
            if (!mLocalViewIsBig) {
                mLocalViewIsBig = true;
                swapLocalRemoteDisplay();
            } else {
                layoutHideWhenFaceUp.setVisibility(View.VISIBLE);
                effectContainer.setVisibility(View.GONE);
                beautyContainer.setVisibility(View.GONE);
            }
        } else if (i == R.id.big_video_view_container) {
            mPresenter.reportCall(ReportConstant.MEDIACHAT_NORMAL_SWITCH_SCREEN, "");
            if (mLocalViewIsBig) {
                mLocalViewIsBig = false;
                swapLocalRemoteDisplay();
            } else {
                beautyContainer.setVisibility(View.GONE);
                effectContainer.setVisibility(View.GONE);
                layoutHideWhenFaceUp.setVisibility(View.VISIBLE);
            }
        } else if (i == R.id.iv_mute) {
            mPresenter.reportCall(ReportConstant.MEDIACHAT_NORMAL_CLICK_MUTEBTN, "");
            onLocalAudioMuteClicked(view);
        } else if (i == R.id.iv_sound) {
            mPresenter.reportCall(ReportConstant.MEDIACHAT_NORMAL_CLICK_SPEAKERPHONEBTN, "");
            onSpeakerMuted(view);
        } else if (i == R.id.iv_switch_camera) {
            mPresenter.reportCall(ReportConstant.MIRROR_SWITCH_CAMERA, status);
            fuManager.changeCamera();
        } else if (i == R.id.iv_end_call) {
            onEndCallClicked();
        } else if (i == R.id.iv_mute_local_video_stream) {
            onMuteVideo(view);
            mPresenter.reportCall(ReportConstant.MEDIACHAT_NORMAL_CLICK_CAMERABTN, "");
        } else if (i == R.id.iv_change_layout) {
            mPresenter.reportCall(ReportConstant.MEDIACHAT_NORMAL_CHANGE_LAYOUT, "");
            //左上角切换布局
            isBigSmallLayout = !isBigSmallLayout;
            changeLayout(isBigSmallLayout);
        } else if (i == R.id.iv_refuse) {
            //拒绝
            mPresenter.reportCall(ReportConstant.ANSWER_BEFORE_CANCELBTN, "");
//            MediaHelper.getInstance().stopPlayRing();
            videoChatManager.stopPlayRing();
            mPresenter.sendCloseCallMessage(ImCallMessageBean.HangUpType.USER_CLOSE, faceVideoHelper.getChronometerSeconds(chrTime));

            mPresenter.reportCallStatus(ReportConstant.WAITCALL_SECONDCANCEL, ReportConstant.SECONDPERSONUSERID, UserManager.getInstance().getUser().userId + "");
            leaveChannel();
        } else if (i == R.id.iv_accept || i == R.id.iv_accept_with_audio) {
            //接听
            mPresenter.reportCallStatus(ReportConstant.WAITCALL_SECONDJOINCHANNEL, ReportConstant.SECONDJOINPERSONUSERID, UserManager.getInstance().getUser().userId + "");
            layoutShadow.setVisibility(View.GONE);
//            MediaHelper.getInstance().stopPlayRing();
            videoChatManager.stopPlayRing();
            chrTime.setTextColor(Color.WHITE);
            chrTime.setBase(SystemClock.elapsedRealtime());
            chrTime.start();
            faceVideoHelper.startTimer();
            faceVideoHelper.setViewVisible(tvTitle, chrTime);
            mPresenter.isConnected = true;
            toSendMessage.setVisibility(View.GONE);
            audioCall.setText(R.string.video_title_audio);
            layoutShadow.setVisibility(View.GONE);
//            tvSwitchVideoAudio.setTextColor(getResources().getColor(R.color.common_white));
            ivMuteLocalVideoStream.setImageView(R.drawable.video_icon_switch_audio);
            ivMuteLocalVideoStream.setSelected(false);
            ivMute.setImageView(R.drawable.video_icon_audio);
            ivMute.setSelected(false);
//            tvMute.setTextColor(getResources().getColor(R.color.common_white));
//            ((TextView) findViewById(R.id.tv_cancel)).setText(getString(R.string.video_text_shutdown));
            ivSound.setSelected(true);
            if (mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VOICE) {
                mPresenter.reportCall(ReportConstant.ANSWER_BEFORE_ANSWERBTN, "");
                videoChatManager.muteLocalVideoStream(true);
                rlUserInfo.setVisibility(View.VISIBLE);
                videoChatManager.closeSpeaker();
            } else {
                isRemoteVideoMuted = false;
                rlUserInfo.setVisibility(View.INVISIBLE);
                ivSound.setSelected(true);
//                ivSound.setColorFilter(getResources().getColor(R.color.common_color_primary_blue), PorterDuff.Mode.MULTIPLY);
//                tvSound.setTextColor(getResources().getColor(R.color.common_color_primary_blue));

                if (i == R.id.iv_accept_with_audio) {
                    //语音接听
                    mPresenter.reportCall(ReportConstant.ANSWER_BEFORE_AUDIOCHATBTN, "");
                    ivMuteLocalVideoStream.performClick();
                    layoutFaceUnity.setVisibility(View.INVISIBLE);
                    videoChatManager.muteLocalVideoStream(true);
                    mPresenter.isLocalVideoMuted = true;
                } else {
                    mPresenter.reportCall(ReportConstant.ANSWER_BEFORE_ANSWERBTN, "");
                    ivMuteLocalVideoStream.setSelected(true);
//                    ivMuteLocalVideoStream
//                            .setColorFilter(getResources().getColor(R.color.common_color_primary_blue), PorterDuff.Mode.MULTIPLY);
//                    tvSwitchVideoAudio.setTextColor(getResources().getColor(R.color.common_color_primary_blue));
                    layoutFaceUnity.setVisibility(View.VISIBLE);
                    videoChatManager.muteLocalVideoStream(false);
                    mPresenter.isLocalVideoMuted = false;
                }
            }
            if (isHeadset) {
                ivSound.setImageView(R.drawable.video_icon_sound_disable);
                ivSound.setEnabled(false);
//                ivSound.setColorFilter(getResources().getColor(R.color.common_bg_transparent_white_7a), PorterDuff.Mode.MULTIPLY);
//                tvSound.setTextColor(getResources().getColor(R.color.common_bg_transparent_white_7a));
            }

            videoChatManager.muteLocalAudioStream(false);
            faceVideoHelper.setViewInVisible(clRefuseAccept, rlCallBefore);
            layoutHideWhenVideo.setVisibility(View.INVISIBLE);
            clBetweenCalling.setVisibility(View.VISIBLE);
            showBigRemote();
            if (mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VOICE) {
                ivChangeLayout.setVisibility(View.INVISIBLE);
                rlUserInfo.setVisibility(View.VISIBLE);
            }
//            faceVideoHelper.sendChatEvent(CallAction.ACTION_CONNECTION, System.currentTimeMillis(), mPresenter.imCallMessageBean);
        } else if (i == R.id.iv_face_up || i == R.id.tv_face_up) {
//            mPresenter.reportCall(ReportConstant.MIRROR_CLICK_BEAUTYBTN, status);
//            layoutHideWhenFaceUp.setVisibility(beautyContainer.isShown() ? View.VISIBLE : View.INVISIBLE);
//            beautyContainer.setVisibility(beautyContainer.isShown() ? View.INVISIBLE : View.VISIBLE);
//            effectContainer.setVisibility(View.INVISIBLE);

            //以前的美颜改成道具
            mPresenter.reportCall(ReportConstant.MIRROR_CLICK_MASKBTN, status);
            beautyContainer.setVisibility(View.INVISIBLE);
            layoutHideWhenFaceUp.setVisibility(effectContainer.isShown() ? View.VISIBLE : View.INVISIBLE);
            effectContainer.setVisibility(effectContainer.isShown() ? View.INVISIBLE : View.VISIBLE);
        } else if (i == R.id.iv_face_effect) {
            changeVoice();
        } else if (i == R.id.tv_to_message_layout) {
            leaveChannel();
            mPresenter.sendCloseCallMessage(ImCallMessageBean.HangUpType.USER_CLOSE, faceVideoHelper.getChronometerSeconds(chrTime));

            if (mPresenter.imCallMessageBean.callType == ImCallMessageBean.CallType.CALL_VIDEO) {
                ARouter.getInstance().build(ActivityPath.VIDEO_RECORD)
                        .withInt(Keys.FROM, Keys.LEAVE_MESSAGE)
                        .withParcelable(Keys.CONTACT_BEAN, mPresenter.imCallMessageBean.sendToContact).navigation();
            } else {
                startActivity(AudioRecordActivity.getIntent(this, mPresenter.imCallMessageBean.sendToContact));
            }
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
                .build(FaceVideoActivity.this).show();
    }
    private static final int delay = 10 * 1000;


    private void onSpeakerMuted(View view) {
        ImageDotView iv = (ImageDotView) view;
        iv.setSelected(!iv.isSelected());
        if (iv.isSelected()) {
            videoChatManager.openSpeaker();
        } else {
            videoChatManager.closeSpeaker();
        }
    }


    private void onMuteVideo(View view) {
        if (!mPresenter.isConnected) {
            return;
        }
        mPresenter.count++;
        mPresenter.isLocalVideoMuted = !mPresenter.isLocalVideoMuted;
        onMuteLocalVideoStream(mPresenter.isLocalVideoMuted);
        layoutFaceUnity.setVisibility(mPresenter.isLocalVideoMuted ? View.INVISIBLE : View.VISIBLE);

        ImageDotView iv = (ImageDotView) view;
        iv.setSelected(!iv.isSelected());
        if (iv.isSelected()) {
            faceVideoHelper.startTimer();
        }
    }

    private void changeLayout(boolean isBigSmallLayout) {
        ivChangeLayout.setImageResource(!isBigSmallLayout ? R.drawable.video_icon_big_small : R.drawable.video_icon_left_right);
        setupLocalVideo(isBigSmallLayout ? R.id.small_video_view_container : R.id.left_video_view_container);
    }

    boolean isBigSmallLayout = true;

    boolean isRemoteVideoMuted = true;

    /**
     * 发送本地视频流 true：不发送本地视频流 false：发送本地视频流（默认）
     */
    private void onMuteLocalVideoStream(boolean muted) {
        mPresenter.isLocalVideoMuted = muted;
        videoChatManager.muteLocalVideoStream(muted);
        if (isBigSmallLayout) {
            if (remoteVideoViewContainer.getChildCount() > 0) {
                if (muted) {
                    //自己关闭视频
                    ivChangeLayout.setVisibility(View.INVISIBLE);
                    if (mSurfaceViewRemote.isShown()) {
                        //远端视频打开状态
                        setupLocalVideo(R.id.small_video_view_container);
                        faceVideoHelper.setViewInVisible(mSurfaceViewLocal, localVideoViewContainer);
                    } else {
                        //远端视频关闭状态（切换到语音聊天）
                        switchVideoAudioLayout(false);
                        ivChangeLayout.setVisibility(View.INVISIBLE);
                    }
                } else {
                    //自己打开视频
                    if (isRemoteVideoMuted) {
                        //对方没有打开视频
                        switchVideoAudioLayout(true);
                        ivChangeLayout.setVisibility(View.INVISIBLE);
                        setupLocalVideo(R.id.big_video_view_container);
                        faceVideoHelper.setViewInVisible(mSurfaceViewRemote, remoteVideoViewContainer);
                    } else {
                        //对方已经打开视频
                        ivChangeLayout.setVisibility(View.INVISIBLE);
                        ivChangeLayout.setImageResource(R.drawable.video_icon_left_right);
                        setupLocalVideo(R.id.small_video_view_container);
                    }
                }
            }
        } else {
            //左右布局
            if (muted) {
                //自己关闭视频
                ivChangeLayout.setVisibility(View.INVISIBLE);
                isBigSmallLayout = true;
                setupLocalVideo(R.id.small_video_view_container);
                faceVideoHelper.setViewInVisible(mSurfaceViewLocal, localVideoViewContainer);
            }  //自己打开视频（不会出现这种情况）

        }
    }

    private boolean mLocalViewIsBig = true;

    private void swapLocalRemoteDisplay() {
        faceVideoHelper.setViewVisible(localVideoViewContainer, remoteVideoViewContainer, mSurfaceViewLocal, mSurfaceViewRemote);
        if (isBigSmallLayout) {
            //大小布局
            if (!mLocalViewIsBig) {
                //小窗显示本地视频
                setBigViewParams(remoteVideoViewContainer);
                removeThenAddView(remoteVideoViewContainer, false);
                setSmallViewParams(localVideoViewContainer);
            } else {
                //大窗显示本地视频
                setBigViewParams(localVideoViewContainer);
                setSmallViewParams(remoteVideoViewContainer);
                removeThenAddView(remoteVideoViewContainer, true);
            }
        } else {
            //左右布局
            setTopBottomViewParams(localVideoViewContainer, false);
            setTopBottomViewParams(remoteVideoViewContainer, true);
        }
        faceVideoHelper.bringToFront(clickLayout, clBetweenCalling, effectContainer, beautyContainer, layoutFaceUnity);
    }

    private void setTopBottomViewParams(View view, boolean isTop) {
        faceVideoHelper.setTopBottomViewParams(view, isTop);
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

    /**
     * 本地音频开关点击
     *
     * @param view
     */
    public void onLocalAudioMuteClicked(View view) {
        if (!mPresenter.isConnected) {
            return;
        }
        ImageDotView iv = (ImageDotView) view;
        iv.setSelected(!iv.isSelected());
//        faceVideoHelper.changeAudioImageView(iv);
        videoChatManager.muteLocalAudioStream(iv.isSelected());
//        if (!iv.isSelected()) {
//            tvMute.setTextColor(getResources().getColor(R.color.common_white));
//        } else {
//            tvMute.setTextColor(getResources().getColor(R.color.common_color_primary_blue));
//        }
    }

    /**
     * 结束聊天点击
     */
    public void onEndCallClicked() {
        if (mActionStatus == ACTION_STATUS_WAITING || mActionStatus == ACTION_STATUS_CALLING) {
            if (!FaceVideoPresenter.isReceiver && !mPresenter.isConnected) {
//            mPresenter.reportCall(ReportConstant.CALL_BEFORE_SHURDOWN, "");
            }
            mPresenter.sendCloseCallMessage(ImCallMessageBean.HangUpType.USER_CLOSE, faceVideoHelper.getChronometerSeconds(chrTime));
            if (mPresenter.isConnected) {
//            mPresenter.reportCallStatus(ReportConstant.ONCALL_ANYONELEAVECHANNEL, ReportConstant.FIRSTLEAVEPERSONUSERID, UserManager.getInstance().getUser().userId + "");
            } else {
//            mPresenter.reportCallStatus(ReportConstant.WAITCALL_FIRSTLEAVECHANNEL, ReportConstant.FIRSTJOINPESRSONUSERID, UserManager.getInstance().getUser().userId + "");
            }
        }
        leaveChannel();
    }


    @Override
    protected void onDestroy() {

        checkIsPlayVideo();
        mPresenter.detachView();
        HeadsetReceiver.isFirst = true;
        if (mFaceVideoView != null) {
            mFaceVideoView.release();
        }
        if (mCountDownScriber != null) {
            mCountDownScriber.unsubscribe();
        }

        EventBus.getDefault().unregister(this);
        //当 activit结束时  需要把 注册的广播接收者 注销掉。
        super.unregisterReceiver(myReceiver);
        FaceVideoPresenter.fromPhone = "";
        FaceVideoPresenter.toPhone = "";
        FaceVideoPresenter.whoCallPageSwitch = "";

        if (videoChatManager.getMediaDataObserverPlugin() != null) {
            videoChatManager.removeVideoObserver();
            videoChatManager.getMediaDataObserverPlugin().removeAllBuffer();
        }
        MediaPreProcessing.releasePoint();
//        MediaHelper.getInstance().stopPlayRing();
        videoChatManager.stopPlayRing();
        RtcEngine.destroy();
        if (faceVideoHelper != null) {
            faceVideoHelper.onDestroy();
        }
        if (videoChatManager != null) {
            videoChatManager.onDestroy();
        }
        isCalling = false;
        super.onDestroy();
        EventBus.getDefault().post(new ShutVideoEvent(FaceVideoPresenter.currentChannelId, mPresenter.isConnected));
        FaceVideoPresenter.currentChannelId = "";
    }

    private void leaveChannel() {
//        MediaHelper.getInstance().stopPlayRing();
        videoChatManager.stopPlayRing();
        if (fuManager != null) {
            fuManager.onPause();
            fuManager.onDestroy();
        }
//        if (faceVideoHelper != null) {
//            if (FaceVideoPresenter.isReceiver) {
//                faceVideoHelper.sendChatEvent(CallAction.ACTION_DISCONNECTION, System.currentTimeMillis(), mPresenter.imCallMessageBean);
//            } else {
//                faceVideoHelper.sendChatEvent(CallAction.ACTION_DISCONNECTION, System.currentTimeMillis(), mPresenter.imCallMessageBean);
//            }
//        }
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
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
        faceVideoHelper.captureScreen(localBitmap, bitmap, layoutShare);
    }

    @Override
    public void run() {
        if (!isFinishing()) {
            runOnUiThread(() -> {
                if (localVideoViewContainer.isShown() || remoteVideoViewContainer.isShown()) {
                    buttonLayout.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    private boolean isHeadset;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(HeadsetEvent headsetEvent) {
        videoChatManager.closeSpeaker();
        isHeadset = headsetEvent.state == 1;
        if (isHeadset) {
            //插上耳机
            ivSound.setImageView(R.drawable.video_icon_sound_disable);
            ivSound.setEnabled(false);
//            ivSound.setColorFilter(getResources().getColor(R.color.common_bg_transparent_white_7a), PorterDuff.Mode.MULTIPLY);
//            tvSound.setTextColor(getResources().getColor(R.color.common_bg_transparent_white_7a));
        } else {
            //拔下耳机
            ivSound.setImageView(R.drawable.video_icon_sound);
            ivSound.setEnabled(true);
            ivSound.setSelected(false);
//            faceVideoHelper.changeImageView(ivSound);
//            tvSound.setTextColor(getResources().getColor(R.color.common_white));
        }
    }

    private void initTouchView(TouchRelativeLayout view) {
        view.setTouchListener(() -> {

            layoutHideWhenFaceUp.setVisibility(View.VISIBLE);
            effectContainer.setVisibility(View.GONE);
            beautyContainer.setVisibility(View.GONE);
            if (mPresenter.isConnected && chrTime.isShown() && (localVideoViewContainer.isShown() || remoteVideoViewContainer.isShown())) {
                buttonLayout.setVisibility(View.INVISIBLE);
                faceVideoHelper.stopTimer();
            }
        });
    }


    private void setViewWhilePlayingVideo() {
        if (isFinishing()) {
            return;
        }
        //buttonLayout.setVisibility(View.VISIBLE);
        faceVideoHelper.startTimer();
        chrTime.setTextColor(Color.WHITE);
        chrTime.setBase(SystemClock.elapsedRealtime());
        chrTime.start();
        rlUserInfo.setVisibility(View.INVISIBLE);
        isBigSmallLayout = true;
        //发起方
        rlCallBefore.setVisibility(View.INVISIBLE);
        faceVideoHelper.setViewVisible(ivMute, ivSound, ivMuteLocalVideoStream);
        setupLocalVideo(R.id.small_video_view_container);
        videoChatManager.muteLocalVideoStream(false);
        faceVideoHelper.setViewVisible(localVideoViewContainer);
        faceVideoHelper.setViewVisible(remoteVideoViewContainer);
    }

    /**
     * 给匹配到的用户拨打电话
     */
    private void callMatchedUser(){
        //mActionStatus = ACTION_STATUS_WAITING;
        mPresenter.imCallMessageBean = ImMessageUtily.getInstance().formatCallMessage(ImCallMessageBean.CallType.CALL_VIDEO, mMatchUserResponse.getMatchUser());
        //setViewWhileWaiting();
        faceVideoHelper.joinChannel(videoChatManager, mPresenter.imCallMessageBean.channelId);
        //notifyView();
    }


    private void startPlay(){
        setViewWhilePlayingVideo();
        mFaceVideoView.startplay();
        LogUtil.i("kevin_videoplayer", "开始播放");
        LogUtil.i("kevin_video", "width:" + mFaceVideoView.getWidth() + " height:" + mFaceVideoView.getHeight());
    }
    /**
     * 开始播放匹配到的视频
     */
    private void startPrepareMatchedVideo(){
        //进入播放视频逻辑
        mActionStatus = ACTION_STATUS_PLAYINGVIDEO;
        if (mFaceVideoView == null) {
            mFaceVideoView = new FaceVideoView(FaceVideoActivity.this);
            mFaceVideoView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            mFaceVideoView.setBackgroundColor(ContextCompat.getColor(this,R.color.black));
        }
        remoteVideoViewContainer.removeAllViews();
        remoteVideoViewContainer.addView(mFaceVideoView);

        remoteVideoViewContainer.setForegroundGravity(Gravity.CENTER);
        faceVideoHelper.setViewVisible(localVideoViewContainer);
        faceVideoHelper.setViewVisible(remoteVideoViewContainer);

        //faceVideoHelper.bringToFront(remoteVideoViewContainer,localVideoViewContainer);

        faceVideoHelper.bringToFront(remoteVideoViewContainer,localVideoViewContainer,clickLayout, clBetweenCalling, effectContainer, beautyContainer, layoutFaceUnity);

        mFaceVideoView.setVideoListener(new FaceVideoView.IVideoListener() {
            @Override
            public void onStartPlay() {
                LogUtil.i("kevin_video","width:"+mFaceVideoView.getWidth()+" height:"+mFaceVideoView.getHeight());
                mVideoPrepared = true;
                if (checkPlayMatchedVideo()) {
                    startPlay();
                }
            }

            @Override
            public void onCompletion() {
                LogUtil.i("kevin_videoplayer", "播放完成");
                leaveChannel();
                CommonToast.showShort(getResources().getString(R.string.video_remote_shut_down));
            }

            @Override
            public void onBufferingUpdate(int i) {
                LogUtil.i("kevin_videoplayer", "buffering :"+i);
            }

            @Override
            public void onStop() {
                LogUtil.i("kevin_videoplayer", "播放结束");
                leaveChannel();
                CommonToast.showShort(getResources().getString(R.string.video_remote_shut_down));
            }
        });
        mFaceVideoView.startPrepare(mMatchUserResponse.getPlay_url());
    }
    /**
     * 检测是否可以给匹配的用户打电话
     * @return
     */
    private boolean checkCallMatchedUser(){
        if (mCountdownOver && mMatchUserResponse != null){
            return true;
        }
        return false;
    }
    /**
     * 检测是否可以播放视频了
     * @return
     */
    private boolean checkPlayMatchedVideo(){
        if (mCountdownOver && mMatchUserResponse != null && mVideoPrepared){
            return true;
        }
        return false;
    }

    private void startCountdown(){
        mCountdownOver = false;
        mCountDownScriber = RxTools.countDown(5, new RxTools.IRxCountDown() {
            @Override
            public void onStart() {

            }

            @Override
            public void onRemaining(int remaining) {

            }

            @Override
            public void onComplete() {
                countDownOver();
            }

            @Override
            public void onError() {

            }
        });
    }

    private void countDownOver(){
        mCountdownOver = true;
        if (mMatchUserResponse != null) {
            if (mMatchUserResponse.isMatchUser()) {
                if (checkCallMatchedUser()) {
                    callMatchedUser();
                }
            }
            else if (mMatchUserResponse.isPlayVideo()) {
                if (checkPlayMatchedVideo()) {
                    //开始播放视频
                    startPlay();
                }
            }
        }
    }

    private void matchOver(){
        if (mMatchUserResponse == null) {
            leaveChannel();
        } else if (mMatchUserResponse.isMatchUser()) {
            if (checkCallMatchedUser()) {
                callMatchedUser();
            }
        } else if (mMatchUserResponse.isPlayVideo()) {
            startPrepareMatchedVideo();
        } else {
            leaveChannel();
        }
    }

    @Override
    public void onMatchedUser(MatchUserResponse matchResponse) {
        mMatchUserResponse = matchResponse;
        matchOver();
    }

    private void checkIsPlayVideo() {
        if (mMatchUserResponse != null) {
            if (!mMatchUserResponse.isMatchUser() && mMatchUserResponse.isPlayVideo()) {
                mPresenter.videoPlayCallback(mMatchUserResponse.getPlay_url());
            }
        }
    }
}