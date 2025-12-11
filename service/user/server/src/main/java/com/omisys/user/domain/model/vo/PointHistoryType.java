package com.omisys.user.domain.model.vo;

import com.omisys.user.exception.UserErrorCode;
import com.omisys.user.exception.UserException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum PointHistoryType {

    EARN("적립"),
    USE("사용"),
    REFUND("환불");

    private final String type;

    public String getType() {
        return this.type;
    }

    public static PointHistoryType from(String type) {
        return Arrays.stream(PointHistoryType.values())
                .filter(t -> t.getType().equals(type))
                .findFirst()
                .orElseThrow(() -> new UserException(UserErrorCode.INVALID_POINT_HISTORY_TYPE));
    }
}
