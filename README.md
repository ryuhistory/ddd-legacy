# 키친포스

## 퀵 스타트

```sh
cd docker
docker compose -p kitchenpos up -d
```

## 요구 사항

#### 메뉴
  - **상품**
    - [x] 상품을 등록한다.
      - 상품명과, 가격은 필수여야 한다.
      - 상품 가격은 0보다 커야 한다. ( 상품가격 > 0 )
      - 상품명은 비속어를 가질 수 없다. ( Purgomalum 서비스 활용 )
    - [x] 상품 가격을 변경한다.
      - 가격은 필수여야 하며, 0 보다 커야 한다. ( 상품가격 > 0 )
      - 메뉴에 기등록 된 상품인 경우 아래의 조건을 만족시키지 못할경우 상품을 표시 하지 않는다.
          - 상품가격 * 메뉴상품갯수 = 메뉴가격
    - 메뉴그룹 
      - [x] 메뉴 그룹을 등록한다.
        - 메뉴그룹 이름은 필수이다.
  - **메뉴**
    - [x] 메뉴를 등록한다.
      - 메뉴 가격, 메뉴에 등록할 상품정보, 메뉴 이름, 메뉴 종류는 필수로 입력 되어야 한다.
      - 상품정보는 메뉴 등록보다 선행되어야 한다.
      - 메뉴명은 비속어를 가질 수 없다.  ( Purgomalum 서비스 활용 )
      - 상품가격 * 메뉴상품갯수 = 메뉴가격 이다.
    - [x] 메뉴 가격을 변경한다.
      - 변경 할 가격은, 상품가격이 먼저 변경 되어 있어야 한다.
      - 상품가격 * 메뉴상품 = 변경할 메뉴 가격 이어야 한다.
    - [x] 메뉴를 표시 한다.
      - 상품가격 * 메뉴상품갯수 = 메뉴 가격 조건을 만족해야 활성화 할 수 있다.
    - [x] 메뉴를 표시하지 않는다.
    - [x] 메뉴를 전체 조회 한다.
#### 주문
  - **주문**
    - [x] 주문을 생성 한다.
      - 주문 종류, 주문 메뉴, 필수로 입력되어야 한다.
      - 주문메뉴, 메뉴에 등록되어 있어야 주문 가능하다.
      - 주문메뉴는 표시되어 있어야 한다.
      - 주문은 종류에 따라 주문상태를 달리한다.
        - **배달** : *대기* -> *수락* -> *서빙* -> *배달출발* -> *배달완료* -> *종료*
        - **포장**, **매장식사** : *대기* -> *수락* -> *서빙* -> *종료*
      - 메뉴가격 = 주문메뉴가격 이어야 한다. 
      - 주문종류가 매장에서 식사가 아니라면 수량 > 0 이어야 한다.
      - 주문 종류가 배달인경우 배달주소는 입력되어야 한다.
      - 주문종류가 매장식사인경우 매장테이블은 비어 있어야 한다.
    - [x] 주문을 수락 한다.
      - 요청한 주문은 **대기** 상태여야 한다.
      - 주문종류가 **배달** 인경우, 배달을 요청한다.
        - 배달 요청시 주문번호, 주문합계금액, 배달지 주소를 포함해야 한다.
      - 주문상태를 수락으로 변경한다.
    - [x] 주문을 서빙 한다.
      - 요청한 주문은 **수락** 상태여야 한다.
        - 주문종류가 배달 인경우 배달을 요청한다.
      - 주문 상태를 서빙으로 변경한다.
    - [x] 배달을 시작한다.
      - 주문종류는 **배달** 이어야 한다.
      - 주문상태는 **서빙** 상태여야 한다.
      - 주문상태를 배달중으로 변경한다.
    - [x] 배달을 완료 한다.
      - 주문상태는 **배달중** 이어야 한다.
      - 주문상태를 배달완료로 변경한다.
    - [x] 주문을 종료 한다.
      - 주문종류 = **배달** 인경우 주문상태는 **배달완료** 여야 한다.
      - 주문종류가 **포장**, **매장식사** 인경우 주문상태는 **서빙** 이어야 한다.
      - 주문상태를 종료로 변경한다.
      - 주문종류 = **매장** 인 경우
        - 매장테이블 상태를 공석으로 바꾼다.
        - 손님수를 0으로 바꾼다.
    - [x] 주문을 전체 조회한다.
  - **매장테이블**
    - [x] 매장 테이블을 만든다.
      - 테이블 이름은 필수여야 한다.
      - 테이블 손님 수 =0 으로 만든다.
      - 테이블을 공석으로 만든다.
    - [x] 매장 테이블에 손님이 앉는다.
      - 테이블은 기존에 등록 되어 있어야 한다.
      - 테이블이 점유되었다.
    - [x] 매장 테이블을 치운다.
      - 테이블은 기존에 등록 되어 있어야 한다.
      - 매장테이블에서 주문은 **종료** 되어 있여야 한다.
      - 테이블 손님 수 = 0 으로 만든다.
      - 테이블을 공석으로 만든다.
    - [x] 테이블에 앉은 손님 수를 변경한다.
      - 손님 수 > 0 이어야 한다.
      - 테이블은 기존에 등록 되어 있어야 한다.
      - 테이블 상태를 점유 되어 있어야 한다.
    - [x] 매장 테이블을 전체 조회한다.

## 용어 사전

| 한글명 | 영문명 | 설명 |
|-----|-----|----|
|     |     |    |

## 모델링

