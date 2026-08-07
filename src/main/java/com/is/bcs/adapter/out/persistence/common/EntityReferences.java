package com.is.bcs.adapter.out.persistence.common;

import jakarta.persistence.EntityManager;

/**
 * id 하나로 참조 껍데기를 만드는 자리.
 *
 * <p>연관을 객체로 들되 저장 경로는 종전처럼 가볍게 두기 위한 것이다.
 * getReference 는 DB 에 가지 않고 id 만 든 껍데기를 돌려주므로, 행 하나를 넣으려고 상대 행을 읽어 오지 않는다.
 * 그 id 가 실재하는지는 저장 시점에 외래키가 판정한다.
 */
public final class EntityReferences {

    private EntityReferences() {
    }

    public static <T> T of(EntityManager entityManager, Class<T> type, Long id) {
        return id == null ? null : entityManager.getReference(type, id);
    }
}
