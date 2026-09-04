package com.omisys.user.application.service;

import com.omisys.user.domain.model.Address;
import com.omisys.user.domain.model.User;
import com.omisys.user.domain.repository.AddressRepository;
import com.omisys.user.domain.repository.UserRepository;
import com.omisys.user.exception.UserErrorCode;
import com.omisys.user.exception.UserException;
import com.omisys.user.presentation.request.AddressRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;

    @InjectMocks private AddressService addressService;

    @Test
    @DisplayName("createAddress 성공: user 존재 → addressRepository.save 호출")
    void createAddress_success() {
        // given
        long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        AddressRequest.Create request = mock(AddressRequest.Create.class);

        // when
        addressService.createAddress(userId, request);

        // then
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    @DisplayName("createAddress persists structured address fields")
    void createAddress_structuredFields_persisted() {
        long userId = 1L;
        User user = mock(User.class);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        AddressRequest.Create request = createRequest(null, "서울 강동구 양재대로 222", "서울 강동구 성내동", "101동 1001호");

        addressService.createAddress(userId, request);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        Address savedAddress = captor.getValue();
        assertThat(savedAddress.getRoadAddress()).isEqualTo("서울 강동구 양재대로 222");
        assertThat(savedAddress.getJibunAddress()).isEqualTo("서울 강동구 성내동");
        assertThat(savedAddress.getDetailAddress()).isEqualTo("101동 1001호");
        assertThat(savedAddress.getSido()).isEqualTo("서울특별시");
        assertThat(savedAddress.getSigungu()).isEqualTo("강동구");
    }

    @Test
    @DisplayName("createAddress composes address from road address and detail when explicit address is absent")
    void createAddress_composesAddressWhenAbsent() {
        long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        addressService.createAddress(userId, createRequest(null, "서울 강동구 양재대로 222", null, "101동 1001호"));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getAddress()).isEqualTo("서울 강동구 양재대로 222 101동 1001호");
    }

    @Test
    @DisplayName("createAddress keeps explicit address for backwards compatibility")
    void createAddress_keepsExplicitAddress() {
        long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        addressService.createAddress(userId, createRequest("legacy address", "서울 강동구 양재대로 222", null, "101동 1001호"));

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getAddress()).isEqualTo("legacy address");
    }

    @Test
    @DisplayName("update applies structured address fields")
    void updateAddress_structuredFields_updated() {
        Address address = Address.create(mock(User.class), createRequest("old address", null, null, null));
        AddressRequest.Update update = new AddressRequest.Update(
                "office", "홍길동", "01012345678", "54321", null, false,
                "서울 송파구 올림픽로 300", "서울 송파구 신천동", "201동 2002호", "서울특별시", "송파구"
        );

        address.update(update);

        assertThat(address.getAddress()).isEqualTo("서울 송파구 올림픽로 300 201동 2002호");
        assertThat(address.getRoadAddress()).isEqualTo("서울 송파구 올림픽로 300");
        assertThat(address.getJibunAddress()).isEqualTo("서울 송파구 신천동");
        assertThat(address.getDetailAddress()).isEqualTo("201동 2002호");
        assertThat(address.getSido()).isEqualTo("서울특별시");
        assertThat(address.getSigungu()).isEqualTo("송파구");
    }

    @Test
    @DisplayName("createAddress 실패: user 없으면 USER_NOT_FOUND 예외")
    void createAddress_fail_user_not_found() {
        // given
        long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AddressRequest.Create request = mock(AddressRequest.Create.class);

        // when & then
        assertThatThrownBy(() -> addressService.createAddress(userId, request))
                .isInstanceOf(UserException.class)
                .satisfies(ex -> {
                    UserException ue = (UserException) ex;
                    assertThat(ue.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                });

        verify(addressRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateAddress 성공: user 존재 + address 존재 → address.update 호출")
    void updateAddress_success() {
        // given
        long userId = 1L;
        long addressId = 10L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        Address address = mock(Address.class);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        AddressRequest.Update request = mock(AddressRequest.Update.class);

        // when
        addressService.updateAddress(userId, addressId, request);

        // then
        verify(address).update(request);
    }

    @Test
    @DisplayName("deleteAddress 성공: user 존재 + address 존재 → addressRepository.delete 호출")
    void deleteAddress_success() {
        // given
        long userId = 1L;
        long addressId = 10L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(mock(User.class)));

        Address address = mock(Address.class);
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));

        // when
        addressService.deleteAddress(userId, addressId);

        // then
        verify(addressRepository).delete(address);
    }

    private AddressRequest.Create createRequest(String address, String roadAddress, String jibunAddress, String detailAddress) {
        return new AddressRequest.Create(
                "home", "홍길동", "01012345678", "12345", address, true,
                roadAddress, jibunAddress, detailAddress, "서울특별시", "강동구"
        );
    }
}
