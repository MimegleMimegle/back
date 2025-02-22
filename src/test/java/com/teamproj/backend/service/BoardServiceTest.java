package com.teamproj.backend.service;


import com.teamproj.backend.Repository.UserRepository;
import com.teamproj.backend.Repository.board.BoardCategoryRepository;
import com.teamproj.backend.Repository.board.BoardLikeRepository;
import com.teamproj.backend.Repository.board.BoardRepository;
import com.teamproj.backend.Repository.board.BoardTodayLikeRepository;
import com.teamproj.backend.config.S3MockConfig;
import com.teamproj.backend.dto.board.BoardDelete.BoardDeleteResponseDto;
import com.teamproj.backend.dto.board.BoardDetail.BoardDetailResponseDto;
import com.teamproj.backend.dto.board.BoardLike.BoardLikeResponseDto;
import com.teamproj.backend.dto.board.BoardResponseDto;
import com.teamproj.backend.dto.board.BoardUpdate.BoardUpdateRequestDto;
import com.teamproj.backend.dto.board.BoardUpdate.BoardUpdateResponseDto;
import com.teamproj.backend.dto.board.BoardUpload.BoardUploadRequestDto;
import com.teamproj.backend.dto.board.BoardUpload.BoardUploadResponseDto;
import com.teamproj.backend.dto.main.MainMemeImageResponseDto;
import com.teamproj.backend.exception.ExceptionMessages;
import com.teamproj.backend.model.User;
import com.teamproj.backend.model.board.Board;
import com.teamproj.backend.model.board.BoardCategory;
import com.teamproj.backend.model.board.BoardLike;
import com.teamproj.backend.model.board.BoardTodayLike;
import com.teamproj.backend.security.UserDetailsImpl;
import com.teamproj.backend.security.jwt.JwtTokenUtils;
import io.findify.s3mock.S3Mock;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@Import(S3MockConfig.class)

