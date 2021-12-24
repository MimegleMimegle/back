package com.teamproj.backend.service;

import com.teamproj.backend.Repository.board.BoardCategoryRepository;
import com.teamproj.backend.Repository.board.BoardLikeRepository;
import com.teamproj.backend.Repository.board.BoardRepository;
import com.teamproj.backend.Repository.board.BoardSubjectRepository;
import com.teamproj.backend.dto.board.*;
import com.teamproj.backend.model.board.*;
import com.teamproj.backend.security.UserDetailsImpl;
import com.teamproj.backend.util.ManuallyJwtLoginProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final BoardSubjectRepository boardSubjectRepository;
    private final BoardLikeRepository boardLikeRepository;

    private final CommentService commentService;
    private final ManuallyJwtLoginProcessor manuallyJwtLoginProcessor;

    //region 게시글 전체조회
    public List<BoardResponseDto> getBoard(String category) {
        Optional<BoardCategory> boardCategory = boardCategoryRepository.findById(category.toUpperCase());
        if (!boardCategory.isPresent()) {
            throw new NullPointerException("유효한 카테고리가 아닙니다.");
        }

        Optional<List<Board>> boardList = boardRepository.findAllByBoardCategory(boardCategory.get());
        if (!boardList.isPresent()) {
            return new ArrayList<>();
        }

        return boardListToBoardResponseDtoList(boardList.get());
    }

    private List<BoardResponseDto> boardListToBoardResponseDtoList(List<Board> boardList) {
        List<BoardResponseDto> boardResponseDtoList = new ArrayList<>();
        for (Board board : boardList) {
            boardResponseDtoList.add(BoardResponseDto.builder()
                    .postId(board.getPostId())
                    .title(board.getTitle())
                    .nickname(board.getUser().getNickname())
                    .createdAt(board.getCreatedAt().toLocalDate())
                    .subject(board.getBoardSubject() == null ? "" : board.getBoardSubject().getSubject())
                    .likeCnt(board.getLikes().size())
                    .views(board.getViews())
                    .build());
        }

        return boardResponseDtoList;
    }
    //endregion

    //region 게시글 작성
    public BoardUploadResponseDto uploadBoard(UserDetailsImpl userDetails, BoardUploadRequestDto boardUploadRequestDto,
                                              String category) {

        if (boardUploadRequestDto.getTitle().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수 입력 값입니다");
        }
        if (boardUploadRequestDto.getContent().isEmpty()) {
            throw new IllegalArgumentException("내용은 필수 입력 값입니다");
        }

        BoardCategory boardCategory = boardCategoryRepository.findById(boardUploadRequestDto.getCategory()).orElseThrow(
                    () -> new NullPointerException("해당 카테고리가 없습니다.")
                );

        BoardSubject subject = boardSubjectRepository.findBySubject(boardUploadRequestDto.getSubject())
                .orElseThrow(
                        () -> new NullPointerException("해당 글머리가 없습니다.")
                );


//        Optional<BoardCategory> boardCategory = boardCategoryRepository.findById(category);
//        if(!boardCategory.isPresent()){
//            throw new NullPointerException("유효하지 않은 카테고리입니다.");
//        }

        // To Do: 아래 코드는 차 후 삭제할 예정
//        BoardCategory boardCategory = new BoardCategory(category, null);
//        boardCategoryRepository.save(boardCategory);


        Board board = Board.builder()
                .title(boardUploadRequestDto.getTitle())
                .content(boardUploadRequestDto.getContent())
//                .boardCategory(boardCategory.get())
                .boardCategory(boardCategory)
                .boardSubject(null)
                .user(userDetails.getUser())
                .build();
        boardRepository.save(board);


        return BoardUploadResponseDto.builder()
                .boardId(board.getPostId())
                .title(board.getTitle())
                .content(board.getContent())
                .subject(board.getBoardSubject() == null ? "" : board.getBoardSubject().getSubject())
                .category(board.getBoardCategory().getCategoryName())
                .createdAt(board.getCreatedAt() == null ? null :  board.getCreatedAt().toLocalDate())
                .build();
    }
    //endregion

    //region 게시글 상세 조회
    public BoardDetailResponseDto getBoardDetail(Long postId, String token) {
        UserDetailsImpl userDetails = manuallyJwtLoginProcessor.forceLogin(token);
        Board board = boardRepository.findById(postId)
                .orElseThrow(
                        () -> new NullPointerException("해당 게시글이 없습니다.")
                );

        boolean isLike = false;
        if(userDetails != null) {
            Optional<BoardLike> boardLike = boardLikeRepository.findByBoardAndUser(board, userDetails.getUser());
            if(boardLike.isPresent()) {
                isLike = true;
            }
        }

        boardRepository.updateView(postId);

        List<BoardLike> boardLikeList = boardLikeRepository.findAllByBoard(board);

        return BoardDetailResponseDto.builder()
                .boardId(board.getPostId())
                .title(board.getTitle())
                .content(board.getContent())
                .writer(board.getUser().getNickname())
                .createdAt(board.getCreatedAt().toLocalDate())
                .subject(board.getBoardSubject() == null ? "" : board.getBoardSubject().getSubject())
                .views(board.getViews())
                .likeCnt(boardLikeList.size())
                .isLike(isLike)
                .commentList(commentService.getCommentList(board.getPostId(), 0, 10))
                .build();
    }
    //endregion

    //region 게시글 업데이트(수정)
    public String updateBoard(Long postId, UserDetailsImpl userDetails, BoardUploadRequestDto boardUploadRequestDto) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(
                        () -> new NullPointerException("해당 게시글이 없습니다.")
                );
        if (userDetails.getUser().getId() != board.getUser().getId()) {
            throw new IllegalArgumentException("게시글을 작성한 유저만 수정이 가능합니다.");
        }

