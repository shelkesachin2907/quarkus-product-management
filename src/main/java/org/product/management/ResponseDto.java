package org.product.management;

public class ResponseDto<T> {

    private Integer code;
    private String message;
    private T data;

    public ResponseDto(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ResponseDto<T> createResponse(String status, int code, T data) {
        return new ResponseDto<T>(code, status, data);
    }
}
