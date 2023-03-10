package com.xunlei.downloadlib;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;

import com.xunlei.downloadlib.parameter.BtIndexSet;
import com.xunlei.downloadlib.parameter.BtSubTaskDetail;
import com.xunlei.downloadlib.parameter.BtTaskParam;
import com.xunlei.downloadlib.parameter.EmuleTaskParam;
import com.xunlei.downloadlib.parameter.GetFileName;
import com.xunlei.downloadlib.parameter.GetTaskId;
import com.xunlei.downloadlib.parameter.InitParam;
import com.xunlei.downloadlib.parameter.MagnetTaskParam;
import com.xunlei.downloadlib.parameter.P2spTaskParam;
import com.xunlei.downloadlib.parameter.TorrentFileInfo;
import com.xunlei.downloadlib.parameter.TorrentInfo;
import com.xunlei.downloadlib.parameter.XLTaskInfo;
import com.xunlei.downloadlib.parameter.XLTaskLocalUrl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oceanzhang on 2017/7/27.
 */

public class XLTaskHelper {
    private static final String TAG = "XLTaskHelper";

    public static void init(Context context) {
        XLDownloadManager instance = XLDownloadManager.getInstance();
        InitParam initParam = new InitParam();
        initParam.mAppKey = "bpIzNjAxNTsxNTA0MDk0ODg4LjQyODAwMA&&OxNw==^a2cec7^10e7f1756b15519e20ffb6cf0fbf671f";
        initParam.mAppVersion = "5.45.2.5080";
        initParam.mStatSavePath = context.getFilesDir().getPath();
        initParam.mStatCfgSavePath = context.getFilesDir().getPath();
        initParam.mPermissionLevel = 2;
        instance.init(context, initParam);
        instance.setOSVersion(Build.VERSION.INCREMENTAL);
        instance.setSpeedLimit(-1, -1);
        XLDownloadManager.getInstance().setUserId("");
    }


    private String innerSDCardPath;
    private Context context;
    private AtomicInteger seq = new AtomicInteger(0);

    private XLTaskHelper(final Context context) {
        this.context = context;
        innerSDCardPath = Environment.getExternalStorageDirectory().getPath();
    }


    private static volatile XLTaskHelper instance = null;

    public static XLTaskHelper instance(Context context) {
        if (instance == null) {
            synchronized (XLTaskHelper.class) {
                if (instance == null) {
                    instance = new XLTaskHelper(context.getApplicationContext());
                }
            }

        }
        return instance;
    }

    /**
     * ????????????????????? ???????????????????????????????????????????????????????????????
     * mDownloadSize:???????????????  mDownloadSpeed:???????????? mFileSize:??????????????? mTaskStatus:???????????????0?????????1????????? 2???????????? 3?????? mAdditionalResDCDNSpeed DCDN?????? ??????
     * @param taskId
     * @return
     */
    public synchronized XLTaskInfo getTaskInfo(long taskId) {
        XLTaskInfo taskInfo = new XLTaskInfo();
        XLDownloadManager.getInstance().getTaskInfo(taskId,1,taskInfo);
        return taskInfo;
    }

