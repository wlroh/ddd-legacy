package kitchenpos.application;

import kitchenpos.fixture.MenuFixture;
import kitchenpos.fixture.OrderLineItemFixture;
import kitchenpos.fixture.OrderFixture;
import kitchenpos.fixture.OrderTableFixture;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuRepository;
import kitchenpos.domain.Order;
import kitchenpos.domain.OrderLineItem;
import kitchenpos.domain.OrderRepository;
import kitchenpos.domain.OrderStatus;
import kitchenpos.domain.OrderTable;
import kitchenpos.domain.OrderTableRepository;
import kitchenpos.domain.OrderType;
import kitchenpos.infra.KitchenridersClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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

    @InjectMocks
    private OrderService orderService;

    @DisplayName("주문을 등록")
    @Nested
    class CreateTest {

        @DisplayName("주문타입은 null 일 수 없다.")
        @Test
        void null_type() {
            // given
            final Order request = OrderFixture.createRequest(null);

            // then
            assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "주문 내역은 비어있을 수 없다. orderLineItems={0}")
        @NullAndEmptySource
        void null_or_empty_orderLineItems(List<OrderLineItem> orderLineItems) {
            // given
            final Order request = OrderFixture.createRequest(OrderType.EAT_IN);
            request.setOrderLineItems(orderLineItems);

            // then
            assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("주문 내역의 메뉴들은 모두 등록된 메뉴여야 한다.")
        @Test
        void contain_not_created_menu() {
            // given
            final Order request = OrderFixture.createRequest(OrderType.EAT_IN);
            given(menuRepository.findAllByIdIn(any())).willReturn(Collections.emptyList());

            // then
            assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("주문 내역의 메뉴들은 모두 개시된 메뉴여야 한다.")
        @Test
        void contain_hidden_menu() {
            // given
            final Order request = OrderFixture.createRequest(OrderType.EAT_IN);
            final Menu menu = MenuFixture.createDefault();
            menu.setDisplayed(false);
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(menu));
            given(menuRepository.findById(any())).willReturn(Optional.of(menu));

            // then
            assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("주문 내역의 가격은 메뉴에 있는 가격과 동일해야 한다.")
        @Test
        void same_menuPrice_and_orderLineItemsPrice() {
            // given
            final Order request = OrderFixture.createRequest(OrderType.EAT_IN);
            final OrderLineItem orderLineItemRequest = OrderLineItemFixture.createRequest();
            orderLineItemRequest.setPrice(BigDecimal.valueOf(10_000));
            request.setOrderLineItems(List.of(orderLineItemRequest));
            final Menu menu = MenuFixture.createDefault();
            given(menuRepository.findAllByIdIn(any())).willReturn(List.of(menu));
            given(menuRepository.findById(any())).willReturn(Optional.of(menu));

            // then
            assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("매장 식사 주문")
        @Nested
        class EatIn {

            @DisplayName("등록 성공")
            @Test
            void create() {
                // given
                final Order request = OrderFixture.createRequest(OrderType.EAT_IN);
                final OrderTable orderTable = OrderTableFixture.createUsedTable();
                final Menu menu = MenuFixture.createDefault();
                given(menuRepository.findAllByIdIn(any())).willReturn(List.of(menu));
                given(menuRepository.findById(any())).willReturn(Optional.of(menu));
                given(orderTableRepository.findById(any())).willReturn(Optional.of(orderTable));
                given(orderRepository.save(any())).will(AdditionalAnswers.returnsFirstArg());

                // when
                final Order result = orderService.create(request);

                assertAll(() -> {
                    assertThat(result.getId()).isNotNull();
                    assertThat(result.getType()).isEqualTo(OrderType.EAT_IN);
                    assertThat(result.getStatus()).isEqualTo(OrderStatus.WAITING);
                    assertThat(result.getOrderDateTime()).isNotNull();
                    assertThat(result.getOrderLineItems().size()).isNotZero();
                    assertThat(result.getOrderTable()).isNotNull();
                });
            }

            @DisplayName("사용중인 테이블이 지정되어 있어야 한다.")
            @Test
            void not_occupied_table() {
                // given
                final Order request = OrderFixture.createRequest(OrderType.EAT_IN);
                final OrderTable orderTable = OrderTableFixture.create("1번", 0, false);
                final Menu menu = MenuFixture.createDefault();
                given(menuRepository.findAllByIdIn(any())).willReturn(List.of(menu));
                given(menuRepository.findById(any())).willReturn(Optional.of(menu));
                given(orderTableRepository.findById(any())).willReturn(Optional.of(orderTable));

                // then
                assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(IllegalStateException.class);
            }
        }

        @DisplayName("포장 주문")
        @Nested
        class Takeout {

            @DisplayName("등록 성공")
            @Test
            void create() {
                // given
                final Order request = OrderFixture.createRequest(OrderType.TAKEOUT);
                final OrderTable orderTable = OrderTableFixture.createUsedTable();
                final Menu menu = MenuFixture.createDefault();
                given(menuRepository.findAllByIdIn(any())).willReturn(List.of(menu));
                given(menuRepository.findById(any())).willReturn(Optional.of(menu));
                given(orderRepository.save(any())).will(AdditionalAnswers.returnsFirstArg());

                // when
                final Order result = orderService.create(request);

                // then
                assertAll(() -> {
                    assertThat(result.getId()).isNotNull();
                    assertThat(result.getType()).isEqualTo(OrderType.TAKEOUT);
                    assertThat(result.getStatus()).isEqualTo(OrderStatus.WAITING);
                    assertThat(result.getOrderDateTime()).isNotNull();
                    assertThat(result.getOrderLineItems().size()).isNotZero();
                });
            }

            @DisplayName("주문 내역의 메뉴 개수는 0개 이상이여야 한다.")
            @Test
            void negative_menu_quantity() {
                // given
                final Order request = OrderFixture.createRequest(OrderType.TAKEOUT);
                final OrderLineItem orderLineItemRequest = OrderLineItemFixture.createRequest();
                orderLineItemRequest.setQuantity(-1L);
                request.setOrderLineItems(List.of(orderLineItemRequest));
                given(menuRepository.findAllByIdIn(any())).willReturn(List.of(MenuFixture.createDefault()));

                // then
                assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(IllegalArgumentException.class);
            }
        }

        @DisplayName("배달 주문")
        @Nested
        class Delivery {

            @DisplayName("등록 성공")
            @Test
            void create() {
                // given
                final Order request = OrderFixture.createRequest(OrderType.DELIVERY);
                final OrderTable orderTable = OrderTableFixture.createUsedTable();
                final Menu menu = MenuFixture.createDefault();
                given(menuRepository.findAllByIdIn(any())).willReturn(List.of(menu));
                given(menuRepository.findById(any())).willReturn(Optional.of(menu));
                given(orderRepository.save(any())).will(AdditionalAnswers.returnsFirstArg());

                // when
                final Order result = orderService.create(request);

                // then
                assertAll(() -> {
                    assertThat(result.getId()).isNotNull();
                    assertThat(result.getType()).isEqualTo(OrderType.DELIVERY);
                    assertThat(result.getStatus()).isEqualTo(OrderStatus.WAITING);
                    assertThat(result.getOrderDateTime()).isNotNull();
                    assertThat(result.getOrderLineItems().size()).isNotZero();
                    assertThat(result.getDeliveryAddress()).isNotNull();
                });
            }

            @DisplayName("주문 내역의 메뉴 개수는 0개 이상이여야 한다.")
            @Test
            void negative_menu_quantity() {
                // given
                final Order request = OrderFixture.createRequest(OrderType.DELIVERY);
                final OrderLineItem orderLineItemRequest = OrderLineItemFixture.createRequest();
                orderLineItemRequest.setQuantity(-1L);
                request.setOrderLineItems(List.of(orderLineItemRequest));
                given(menuRepository.findAllByIdIn(any())).willReturn(List.of(MenuFixture.createDefault()));

                // then
                assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(IllegalArgumentException.class);
            }

            @ParameterizedTest(name = "주소지가 비어있을 수 없다. deliveryAddress={0}")
            @NullAndEmptySource
            void null_or_empty_address(String deliveryAddress) {
                // given
                final Order request = OrderFixture.createRequest(OrderType.DELIVERY);
                request.setDeliveryAddress(deliveryAddress);
                final Menu menu = MenuFixture.createDefault();
                given(menuRepository.findAllByIdIn(any())).willReturn(List.of(menu));
                given(menuRepository.findById(any())).willReturn(Optional.of(menu));

                // then
                assertThatThrownBy(() -> orderService.create(request)).isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @DisplayName("주문을 승인한다.")
    @Nested
    class AcceptTest {

        @DisplayName("승인 성공")
        @Test
        void accept() {
            // given
            final Order order = OrderFixture.create(OrderType.EAT_IN, OrderStatus.WAITING);
            given(orderRepository.findById(any())).willReturn(Optional.of(order));

            // when
            Order result = orderService.accept(order.getId());

            // then
            assertThat(result.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }

        @DisplayName("주문 상태가 대기 상태이여야한다.")
        @Test
        void order_status_not_waiting() {
            // given
            final Order order = OrderFixture.create(OrderType.EAT_IN, OrderStatus.ACCEPTED);
            given(orderRepository.findById(any())).willReturn(Optional.of(order));

            // then
            assertThatThrownBy(() -> orderService.accept(order.getId())).isInstanceOf(IllegalStateException.class);
        }
    }

    @DisplayName("주문한 제품을 서빙한다.")
    @Nested
    class ServeTest {

        @DisplayName("서빙 성공")
        @Test
        void serve() {
            // given
            final Order order = OrderFixture.create(OrderType.EAT_IN, OrderStatus.ACCEPTED);
            given(orderRepository.findById(any())).willReturn(Optional.of(order));

            // when
            Order result = orderService.serve(order.getId());

            // then
            assertThat(result.getStatus()).isEqualTo(OrderStatus.SERVED);
        }

        @DisplayName("주문 상태가 승인 상태이여야한다.")
        @Test
        void order_status_not_accepted() {
            // given
            final Order order = OrderFixture.create(OrderType.EAT_IN, OrderStatus.SERVED);
            given(orderRepository.findById(any())).willReturn(Optional.of(order));

            // then
            assertThatThrownBy(() -> orderService.serve(order.getId())).isInstanceOf(IllegalStateException.class);
        }
    }

    @DisplayName("주문한 제품을 배송한다.")
    @Nested
    class StartDeliveryTest {

        @DisplayName("배송 성공")
        @Test
        void startDelivery() {
            // given
            final Order order = OrderFixture.create(OrderType.DELIVERY, OrderStatus.SERVED);
            given(orderRepository.findById(any())).willReturn(Optional.of(order));

            // when
            Order result = orderService.startDelivery(order.getId());

            // then
            assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERING);
        }

        @DisplayName("주문의 타입이 배송이여야 한다.")
        @Test
        void order_type_not_delivery() {
            // given
            final Order order = OrderFixture.create(OrderType.EAT_IN, OrderStatus.SERVED);
            given(orderRepository.findById(any())).willReturn(Optional.of(order));

            // then
            assertThatThrownBy(() -> orderService.startDelivery(order.getId())).isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("주문 상태가 서빙완료 상태이여야한다.")
        @Test
        void order_status_not_served() {
            // given
            final Order order = OrderFixture.create(OrderType.DELIVERY, OrderStatus.ACCEPTED);
            given(orderRepository.findById(any())).willReturn(Optional.of(order));

            // then
            assertThatThrownBy(() -> orderService.startDelivery(order.getId())).isInstanceOf(IllegalStateException.class);
        }
    }

    @DisplayName("주문 배송을 완료한다.")
    @Nested
    class CompleteDeliveryTest {

        @DisplayName("배송 완료 성공")
        @Test
        void completedDelivery() {
            // given
            final Order order = OrderFixture.create(OrderType.DELIVERY, OrderStatus.DELIVERING);
            given(orderRepository.findById(any())).willReturn(Optional.of(order));

            // when
            Order result = orderService.completeDelivery(order.getId());

            // then
            assertThat(result.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        }

        @DisplayName("주문 상태가 배송중 상태이여야한다.")
        @Test
        void order_status_not_delivering() {
            // given
            final Order order = OrderFixture.create(OrderType.DELIVERY, OrderStatus.SERVED);
            given(orderRepository.findById(any())).willReturn(Optional.of(order));

            // then
            assertThatThrownBy(() -> orderService.completeDelivery(order.getId())).isInstanceOf(IllegalStateException.class);
        }
    }

    @DisplayName("주문을 완료한다.")
    @Nested
    class CompleteTest {

        @DisplayName("매장식사")
        @Nested
        class EatIn {

            @DisplayName("완료 성공")
            @Test
            void completed() {
                // given
                final Order order = OrderFixture.create(OrderType.EAT_IN, OrderStatus.SERVED);
                given(orderRepository.findById(any())).willReturn(Optional.of(order));
                given(orderRepository.existsByOrderTableAndStatusNot(any(), any())).willReturn(false);

                // when
                Order result = orderService.complete(order.getId());

                // then
                assertAll(() -> {
                    assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
                    assertThat(result.getOrderTable().getNumberOfGuests()).isZero();
                    assertThat(result.getOrderTable().isOccupied()).isFalse();
                });
            }

            @DisplayName("주문 상태가 서빙완료 상태이여야한다.")
            @Test
            void order_status_not_served() {
                // given
                final Order order = OrderFixture.create(OrderType.EAT_IN, OrderStatus.ACCEPTED);
                given(orderRepository.findById(any())).willReturn(Optional.of(order));

                // then
                assertThatThrownBy(() -> orderService.complete(order.getId())).isInstanceOf(IllegalStateException.class);
            }
        }

        @DisplayName("포장")
        @Nested
        class Takeout {

            @DisplayName("완료 성공")
            @Test
            void completed() {
                // given
                final Order order = OrderFixture.create(OrderType.TAKEOUT, OrderStatus.SERVED);
                given(orderRepository.findById(any())).willReturn(Optional.of(order));

                // when
                Order result = orderService.complete(order.getId());

                // then
                assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            }

            @DisplayName("주문 상태가 서빙완료 상태이여야한다.")
            @Test
            void order_status_not_served() {
                // given
                final Order order = OrderFixture.create(OrderType.TAKEOUT, OrderStatus.ACCEPTED);
                given(orderRepository.findById(any())).willReturn(Optional.of(order));

                // then
                assertThatThrownBy(() -> orderService.complete(order.getId())).isInstanceOf(IllegalStateException.class);
            }
        }

        @DisplayName("배달")
        @Nested
        class Delivery {

            @DisplayName("완료 성공")
            @Test
            void completed() {
                // given
                final Order order = OrderFixture.create(OrderType.DELIVERY, OrderStatus.DELIVERED);
                given(orderRepository.findById(any())).willReturn(Optional.of(order));

                // when
                Order result = orderService.complete(order.getId());

                // then
                assertThat(result.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            }

            @DisplayName("주문 상태가 배송완료 상태이여야한다.")
            @Test
            void order_status_not_delivered() {
                // given
                final Order order = OrderFixture.create(OrderType.DELIVERY, OrderStatus.DELIVERING);
                given(orderRepository.findById(any())).willReturn(Optional.of(order));

                // then
                assertThatThrownBy(() -> orderService.complete(order.getId())).isInstanceOf(IllegalStateException.class);
            }
        }
    }
}
