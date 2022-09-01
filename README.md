# 키친포스

## 퀵 스타트

```sh
cd docker
docker compose -p kitchenpos up -d
```

## 요구 사항

- 제품
  - [ ] 제품을 등록한다.
    - [ ] 제품의 이름은 비어있을 수 없으며, 욕설, 외설 및 기타 원치 않는 용어에 해당할 수 없다. (필터링 기준 https://www.purgomalum.com)
    - [ ] 제품의 가격은 0원 이상이여야 한다.
  - [ ] 제품의 가격을 수정한다.
    - [ ] 수정할 가격은 0원 이상이여야 한다.
    - [ ] 해당 제품을 포함하는 메뉴의 가격이 (변경 된 구성품목 가격 x 구성품목의 개수)의 총합보다 클 경우 메뉴를 숨긴다.
  - [ ] 제품 목록을 조회한다.
- 메뉴
  - [ ] 메뉴를 등록한다.
    - [ ] 메뉴의 이름은 비어있을 수 없으며, 욕설, 외설 및 기타 원치 않는 용어에 해당할 수 없다. (필터링 기준 https://www.purgomalum.com)
    - [ ] 메뉴의 가격은 0원 이상이여야 한다.
    - [ ] 메뉴의 가격은 (구성품목의 가격 * 개수)의 총합보다 클 수 없다.
    - [ ] 메뉴는 메뉴 그룹에 속해야 한다.
    - [ ] 메뉴의 구성품목은 등록된 제품만 가능하다.
    - [ ] 메뉴의 구성품목의 개수는 0개 이상이여야 한다.
    - [ ] 메뉴의 노출여부를 등록한다.
  - [ ] 메뉴의 가격을 수정한다.
    - [ ] 수정하려는 가격이 0원 이상이여야 한다.
    - [ ] 수정하려는 가격은 (구성품목의 가격 * 개수)의 총합보다 클 수 없다.
  - [ ] 메뉴를 노출시킨다.
    - [ ] 메뉴를 노출할 때 메뉴의 가격은 (구성품목의 가격 * 개수)의 총합보다 클 수 없다.
  - [ ] 메뉴를 숨긴다.
  - [ ] 메뉴목록을 조회한다.
- 메뉴 그룹
  - [ ] 메뉴 그룹의 이름은 비어있을 수 없다.
- 테이블
  - [ ] 테이블을 등록한다.
    - [ ] 테이블의 이름은 비어있을 수 없다.
    - [ ] 테이블은 현재 앉아있는 손님의 인원을 가지고 있다.
    - [ ] 테이블은 할당여부를 가지고 있다.
  - [ ] 테이블이 사용중이라고 변경한다.
    - [ ] 테이블의 할당여부를 활성화한다.
  - [ ] 테이블이 비어있다고 변경한다.
    - [ ] 테이블의 주문상태가 완료이여야만 테이블을 비울 수 있다.
    - [ ] 테이블의 손님 수를 0으로 변경하고, 할당여부를 비활성화한다.
  - [ ] 테이블에 앉아있는 손님의 인원을 변경한다.
    - [ ] 손님의 인원을 설정할 수 있다.
    - [ ] 변경하려는 인원 수는 0명보다 작을 수 없다.
    - [ ] 사용되고 있는 테이블만 가능하다.
  - [ ] 테이블 목록을 조회한다.
- 주문
  - [ ] 주문을 등록한다.
    - [ ] 주문은 주문타입은 비어있을 수 없으며 배달, 포장, 매장식사 세가지 타입이 있다.
    - [ ] 주문한 메뉴들은 모두 등록된 메뉴만 가능하다.
    - [ ] 주문한 메뉴들은 모두 노출 중인 메뉴만 가능하다.
    - [ ] 주문한 메뉴의 가격은 메뉴에 있는 가격과 동일해야 한다.
    - [ ] 주문이 등록되면 주문의 상태는 대기 상태이다.
    - [ ] 배달 혹은 포장 주문의 경우, 주문한 메뉴들의 개수는 0개 이상이여야 한다.
    - [ ] 배달 주문의 경우, 주소 정보는 비어있을 수 없다.
    - [ ] 매장 식사 주문의 경우, 테이블이 지정되어 있어야만 한다.
  - [ ] 주문을 승인한다.
    - [ ] 주문 상태가 대기 상태이어야 한다.
    - [ ] 주문번호와 주문 건의 총합, 배달 주소를 라이더에게 요청한다.
    - [ ] 주문 승인이 되면 주문의 상태는 승인상태로 된다.
  - [ ] 주문한 제품을 서빙한다.
    - [ ] 주문을 서빙할 때는 주문 상태가 승인 상태이어야 한다.
    - [ ] 주문 서빙이 완료되면 주문의 상태는 서빙완료상태가 된다.
  - [ ] 주문한 제품을 배송한다.
    - [ ] 주문의 타입이 배송이여야 한다.
    - [ ] 주문상태가 서빙완료 상태이어야 한다.
    - [ ] 배송이 시작되면, 주문 상태는 배송중 상태가 된다.
  - [ ] 주문 배송을 완료한다.
    - [ ] 주문 배송을 완료하려면 주문의 상태는 배송중 이여야 한다.
    - [ ] 배송이 완료되면, 주문 상태는 배송완료 상태가 된다.
  - [ ] 주문을 완료한다.
    - [ ] 배송 주문의 경우, 주문의 상태가 배송완료 이어야만 한다.
    - [ ] 포장, 매장식사 주문의 경우, 주문의 상태가 서빙완료 이여야만 한다.
    - [ ] 주문이 완료되면, 주문의 상태는 완료가 된다.
    - [ ] 매장 식사 주문의 경우, 완료가 되면 식사한 테이블을 치운다.(테이블이 비어지고, 해당 테이블의 손님이 없음)
  - [ ] 주문목록을 조회한다.

## 용어 사전

| 한글명 | 영문명 | 설명 |
| --- | --- | --- |
|  |  |  |

## 모델링
