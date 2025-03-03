package com.mszlu.rpc.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LthResponse<T> implements Serializable {
    private String requestId;
    /**
     * response code
     */
    private Integer code;
    /**
     * response message
     */
    private String message;
    /**
     * response body
     */
    private T data;

    public static <T> LthResponse<T> success(T data, String requestId) {
        LthResponse<T> response = new LthResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setRequestId(requestId);
        if (null != data) {
            response.setData(data);
        }
        return response;
    }

    public static <T> LthResponse<T> fail(String message) {
        LthResponse<T> response = new LthResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

}
