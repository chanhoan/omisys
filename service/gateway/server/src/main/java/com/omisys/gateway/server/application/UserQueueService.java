package com.omisys.gateway.server.application;

import com.omisys.gateway.server.application.dto.RegisterUserResponse;
import com.omisys.gateway.server.infrastructure.exception.GatewayErrorCode;
import com.omisys.gateway.server.infrastructure.exception.GatewayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueueService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final DistributedLockComponent lockComponent;

    /** 대기 큐(Sorted Set): score = 요청 시각(Unix time), 오래 기다린 사용자부터 승격(popMin) */
    private final String USER_QUEUE_WAIT_KEY = "users:queue:wait";

    /** 진행 큐(Sorted Set): score = 마지막 활동 시각(Unix time), 일정 시간 무활동이면 제거 */
    private final String USER_QUEUE_PROCEED_KEY = "users:queue:proceed";

    /** 활성 사용자(Set): 현재 진행 큐에 “허용된 사용자 수”를 빠르게 계산하기 위한 집합 */
    private final String USER_ACTIVE_SET_KEY  = "users:active";

    /** 동시에 proceed(진행) 상태로 허용할 최대 사용자 수 */
    @Value("${MAX_ACTIVE_USERS}")
    private long MAX_ACTIVE_USERS;

    /** 무활동 사용자 제거 기준(초): proceed 큐에서 마지막 활동 시간이 이 값을 넘으면 제거 */
    private final long INACTIVITY_THRESHOLD = 300;

    /**
     * 사용자 등록(또는 재등록) 요청을 처리한다.
     *
     * <p>처리 흐름:</p>
     * <ul>
     *   <li>이미 진행 큐(proceed)에 포함된 사용자면: 활동 시간을 갱신하고 "대기 없음(0)" 반환</li>
     *   <li>신규 사용자면: 현재 활성 사용자 수가 허용치 미만이면 proceed로, 아니면 wait 큐로 보낸다</li>
     * </ul>
     *
     * @param userId 사용자 식별자
     * @return RegisterUserResponse (0이면 proceed 상태, 1 이상이면 대기 순번)
     */
    public Mono<RegisterUserResponse> registerUser(String userId) {
        return reactiveRedisTemplate.opsForZSet()
                .rank(USER_QUEUE_PROCEED_KEY, userId)
                .defaultIfEmpty(-1L)
                .flatMap(rank -> rank >= 0 ? handleProceedUser(userId) : handleNewUser(userId));
    }

    /**
     * 진행 큐(proceed)에 사용자를 추가한다.
     *
     * <p>의도:</p>
     * <ul>
     *   <li>동시에 여러 요청이 들어올 때 proceed/active 반영 과정에서 경쟁 조건이 발생할 수 있으므로
     *       분산락을 통해 "추가 과정"을 직렬화한다.</li>
     *   <li>락 획득 후 addUserToQueue()를 실행하고, 결과를 sink로 전달하여 Mono를 완료시킨다.</li>
     * </ul>
     *
     * @param userId 사용자 식별자
     * @return RegisterUserResponse (성공 시 0, 실패 시 대기 순번)
     */
    public Mono<RegisterUserResponse> addToProceedQueue(String userId) {
        return Mono.create(sink -> {
            lockComponent.execute(userId, 1000, 1000, () -> {
                try {
                    addUserToQueue(userId)
                            .doOnSuccess(sink::success)
                            .doOnError(sink::error)
                            .subscribe();
                } catch (Exception e) {
                    sink.error(e);
                }
            });
        });
    }

    /**
     * 주기적으로(30초마다) 큐 상태를 정리하고, 대기 큐에서 진행 큐로 사용자를 승격시킨다.
     *
     * <p>실행 내용:</p>
     * <ol>
     *   <li>진행 큐에서 무활동 사용자 제거</li>
     *   <li>빈 자리가 있으면 대기 큐(wait)에서 오래 기다린 사용자부터 진행 큐(proceed)로 이동</li>
     * </ol>
     *
     * <p>주의:</p>
     * <ul>
     *   <li>리액티브 체인은 subscribe()가 호출되어야 실제 수행된다.</li>
     *   <li>스케줄러 메서드는 void이므로 내부에서 구독을 트리거한다.</li>
     * </ul>
     */
    @Scheduled(fixedRate = 30000)
    public void scheduleAllUser() {
        removeInactiveUsers()
                .then(allowUserTask())
                .subscribe(
                        movedUsers -> {},
                        error -> log.error(GatewayErrorCode.INTERNAL_SERVER_ERROR.getMessage(), error)
                );
    }

    /**
     * 현재 사용자가 "진행 허용(proceed)" 상태인지 확인한다.
     *
     * <p>진행 큐에 존재하면 true이며, 이때 활동 시간을 갱신(heartbeat)한다.</p>
     *
     * @param userId 사용자 식별자
     * @return proceed 큐 포함 여부 (포함 시 true, 미포함 시 false)
     */
    public Mono<Boolean> isAllowed(String userId) {
        return reactiveRedisTemplate.opsForZSet()
                .rank(USER_QUEUE_PROCEED_KEY, userId)
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0)
                .flatMap(isAllowed -> {
                    if (isAllowed) {
                        return updateUserActivityTime(userId).thenReturn(true);
                    }
                    return Mono.just(false);
                });
    }

    /**
     * 사용자의 현재 순번(대기/진행)을 조회한다.
     *
     * <p>현재 구현은 proceed 큐에서의 rank를 반환한다.</p>
     * <ul>
     *   <li>rank가 0부터 시작하므로, 사용자 응답은 rank+1로 보정한다.</li>
     *   <li>존재하지 않으면 -1을 반환한다.</li>
     * </ul>
     *
     * @param userId 사용자 식별자
     * @return wait 큐에서의 순번(1부터 시작), 미존재 시 -1
     */
    public Mono<Long> getRank(String userId) {
        return reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY, userId)
                .defaultIfEmpty(-1L)
                .map(rank -> rank >= 0 ? rank + 1 : rank);
    }

    /**
     * 진행 큐에 이미 포함된 사용자(= 통과 사용자)에 대한 처리.
     *
     * <p>활동 시간을 갱신하고 대기 없음(0)을 반환한다.</p>
     *
     * @param userId 사용자 식별자
     * @return RegisterUserResponse(0)
     */
    private Mono<RegisterUserResponse> handleProceedUser(String userId) {
        return updateUserActivityTime(userId)
                .thenReturn(new RegisterUserResponse(0L));
    }

    /**
     * 신규 사용자 처리.
     *
     * <p>현재 활성 사용자 수(active set)가 허용치보다 작으면 proceed로, 아니면 wait 큐로 보낸다.</p>
     *
     * @param userId 사용자 식별자
     * @return RegisterUserResponse (0 또는 대기 순번)
     */
    private Mono<RegisterUserResponse> handleNewUser(String userId) {
        return reactiveRedisTemplate.opsForSet().size(USER_ACTIVE_SET_KEY)
                .flatMap(activeUsers -> activeUsers < MAX_ACTIVE_USERS ? addToProceedQueue(userId)
                        : checkAndAddToQueue(userId));
    }

    /**
     * 사용자의 "마지막 활동 시간"을 갱신한다.
     *
     * <p>활동 시간은 proceed 큐의 score로 저장되어, 무활동 사용자 제거 기준에 사용된다.</p>
     *
     * @param userId 사용자 식별자
     * @return 갱신 결과 (true/false)
     */
    private Mono<Boolean> updateUserActivityTime(String userId) {
        long currentTime = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_PROCEED_KEY, userId, currentTime);
    }

    /**
     * 사용자가 이미 대기 큐에 존재하는지 확인한 뒤, 정책에 맞게 대기열에 반영한다.
     *
     * <ul>
     *   <li>이미 대기 중이면 score 갱신 후 최신 rank 반환</li>
     *   <li>대기 중이 아니면 대기 큐에 추가 후 rank 반환</li>
     * </ul>
     *
     * @param userId 사용자 식별자
     * @return RegisterUserResponse(대기 순번)
     */
    private Mono<RegisterUserResponse> checkAndAddToQueue(String userId) {
        return reactiveRedisTemplate.opsForZSet().score(USER_QUEUE_WAIT_KEY, userId)
                .defaultIfEmpty(-1.0)
                .flatMap(score -> {
                    if (score >= 0) {
                        return updateWaitQueueScore(userId);
                    } else {
                        return addToWaitQueue(userId);
                    }
                });
    }

    /**
     * 대기 큐에 이미 존재하는 사용자의 score(우선순위/시간)를 갱신한다.
     *
     * <p>현재 구현은 score를 "현재 시간"으로 덮어쓴다. 이 정책은 대기열에서 순번이 변경될 수 있으므로
     * 의도된 설계(재요청 시 뒤로 보내기 등)인지 확인이 필요하다.</p>
     *
     * @param userId 사용자 식별자
     * @return RegisterUserResponse(갱신 후 대기 순번)
     */
    private Mono<RegisterUserResponse> updateWaitQueueScore(String userId) {
        double newScore = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet().score(USER_QUEUE_WAIT_KEY, userId)
                .flatMap(oldScore ->
                        reactiveRedisTemplate.opsForZSet().add(USER_QUEUE_WAIT_KEY, userId, newScore)
                                .then(reactiveRedisTemplate.opsForZSet().rank(USER_QUEUE_WAIT_KEY, userId))
                )
                .map(rank -> new RegisterUserResponse(rank + 1));
    }

    /**
     * 사용자를 대기 큐(wait)에 추가한다.
     *
     * <p>추가에 성공하면 rank를 계산하여 대기 순번(1부터 시작)을 반환한다.</p>
     * <p>추가에 실패하면 TOO_MANY_REQUESTS 예외를 발생시킨다.</p>
     *
     * @param userId 사용자 식별자
     * @return RegisterUserResponse(대기 순번)
     * @throws GatewayException 대기열 등록 실패 시 (TOO_MANY_REQUESTS)
     */
    private Mono<RegisterUserResponse> addToWaitQueue(String userId) {
        var unixTime = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet()
                .add(USER_QUEUE_WAIT_KEY, userId, unixTime)
                .filter(i -> i)
                .switchIfEmpty(Mono.error(new GatewayException(GatewayErrorCode.TOO_MANY_REQUESTS)))
                .flatMap(i -> reactiveRedisTemplate.opsForZSet()
                        .rank(USER_QUEUE_WAIT_KEY, userId))
                .map(rank -> new RegisterUserResponse(rank + 1));
    }

    /**
     * 사용자를 진행 큐(proceed)에 추가하고, 성공 시 active set에도 반영한다.
     *
     * <p>proceed 큐 추가가 실패하면(예: 이미 존재/경쟁 조건 등) 대기열 처리(checkAndAddToQueue)로 폴백한다.</p>
     *
     * @param userId 사용자 식별자
     * @return RegisterUserResponse(0 또는 대기 순번)
     */
    private Mono<RegisterUserResponse> addUserToQueue(String userId) {
        var unixTime = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet()
                .add(USER_QUEUE_PROCEED_KEY, userId, unixTime)
                .filter(success -> success)
                .flatMap(success -> {
                    if (success) {
                        return addToActiveSet(userId);
                    } else {
                        return checkAndAddToQueue(userId);
                    }
                });
    }

    /**
     * 활성 사용자 집합(active set)에 사용자를 추가한다.
     *
     * <p>active set은 현재 허용된 사용자 수를 O(1)에 가깝게 계산하기 위한 용도다.</p>
     *
     * @param userId 사용자 식별자
     * @return RegisterUserResponse(0)
     */
    private Mono<RegisterUserResponse> addToActiveSet(String userId) {
        return reactiveRedisTemplate.opsForSet()
                .add(USER_ACTIVE_SET_KEY, userId)
                .map(i -> new RegisterUserResponse(0L));
    }

    /**
     * 진행 큐(proceed)에서 일정 시간 이상 무활동인 사용자를 제거한다.
     *
     * <p>제거 대상:</p>
     * <ul>
     *   <li>현재 시간 - proceed 큐 score(마지막 활동 시간) > INACTIVITY_THRESHOLD</li>
     * </ul>
     *
     * <p>제거 작업:</p>
     * <ul>
     *   <li>proceed 큐에서 제거</li>
     *   <li>active set에서도 제거</li>
     * </ul>
     *
     * @return 완료 신호(Mono<Void>)
     */
    private Mono<Void> removeInactiveUsers() {
        long currentTime = Instant.now().getEpochSecond();
        return reactiveRedisTemplate.opsForZSet()
                .rangeWithScores(USER_QUEUE_PROCEED_KEY, Range.closed(0L, -1L))
                .filter(userWithScore -> currentTime - userWithScore.getScore() > INACTIVITY_THRESHOLD)
                .flatMap(userWithScore -> {
                    String userId = userWithScore.getValue();
                    return reactiveRedisTemplate.opsForZSet().remove(USER_QUEUE_PROCEED_KEY, userId)
                            .then(reactiveRedisTemplate.opsForSet().remove(USER_ACTIVE_SET_KEY, userId));
                })
                .then();
    }

    /**
     * 현재 활성 사용자 수를 기준으로 빈 슬롯을 계산하고, 그 수만큼 대기열에서 진행열로 승격시킨다.
     *
     * @return 승격된 사용자 수
     */
    private Mono<Long> allowUserTask() {
        return reactiveRedisTemplate.opsForSet().size(USER_ACTIVE_SET_KEY)
                .flatMap(activeUsers -> {
                    long slotsAvailable = MAX_ACTIVE_USERS - activeUsers;
                    if (slotsAvailable < 0) {
                        return Mono.just(0L);
                    }
                    return moveUserToProceeds(slotsAvailable);
                });
    }

    /**
     * 대기 큐(wait)에서 오래 기다린 사용자부터(count 만큼) 꺼내 진행 상태로 승격한다.
     *
     * <p>구현 방식:</p>
     * <ul>
     *   <li>wait 큐에서 popMin을 사용하여 score가 작은 사용자부터 꺼낸다(공정성)</li>
     *   <li>승격된 사용자는 활동 시간을 갱신하고(active set에 추가) "진행 허용" 상태로 만든다</li>
     * </ul>
     *
     * @param count 승격할 사용자 수
     * @return 실제 승격된 사용자 수
     */
    private Mono<Long> moveUserToProceeds(long count) {
        return reactiveRedisTemplate.opsForZSet()
                .popMin(USER_QUEUE_WAIT_KEY, count)
                .flatMap(user -> {
                    String userId = Objects.requireNonNull(user.getValue());
                    return updateUserActivityTime(userId)
                            .then(reactiveRedisTemplate.opsForSet().add(USER_ACTIVE_SET_KEY, userId));
                })
                .count();
    }
}