//        BoardSubject boardSubject = boardSubjectRepository.findBySubject(boardUploadRequestDto.getSubject())
//                .orElseThrow(
//                        () -> new NullPointerException("해당 글머리가 없습니다.")
//                );


        // To Do: 차 후 삭제할 코드
        BoardSubject boardSubject = null;

        // To Do: 글머리도 수정 가능할 시 추가 작성 필요
        board.update(boardUploadRequestDto, boardSubject);

        boardRepository.save(board);
        return "게시글 수정 완료";
    }
    //endregion

    //region 게시글 삭제
    public String deleteBoard(UserDetailsImpl userDetails, Long postId) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(
                        () -> new NullPointerException("해당 게시글이 없습니다.")
                );
        if (userDetails.getUser().getId() != board.getUser().getId()) {
            throw new IllegalArgumentException("게시글을 작성한 유저만 삭제가 가능합니다.");
        }

        boardRepository.delete(board);
        return "게시글 삭제 완료";
    }
    //endregion

    //region 글머리 불러오기
    public List<BoardSubjectResponseDto> getBoardSubject(String category) {
        Optional<BoardCategory> boardCategory = boardCategoryRepository.findById(category);
        if (!boardCategory.isPresent()) {
            throw new NullPointerException("존재하지 않는 카테고리입니다.");
        }

        return boardSubjectListToBoardSubjectResponseDto(boardCategory.get().getBoardSubjectList());
    }

    private List<BoardSubjectResponseDto> boardSubjectListToBoardSubjectResponseDto(List<BoardSubject> boardSubjectList) {
        List<BoardSubjectResponseDto> boardSubjectResponseDtoList = new ArrayList<>();
        for (BoardSubject boardSubject : boardSubjectList) {
            boardSubjectResponseDtoList.add(BoardSubjectResponseDto.builder()
                    .subjectId(boardSubject.getSubjectId())
                    .content(boardSubject.getSubject())
                    .build());
        }
        return boardSubjectResponseDtoList;
    }
    //endregion

    //region 게시글 좋아요
    public Boolean boardLike(UserDetailsImpl userDetails, Long postId) {
        Board board = boardRepository.findById(postId)
                .orElseThrow(
                        () -> new NullPointerException("해당 게시글이 없습니다.")
                );

        Optional<BoardLike> findBoardLike = boardLikeRepository.findByBoardAndUser(board, userDetails.getUser());
        if (findBoardLike.isPresent()) {
            boardLikeRepository.delete(findBoardLike.get());
            return false;
        }

        BoardLike boardLike = BoardLike.builder()
                .board(board)
                .user(userDetails.getUser())
                .build();

        boardLikeRepository.save(boardLike);

        return true;
    }
    //endregion
}
