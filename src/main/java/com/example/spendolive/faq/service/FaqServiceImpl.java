package com.example.spendolive.faq.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.spendolive.faq.domain.FaqVO;
import com.example.spendolive.faq.repository.FaqRepository;

/**
 * FaqService 구현체.
 * - 대부분의 메서드는 FaqRepository 호출을 그대로 위임만 함.
 * - 실질적인 로직은 두 곳: groupByCategory(카테고리별 그룹핑 + 빈 카테고리 제외),
 *   moveFaq(▲▼ 순서 바꾸기, sort_order 재정렬 후 스왑).
 */
@Service
public class FaqServiceImpl implements FaqService {

    // faqList.jsp에 보여줄 카테고리 고정 순서. 여기 없는 카테고리 값이 들어오면 그냥 안 보임(방어적으로 무시).
    private static final String[] CATEGORY_ORDER = {"account", "expense", "ott", "notice", "etc"};

    private final FaqRepository faqRepository;

    // 생성자 주입 - 스프링이 빈 등록할 때 이 생성자를 보고 FaqRepository를 자동으로 넣어줌
    public FaqServiceImpl(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    @Override
    public Map<String, List<FaqVO>> getVisibleFaqGroupedByCategory() {
        return groupByCategory(faqRepository.findAllVisible());
    }

    @Override
    public List<FaqVO> getAllFaqList() {
        return faqRepository.findAll();
    }

    @Override
    public Map<String, List<FaqVO>> getAllFaqGroupedByCategory() {
        return groupByCategory(faqRepository.findAll()); // 숨김(N) 포함 전체
    }

    @Override
    public FaqVO getFaqDetail(int faq_id) {
        return faqRepository.findById(faq_id);
    }

    @Override
    public int insertFaq(FaqVO faq) {
        return faqRepository.insertFaq(faq);
    }

    @Override
    public void updateFaq(FaqVO faq) {
        faqRepository.updateFaq(faq);
    }

    @Override
    public void deleteFaq(int faq_id) {
        faqRepository.deleteFaq(faq_id);
    }

    @Override
    public int getNextSortOrder(String category) {
        return faqRepository.getNextSortOrder(category);
    }

    // moveFaq(up=true)로 위임. 실제 스왑 로직은 moveFaq 하나에 몰아넣고
    // moveFaqUp/moveFaqDown은 방향만 다르게 넘겨주는 얇은 래퍼로 둠 (중복 방지)
    @Override
    public void moveFaqUp(int faq_id) {
        moveFaq(faq_id, true);
    }

    @Override
    public void moveFaqDown(int faq_id) {
        moveFaq(faq_id, false);
    }

    /* FAQ 리스트를 CATEGORY_ORDER 순서대로 그룹핑.
       항목이 하나도 없는 카테고리는 아예 map에 안 넣음
       → jsp에서 그 카테고리 섹션 자체가 안 뜨고, 필터 버튼 눌러도 "등록된 FAQ 없음"으로 자연스럽게 처리됨 */
    private Map<String, List<FaqVO>> groupByCategory(List<FaqVO> all) {
        Map<String, List<FaqVO>> grouped = new LinkedHashMap<>();
        for (String cat : CATEGORY_ORDER) {
            List<FaqVO> inCat = all.stream()
                    .filter(f -> cat.equals(f.getCategory()))
                    .toList();
            if (!inCat.isEmpty()) {
                grouped.put(cat, inCat);
            }
        }
        return grouped;
    }


    /**
     * 같은 카테고리 안에서 FAQ 하나를 한 칸 위(up=true) 또는 아래(up=false)로 이동시킴.
     *
     * 동작 순서:
     *  1) 대상 FAQ의 카테고리를 확인하고, 그 카테고리 FAQ 전체를 sort_order 순서대로 가져옴
     *  2) 대상이 배열의 몇 번째(idx)인지, 옮겨갈 자리(neighborIdx)가 배열 범위 안인지 확인
     *     (맨 위에서 ▲ 누르거나 맨 아래에서 ▼ 누르면 neighborIdx가 범위를 벗어나서 조용히 무시됨)
     *  3) 스왑하기 전에 카테고리 전체 sort_order를 배열 인덱스(0,1,2...)로 한 번 다시 써줌
     *     → 예전 데이터에 sort_order가 중복되거나 듬성듬성 비어있어도 여기서 깨끗하게 정리됨
     *  4) 정리된 상태에서 대상과 이웃의 sort_order만 서로 바꿔치기(swap)해서 순서 이동을 완성
     */
    private void moveFaq(int faq_id, boolean up) {
        // 1) 대상 FAQ 단건만 조회해서 카테고리 확인 (전체 테이블 안 불러옴)
        FaqVO target = faqRepository.findById(faq_id);
        if (target == null) return;
        String category = target.getCategory();
    
        // 2) 해당 카테고리 안의 FAQ만 조회 (범위를 좁혀서 조회)
        List<FaqVO> sameCat = faqRepository.findByCategory(category);
    
        int idx = -1;
        for (int i = 0; i < sameCat.size(); i++) {
            if (sameCat.get(i).getFaq_id() == faq_id) { idx = i; break; }
        }
        if (idx < 0) return;

        int neighborIdx = up ? idx - 1 : idx + 1;
        if (neighborIdx < 0 || neighborIdx >= sameCat.size()) return;

        // 3) sort_order를 배열 인덱스로 재정렬 (기존 값에 중복/구멍이 있어도 여기서 정리됨)
        for (int i = 0; i < sameCat.size(); i++) {
            faqRepository.updateSortOrder(sameCat.get(i).getFaq_id(), i);
        }
        // 4) 정리된 순서 기준으로 대상 ↔ 이웃 자리만 서로 바꿈
        faqRepository.updateSortOrder(sameCat.get(idx).getFaq_id(), neighborIdx);
        faqRepository.updateSortOrder(sameCat.get(neighborIdx).getFaq_id(), idx);
    }
}