package com.growingio.android.sdk.ipc;

import android.support.annotation.NonNull;

/**
 * 表示一个位于VariableSharer的变量meta-data
 * Created by liangdengke on 2018/8/13.
 */
public class VariableEntity {

    private final String name;
    private final int maxSize;
    private boolean persistent = false;   // 此变量是否持久化， 在冷启动时是否需要保留
    private int index = -1;               // 下标， 对应在VariableSharer标量表中的下表位置
    private int start = -1, end = -1;     // 表示此变量位于文件位置
    private int modCount = -1;            // 当前的modCount
    private boolean isChanged = false;
    private int lenSize = 2;
    private Object obj;

    /**
     * @param name     变量的key
     * @param maxSize  变量值最大占位数
     */
    public VariableEntity(@NonNull String name, int maxSize) {
        this.name = name;
        this.maxSize = maxSize;
    }

    public static VariableEntity createIntVariable(String variableName){
        VariableEntity entity = new VariableEntity(variableName, 4);
        entity.setLenSize(0);
        return entity;
    }

    public static VariableEntity createStringVariable(String variableName, int strLen){
        VariableEntity entity = new VariableEntity(variableName, 4 * strLen);
        entity.setLenSize(2);
        return entity;
    }

    public static VariableEntity createLongVariable(String variableName){
        VariableEntity entity = new VariableEntity(variableName, 8);
        entity.setLenSize(0);
        return entity;
    }

    public String getName() {
        return name;
    }

    public void setLenSize(int lenSize) {
        this.lenSize = lenSize;
    }

    public int getLenSize() {
        return lenSize;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getStart() {
        return start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public int getEnd() {
        return end;
    }

    public int getModCount() {
        return modCount;
    }

    public void setModCount(int modCount) {
        this.modCount = modCount;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Object getObj() {
        return obj;
    }

    public void setChanged(boolean changed) {
        isChanged = changed;
    }

    public boolean isChanged() {
        return isChanged;
    }
}
