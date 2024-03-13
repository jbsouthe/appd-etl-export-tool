package com.cisco.josouthe.config.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class LoggingConfig {
    @XmlElement(name = "Level")
    private String level;

    @XmlElement(name = "FileName")
    private String fileName;

    @XmlElement(name = "FilePattern")
    private String filePattern;

    @XmlElement(name = "FileCronPolicy")
    private String fileCronPolicy;

    @XmlElement(name = "FileSizePolicy")
    private String fileSizePolicy;

    @XmlElement(name = "Pattern")
    private String pattern;

    @XmlElement(name = "Logger")
    private List<OptionalLoggerConfig> loggerList = new ArrayList<>();

    public String getLevel () {
        return level;
    }

    public void setLevel (String level) {
        this.level = level;
    }

    public String getFileName () {
        return fileName;
    }

    public void setFileName (String fileName) {
        this.fileName = fileName;
    }

    public String getFilePattern () {
        return filePattern;
    }

    public void setFilePattern (String filePattern) {
        this.filePattern = filePattern;
    }

    public String getFileCronPolicy () {
        return fileCronPolicy;
    }

    public void setFileCronPolicy (String fileCronPolicy) {
        this.fileCronPolicy = fileCronPolicy;
    }

    public String getFileSizePolicy () {
        return fileSizePolicy;
    }

    public void setFileSizePolicy (String fileSizePolicy) {
        this.fileSizePolicy = fileSizePolicy;
    }

    public String getPattern () {
        return pattern;
    }

    public void setPattern (String pattern) {
        this.pattern = pattern;
    }

    public List<OptionalLoggerConfig> getLoggerList () {
        return loggerList;
    }

    public void setLoggerList (List<OptionalLoggerConfig> loggerList) {
        this.loggerList = loggerList;
    }
}
