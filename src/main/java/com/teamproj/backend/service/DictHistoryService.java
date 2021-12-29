package com.teamproj.backend.service;

import com.teamproj.backend.Repository.dict.DictHistoryRepository;
import com.teamproj.backend.Repository.dict.DictRepository;
import com.teamproj.backend.dto.dictHistory.DictHistoryDetailResponseDto;
import com.teamproj.backend.dto.dictHistory.DictHistoryRecentResponseDto;
import com.teamproj.backend.dto.dictHistory.DictHistoryResponseDto;
import com.teamproj.backend.dto.dictHistory.DictRevertResponseDto;
import com.teamproj.backend.model.dict.Dict;
import com.teamproj.backend.model.dict.DictHistory;
import com.teamproj.backend.security.UserDetailsImpl;
import com.teamproj.backend.util.ValidChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.teamproj.backend.exception.ExceptionMessages.NOT_EXIST_DICT;
import static com.teamproj.backend.exception.ExceptionMessages.NOT_EXIST_DICT_HISTORY;

@Service
@RequiredArgsConstructor
public class DictHistoryService {
    private final DictRepository dictRepository;
    private final DictHistoryRepository dictHistoryRepository;

    // 용어 사전 수정내역 목록
    public DictHistoryResponseDto getDictHistory(Long dictId) {
        Dict dict = getSafeDict(dictId);
        List<DictHistory> dictHistoryList = getSafeDictHistoryList(dict);

        return DictHistoryResponseDto.builder()
                .dictId(dict.getDictId())
                .title(dict.getDictName())
                .firstWriter(dict.getFirstAuthor().getNickname())
                .history(dictHistoryListToDictHistoryRecentResponseDtoList(dictHistoryList))
                .build();
    }

    // 용어 사전 수정내역 상세
    public DictHistoryDetailResponseDto getDictHistoryDetail(Long historyId) {
        DictHistory dictHistory = getSafeDictHistory(historyId);

        return DictHistoryDetailResponseDto.builder()
                .dictId(dictHistory.getDict().getDictId())
                .revertFrom(dictHistory.getRevertFrom() == null ? null : dictHistory.getRevertFrom().getDictHistoryId())
                .title(dictHistory.getDict().getDictName())
                .firstWriter(dictHistory.getDict().getFirstAuthor().getNickname())
                .modifier(dictHistory.getUser().getNickname())
                .summary(dictHistory.getPrevSummary())
                .content(dictHistory.getPrevContent())
                .createdAt(dictHistory.getCreatedAt())
                .build();
    }

    // 용어사전 롤백
    @Transactional
    public DictRevertResponseDto revertDict(Long historyId, UserDetailsImpl userDetails) {
        /*
            롤백 기능 수행 절차
            1. 기존의 데이터 recentDict를 DictHistory로 선언해 저장
            2. 롤백하고자 하는 데이터를 dict로 덮어쓰기
            3. good!

            개선사항
            1. 사전을 생성하는 순간부터 최초생성 역사를 생성시켜 관리하도록 하면, 수정시 생긴 최근 역사에 reverFrom을 부여할 시
               롤백 시 어떤 데이터에서 가져온 건지 알 수 있음
            2.
         */
        ValidChecker.loginCheck(userDetails);

        DictHistory dictHistory = getSafeDictHistory(historyId);
        Dict dict = getSafeDict(dictHistory.getDict().getDictId());

        DictHistory recentDict = DictHistory.builder()
                .prevSummary(dict.getSummary())
                .prevContent(dict.getContent())
                .user(dict.getRecentModifier())
                .dict(dict)
                .revertFrom(dictHistory)
                .build();
        dictHistoryRepository.save(recentDict);

        dict.setSummary(dictHistory.getPrevSummary());
        dict.setContent(dictHistory.getPrevContent());
        dict.setRecentModifier(dictHistory.getUser());

        return DictRevertResponseDto.builder()
                .result("롤백 성공")
                .build();
    }


    // region 보조 기능
    // Get SafeEntity
    // Dict
    private Dict getSafeDict(Long dictId) {
        Optional<Dict> dict = dictRepository.findById(dictId);
        if (!dict.isPresent()) {
            throw new NullPointerException(NOT_EXIST_DICT);
        }
        return dict.get();
    }

    // DictHistory
    private DictHistory getSafeDictHistory(Long historyId) {
        Optional<DictHistory> dictHistory = dictHistoryRepository.findById(historyId);
        if (!dictHistory.isPresent()) {
            throw new NullPointerException(NOT_EXIST_DICT_HISTORY);
        }
        return dictHistory.get();
    }

    // DictHistoryList
    private List<DictHistory> getSafeDictHistoryList(Dict dict) {
        Optional<List<DictHistory>> dictHistory = dictHistoryRepository.findAllByDict(dict);
        return dictHistory.orElseGet(ArrayList::new);
    }

    // Entity To Dto
    // DictHistoryList to DictHistoryRecentResponseDtoList
    public List<DictHistoryRecentResponseDto> dictHistoryListToDictHistoryRecentResponseDtoList(List<DictHistory> dictHistoryList) {
        List<DictHistoryRecentResponseDto> dictHistoryRecentResponseDtoList = new ArrayList<>();

        for (DictHistory dictHistory : dictHistoryList) {
            dictHistoryRecentResponseDtoList.add(DictHistoryRecentResponseDto.builder()
                    .historyId(dictHistory.getDictHistoryId())
                    .revertFrom(dictHistory.getRevertFrom() == null ? null : dictHistory.getRevertFrom().getDictHistoryId())
                    .writer(dictHistory.getUser().getNickname())
                    .createdAt(dictHistory.getCreatedAt())
                    .build());
        }

        return dictHistoryRecentResponseDtoList;
    }
    // endregion
}