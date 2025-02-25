package kitchenpos.application;

import kitchenpos.fixture.MenuGroupFixture;
import kitchenpos.fixture.MenuProductFixture;
import kitchenpos.fixture.MenuFixture;
import kitchenpos.fixture.ProductFixture;
import kitchenpos.domain.Menu;
import kitchenpos.domain.MenuGroupRepository;
import kitchenpos.domain.MenuRepository;
import kitchenpos.domain.Product;
import kitchenpos.domain.ProductRepository;
import kitchenpos.infra.PurgomalumClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    MenuRepository menuRepository;

    @Mock
    MenuGroupRepository menuGroupRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    PurgomalumClient purgomalumClient;

    @InjectMocks
    MenuService menuService;

    @DisplayName("메뉴 등록")
    @Nested
    class CreateTest {

        @DisplayName("등록 성공")
        @Test
        void createdMenu() {
            // given
            final Menu request = MenuFixture.createRequest(
                    "후라이드 치킨",
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault().getId(),
                    List.of(MenuProductFixture.createRequest(1L))
            );
            final Product product = ProductFixture.createDefault();
            given(menuGroupRepository.findById(any())).willReturn(Optional.of(MenuGroupFixture.createDefault()));
            given(productRepository.findAllByIdIn(any())).willReturn(List.of(product));
            given(productRepository.findById(any())).willReturn(Optional.of(product));
            given(purgomalumClient.containsProfanity(anyString())).willReturn(Boolean.FALSE);
            given(menuRepository.save(any())).will(AdditionalAnswers.returnsFirstArg());

            // when
            final Menu result = menuService.create(request);

            // then
            assertAll(() -> {
                assertThat(result.getId()).isNotNull();
                assertThat(result.getName()).isEqualTo("후라이드 치킨");
                assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(15_000));
                assertThat(result.getMenuGroup()).isNotNull();
                assertThat(result.getMenuProducts().size()).isEqualTo(1);
            });
        }

        @DisplayName("가격은 0원 이상이여야 한다.")
        @Test
        void negative_price() {
            // given
            final Menu request = MenuFixture.createRequest(
                    "후라이드 치킨",
                    BigDecimal.valueOf(-15_000),
                    true,
                    MenuGroupFixture.createDefault().getId(),
                    List.of(MenuProductFixture.createRequest(1L))
            );

            // then
            assertThatThrownBy(() -> menuService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("메뉴그룹에 속해야 한다.")
        @Test
        void not_contain_menuGroup() {
            // given
            final Menu request = MenuFixture.createRequest(
                    "후라이드 치킨",
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault().getId(),
                    List.of(MenuProductFixture.createRequest(1L))
            );
            given(menuGroupRepository.findById(any())).willReturn(Optional.empty());

            // then
            assertThatThrownBy(() -> menuService.create(request)).isInstanceOf(NoSuchElementException.class);
        }

        @DisplayName("구성품목은 비어있을 수 없다.")
        @Test
        void empty_menuProduct() {
            // given
            final Menu request = MenuFixture.createRequest(
                    "후라이드 치킨",
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault().getId(),
                    Collections.emptyList()
            );
            given(menuGroupRepository.findById(any())).willReturn(Optional.of(MenuGroupFixture.createDefault()));

            // then
            assertThatThrownBy(() -> menuService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("구성품목은 null 일 수 없다.")
        @Test
        void null_menuProduct() {
            // given
            final Menu request = MenuFixture.createRequest(
                    "후라이드 치킨",
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault().getId(),
                    null
            );
            given(menuGroupRepository.findById(any())).willReturn(Optional.of(MenuGroupFixture.createDefault()));

            // then
            assertThatThrownBy(() -> menuService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("구성품목은 등록된 제품만 가능하다.")
        @Test
        void not_created_product() {
            // given
            final Menu request = MenuFixture.createRequest(
                    "후라이드 치킨",
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault().getId(),
                    List.of(MenuProductFixture.createRequest(1L))
            );
            given(menuGroupRepository.findById(any())).willReturn(Optional.of(MenuGroupFixture.createDefault()));
            given(productRepository.findAllByIdIn(any())).willReturn(Collections.emptyList());

            // then
            assertThatThrownBy(() -> menuService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("구성품목의 개수는 0개 이상이여야 한다.")
        @Test
        void negative_menuProduct_quantity() {
            // given
            final Menu request = MenuFixture.createRequest(
                    "후라이드 치킨",
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault().getId(),
                    List.of(MenuProductFixture.createRequest(-1L))
            );
            given(menuGroupRepository.findById(any())).willReturn(Optional.of(MenuGroupFixture.createDefault()));
            given(productRepository.findAllByIdIn(any())).willReturn(List.of(ProductFixture.createDefault()));

            // then
            assertThatThrownBy(() -> menuService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("가격은 (구성품목의 가격 * 개수)의 총합보다 클 수 없다.")
        @Test
        void over_than_product_price() {
            // given
            final Menu request = MenuFixture.createRequest(
                    "후라이드 치킨",
                    BigDecimal.valueOf(30_000),
                    true,
                    MenuGroupFixture.createDefault().getId(),
                    List.of(MenuProductFixture.createRequest(1L))
            );
            final Product product = ProductFixture.createDefault();
            given(menuGroupRepository.findById(any())).willReturn(Optional.of(MenuGroupFixture.createDefault()));
            given(productRepository.findAllByIdIn(any())).willReturn(List.of(product));
            given(productRepository.findById(any())).willReturn(Optional.of(product));

            // then
            assertThatThrownBy(() -> menuService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("이름은 비어있을 수 없다.")
        @Test
        void null_name() {
            // given
            final Menu request = MenuFixture.createRequest(
                    null,
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault().getId(),
                    List.of(MenuProductFixture.createRequest(1L))
            );
            final Product product = ProductFixture.createDefault();
            given(menuGroupRepository.findById(any())).willReturn(Optional.of(MenuGroupFixture.createDefault()));
            given(productRepository.findAllByIdIn(any())).willReturn(List.of(product));
            given(productRepository.findById(any())).willReturn(Optional.of(product));

            // then
            assertThatThrownBy(() -> menuService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("이름은 욕설, 외설 및 기타 원치 않는 용어에 해당할 수 없다.")
        @Test
        void invalid_name() {
            // given
            final Menu request = MenuFixture.create(
                    "비속어",
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault(),
                    List.of(MenuProductFixture.createDefault())
            );
            given(menuGroupRepository.findById(any())).willReturn(Optional.of(request.getMenuGroup()));
            given(productRepository.findAllByIdIn(any())).willReturn(List.of(ProductFixture.createDefault()));
            given(productRepository.findById(any())).willReturn(Optional.of(ProductFixture.createDefault()));
            given(purgomalumClient.containsProfanity(anyString())).willReturn(Boolean.TRUE);

            // then
            assertThatThrownBy(() -> menuService.create(request)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @DisplayName("메뉴의 가격 수정")
    @Nested
    class ChangePriceTest {

        @DisplayName("수정 성공")
        @Test
        void changed_price() {
            // given
            final Menu request = MenuFixture.create(
                    "후라이드 치킨",
                    BigDecimal.valueOf(10_000),
                    true,
                    MenuGroupFixture.createDefault(),
                    List.of(MenuProductFixture.createDefault())
            );
            final Menu findMenu = MenuFixture.create(
                    "후라이드 치킨",
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault(),
                    List.of(MenuProductFixture.createDefault())
            );
            given(menuRepository.findById(any())).willReturn(Optional.of(findMenu));

            // when
            final Menu result = menuService.changePrice(request.getId(), request);

            // then
            assertThat(result.getPrice()).isEqualTo(BigDecimal.valueOf(10_000));
        }


        @DisplayName("수정하려는 가격은 null 이면 안된다.")
        @Test
        void null_price() {
            // given
            final Menu request = MenuFixture.create(
                    "후라이드 치킨",
                    null,
                    true,
                    MenuGroupFixture.createDefault(),
                    List.of(MenuProductFixture.createDefault())
            );

            // then
            assertThatThrownBy(() -> menuService.changePrice(request.getId(), request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("수정하려는 가격은 0원 이상이여야 한다.")
        @Test
        void negative_price() {
            // given
            final Menu request = MenuFixture.create(
                    "후라이드 치킨",
                    BigDecimal.valueOf(-1),
                    true,
                    MenuGroupFixture.createDefault(),
                    List.of(MenuProductFixture.createDefault())
            );

            // then
            assertThatThrownBy(() -> menuService.changePrice(request.getId(), request)).isInstanceOf(IllegalArgumentException.class);
        }

        @DisplayName("수정하려는 가격은 (구성품목의 가격 * 개수)의 총합보다 클 수 없다.")
        @Test
        void over_than_product_price() {
            // given
            final Menu request = MenuFixture.create(
                    "후라이드 치킨",
                    BigDecimal.valueOf(30_000),
                    true,
                    MenuGroupFixture.createDefault(),
                    List.of(MenuProductFixture.createDefault())
            );
            final Menu findMenu = MenuFixture.create(
                    "후라이드 치킨",
                    BigDecimal.valueOf(15_000),
                    true,
                    MenuGroupFixture.createDefault(),
                    List.of(MenuProductFixture.createDefault())
            );
            given(menuRepository.findById(any())).willReturn(Optional.of(findMenu));

            // then
            assertThatThrownBy(() -> menuService.changePrice(request.getId(), request)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @DisplayName("메뉴 개시")
    @Nested
    class displayTest {

        @DisplayName("개시 성공")
        @Test
        void displayed() {
            // given
            final Menu menu = MenuFixture.create(
                    "후라이드 치킨",
                    BigDecimal.valueOf(15_000),
                    false,
                    MenuGroupFixture.createDefault(),
                    List.of(MenuProductFixture.createDefault())
            );
            given(menuRepository.findById(any())).willReturn(Optional.of(menu));

            // when
            Menu result = menuService.display(menu.getId());

            // then
            assertThat(result.isDisplayed()).isTrue();
        }

        @DisplayName("메뉴의 가격이 (구성품목의 가격 x 개수)의 총합보다 클 수 없다.")
        @Test
        void over_than_product_price() {
            // given
            final Menu menu = MenuFixture.create(
                    "후라이드 치킨",
                    BigDecimal.valueOf(30_000),
                    false,
                    MenuGroupFixture.createDefault(),
                    List.of(MenuProductFixture.create(
                            ProductFixture.create("후라이드 치킨", BigDecimal.valueOf(10_000)),
                            1L
                    ))
            );
            given(menuRepository.findById(any())).willReturn(Optional.of(menu));

            // then
            assertThatThrownBy(() -> menuService.display(menu.getId())).isInstanceOf(IllegalStateException.class);
        }
    }

    @DisplayName("메뉴를 숨긴다.")
    @Test
    void hideMenu() {
        // given
        final Menu menu = MenuFixture.create(
                "후라이드 치킨",
                BigDecimal.valueOf(15_000),
                true,
                MenuGroupFixture.createDefault(),
                List.of(MenuProductFixture.createDefault())
        );
        given(menuRepository.findById(any())).willReturn(Optional.of(menu));

        // when
        Menu result = menuService.hide(menu.getId());

        // then
        assertThat(result.isDisplayed()).isFalse();
    }
}