@Transactional
@Rollback
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Autowired
    private BoardService boardService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardCategoryRepository boardCategoryRepository;

    @Autowired
    private BoardLikeRepository boardLikeRepository;

    @Autowired
    private BoardTodayLikeRepository boardTodayLikeRepository;

    @Autowired
    private UserRepository userRepository;


    @Mock
    private ServletRequestAttributes attributes;

    @Autowired
    S3Mock s3Mock;

    UserDetailsImpl userDetails;

    String boardTitle;
    String boardContent;
    User user;


    @BeforeEach
    void setup() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
        attributes = new ServletRequestAttributes(mockHttpServletRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        boardTitle = "타이틀";
        boardContent = "내용";

        user = User.builder()
                .username("테스트유저")
                .nickname("테스트닉네임")
                .profileImage("프로필이미지")
                .password("Q1234567")
                .build();

        userRepository.save(user);
        userDetails = UserDetailsImpl.builder()
                .username("테스트유저")
                .password("Q1234567")
                .build();
    }

    //region 게시글 전체조회
    @Nested
    @DisplayName("게시글 전체조회")
    class getBoard {

        @Test
        @DisplayName("성공")
        void getBoard_success() {
            // given
            BoardCategory boardCategory = new BoardCategory("IMAGEBOARD");
            boardCategoryRepository.save(boardCategory);

            // when
            List<BoardResponseDto> boardResponseDtoList = boardService.getBoard("IMAGEBOARD", 0, 1, "token");

            // then
            for(BoardResponseDto boardResponseDto : boardResponseDtoList) {
                assertNotNull(boardResponseDto);
            }
        }
    }
    //endregion

    //region 게시글 작성
    @Nested
    @DisplayName("게시글 작성")
    class uploadBoard {
        @Test
        @DisplayName("게시글 작성 / 성공")
        void uploadBoard_sucess() throws IOException {
            // givien
            BoardCategory boardCategory = new BoardCategory("카테고리");
            boardCategoryRepository.save(boardCategory);

            BoardUploadRequestDto boardUploadRequestDto = BoardUploadRequestDto.builder()
                    .title(boardTitle)
                    .content(boardContent)
                    .build();

            MockMultipartFile mockMultipartFile = new MockMultipartFile(
                    "testJunit", "originalName", null, "image".getBytes()
            );

            // when
            BoardUploadResponseDto boardUploadResponseDto = boardService.uploadBoard(
                    userDetails, boardUploadRequestDto, boardCategory.getCategoryName(), mockMultipartFile
            );
            Optional<Board> board = boardRepository.findById(boardUploadResponseDto.getBoardId());

            // then
            assertEquals(board.get().getBoardId(), boardUploadResponseDto.getBoardId());
            assertEquals(boardTitle, boardUploadResponseDto.getTitle());
            assertEquals(boardContent, boardUploadResponseDto.getContent());
        }

        @Nested
        @DisplayName("게시글 작성 / 실패")
        class uploadBoard_fail {

            @Test
            @DisplayName("실패1 / 제목 미입력")
            void uploadBoard_fail() {
                // givien
                boardTitle = "";

                BoardUploadRequestDto boardUploadRequestDto = BoardUploadRequestDto.builder()
                        .title(boardTitle)
                        .content(boardContent)
                        .build();

                MockMultipartFile mockMultipartFile = new MockMultipartFile(
                        "testJunit", "originalName", null, "image".getBytes()
                );

                // when
                Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                    boardService.uploadBoard(userDetails, boardUploadRequestDto, "카테고리", mockMultipartFile);
                });

                // then
                assertEquals(ExceptionMessages.TITLE_IS_EMPTY, exception.getMessage());
            }

            @Test
            @DisplayName("실패2 / 내용 미입력")
            void uploadBoard_fail2() {
                // givien
                String boardContent = "";

                BoardUploadRequestDto boardUploadRequestDto = BoardUploadRequestDto.builder()
                        .title(boardTitle)
                        .content(boardContent)
                        .build();


                MockMultipartFile mockMultipartFile = new MockMultipartFile(
                        "testJunit", "originalName", null, "image".getBytes()
                );

                // when
                Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                    boardService.uploadBoard(userDetails, boardUploadRequestDto, "카테고리", mockMultipartFile);
                });

                // then
                assertEquals(ExceptionMessages.CONTENT_IS_EMPTY, exception.getMessage());
            }

            @Test
            @DisplayName("실패3 / 해당 카테고리가 없습니다.")
            void uploadBoard_fail3() {
                // givien
                BoardCategory boardCategory = new BoardCategory("카테고리");
                BoardUploadRequestDto boardUploadRequestDto = BoardUploadRequestDto.builder()
                        .title(boardTitle)
                        .content(boardContent)
                        .build();

                MockMultipartFile mockMultipartFile = new MockMultipartFile(
                        "testJunit", "originalName", null, "image".getBytes()
                );

                // when
                Exception exception = assertThrows(NullPointerException.class, () -> {
                    boardService.uploadBoard(userDetails, boardUploadRequestDto, "카테고리", mockMultipartFile);
                });

                // then
                assertEquals(ExceptionMessages.NOT_EXIST_CATEGORY, exception.getMessage());
            }
        }
    }
    //endregion

    //region 게시글 상세 조회
    @Nested
    @DisplayName("게시글 상세 조회")
    class getBoardDetail {

        @Test
        @DisplayName("성공")
        void getBoardDetail_success() {
            // given
            BoardCategory boardCategory = new BoardCategory("카테고리");
            boardCategoryRepository.save(boardCategory);


            Board board = Board.builder()
                    .user(user)
                    .content("내용")
                    .title("제목")
                    .boardCategory(boardCategory)
                    .enabled(true)
                    .thumbNail("썸네일URL")
                    .build();

            boardRepository.save(board);


            String token = "BEARER " + JwtTokenUtils.generateJwtToken(userDetails);

            // when
            BoardDetailResponseDto boardDetailResponseDto = boardService.getBoardDetail(board.getBoardId(), token);

            // then
            assertNull(boardDetailResponseDto.getTitle());
            assertNull(boardDetailResponseDto.getContent());
            assertNotNull(boardDetailResponseDto.getCreatedAt());
            assertNotNull(boardDetailResponseDto.getCommentCnt());
            assertEquals(board.getBoardId(), boardDetailResponseDto.getBoardId());
            assertEquals(board.getUser().getUsername(), boardDetailResponseDto.getUsername());
            assertEquals(board.getUser().getProfileImage(), boardDetailResponseDto.getProfileImageUrl());
            assertEquals(board.getViews(), boardDetailResponseDto.getViews());
            assertEquals(board.getBoardLikeList().size(), boardDetailResponseDto.getLikeCnt());
            assertEquals(false, boardDetailResponseDto.getIsLike());
            assertEquals(board.getThumbNail(), boardDetailResponseDto.getThumbNail());
            assertEquals(board.getUser().getNickname(), boardDetailResponseDto.getWriter());
        }

        @Test
        @DisplayName("실패 / 유효하지 않은 게시글입니다.")
        void getBoardDetail_fail() {
            // given
            String token = "BEARER " + JwtTokenUtils.generateJwtToken(userDetails);


            // when
            Exception exception = assertThrows(NullPointerException.class, () -> {
                boardService.getBoardDetail(0L, token);
            });

            // then
            assertEquals(ExceptionMessages.NOT_EXIST_BOARD, exception.getMessage());
        }
    }
    //endregion

    //region 게시글 업데이트(수정)
    @Nested
    @DisplayName("게시글 업데이트(수정)")
    class updateBoard {


        @Nested
        @DisplayName("성공")
        class updateBoard_success {

            @Test
            @DisplayName("이미지 있음")
            void updateBoard_success1() {
                // given
                BoardCategory boardCategory = new BoardCategory("카테고리");
                Board board = Board.builder()
                        .title(boardTitle)
                        .content(boardContent)
                        .boardCategory(boardCategory)
                        .user(user)
                        .thumbNail("썸네일URL")
                        .build();

                boardCategoryRepository.save(boardCategory);
                userRepository.save(user);
                boardRepository.save(board);

                BoardUpdateRequestDto boardUpdateRequestDto = BoardUpdateRequestDto.builder()
                        .title(board.getTitle())
                        .content(board.getContent())
                        .build();

                MockMultipartFile mockMultipartFile = new MockMultipartFile(
                        "testJunit", "originalName", null, "image".getBytes()
                );

                // when
                BoardUpdateResponseDto result = boardService.updateBoard(
                        board.getBoardId(), userDetails, boardUpdateRequestDto, mockMultipartFile
                );

                // then
                assertEquals("게시글 수정 완료", result.getResult());
            }

            @Test
            @DisplayName("이미지 Empty")
            void updateBoard_success2() {
                // given
                BoardCategory boardCategory = new BoardCategory("카테고리");
                Board board = Board.builder()
                        .title(boardTitle)
                        .content(boardContent)
                        .boardCategory(boardCategory)
                        .user(user)
                        .thumbNail("썸네일URL")
                        .build();

                boardCategoryRepository.save(boardCategory);
                userRepository.save(user);
                boardRepository.save(board);

                BoardUpdateRequestDto boardUpdateRequestDto = BoardUpdateRequestDto.builder()
                        .title(board.getTitle())
                        .content(board.getContent())
                        .build();

                MockMultipartFile mockMultipartFile = new MockMultipartFile(
                        "image", "", null, "".getBytes()
                );

                // when
                BoardUpdateResponseDto result = boardService.updateBoard(
                        board.getBoardId(), userDetails, boardUpdateRequestDto, mockMultipartFile
                );

                // then
                assertEquals("게시글 수정 완료", result.getResult());
            }
        }


        @Nested
        @DisplayName("실패")
        class updateBoard_fail {

            @Test
            @DisplayName("실패 / 유효하지 않은 게시글입니다.")
            void updateBoard_fail() {
                // given
                BoardUpdateRequestDto boardUpdateRequestDto = BoardUpdateRequestDto.builder()
                        .title("수정된 제목")
                        .content("수정된 내용")
                        .build();

                MockMultipartFile mockMultipartFile = new MockMultipartFile(
                        "testJunit", "originalName", null, "image".getBytes()
                );

                // when
                Exception exception = assertThrows(NullPointerException.class, () -> {
                    boardService.updateBoard(0L, userDetails, boardUpdateRequestDto, mockMultipartFile);
                });

                // then
                assertEquals(ExceptionMessages.NOT_EXIST_BOARD, exception.getMessage());
            }

            @Test
            @DisplayName("실패2 / 권한이 없습니다.")
            void updateBoard_fail2() {
                // given
                BoardCategory boardCategory = new BoardCategory("카테고리");
                User user2 = User.builder()
                        .username("유저네임2")
                        .nickname("닉네임2")
                        .password("qwer1234")
                        .build();

                boardCategoryRepository.save(boardCategory);
                userRepository.save(user2);

                Board board = Board.builder()
                        .title(boardTitle)
                        .content(boardContent)
                        .user(user2)
                        .boardCategory(boardCategory)
                        .thumbNail("썸네일URL")
                        .build();

                boardRepository.save(board);
                BoardUpdateRequestDto boardUpdateRequestDto = BoardUpdateRequestDto.builder()
                        .title(board.getTitle())
                        .content(board.getContent())
                        .build();

                MockMultipartFile mockMultipartFile = new MockMultipartFile(
                        "testJunit", "originalName", null, "image".getBytes()
                );


                // when
                Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                    boardService.updateBoard(
                            board.getBoardId(), userDetails, boardUpdateRequestDto, mockMultipartFile
                    );
                });

                // then
                assertEquals(ExceptionMessages.NOT_MY_BOARD, exception.getMessage());
            }
        }
    }

    //endregion

    //region 게시글 삭제
    @Nested
    @Transactional
    @DisplayName("게시글 삭제")
    class deleteBoard {

        @Test
        @DisplayName("게시글 삭제 / 성공")
        void deleteBoard_success() {
            // given

            BoardCategory boardCategory = new BoardCategory("카테고리");

            userRepository.save(user);
            boardCategoryRepository.save(boardCategory);

            Board board = Board.builder()
                    .user(user)
                    .boardCategory(boardCategory)
                    .content("콘텐츠")
                    .title("타이틀")
                    .thumbNail("썸네일URL")
                    .build();

            boardRepository.save(board);


            // when
            BoardDeleteResponseDto result = boardService.deleteBoard(userDetails, board.getBoardId());


            // then
            assertEquals("게시글 삭제 완료", result.getResult());
        }

        @Nested
        @DisplayName("게시글 삭제 / 실패")
        class deleteBoard_fail {

            @Test
            @DisplayName("실패1 / 유효하지 않은 게시글입니다.")
            void deleteBoard_fail() {

                // when
                Exception exception = assertThrows(NullPointerException.class, () -> {
                    boardService.deleteBoard(userDetails, 0L);
                });

                // then
                assertEquals(ExceptionMessages.NOT_EXIST_BOARD, exception.getMessage());
            }

            @Test
            @DisplayName("실패2 / 권한이 없습니다.")
            void deleteBoard_fail2() {
                // given

                BoardCategory boardCategory = new BoardCategory("카테고리");

                userRepository.save(user);
                boardCategoryRepository.save(boardCategory);

                Board board = Board.builder()
                        .user(user)
                        .boardCategory(boardCategory)
                        .content("콘텐츠")
                        .title("타이틀")
                        .thumbNail("썸네일URL")
                        .build();

                boardRepository.save(board);

                User user2 = User.builder()
                        .username("newuser2")
                        .nickname("닉네임22")
                        .password("Q1w2e3")
                        .build();

                userRepository.save(user2);
                UserDetailsImpl userDetails2 = UserDetailsImpl.builder()
                        .username(user2.getUsername())
                        .password(user2.getPassword())
                        .build();

                // when
                Exception exception = assertThrows(IllegalArgumentException.class, () -> {
                    boardService.deleteBoard(userDetails2, board.getBoardId());
                });

                // then
                assertEquals(ExceptionMessages.NOT_MY_BOARD, exception.getMessage());
            }
        }

    }
    //endregion

    //region 게시글 좋아요
    @Nested
    @DisplayName("게시글 좋아요")
    class boardLike {

        @Nested
        @DisplayName("성공")
        class boardLike_success {

            @Test
            @DisplayName("성공 케이스1")
            void boardLike_success1() {
                // given
                BoardCategory boardCategory = new BoardCategory("카테고리");

                Board board = Board.builder()
                        .title(boardTitle)
                        .content(boardContent)
                        .thumbNail("썸네일URL")
                        .boardCategory(boardCategory)
                        .user(user)
                        .build();


                boardCategoryRepository.save(boardCategory);
                userRepository.save(user);
                boardRepository.save(board);

                // when
                BoardLikeResponseDto result = boardService.boardLike(userDetails, board.getBoardId());

                // then
                assertEquals(true, result.getResult());
            }


            @Test
            @DisplayName("성공 케이스2")
            void boardLike_success2() {
                // given
                BoardCategory boardCategory = new BoardCategory("IMAGEBOARD");

                Board board = Board.builder()
                        .title(boardTitle)
                        .content(boardContent)
                        .thumbNail("썸네일URL")
                        .boardCategory(boardCategory)
                        .user(user)
                        .build();

                BoardLike boardLike = BoardLike.builder()
                                .user(user)
                                .board(board)
                                .build();

                BoardTodayLike boardTodayLike = BoardTodayLike.builder()
                                .board(board)
                                .boardCategory(boardCategory)
                                .likeCount(1L)
                                .build();

                boardCategoryRepository.save(boardCategory);
                userRepository.save(user);
                boardRepository.save(board);
                boardLikeRepository.save(boardLike);
                boardTodayLikeRepository.save(boardTodayLike);

                // when
                BoardLikeResponseDto result = boardService.boardLike(userDetails, board.getBoardId());

                // then
                assertEquals(false, result.getResult());
            }
        }

        @Test
        @DisplayName("실패 / 유효하지 않은 게시글입니다.")
        void boardLike_fail() {

            // when
            Exception exception = assertThrows(NullPointerException.class, () -> {
                boardService.boardLike(userDetails, 0L);
            });

            // then
            assertEquals(ExceptionMessages.NOT_EXIST_BOARD, exception.getMessage());
        }
    }
    //endregion

    //region 명예의 전당
    @Test
    @DisplayName("성공")
    void getTodayImage_success() {
        // when
        List<MainMemeImageResponseDto> mainMemeImageResponseDtoList = boardService.getTodayImage(5);

        //then
        assertNotEquals(0, mainMemeImageResponseDtoList.size());
    }
    //endregion

    //region 카테고리별 게시글 총 개수
    @Test
    @DisplayName("카테고리별 게시글 총 개수 / 성공")
    void getTotalBoardCount_success() {
        // given
        BoardCategory boardCategory = BoardCategory.builder()
                .categoryName("IMAGEBOARD")
                .build();
        Long boardCount = boardRepository.countByBoardCategoryAndEnabled(boardCategory, true);

        // when
        Long result = boardService.getTotalBoardCount("IMAGEBOARD");

        // then
        assertEquals(boardCount, result);
    }

    //endregion
}