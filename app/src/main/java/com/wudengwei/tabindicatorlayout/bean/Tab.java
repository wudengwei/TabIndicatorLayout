package com.wudengwei.tabindicatorlayout.bean;

/**
 * Copyright (C)
 * FileName: Tab
 * Author: wudengwei
 * Date: 2019/7/10 21:16
 * Description: ${DESCRIPTION}
 */
public class Tab {
    private String title;
    private String num;

    public Tab(String title,String num) {
        this.title = title;
        this.num = num;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }
}
