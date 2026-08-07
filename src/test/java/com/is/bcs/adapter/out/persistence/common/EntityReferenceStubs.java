package com.is.bcs.adapter.out.persistence.common;

import jakarta.persistence.EntityManager;
import org.mockito.Mockito;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * 매핑 왕복 테스트용 EntityManager.
 *
 * <p>연관을 객체로 들면서 저장 경로는 getReference 로 껍데기만 만든다.
 * 순수 매핑 테스트에는 영속성 컨텍스트가 없으므로, id 만 채운 실제 인스턴스를 껍데기 대신 돌려준다.
 * 왕복 검증이 보는 것은 그 id 가 도메인에서 엔티티를 거쳐 다시 도메인으로 돌아오는지뿐이다.
 */
public final class EntityReferenceStubs {

    private EntityReferenceStubs() {
    }

    public static EntityManager entityManager() {
        EntityManager entityManager = Mockito.mock(EntityManager.class);
        Mockito.when(entityManager.getReference(Mockito.any(Class.class), Mockito.any()))
                .thenAnswer(invocation -> withId(invocation.getArgument(0), invocation.getArgument(1)));
        return entityManager;
    }

    private static Object withId(Class<?> type, Object id) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();
        Field field = type.getDeclaredField("id");
        field.setAccessible(true);
        field.set(instance, id);
        return instance;
    }
}
