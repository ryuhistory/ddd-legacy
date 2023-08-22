package kitchenpos.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuGroup;
import kitchenpos.domain.MenuRepository;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderLineItem;
import kitchenpos.domain.OrderRepository;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.OrderTableRepository;
import kitchenpos.domain.OrderType;
import kitchenpos.infra.KitchenridersClient;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MenuRepository menuRepository;
    @Mock
    private OrderTableRepository orderTableRepository;
    @Mock
    private KitchenridersClient kitchenridersClient;
    @Autowired
    private OrderService orderService;
    private UUID 돈가스_세트_메뉴id;
    private UUID 김밥_세트_메뉴id;

    @BeforeEach
    void setUp() {
        돈가스_세트_메뉴id = UUID.randomUUID();
        김밥_세트_메뉴id = UUID.randomUUID();

        orderService = new OrderService(orderRepository, menuRepository, orderTableRepository, kitchenridersClient);
    }

    @Nested
    @DisplayName("주문 생성")
    class create {
        @DisplayName("주문종류가 null 이면 오류가 발생한다.")
        @Test
        void nullOfOrderType() {
            //given
            Order order = new Order();

            //when
            //then
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> orderService.create(order));
        }

        @DisplayName("주문메뉴가 없으면 오류가 발생한다..")
        @Test
        void nullOfOrderLines() {
            //given
            Order order = new Order();
            order.setType(OrderType.EAT_IN);

            //when
            //then
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> orderService.create(order));
        }

        @DisplayName("주문메뉴에 있는 메뉴가, 메뉴에 등록되어 있지 않으면 오류가 발생한다.")
        @Test
        void notExistsMenu() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            Menu 김밥_세트 = new Menu(김밥_세트_메뉴id, "김밥_세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(12000));
            OrderLineItem 김밥_주문 = new OrderLineItem(김밥_세트, 1, 김밥_세트_메뉴id, BigDecimal.valueOf(10000));
            Order order = new Order();
            order.setType(OrderType.EAT_IN);
            order.setOrderLineItems(List.of(돈가스_주문, 김밥_주문));
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));

            //when
            //then
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> orderService.create(order));
        }

        @DisplayName("매장식사가 아닌경우에, 주문메뉴 양이 0보다 작으면 오류가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"DELIVERY", "TAKEOUT"})
        void negativeOfOrderLineQuantity(OrderType orderType) {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, -1, 돈가스_세트_메뉴id, BigDecimal.valueOf(12000));
            Order order = new Order();
            order.setType(orderType);
            order.setOrderLineItems(List.of(돈가스_주문));
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));

            //when
            //then
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> orderService.create(order));
        }

        @DisplayName("주문메뉴 전체 등록 검증에서는 통과 했으나, 개별 등록 이 되어 있는지 검증에서 실패하면 오류가 발생한다.")
        @Test
        void singleOrderLineMenuNotExists() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(12000));
            Order order = new Order();
            order.setType(OrderType.EAT_IN);
            order.setOrderLineItems(List.of(돈가스_주문));
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.empty());

            //when
            //then
            assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> orderService.create(order));
            then(menuRepository).should(times(1)).findAllByIdIn(any());
            then(menuRepository).should(times(1)).findById(돈가스_세트_메뉴id);
        }

        @DisplayName("주문메뉴에 있는 메뉴의 표시(display)가 false 인경우, 오류가 발생한다.")
        @Test
        void falseOfDisplayed() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), false);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(12000));
            Order order = new Order();
            order.setType(OrderType.EAT_IN);
            order.setOrderLineItems(List.of(돈가스_주문));
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.of(돈가스_세트));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.create(order));
            then(menuRepository).should(times(1)).findAllByIdIn(any());
            then(menuRepository).should(times(1)).findById(돈가스_세트_메뉴id);
        }

        @DisplayName("메뉴가격과 주문메뉴 가격이 다른경우 오류가 발생한다.")
        @Test
        void differentPrice() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(12001));
            Order order = new Order();
            order.setType(OrderType.EAT_IN);
            order.setOrderLineItems(List.of(돈가스_주문));
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.of(돈가스_세트));

            //when
            //then
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> orderService.create(order));
            then(menuRepository).should(times(1)).findAllByIdIn(any());
            then(menuRepository).should(times(1)).findById(돈가스_세트_메뉴id);
        }

        @DisplayName("배달 일때, 배달주소가 null 이면 오류가 발생한다.")
        @ParameterizedTest
        @NullSource
        void deliveryAddressIsNull(String address) {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(120000));
            Order order = new Order();
            order.setType(OrderType.DELIVERY);
            order.setOrderLineItems(List.of(돈가스_주문));
            order.setDeliveryAddress(address);
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.of(돈가스_세트));

            //when
            //then
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> orderService.create(order));
            then(menuRepository).should(times(1)).findAllByIdIn(any());
            then(menuRepository).should(times(1)).findById(돈가스_세트_메뉴id);
        }

        @DisplayName("배달 일때, 배달주소가 공백 이면 오류가 발생한다.")
        @Test
        void deliveryAddressIsNull() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(120000));
            Order order = new Order();
            order.setType(OrderType.DELIVERY);
            order.setOrderLineItems(List.of(돈가스_주문));
            order.setDeliveryAddress("");
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.of(돈가스_세트));

            //when
            //then
            assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> orderService.create(order));
            then(menuRepository).should(times(1)).findAllByIdIn(any());
            then(menuRepository).should(times(1)).findById(돈가스_세트_메뉴id);
        }

        @DisplayName("배달 일떄, 배달주소가 정상적으로 등록되었다.")
        @Test
        void normalRegisterDeliveryAddr() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(120000));
            Order order = new Order();
            order.setType(OrderType.DELIVERY);
            order.setOrderLineItems(List.of(돈가스_주문));
            order.setDeliveryAddress("addr");
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.of(돈가스_세트));
            given(orderRepository.save(any())).willReturn(order);

            //when
            Order returnOrder = orderService.create(order);

            //then
            assertThat(returnOrder.getDeliveryAddress()).isEqualTo("addr");
        }

        @DisplayName("매장식사 일때, 매장 테이블이 등록이 안되어 있으면 오류가 발생한다.")
        @Test
        void notExistsOrderTable() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(120000));
            Order order = new Order();
            order.setType(OrderType.EAT_IN);
            order.setOrderLineItems(List.of(돈가스_주문));
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.of(돈가스_세트));
            given(orderTableRepository.findById(any())).willReturn(Optional.empty());

            //when
            //then
            assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> orderService.create(order));
            then(menuRepository).should(times(1)).findAllByIdIn(any());
            then(menuRepository).should(times(1)).findById(돈가스_세트_메뉴id);
            then(orderTableRepository).should(times(1)).findById(any());
        }

        @DisplayName("매장식사 일떄, 손님이 자리에 앉지 않았으면 오류가 발생한다.")
        @Test
        void notSit() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(120000));
            Order order = new Order();
            order.setType(OrderType.EAT_IN);
            order.setOrderLineItems(List.of(돈가스_주문));
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.of(돈가스_세트));
            given(orderTableRepository.findById(any())).willReturn(
                Optional.of(new OrderTable(UUID.randomUUID(), "가_테이블")));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.create(order));
            then(menuRepository).should(times(1)).findAllByIdIn(any());
            then(menuRepository).should(times(1)).findById(돈가스_세트_메뉴id);
            then(orderTableRepository).should(times(1)).findById(any());
        }

        @DisplayName("매장식사 일떄, 손님이 자리에 앉지 앉으면 주문을 세팅한다.")
        @Test
        void normalEatIn() {
            //given
            UUID orderTableId = UUID.randomUUID();
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(120000));
            OrderTable orderTable = new OrderTable(orderTableId, "가_테이블");
            orderTable.setOccupied(true);
            Order order = new Order();
            order.setType(OrderType.EAT_IN);
            order.setOrderLineItems(List.of(돈가스_주문));
            order.setOrderTable(orderTable);
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.of(돈가스_세트));
            given(orderTableRepository.findById(any())).willReturn(
                Optional.of(orderTable));
            given(orderRepository.save(any())).willReturn(order);

            //when
            Order returnOrder = orderService.create(order);

            //then
            assertThat(returnOrder.getOrderTable().getId()).isEqualTo(orderTableId);

        }

        @DisplayName("주문이 정상 처리된다.")
        @Test
        void normalCreate() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(120000));
            Order order = new Order();
            UUID orderId = UUID.randomUUID();
            order.setId(orderId);
            order.setType(OrderType.TAKEOUT);
            order.setOrderLineItems(List.of(돈가스_주문));
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(돈가스_세트));
            given(menuRepository.findById(돈가스_세트_메뉴id)).willReturn(Optional.of(돈가스_세트));
            given(orderRepository.save(any())).willReturn(order);

            //when
            Order realReturn = orderService.create(order);

            //then
            assertThat(realReturn.getId()).isEqualTo(order.getId());
            then(menuRepository).should(times(1)).findAllByIdIn(any());
            then(menuRepository).should(times(1)).findById(돈가스_세트_메뉴id);
            then(orderRepository).should(times(1)).save(any());
        }

    }

    @Nested
    @DisplayName("주문을 수락한다.")
    class accept {
        @DisplayName("주문번호로 등록된 주문을 찾을 수 없어서 오류가 발생한다.")
        @Test
        void notExistsOrder() {
            //given
            UUID orderId = UUID.randomUUID();
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            //when
            //then
            assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> orderService.accept(orderId));
        }

        @DisplayName("주문 상태가 WAITING 이 아니면 오류가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"ACCEPTED", "SERVED", "DELIVERING", "DELIVERED", "COMPLETED"})
        void notCorrectOrderStatus(OrderStatus orderStatus) {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(orderStatus);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.accept(orderId));
        }

        @DisplayName("주문종류가 배달인경우, 배달요청을 1번 한다.")
        @Test
        void requestDelivery() {
            //given
            Menu 돈가스_세트 = new Menu(돈가스_세트_메뉴id, "돈가스세트", BigDecimal.valueOf(120000), new MenuGroup(), true);
            OrderLineItem 돈가스_주문 = new OrderLineItem(돈가스_세트, 1, 돈가스_세트_메뉴id, BigDecimal.valueOf(120000));
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(OrderStatus.WAITING);
            order.setType(OrderType.DELIVERY);
            order.setOrderLineItems(List.of(돈가스_주문));
            order.setDeliveryAddress("addr");
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            orderService.accept(orderId);

            //then
            then(kitchenridersClient).should(times(1))
                .requestDelivery(orderId, BigDecimal.valueOf(120000), "addr");
        }

        @DisplayName("주문이 정상적으로 수락된다.")
        @Test
        void normalAccept() {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(OrderStatus.WAITING);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            Order returnOrder = orderService.accept(orderId);

            //then
            assertThat(returnOrder.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }
    }

    @Nested
    @DisplayName("주문을 서빙한다.")
    class serve {
        @DisplayName("주문번호로 등록된 주문을 찾을 수 없어서 오류가 발생한다.")
        @Test
        void notExistsOrder() {
            //given
            UUID orderId = UUID.randomUUID();
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            //when
            //then
            assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> orderService.serve(orderId));
        }

        @DisplayName("주문 상태가 ACCEPT가 아니면 오류가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"WAITING", "SERVED", "DELIVERING", "DELIVERED", "COMPLETED"})
        void orderStatusNotAccept(OrderStatus orderStatus) {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(orderStatus);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.serve(orderId));
        }

        @DisplayName("주문 상태가 SERVED로 정상 변경된다.")
        @Test
        void normalServe() {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(OrderStatus.ACCEPTED);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            Order returnOrder = orderService.serve(orderId);

            //then
            assertThat(returnOrder.getStatus()).isEqualTo(OrderStatus.SERVED);
        }
    }

    @Nested
    @DisplayName("주문을 시작한다.")
    class startDelivery {
        @DisplayName("주문번호로 등록된 주문을 찾을 수 없어서 오류가 발생한다.")
        @Test
        void notExistsOrder() {
            //given
            UUID orderId = UUID.randomUUID();
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            //when
            //then
            assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> orderService.startDelivery(orderId));
        }

        @DisplayName("주문 종류가 배달이 아니면 오류가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"TAKEOUT", "EAT_IN"})
        void orderStatusNotAccept(OrderType orderType) {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setType(orderType);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.startDelivery(orderId));
        }

        @DisplayName("주문 상태가 SERVED가 아니면 오류가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"WAITING", "ACCEPTED", "DELIVERING", "DELIVERED", "COMPLETED"})
        void orderStatusNotAccept(OrderStatus orderStatus) {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(orderStatus);
            order.setType(OrderType.DELIVERY);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.startDelivery(orderId));
        }

        @DisplayName("주문 상태가 DELIVERING로 정상 변경된다.")
        @Test
        void normalServe() {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(OrderStatus.SERVED);
            order.setType(OrderType.DELIVERY);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            Order returnOrder = orderService.startDelivery(orderId);

            //then
            assertThat(returnOrder.getStatus()).isEqualTo(OrderStatus.DELIVERING);
        }
    }

    @Nested
    @DisplayName("배달을 종료한다.")
    class completeDelivery {

        @DisplayName("주문번호로 등록된 주문을 찾을 수 없어서 오류가 발생한다.")
        @Test
        void notExistsOrder() {
            //given
            UUID orderId = UUID.randomUUID();
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            //when
            //then
            assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> orderService.completeDelivery(orderId));
        }

        @DisplayName("주문 상태가 DELIVERING가 아니면 오류가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"WAITING", "ACCEPTED", "SERVED", "DELIVERED", "COMPLETED"})
        void orderStatusNotAccept(OrderStatus orderStatus) {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(orderStatus);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.completeDelivery(orderId));
        }

        @DisplayName("배달이 정상적으로 완료 되었다.")
        @Test
        void normalCompleteDelivery() {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(OrderStatus.DELIVERING);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            Order returnOrder = orderService.completeDelivery(orderId);

            //then
            assertThat(returnOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }
    }

    @Nested
    @DisplayName("주문을 완료 한다.")
    class complete {
        @DisplayName("주문번호로 등록된 주문을 찾을 수 없어서 오류가 발생한다.")
        @Test
        void notExistsOrder() {
            //given
            UUID orderId = UUID.randomUUID();
            given(orderRepository.findById(orderId)).willReturn(Optional.empty());

            //when
            //then
            assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> orderService.complete(orderId));
        }

        @DisplayName("주문구분이 배달인데, 주문 상태가 DELIVERED가 아니면 오류가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"WAITING", "ACCEPTED", "DELIVERING", "SERVED", "COMPLETED"})
        void orderStatusNotDELIVERED(OrderStatus orderStatus) {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(orderStatus);
            order.setType(OrderType.DELIVERY);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.complete(orderId));
        }

        @DisplayName("주문구분이 포장인데, 주문 상태가 SERVED가 아니면 오류가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"WAITING", "ACCEPTED", "DELIVERING", "DELIVERED", "COMPLETED"})
        void orderStatusNotSERVED1(OrderStatus orderStatus) {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(orderStatus);
            order.setType(OrderType.TAKEOUT);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.complete(orderId));
        }

        @DisplayName("주문구분이 매장식사인데, 주문 상태가 SERVED가 아니면 오류가 발생한다.")
        @ParameterizedTest
        @ValueSource(strings = {"WAITING", "ACCEPTED", "DELIVERING", "DELIVERED", "COMPLETED"})
        void orderStatusNotSERVED2(OrderStatus orderStatus) {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(orderStatus);
            order.setType(OrderType.EAT_IN);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            //then
            assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> orderService.complete(orderId));
        }

        @DisplayName("주문구분이 매장식사인데, 매장테이블에 앉아 있는 손님중에 주문완료가 아니라면 매장테이블을 치운다.")
        @Test
        void cleanTable1() {
            //given
            OrderTable orderTable = new OrderTable(UUID.randomUUID(), "가_테이블");
            orderTable.setOccupied(true);
            orderTable.setNumberOfGuests(3);
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setType(OrderType.EAT_IN);
            order.setStatus(OrderStatus.SERVED);
            order.setOrderTable(orderTable);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(orderRepository.existsByOrderTableAndStatusNot(any(), any())).willReturn(false);

            //when
            Order returnOrder = orderService.complete(orderId);

            //then
            assertThat(returnOrder.getOrderTable().isOccupied()).isFalse();
            assertThat(returnOrder.getOrderTable().getNumberOfGuests()).isSameAs(0);
        }

        @DisplayName("주문구분이 매장식사인데, 매장테이블에 앉아 있는 손님이 없으면 아무것도 하지 않는다.")
        @Test
        void cleanTable12() {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setType(OrderType.EAT_IN);
            order.setStatus(OrderStatus.SERVED);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
            given(orderRepository.existsByOrderTableAndStatusNot(any(), any())).willReturn(true);

            //when
            Order returnOrder = orderService.complete(orderId);

            //then
            assertThat(returnOrder.getOrderTable()).isNull();
        }

        @DisplayName("주문이 정산 완료 처리된다.")
        @Test
        void normalComplete() {
            //given
            UUID orderId = UUID.randomUUID();
            Order order = new Order();
            order.setStatus(OrderStatus.SERVED);
            order.setType(OrderType.TAKEOUT);
            given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

            //when
            Order returnOrder = orderService.complete(orderId);

            //then
            assertThat(returnOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

    }

    @DisplayName("전체 주문 조회가 정상적으로 됐다.")
    @Test
    void findAll() {
        //given
        Order order1 = new Order();
        Order order2 = new Order();
        Order order3 = new Order();
        given(orderRepository.findAll()).willReturn(List.of(order1, order2, order3));

        //when
        List<Order> orders = orderService.findAll();

        //then
        assertThat(orders).containsOnly(order1, order2, order3);
    }
}