    /**
     * ???????????????????????? ??????thunder:// ftp:// ed2k:// http:// https:// ??????
     * @param url
     * @param savePath ????????????????????????
     * @param fileName ??????????????? ???????????? getFileName(url) ?????????,???????????????getFileName(url)??????
     * @return
     */
    public synchronized long addThunderTask(String url,String savePath,String fileName) throws Exception {
        if (url.startsWith("thunder://")) url = XLDownloadManager.getInstance().parserThunderUrl(url);
        final GetTaskId getTaskId = new GetTaskId();
        GetFileName getFileName = new GetFileName();
        XLDownloadManager.getInstance().getFileNameFromUrl(url, getFileName);
        if (url.startsWith("ftp://") || url.startsWith("http")) {
            P2spTaskParam taskParam = new P2spTaskParam();
            taskParam.setCreateMode(1);
            taskParam.setFileName(getFileName.getFileName());
            taskParam.setFilePath(savePath);
            taskParam.setUrl(url);
            taskParam.setSeqId(seq.incrementAndGet());
            taskParam.setCookie("");
            taskParam.setRefUrl("");
            taskParam.setUser("");
            taskParam.setPass("");
            XLDownloadManager.getInstance().createP2spTask(taskParam, getTaskId);
        } else if (url.startsWith("ed2k://")) {
            EmuleTaskParam taskParam = new EmuleTaskParam();
            taskParam.setFilePath(savePath);
            taskParam.setFileName(getFileName.getFileName());
            taskParam.setUrl(url);
            taskParam.setSeqId(seq.incrementAndGet());
            taskParam.setCreateMode(1);
            XLDownloadManager.getInstance().createEmuleTask(taskParam, getTaskId);
        } else {
            throw new Exception("url illegal.");
        }

        XLDownloadManager.getInstance().setDownloadTaskOrigin(getTaskId.getTaskId(), "out_app/out_app_paste");
        XLDownloadManager.getInstance().setOriginUserAgent(getTaskId.getTaskId(), "AndroidDownloadManager/4.4.4 (Linux; U; Android 4.4.4; Build/KTU84Q)");
        XLDownloadManager.getInstance().startTask(getTaskId.getTaskId(), false);
        XLDownloadManager.getInstance().setTaskLxState(getTaskId.getTaskId(), 0, 1);
        XLDownloadManager.getInstance().startDcdn(getTaskId.getTaskId(), 0, "", "", "");

        return getTaskId.getTaskId();
    }
    /**
     * ?????????????????????
     * @param url ???????????? magnet:? ??????
     * @param savePath
     * @param fileName
     * @return
     * @throws Exception
     */
    public synchronized long addMagentTask(final String url,final String savePath,String fileName) throws Exception {
        if (url.startsWith("magnet:?")) {
            if(TextUtils.isEmpty(fileName)) {
                final GetFileName getFileName = new GetFileName();
                XLDownloadManager.getInstance().getFileNameFromUrl(url, getFileName);
                fileName = getFileName.getFileName();
            }
            MagnetTaskParam magnetTaskParam = new MagnetTaskParam();
            magnetTaskParam.setFileName(fileName);
            magnetTaskParam.setFilePath(savePath);
            magnetTaskParam.setUrl(url);
            final GetTaskId getTaskId = new GetTaskId();
            XLDownloadManager.getInstance().createBtMagnetTask(magnetTaskParam, getTaskId);

            XLDownloadManager.getInstance().setTaskLxState(getTaskId.getTaskId(), 0, 1);
            XLDownloadManager.getInstance().startDcdn(getTaskId.getTaskId(), 0, "", "", "");
            XLDownloadManager.getInstance().startTask(getTaskId.getTaskId(), false);
            return getTaskId.getTaskId();
        } else {
            throw new Exception("url illegal.");
        }
    }
    /**
     * ??????????????????
     * @param torrentPath
     * @return
     */
    public synchronized TorrentInfo getTorrentInfo(String torrentPath) {
        TorrentInfo torrentInfo = new TorrentInfo();
        XLDownloadManager.getInstance().getTorrentInfo(torrentPath,torrentInfo);
        return torrentInfo;
    }

