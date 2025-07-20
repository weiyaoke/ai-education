package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.QuestionAdminPageQuery;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionAdminVO;
import com.tianji.learning.domain.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author ke
 * @since 2025-03-21
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    void saveInteractionQuestion(QuestionFormDTO questionFormDTO);

    PageDTO<QuestionVO> interactionQuestionPage(QuestionPageQuery questionPageQuery);

    QuestionVO selectInteractionQuestionById(String id);

    PageDTO<QuestionAdminVO> interactionQuestionAdminPage(QuestionAdminPageQuery questionAdminPageQuery);

    QuestionAdminVO selectInteractionQuestionAdminById(Long questionAdminId);

    void ishidden(Long id, Boolean hidden);

}
