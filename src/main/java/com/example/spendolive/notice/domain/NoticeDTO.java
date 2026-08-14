package com.example.spendolive.notice.domain;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter

public class NoticeDTO {

    private int notice_id;           // 공지 고유 번호(PK)
    private String admin_id;         // 작성한 관리자 ID
    private String title;           // 공지 제목
    private String content;         // 공지 내용
    private String pinned_yn;        // 상단 공지 여부
    private String created_at;       // 등록일
    private String updated_at;       // 최종 수정일
    private String read_yn;          // notice_tb 자체 컬럼 X, notice_read_tb 조인 결과 | 로그인 회원이 읽었으면 Y, 안 읽었으면 N
    private String star_yn;          // notice_tb 자체 컬럼 X, notice_favorite_tb 조인 결과 | 로그인 회원이 찜했으면 Y, 안 했으면 N

}