    /**
     * ????????????????????????,?????????????????????????????????addMagentTask?????????????????????
     * @param torrentPath ????????????
     * @param savePath ????????????
     * @param indexs ???????????????????????????
     * @return
     * @throws Exception
     */
    public synchronized long addTorrentTask(String torrentPath,String savePath,int []indexs) throws Exception {
        TorrentInfo torrentInfo = new TorrentInfo();
        XLDownloadManager.getInstance().getTorrentInfo(torrentPath,torrentInfo);
        TorrentFileInfo[] fileInfos = torrentInfo.mSubFileInfo;
        BtTaskParam taskParam = new BtTaskParam();
        taskParam.setCreateMode(1);
        taskParam.setFilePath(savePath);
        taskParam.setMaxConcurrent(3);
        taskParam.setSeqId(seq.incrementAndGet());
        taskParam.setTorrentPath(torrentPath);
        GetTaskId getTaskId = new GetTaskId();
        XLDownloadManager.getInstance().createBtTask(taskParam,getTaskId);
        if(fileInfos.length > 1 && indexs != null && indexs.length > 0) {
            BtIndexSet btIndexSet = new BtIndexSet(indexs.length);
            int i = 0;
            for(int index : indexs) {
                btIndexSet.mIndexSet[i++] = index;
            }
            XLDownloadManager.getInstance().selectBtSubTask(getTaskId.getTaskId(),btIndexSet);
        }
        XLDownloadManager.getInstance().setTaskLxState(getTaskId.getTaskId(), 0, 1);
//        XLDownloadManager.getInstance().startDcdn(getTaskId.getTaskId(), currentFileInfo.mRealIndex, "", "", "");
        XLDownloadManager.getInstance().startTask(getTaskId.getTaskId(), false);
//        XLDownloadManager.getInstance().setBtPriorSubTask(getTaskId.getTaskId(),currentFileInfo.mRealIndex);
//        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
//        XLDownloadManager.getInstance().getLocalUrl(savePath+"/" +(TextUtils.isEmpty(currentFileInfo.mSubPath) ? "" : currentFileInfo.mSubPath+"/")+ currentFileInfo.mFileName,localUrl);
//        currentFileInfo.playUrl = localUrl.mStrUrl;
//        currentFileInfo.hash = torrentInfo.mInfoHash;
//        return currentFileInfo;
        return getTaskId.getTaskId();
    }

    /**
     * ???????????????????????????proxy url,????????????????????????????????????????????????
     * @param filePath
     * @return
     */
    public synchronized String getLoclUrl(String filePath) {
        XLTaskLocalUrl localUrl = new XLTaskLocalUrl();
        XLDownloadManager.getInstance().getLocalUrl(filePath,localUrl);
        return localUrl.mStrUrl;
    }

    /**
     * ???????????? ????????????
     * @param taskId
     */
    public synchronized void stopTask(long taskId) {
        XLDownloadManager.getInstance().stopTask(taskId);
        XLDownloadManager.getInstance().releaseTask(taskId);
    }

    /**
     * ??????????????????????????????????????????
     * @param taskId
     * @param savePath
     */
    public synchronized void deleteTask(long taskId,final String savePath) {
        stopTask(taskId);
        new Handler(Daemon.looper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    new LinuxFileCommand(Runtime.getRuntime()).deleteDirectory(savePath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * ???????????????????????????
     * @param url
     * @return
     */
    public synchronized String getFileName(String url) {
        if (url.startsWith("thunder://")) url = XLDownloadManager.getInstance().parserThunderUrl(url);
        GetFileName getFileName = new GetFileName();
        XLDownloadManager.getInstance().getFileNameFromUrl(url, getFileName);
        return getFileName.getFileName();
    }

    /**
     * ????????????????????????????????????
     * @param taskId
     * @param fileIndex
     * @return
     */
    public synchronized BtSubTaskDetail getBtSubTaskInfo(long taskId, int fileIndex) {
        BtSubTaskDetail subTaskDetail = new BtSubTaskDetail();
        XLDownloadManager.getInstance().getBtSubTaskInfo(taskId,fileIndex,subTaskDetail);
        return subTaskDetail;
    }

    /**
     * ??????dcdn??????
     * @param taskId
     * @param btFileIndex
     */
    public synchronized void startDcdn(long taskId,int btFileIndex) {
        XLDownloadManager.getInstance().startDcdn(taskId, btFileIndex, "", "", "");
    }


    /**
     * ??????dcdn??????
     * @param taskId
     * @param btFileIndex
     */
    public synchronized void stopDcdn(long taskId,int btFileIndex) {
        XLDownloadManager.getInstance().stopDcdn(taskId,btFileIndex);
    }


}
