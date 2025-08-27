package com.growingio.android.sdk.ipc;

import android.content.Context;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.growingio.android.sdk.utils.LogUtil;
import com.growingio.android.sdk.utils.SystemUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 变量共享者， 使用文件映射内存的形式共享变量
 * <p>
 * /magic_num/process_num/process_id/process_id/process_id/..../
 * /modCount/ modCount   / modCount /...
 * /len     / xxxxxxx/
 * - magic_num: 0x13e: 表明此文件用于VariableSharer ---> short  2字节
 * - process_num: 当前进程数量                      ---> short  2字节
 * - process_id: 其中的一个进程进程号                ----> int   4字节
 * 进程号最多有10进程， 占位40字节
 * - modCount: 总体的modCount, 单个Index的modCount
 * <p>
 * <p>
 * 目前VariableSharer中有三种锁, lock data, lock meta, lock process, lock mByteBuffer
 * <p>
 * 如果涉及多个锁时， 获取锁顺序为: lock mByteBuffer --> lock data --> lock meta 其他锁不允许交叉获取
 * <p>
 * Created by liangdengke on 2018/8/13.
 */
public class VariableSharer {
    private static final String TAG = "GIO.Sharer";

    @VisibleForTesting
    static short MAGIC_NUM = 0x13e;   // short int

    private int totalModCount = -1;
    private int currentVariableIndex = 0;
    private int currentVariableOffset = 0;        // 当前变量位置的偏移量
    // 初始化完成后， 开始并发读, 所以不会有多线程问题
    @VisibleForTesting
    List<VariableEntity> entityList = new ArrayList<>();
    @VisibleForTesting
    int metaBaseAddress = 44;             // 原信息基地址(modCount的开始位置) 2 + 2 + 40
    @VisibleForTesting
    int variableBaseAddress = -1;         // 变量存储区的基地址

    private boolean fileChannelInitialize = true;            // 多进程写文件保护
    private final boolean requireAppProcesses;          // 是否运行运行 getRunningProcesses

    private FileChannel mFileChannel;
    int mPid;
    @VisibleForTesting
    ByteBuffer mByteBuffer;
    boolean isFirstInit = true;                         // 是不是冷启动

