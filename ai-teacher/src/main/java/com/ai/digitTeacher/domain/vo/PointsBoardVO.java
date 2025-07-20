package com.ai.digitTeacher.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class PointsBoardVO {
    private Integer rank;
    private Integer points;
    private List<PointsBoardItemVO> boardList;
}
