package com.studyforge.help.service.impl;

import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.help.dto.CreateHelpAnswerRequest;
import com.studyforge.help.dto.CreateHelpRequest;
import com.studyforge.help.entity.HelpAnswer;
import com.studyforge.help.entity.HelpRequest;
import com.studyforge.help.mapper.HelpAnswerMapper;
import com.studyforge.help.mapper.HelpRequestMapper;
import com.studyforge.help.service.HelpRequestService;
import com.studyforge.help.vo.HelpAnswerVO;
import com.studyforge.help.vo.HelpRequestVO;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HelpRequestServiceImpl implements HelpRequestService {
    private final HelpRequestMapper helpRequestMapper;
    private final HelpAnswerMapper helpAnswerMapper;

    public HelpRequestServiceImpl(HelpRequestMapper helpRequestMapper, HelpAnswerMapper helpAnswerMapper) {
        this.helpRequestMapper = helpRequestMapper;
        this.helpAnswerMapper = helpAnswerMapper;
    }

    @Override
    public Long create(Long userId, String title, String description) {
        return create(userId, new CreateHelpRequest(title, description, null, 0));
    }

    @Override
    @Transactional
    public Long create(Long userId, CreateHelpRequest request) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (request == null || isBlank(request.title()) || isBlank(request.description())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "title and description are required");
        }
        HelpRequest help = new HelpRequest();
        help.setUserId(userId);
        help.setTitle(request.title().trim());
        help.setDescription(request.description().trim());
        help.setCategoryId(request.categoryId());
        help.setStatus("OPEN");
        help.setRewardPoints(request.rewardPoints() == null ? 0 : Math.max(0, request.rewardPoints()));
        helpRequestMapper.insert(help);
        return help.getHelpId();
    }

    @Override
    public List<HelpRequestVO> list(String status, int limit) {
        int normalizedLimit = limit <= 0 ? 30 : Math.min(limit, 80);
        return helpRequestMapper.selectList(isBlank(status) || "ALL".equals(status) ? null : status, normalizedLimit)
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public HelpRequestVO detail(Long helpId) {
        return toVO(requireHelp(helpId));
    }

    @Override
    @Transactional
    public HelpAnswerVO answer(Long helpId, Long userId, CreateHelpAnswerRequest request) {
        requireHelp(helpId);
        if (request == null || isBlank(request.content())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "answer content is required");
        }
        HelpAnswer answer = new HelpAnswer();
        answer.setHelpId(helpId);
        answer.setUserId(userId);
        answer.setContent(request.content().trim());
        answer.setAccepted(0);
        helpAnswerMapper.insert(answer);
        return toVO(answer);
    }

    @Override
    public List<HelpAnswerVO> answers(Long helpId) {
        requireHelp(helpId);
        return helpAnswerMapper.selectByHelpId(helpId)
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public void accept(Long helpId, Long answerId, Long userId) {
        HelpRequest help = requireHelp(helpId);
        if (!help.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "only the question owner can accept an answer");
        }
        HelpAnswer answer = helpAnswerMapper.selectById(answerId);
        if (answer == null || !helpId.equals(answer.getHelpId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "answer not found");
        }
        helpAnswerMapper.clearAccepted(helpId);
        helpAnswerMapper.accept(answerId);
        helpRequestMapper.updateStatus(helpId, "RESOLVED");
    }

    private HelpRequest requireHelp(Long helpId) {
        HelpRequest help = helpRequestMapper.selectById(helpId);
        if (help == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "help request not found");
        }
        return help;
    }

    private HelpRequestVO toVO(HelpRequest help) {
        return new HelpRequestVO(
                help.getHelpId(),
                help.getUserId(),
                help.getTitle(),
                help.getDescription(),
                help.getCategoryId(),
                help.getStatus(),
                help.getRewardPoints(),
                help.getCreatedTime()
        );
    }

    private HelpAnswerVO toVO(HelpAnswer answer) {
        return new HelpAnswerVO(
                answer.getAnswerId(),
                answer.getHelpId(),
                answer.getUserId(),
                answer.getContent(),
                answer.getAccepted(),
                answer.getCreatedTime()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
