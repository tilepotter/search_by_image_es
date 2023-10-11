package com.wyk.image.common;

/**
 * @author: wyk
 * @date 2020/11/12 18:02
 * @Description
 */
public class ResultUtils {

    public static <T> Result<T> success(T resultBody) {
        if (resultBody instanceof Boolean) {
            Boolean result = (Boolean) resultBody;
            return new Result<>(result, 200, resultBody);
        } else {
            return new Result<>(true, 200, resultBody);
        }
    }

    public static <T> Result<T> error(Integer code, String errorMsg) {
        return new Result<>(false, null, errorMsg, code);
    }

}
