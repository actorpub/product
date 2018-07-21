package com.music.concertoplayer.utils;


import com.music.concertoplayer.entity.FolderInfo;

import java.util.Comparator;

public class FolderComparator implements Comparator<FolderInfo> {

    @Override
    public int compare(FolderInfo a1, FolderInfo a2) {
        String py1 = a1.getFolder_sort();
        String py2 = a2.getFolder_sort();
        // 判断是否为空""  
        if (isEmpty(py1) && isEmpty(py2))
            return 0;
        if (isEmpty(py1))
            return -1;
        if (isEmpty(py2))
            return 1;
        if (isNumeric(py1) && !isNumeric(py2)) {
            return 1;
        }
        if (!isNumeric(py1) && isNumeric(py2)) {
            return -1;
        }
            return py1.compareTo(py2);

    }

    private boolean isEmpty(String str) {
        return "".equals(str.trim());
    }

    private boolean isNumeric(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;

    }
}
