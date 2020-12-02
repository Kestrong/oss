package com.xjbg.oss.enums;

import lombok.Getter;

/**
 * @author kesc
 * @date 2020-08-07 16:32
 */
@Getter
public enum FileType {
    DIRECTORY("directory"),
    FILE("file");

    private String type;

    FileType(String type) {
        this.type = type;
    }
}
