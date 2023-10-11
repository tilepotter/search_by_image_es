package com.wyk.image.common;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;

import java.io.Serializable;

/**
 * @author: wyk
 * @date 2020/11/12 17:13
 * @Description
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 2171719812197784996L;

    /**
     * 调用成功/失败标识
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 错误码
     */
    private Integer errCode;

    /**
     * 实体对象
     */
    private T resultBody;

    public Result() {
        super();
    }

    public Result(Boolean success, T resultBody, String errorMsg, Integer errCode) {
        this.success = success;
        this.resultBody = resultBody;
        this.errorMsg = errorMsg;
        this.errCode = errCode;
    }

    public Result(Boolean success, T resultBody) {
        this.success = success;
        this.resultBody = resultBody;
    }

    public Result(Boolean success, Integer errCode, T resultBody) {
        this.success = success;
        this.errCode = errCode;
        this.resultBody = resultBody;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Integer getErrCode() {
        return errCode;
    }

    public void setErrCode(Integer errCode) {
        this.errCode = errCode;
    }

    public T getResultBody() {
        return resultBody;
    }

    public void setResultBody(T resultBody) {
        this.resultBody = resultBody;
    }

    @Override
    public String toString() {
        return "{" + "\"" + "success" + "\"" + ":" + success + "," + "\"" +
                "errorMsg" + "\"" + ":" + errorMsg + "," + "\"" +
                "errCode" + "\"" + ":" + errCode + "," + "\"" +
                "resultBody" + "\"" + ":" + JSONUtil.toJsonStr(resultBody, new JSONConfig().setIgnoreNullValue(false)) +
                '}';
    }
}
