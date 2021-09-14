package com.xjbg.oss.enums;

import lombok.Getter;

/**
 * @author kesc
 * @date 2020-08-07 16:32
 */
@Getter
public enum FileType {
    DIRECTORY("directory"),
    FILE("file"),
    SYMLINK("symlink");

    private String type;

    FileType(String type) {
        this.type = type;
    }

    public static FileType getType(String type) {
        for (FileType fileType : values()) {
            if (fileType.getType().equalsIgnoreCase(type)) {
                return fileType;
            }
        }
        return FILE;
    }
}