    /**
     * @param file 共享文件
     */
    public VariableSharer(File file, boolean requireAppProcesses, int pid) {
        this.requireAppProcesses = requireAppProcesses;
        mPid = pid;
        if (fileChannelInitialize) {
            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
                mFileChannel = randomAccessFile.getChannel();
            } catch (FileNotFoundException e) {
                LogUtil.e(TAG, "多进程共享初始化失败: ", e);
                this.fileChannelInitialize = false;
            }
        }
    }

    public void destroy() {
        if (mFileChannel != null) {
            try {
                mFileChannel.close();
            } catch (IOException e) {
                LogUtil.d(TAG, "close failed");
            } finally {
                mFileChannel = null;
                mByteBuffer = null;
                fileChannelInitialize = false;
            }
        }
    }

    public boolean isFirstInit() {
        return isFirstInit;
    }

    /**
     * 向变量表中添加一个新的Item, 并计算index与位置偏移量
     *
     * @return 返回改Item对应的下表
     */
    public int addVariableEntity(@NonNull VariableEntity entity) {
        entity.setIndex(currentVariableIndex++);
        entityList.add(entity);
        entity.setStart(currentVariableOffset);
        currentVariableOffset += entity.getMaxSize() + entity.getLenSize();
        entity.setEnd(currentVariableOffset);
        entity.setChanged(true);
        return entity.getIndex();
    }

    /**
     * 原信息补充完全，开始计算各基地址与偏移量
     */
    public void completeMetaData(final Context context) {
        if (fileChannelInitialize) {
            variableBaseAddress = metaBaseAddress + currentVariableIndex * 4 + 4;
            try {
                mByteBuffer = mFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, currentVariableOffset + variableBaseAddress);
                withLockProcessArea(new Runnable() {
                    @Override
                    public void run() {
                        checkOrPrepareMagic();
                        repairPid(context);
                    }
                });
            } catch (IOException e) {
                LogUtil.e(TAG, "多进程映射内存失败: ", e);
                fileChannelInitialize = false;
            }
        } else {
            isFirstInit = SystemUtil.isMainProcess(context);
        }
    }


    // 整理修复Pid列表
    void repairPid(Context context) {
        List<Integer> alivePid = new ArrayList<>();
        if (requireAppProcesses) {
            getAlivePidWithLock(alivePid, SystemUtil.getRunningProcess(context));
            if (alivePid.size() >= 10) {
                LogUtil.e(TAG, "alivePid num large than 10, failed");
                fileChannelInitialize = false;
                return;
            }
            if (alivePid.size() == 0) {
                LogUtil.d(TAG, "find first init process, and reset variable");
                isFirstInit = true;
            } else {
                isFirstInit = false;
            }
        } else {
            isFirstInit = SystemUtil.isMainProcess(context);
        }
        alivePid.add(mPid);
        mByteBuffer.position(2);
        mByteBuffer.putShort((short) alivePid.size());
        for (int pid : alivePid) {
            mByteBuffer.putInt(pid);
        }
    }

    @NonNull
    private List<Integer> getAlivePidWithLock(List<Integer> alivePid, Set<Integer> runningProcess) {
        int processNum = mByteBuffer.getShort(2);
        for (int i = 0; i < processNum; i++) {
            int pid = mByteBuffer.getInt(4 + i * 4);
            if (pid < 0) {
                continue;
            }
            if (runningProcess != null && runningProcess.contains(pid)) {
                alivePid.add(pid);
            }
        }
        return alivePid;
    }

    public List<Integer> getAlivePid(final Set<Integer> runningProcess) {
        final List<Integer> alivePid = new ArrayList<>();
        if (fileChannelInitialize) {
            withLockProcessArea(new Runnable() {
                @Override
                public void run() {
                    getAlivePidWithLock(alivePid, runningProcess);
                }
            });
        } else {
            alivePid.add(Process.myPid());
        }
        return alivePid;
    }

    @VisibleForTesting
    void checkOrPrepareMagic() {
        mByteBuffer.rewind();
        try {
            int magicNum = mByteBuffer.getShort();
            if (magicNum == 0) {
                LogUtil.d(TAG, "first init multi process file");
                prepareMagic();
            } else if (magicNum != MAGIC_NUM) {
                LogUtil.e(TAG, "文件校验失败, 多进程共享失败");
                fileChannelInitialize = false;
            }
        } catch (BufferUnderflowException e) {
            prepareMagic();
        }
    }

    public boolean isFileChannelInitialize() {
        return fileChannelInitialize;
    }

    private void prepareMagic() {
        mByteBuffer.rewind();
        mByteBuffer.putShort(MAGIC_NUM);
    }

    public long getLongByIndex(int index) {
        checkEntityChanged();
        final VariableEntity entity = entityList.get(index);
        synchronized (this) {
            if (fileChannelInitialize && entity.isChanged()) {
                withLockData(new Runnable() {
                    @Override
                    public void run() {
                        long result = mByteBuffer.getLong(variableBaseAddress + entity.getStart() + entity.getLenSize());
                        entity.setObj(result);
                        entity.setChanged(false);
                    }
                }, entity);
            }
            return entity.getObj() == null ? 0 : (long) entity.getObj();
        }
    }

    public void putLongByIndex(int index, final long value) {
        final VariableEntity entity = entityList.get(index);
        synchronized (this) {
            entity.setObj(value);
            if (fileChannelInitialize) {
                withLockData(new Runnable() {
                    @Override
                    public void run() {
                        mByteBuffer.putLong(variableBaseAddress + entity.getStart() + entity.getLenSize(), value);
                        updateMetaWithLock(entity);
                    }
                }, entity);
            }
        }
    }

    public boolean compareAndSetIntByIndex(int index, final int oldValue, final int newValue) {
        final VariableEntity entity = entityList.get(index);
        final AtomicBoolean result = new AtomicBoolean();
        synchronized (this) {
            if (fileChannelInitialize) {
                withLockData(new Runnable() {
                    @Override
                    public void run() {
                        int value = mByteBuffer.getInt(variableBaseAddress + entity.getStart() + entity.getLenSize());
                        if (value == oldValue) {
                            result.set(true);
                            mByteBuffer.putInt(variableBaseAddress + entity.getStart() + entity.getLenSize(), newValue);
                            updateMetaWithLock(entity);
                        } else {
                            result.set(false);
                        }
                    }
                }, entity);
            } else {
                if (Integer.valueOf(oldValue).equals(entity.getObj())) {
                    entity.setObj(newValue);
                    return true;
                }
            }
        }
        return result.get();
    }

    /**
     * 根据变量的index获取属性值int值
     */
    public int getIntByIndex(int index) {
        checkEntityChanged();
        final VariableEntity entity = entityList.get(index);
        synchronized (this) {
            if (fileChannelInitialize && entity.isChanged()) {
                withLockData(new Runnable() {
                    @Override
                    public void run() {
                        int result = mByteBuffer.getInt(variableBaseAddress + entity.getStart() + entity.getLenSize());
                        entity.setObj(result);
                        entity.setChanged(false);
                    }
                }, entity);
            }
            if (entity.getObj() == null) return 0;
            return entity.getObj() == null ? 0 : (int) entity.getObj();
        }
    }

    public void putIntByIndex(int index, final int value) {
        final VariableEntity entity = entityList.get(index);
        synchronized (this) {
            entity.setObj(value);

            if (fileChannelInitialize) {
                withLockData(new Runnable() {
                    @Override
                    public void run() {
                        mByteBuffer.putInt(variableBaseAddress + entity.getStart() + entity.getLenSize(), value);
                        updateMetaWithLock(entity);
                    }
                }, entity);
            }
        }
    }

    public void putStringByIndex(int index, @Nullable String value) {
        putDataByIndex(index, value == null ? null : value.getBytes());
    }

    public String getStringByIndex(int index) {
        byte[] data = getDataByIndex(index);
        if (data == null || data.length == 0)
            return null;
        return new String(data);
    }

    public void putDataByIndex(int index, @Nullable final byte[] bytes) {
        final VariableEntity entity = entityList.get(index);
        synchronized (this) {
            entity.setObj(bytes);

            if (fileChannelInitialize) {
                withLockData(new Runnable() {
                    @Override
                    public void run() {
                        int len = bytes == null ? 0 : bytes.length;
                        if (entity.getLenSize() == 2) {
                            mByteBuffer.putShort((short) len);
                        } else if (entity.getLenSize() == 4) {
                            mByteBuffer.putInt(len);
                        } else {
                            throw new IllegalStateException("String type len must be 2 or 4");
                        }
                        if (bytes != null) {
                            mByteBuffer.put(bytes);
                        }
                        updateMetaWithLock(entity);

                    }
                }, entity);
            }
        }
    }

    public byte[] getDataByIndex(final int index) {
        checkEntityChanged();
        final VariableEntity entity = entityList.get(index);
        synchronized (this) {
            if (fileChannelInitialize && entity.isChanged()) {
                withLockData(new Runnable() {
                    @Override
                    public void run() {
                        short len = mByteBuffer.getShort();
                        byte[] result;
                        if (len == 0) {
                            result = null;
                        } else {
                            result = new byte[len];
                            mByteBuffer.get(result);
                        }
                        entity.setObj(result);
                        entity.setChanged(false);
                    }
                }, entity);
            }
            return (byte[]) entity.getObj();
        }
    }

    private void updateMetaWithLock(final VariableEntity entity) {
        withLockMeta(new Runnable() {
            @Override
            public void run() {
                int modCount = mByteBuffer.getInt(metaBaseAddress);
                if (modCount != totalModCount) {
                    //  other process has update mod count
                    checkEntityChanged();
                }
                int entityModCountIndex = metaBaseAddress + (entity.getIndex() + 1) * 4;
                int indexModCount = mByteBuffer.getInt(entityModCountIndex);
                mByteBuffer.putInt(metaBaseAddress, ++modCount);
                totalModCount = modCount;

                mByteBuffer.putInt(entityModCountIndex, ++indexModCount);
                entity.setModCount(indexModCount);
            }
        });
    }

    private void withLockData(Runnable runnable, VariableEntity entity) {
        int position = variableBaseAddress + entity.getStart();
        FileLock lock = null;
        try {
            mByteBuffer.position(position);
            lock = mFileChannel.lock(position, entity.getMaxSize(), false);
            runnable.run();
        } catch (Exception e) {
            LogUtil.e(TAG, "数据区加锁失败: ", e);
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /* lock meta时已经全部lock data完成， 不需要额外加锁synchronized */
    private void withLockMeta(Runnable runnable) {
        FileLock lock = null;
        try {
            lock = mFileChannel.lock(metaBaseAddress, variableBaseAddress - metaBaseAddress, false);
            runnable.run();
        } catch (Exception e) {
            LogUtil.e(TAG, "文件原信息失败", e);
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

    private void withLockProcessArea(Runnable runnable) {
        FileLock lock = null;
        try {
            lock = mFileChannel.lock(0, metaBaseAddress, false);
            runnable.run();
        } catch (Exception e) {
            LogUtil.e(TAG, "文件进程区加锁失败", e);
        } finally {
            if (lock != null) {
                try {
                    lock.release();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    // 作为一个纯读操作， 并不需要文件锁
    @VisibleForTesting
    void checkEntityChanged() {
        if (!fileChannelInitialize) return;
        try {
            synchronized (this) {
                mByteBuffer.position(metaBaseAddress);
                int modCount = mByteBuffer.getInt();
                if (totalModCount == modCount)
                    // 没有任何改动
                    return;

                for (VariableEntity entity : entityList) {
                    int mod = mByteBuffer.getInt();
                    if (mod != entity.getModCount()) {
                        entity.setModCount(mod);
                        entity.setChanged(true);
                    }
                }
                totalModCount = modCount;
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "check changed failed: ", e);
        }
    }

    public void dumpModCountInfo() {
        StringBuilder builder = new StringBuilder();
        builder.append("dumpModCountInfo: \n");
        synchronized (this) {
            mByteBuffer.position(metaBaseAddress);
            int modCount = mByteBuffer.getInt();
            builder.append("modCount=").append(modCount).append("\n");
            for (VariableEntity entity : entityList) {
                builder.append(entity.getName()).append("'s modCount=").append(mByteBuffer.getInt()).append("\n");
            }
        }
        builder.append(")");
        LogUtil.d(TAG, builder.toString());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("VariableSharer(");
        builder.append("totalModCount=").append(totalModCount).append(", \n");
        for (VariableEntity entity : entityList) {
            builder.append(entity.getName()).append("=").append(entity.getObj()).append("\n");
        }
        builder.append(")");
        return builder.toString();
    }
}
