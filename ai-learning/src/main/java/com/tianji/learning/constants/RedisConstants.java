package com.tianji.learning.constants;

public interface RedisConstants {
    /**
     * 用户签到前缀
     */
    String SIGN_RECORD_KEY_PREFIX = "sign:uid:";
    /**
     * 排行榜签到前缀
     */
    String POINTS_BOARD_KEY_PREFIX = "board:";
    /**
     * 积分榜数据表前缀
     */
    String POINTS_BOARD_TABLE_PREFIX = "points_board_";
    /**
     * 积分明细表前缀
     */
    String POINTS_BOARD_RECORD_TABLE_PREFIX = "points_record_";
}
